package se.jbee.task.engine;

import static java.lang.Math.max;
import static se.jbee.task.engine.Bincoder.area2bin;
import static se.jbee.task.engine.Bincoder.output2bin;
import static se.jbee.task.engine.Bincoder.page2bin;
import static se.jbee.task.engine.Bincoder.poll2bin;
import static se.jbee.task.engine.Bincoder.task2bin;
import static se.jbee.task.engine.Bincoder.user2bin;
import static se.jbee.task.engine.Bincoder.version2bin;
import static se.jbee.task.engine.ChangeLog.changes;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import se.jbee.task.db.DB;
import se.jbee.task.db.DB.Write;
import se.jbee.task.engine.Change.Operation;
import se.jbee.task.engine.Change.Tx;
import se.jbee.task.engine.Event.Transition;
import se.jbee.task.engine.Limits.ConcurrentUsage;
import se.jbee.task.model.Area;
import se.jbee.task.model.Entity;
import se.jbee.task.model.ID;
import se.jbee.task.model.Name;
import se.jbee.task.model.Output;
import se.jbee.task.model.Page;
import se.jbee.task.model.Poll;
import se.jbee.task.model.Task;
import se.jbee.task.model.Transitory;
import se.jbee.task.model.User;
import se.jbee.task.model.Version;

/**
 * A {@link Transaction} keeps track of a change applied as a whole or not at
 * all.
 *
 * It is "smart" in the sense that it will keep track of entities already
 * changed so that when they are loaded more then once the changed entity is
 * returned. It will also keep track of updated user entities without the need
 * to {@link #put(Entity)} them explicitly.
 */
public final class Transaction extends DAO implements Tx {

	/**
	 * We really just need a single buffer for writing as only one TX can write at a time.
	 */
	private static final ByteBuffer WRITE_BUF = ByteBuffer.allocateDirect(8192);

	public static ChangeLog run(Change set, DB db, Server server) throws ConcurrentUsage {
		return run(set, db, server, null);
	}

	/**
	 * Applies the changes to the DB.
	 *
	 * @return a list of changed entities each given as a pair: before and after the change
	 * @throws ConcurrentUsage when trying to change an entity already changed by an ongoing transaction (in another thread)
	 */
	public static ChangeLog run(Change set, DB db, Server server, Consumer<ChangeLog> listener) throws ConcurrentUsage {
		final long now = max(lastTick.incrementAndGet(), server.clock.time());
		final Clock fixedNow = () -> now;
		Limits limits = new StressBlockSpecificLimits(server.limits);
		try (Transaction tx = new Transaction(fixedNow, db, listener)) {
			try {
				set.apply(new Tracker(server.with(fixedNow).with(limits)), tx);
				return tx.commit();
			} finally {
				limits.unblock(null);
			}
		}
	}

	/**
	 * With this we do our little tick so that we can guarantee each transaction has a unique {@link Clock#time()}.
	 * Each time transaction {@link #run(Change, DB, se.jbee.task.engine.Limits.Assurances)} is called the constant time
	 * used during the whole transaction is at least one (ms) larger than the last timestamp used.
	 */
	private static final AtomicLong lastTick = new AtomicLong(Long.MIN_VALUE);

	private final LinkedHashMap<ID, Entity<?>> changed = new LinkedHashMap<>();
	private final HashMap<ID, ArrayList<Change.Operation>> changeTypes = new HashMap<>();
	private final HashMap<ID, User> loadedUsers = new HashMap<>();

	private final Clock clock;
	private final DB db;
	private final Consumer<ChangeLog> listener;

	private ID actor;

	private Transaction(Clock clock, DB db, Consumer<ChangeLog> listener) {
		super(db.read());
		this.clock = clock;
		this.db = db;
		this.listener = listener;
	}

	@Override
	protected Object transactionObject(ID id) {
		Object res = changed.get(id);
		return res != null ? res : loaded.get(id);
	}

	@Override
	public void put(Operation op, Entity<?> e) {
		if (e.isCurrupted())
			throw new IllegalStateException("Currupted entities cannot be stored!");
		final ID id = e.uniqueID();
		if (e != transactionObject(id)) { // only do real updates
			putFields(op, e); // "auto"-update fields with updates
			changed.put(id, e);
			ArrayList<Operation> ops = changeTypes.computeIfAbsent(id, (id_) -> new ArrayList<>());
			ops.add(op);
			putUser(e, id);
		}
	}

	/**
	 * Since users are normally not explicitly "put" we do keep track of loaded users.
	 * They might have changed in place (except when they are stored explicitly).
	 */
	private void putUser(Entity<?> e, final ID id) {
		if (loadedUsers.isEmpty()) {
			if (e instanceof User) {
				trackActor(id); // registered user
			}
		} else {
			// make sure only the acting user can be stored
			for (User user : loadedUsers.values()) {
				if (user.isModified()) {
					final ID userID = user.uniqueID();
					trackActor(userID);
					changed.put(userID, user);
				}
			}
		}
		if (actor == null && e instanceof User) {
			trackActor(e.uniqueID());
		}
	}

