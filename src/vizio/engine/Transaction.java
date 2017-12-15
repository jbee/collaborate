package vizio.engine;

import static java.lang.Math.max;
import static vizio.engine.Convert.area2bin;
import static vizio.engine.Convert.poll2bin;
import static vizio.engine.Convert.product2bin;
import static vizio.engine.Convert.site2bin;
import static vizio.engine.Convert.task2bin;
import static vizio.engine.Convert.user2bin;
import static vizio.engine.Convert.version2bin;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import vizio.db.DB;
import vizio.db.DB.TxRW;
import vizio.engine.Change.Operation;
import vizio.engine.Change.Tx;
import vizio.engine.Event.Transition;
import vizio.model.Area;
import vizio.model.Entity;
import vizio.model.ID;
import vizio.model.Name;
import vizio.model.Poll;
import vizio.model.Product;
import vizio.model.Site;
import vizio.model.Task;
import vizio.model.User;
import vizio.model.Version;

/**
 * A {@link Transaction} keeps track of a change applied as a whole or not at
 * all.
 * 
 * It is "smart" in the sense that it will keep track of entities already
 * changed so that when they are loaded more then once the changed entity is
 * returned. It will also keep track of updated user entities without the need
 * to {@link #put(Entity)} them explicitly.
 */
public final class Transaction extends DAO implements Tx, Limits {

	/**
	 * Applies the changes to the DB.
	 * 
	 * @return a list of changed entities each given as a pair: before and after the change 
	 * @throws ConcurrentUsage when trying to change an entity already changed by an ongoing transaction (in another thread)
	 */
	public static Changes run(Change set, DB db, Clock clock, Limits limits) throws ConcurrentUsage {
		final long now = max(lastTick.incrementAndGet(), clock.time());
		final Clock fixedNow = () -> now;
		try (Transaction tx = new Transaction(fixedNow, limits, db)) {
			try {
				set.apply(new Tracker(fixedNow, tx), tx);
				return tx.commit();
			} finally {
				tx.freeAllocatedLimits();
			}
		}
	}
	
	/**
	 * With this we do our little tick so that we can guarantee each transaction has a unique {@link Clock#time()}.
	 * Each time transaction {@link #run(Change, DB, vizio.engine.Limits.Assurances)} is called the constant time
	 * used during the whole transaction is at least one (ms) larger than the last timestamp used.  
	 */
	private static final AtomicLong lastTick = new AtomicLong(Long.MIN_VALUE);
	
	private final LinkedHashMap<ID, Entity<?>> changed = new LinkedHashMap<>();
	private final HashMap<ID, ArrayList<Change.Operation>> changeTypes = new HashMap<>();
	private final HashMap<ID, User> loadedUsers = new HashMap<>();
	private final Set<Limit> allocated = new HashSet<>();
	
	private final Clock clock;
	private final Limits limits;
	private final DB db;

	private ID originator;
	
	private Transaction(Clock clock, Limits limits, DB db) {
		super(db.read());
		this.clock = clock;
		this.limits = limits;
		this.db = db;
	}
	
	@Override
	protected Object transactionObject(ID id) {
		Object res = changed.get(id);
		return res != null ? res : loaded.get(id);
	}
	
	@Override
	public void put(Operation op, Entity<?> e) {
		final ID id = e.uniqueID();
		if (e != transactionObject(id)) { // only do real updates
			putFields(op, e); // "auto"-update fields with updates
			changed.put(id, e);
			changeTypes.computeIfAbsent(id, (id_) -> new ArrayList<>()).add(op);
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
				trackOriginator(id); // registered user
			}
		} else {
			for (User user : loadedUsers.values()) {
				if (user.isModified()) {
					final ID userID = user.uniqueID();
					trackOriginator(userID);
					changed.put(userID, user);
					loadedUsers.remove(user);	
				}
			}
		}
	}

	private void trackOriginator(final ID userID) {
		if (originator == null) {
			originator = userID;
		} else if (!userID.equalTo(originator)){
			throw new IllegalArgumentException("All changes within one transation must originate from same user.");
		}
	}

	private void putFields(Operation op, Entity<?> e) {
		if (e instanceof Poll) {
			put(op, ((Poll) e).area);
		} else if (e instanceof Task) {
			Task t = (Task) e;
			put(op, t.product);
			put(op, t.area);
			put(op, t.base);
		} else if (e instanceof Product && e.version() == 1) {
			Product p = (Product)e;
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

	private Changes commit() {
		super.close(); // no more reading
		if (originator == null)
			throw new IllegalStateException("Acting user has to be updated during a transaction!");
		if (changed.isEmpty())
			return Changes.EMPTY;
		ByteBuffer buf = ByteBuffer.allocateDirect(4096);
		try (TxRW tx = db.write()) {
			Changes log = writeTx(tx, buf);
			writeHistoryAndEvent(tx, log, buf);
			tx.commit();
			return log;
		}
	}
	
	private Changes writeTx(TxRW tx, ByteBuffer buf) {
		Changes.Entry<?>[] res = new Changes.Entry[changed.size()];
		int i = 0;
		for (Entry<ID,Entity<?>> e : changed.entrySet()) {
			ID id = e.getKey();
			Entity<?> val = e.getValue();
			res[i++] = new Changes.Entry(loaded.get(id), changeTypes.get(id).toArray(new Change.Operation[0]), val);
			switch (id.type) {
			case poll:    write(tx, id, (Poll)val, poll2bin, buf); break;
			case Area:    write(tx, id, (Area)val, area2bin, buf); break;
			case Site:    write(tx, id, (Site)val, site2bin, buf); break;
			case Task:    write(tx, id, (Task)val, task2bin, buf); break;
			case User:    write(tx, id, (User)val, user2bin, buf); break;
			case Product: write(tx, id, (Product)val, product2bin, buf); break;
			case Version: write(tx, id, (Version)val, version2bin, buf); break;
			default: throw new UnsupportedOperationException("Cannot store entities of type: "+id);
			}
		}
		return new Changes(clock.time(), res);
	}

	private void writeHistoryAndEvent(TxRW tx, Changes changes, ByteBuffer buf) {
		final Transition[] transitions = new Transition[changes.length()];
		final long timestamp = changes.timestamp;
		int i = 0;
		for (Changes.Entry<?> e : changes) {
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
		Event e = new Event(timestamp, originator, transitions);
		write(tx, e.uniqueID() , e, Convert.event2bin, buf);
	}
	
	private static <T> void write(TxRW tx, ID id, T e, Convert<T, ByteBuffer> writer, ByteBuffer buf) {
		writer.convert(e, buf).flip();
		tx.put(id, buf);
		buf.clear();
	}
	
	private void freeAllocatedLimits() {
		for (Limit l : allocated) {
			limits.free(l);
		}
		allocated.clear();
	}
	
	@Override
	public boolean stress(Limit limit, Clock clock) throws ConcurrentUsage {
		if (!limit.isSpecific()) {
			return limits.stress(limit, clock);
		}
		return alloc(limit, clock);
	}

	@Override
	public boolean alloc(Limit limit, Clock clock) throws ConcurrentUsage {
		if (allocated.contains(limit))
			return true;
		if (limits.alloc(limit, clock)) {
			allocated.add(limit);
			return true;
		}
		return false;
	}
	
	@Override
	public void free(Limit l) {
		throw new UnsupportedOperationException("`free` should not be called directly on the TX!");
	}
}