	private void trackActor(final ID userID) {
		if (actor == null) {
			actor = userID;
		} else if (!userID.equalTo(actor)){
			throw new IllegalArgumentException("All changes within one transation must originate from same user.");
		}
	}

	private void putFields(Operation op, Entity<?> e) {
		if (e instanceof Poll) {
			put(op, ((Poll) e).area);
		} else if (e instanceof Task) {
			Task t = (Task) e;
			put(op, t.output);
			put(op, t.area);
			put(op, t.base);
		} else if (e instanceof Output && e.initalVersion == 1) {
			Output p = (Output)e;
			put(op, p.origin);
			put(op, p.somewhere);
			put(op, p.somewhen);
		}
	}

	@Override
	public User user(Name user) {
		User res = super.user(user);
		loadedUsers.put(res.uniqueID(), res);
		return res;
	}

	private ChangeLog commit() {
		super.close(); // no more reading
		if (changed.isEmpty())
			return ChangeLog.EMPTY; // empty changesets have serial 0 and can be discarded/ignored
		if (actor == null)
			throw new IllegalStateException("Acting user has to be updated during a transaction!");
		try (Write tx = db.write()) {
			WRITE_BUF.clear();
			ChangeLog.Entry<?>[] log = writeEntities(tx, WRITE_BUF);
			long timestamp = clock.time();
			writeHistoryAndEvent(tx, log, timestamp, WRITE_BUF);
			tx.commit();
			// serial is fetched within the TX write() but after commit() so we know this is successful
			// also only one thread can enter the write block what causes publishing to be in order
			return publish(changes(timestamp, log));
		}
	}

	private ChangeLog publish(ChangeLog changes) {
		if (listener != null)
			try { listener.accept(changes); } catch (RuntimeException e) { /* just ignore this */ }
		return changes;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private ChangeLog.Entry<?>[] writeEntities(Write tx, ByteBuffer buf) {
		ChangeLog.Entry<?>[] res = new ChangeLog.Entry[changed.size()];
		int i = 0;
		for (Entry<ID,Entity<?>> e : changed.entrySet()) {
			ID id = e.getKey();
			Entity<?> val = e.getValue();
			ArrayList<Operation> transitions = changeTypes.get(id);
			Operation[] ops = transitions == null ? new Operation[0] : transitions.toArray(new Operation[0]);
			res[i++] = new ChangeLog.Entry(loaded.get(id), ops, val);
			switch (id.type) {
			case poll:    write(tx, id, (Poll)val, poll2bin, buf); break;
			case Area:    write(tx, id, (Area)val, area2bin, buf); break;
			case Page:    write(tx, id, (Page)val, page2bin, buf); break;
			case Task:    write(tx, id, (Task)val, task2bin, buf); break;
			case User:    write(tx, id, (User)val, user2bin, buf); break;
			case Output: write(tx, id, (Output)val, output2bin, buf); break;
			case Version: write(tx, id, (Version)val, version2bin, buf); break;
			default: throw new UnsupportedOperationException("Cannot store entities of type: "+id);
			}
		}
		return res;
	}

	private void writeHistoryAndEvent(Write tx, ChangeLog.Entry<?>[] changes, long timestamp, ByteBuffer buf) {
		final Transition[] transitions = new Transition[changes.length];
		int i = 0;
		for (ChangeLog.Entry<?> e : changes) {
			ID id = e.after.uniqueID();
			transitions[i++] = new Transition(id, e.transitions);
			ID hid = ID.historyId(id);
			ByteBuffer history = tx.get(hid);
			if (history == null) {
				buf.putLong(timestamp).putLong(timestamp).flip();
			} else {
				if (history.remaining() >= 64) {
					buf.putLong(history.getLong());
					history.getLong(); // throw away oldest
					buf.put(history).putLong(timestamp).flip();
				} else {
					buf.put(history).putLong(timestamp).flip();
				}
			}
			tx.put(hid, buf);
			buf.clear();
		}
		Event e = new Event(timestamp, actor, transitions);
		write(tx, e.uniqueID() , e, Bincoder.event2bin, buf);
	}

	private static <T> void write(Write tx, ID id, T e, Bincoder<T, ByteBuffer> encoder, ByteBuffer buf) {
		if (e instanceof Transitory && ((Transitory) e).obsolete()) {
			tx.delete(id);
		} else {
			encoder.convert(e, buf).flip();
			tx.put(id, buf);
			buf.clear();
		}
	}

}
