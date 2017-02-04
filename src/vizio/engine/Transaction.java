package vizio.engine;

import static java.lang.Math.max;
import static vizio.engine.Convert.area2bin;
import static vizio.engine.Convert.bin2area;
import static vizio.engine.Convert.bin2poll;
import static vizio.engine.Convert.bin2product;
import static vizio.engine.Convert.bin2site;
import static vizio.engine.Convert.bin2task;
import static vizio.engine.Convert.bin2user;
import static vizio.engine.Convert.bin2version;
import static vizio.engine.Convert.poll2bin;
import static vizio.engine.Convert.product2bin;
import static vizio.engine.Convert.site2bin;
import static vizio.engine.Convert.task2bin;
import static vizio.engine.Convert.user2bin;
import static vizio.engine.Convert.version2bin;
import static vizio.model.ID.areaId;
import static vizio.model.ID.pollId;
import static vizio.model.ID.productId;
import static vizio.model.ID.siteId;
import static vizio.model.ID.userId;
import static vizio.model.ID.versionId;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import vizio.db.DB;
import vizio.db.DB.TxW;
import vizio.engine.Change.Operation;
import vizio.engine.Change.Tx;
import vizio.engine.Event.Transition;
import vizio.model.Area;
import vizio.model.Entity;
import vizio.model.ID;
import vizio.model.IDN;
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
public final class Transaction implements Tx, Limits, AutoCloseable {

	/**
	 * Applies the changes to the DB.
	 * 
	 * @return a list of changed entities each given as a pair: before and after the change 
	 * @throws ConcurrentModification when trying to change an entity already changed by an ongoing transaction (in another thread)
	 */
	public static Changelog run(Change set, DB db, Clock clock, Limits limits) throws ConcurrentModification {
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
	private final HashMap<ID, Entity<?>> loaded = new HashMap<>();
	private final HashMap<ID, User> loadedUsers = new HashMap<>();
	private final Set<Limit> allocated = new HashSet<>();
	
	private final Clock clock;
	private final Limits limits;
	private final DB db;
	private final DB.TxR txr;

	private ID originator;
	
	private Transaction(Clock clock, Limits lc, DB limits) {
		super();
		this.clock = clock;
		this.limits = lc;
		this.db = limits;
		this.txr = limits.read();
	}
	
	@Override
	public void close() {
		txr.close();
	}

	@SuppressWarnings("unchecked")
	private <T extends Entity<T>> T load(ID id, Convert<Tx, T> reader) {
		Object res = possiblyChanged(id);
		if (res != null)
			return (T) res;		
		T e = reader.convert(this, txr.get(id));
		loaded.put(id, e);
		return e;
	}
	
	private Object possiblyChanged(ID id) {
		Object res = changed.get(id);
		return res != null ? res : loaded.get(id);
	}
	
	@Override
	public void put(Operation op, Entity<?> e) {
		final ID id = e.uniqueID();
		if (e != possiblyChanged(id)) { // only do real updates
			putFields(op, e); // "auto"-update fields with updates
			changed.put(id, e);
			changeTypes.computeIfAbsent(id, (id_) -> new ArrayList<>()).add(op);
			for (User user : loadedUsers.values()) {
				if (user.version > user.initalVersion) {
					final ID userID = user.uniqueID();
					if (originator == null) {
						originator = userID;
					} else if (!userID.equalTo(originator)){
						throw new IllegalArgumentException("All changes within one transation must originate from same user.");
					}
					changed.put(userID, user);
					loadedUsers.remove(user);	
				}
			}
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
		} else if (e instanceof Product && e.version == 1) {
			Product p = (Product)e;
			put(op, p.origin);
			put(op, p.somewhere);
			put(op, p.somewhen);
		}
	}
	
	@Override
	public User user(Name user) {
		ID id = userId(user);
		User res = load(id, bin2user);
		loadedUsers.put(id, res);
		return res;
	}

	@Override
	public Site site(Name user, Name site) {
		return load(siteId(user, site), bin2site);
	}

	@Override
	public Poll poll(Name product, Name area, IDN serial) {
		return load(pollId(product, area, serial), bin2poll);
	}

	@Override
	public Product product(Name product) {
		return load(productId(product), bin2product);
	}

	@Override
	public Area area(Name product, Name area) {
		return load(areaId(product, area), bin2area);
	}

	@Override
	public Version version(Name product, Name version) {
		return load(versionId(product, version), bin2version);
	}

	@Override
	public Task task(Name product, IDN id) {
		return load(ID.taskId(product, id), bin2task);
	}
	
	
	private Changelog commit() {
		txr.close(); // no more reading
		if (changed.isEmpty())
			return Changelog.EMPTY;
		ByteBuffer buf = ByteBuffer.allocateDirect(1024);
		try (TxW tx = db.write()) {
			Changelog log = writeChanges(buf, tx);
			writeEventLog(tx, log, buf);
			tx.commit();
			return log;
		}
	}
	
	private Changelog writeChanges(ByteBuffer buf, TxW tx) {
		Changelog.Entry<?>[] res = new Changelog.Entry[changed.size()];
		int i = 0;
		for (Entry<ID,Entity<?>> e : changed.entrySet()) {
			ID id = e.getKey();
			Entity<?> val = e.getValue();
			res[i++] = new Changelog.Entry(loaded.get(id), changeTypes.get(id).toArray(new Change.Operation[0]), val);
			switch (id.type) {
			case Area:    write(tx, id, (Area)val, area2bin, buf); break;
			case Poll:    write(tx, id, (Poll)val, poll2bin, buf); break;
			case Site:    write(tx, id, (Site)val, site2bin, buf); break;
			case Task:    write(tx, id, (Task)val, task2bin, buf); break;
			case User:    write(tx, id, (User)val, user2bin, buf); break;
			case Product: write(tx, id, (Product)val, product2bin, buf); break;
			case Version: write(tx, id, (Version)val, version2bin, buf); break;
			default: throw new UnsupportedOperationException("Cannot store entities of type: "+id);
			}
		}
		return new Changelog(clock.time(), res);
	}

	private void writeEventLog(TxW tx, Changelog log, ByteBuffer buf) {
		final Transition[] transitions = new Transition[log.length()];
		final long timestamp = log.timestamp;
		int i = 0;
		for (Changelog.Entry<?> e : log) {
			ID id = e.after.uniqueID();
			transitions[i++] = new Transition(id, e.transitions);
			ID hid = ID.historyId(id);
			ByteBuffer history = tx.get(hid);
			if (history == null) {
				buf.putLong(timestamp).putLong(timestamp).flip();
			} else {
				if (history.remaining() >= 32) {
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
	
	private static <T> void write(TxW tx, ID id, T e, Convert<T, ByteBuffer> writer, ByteBuffer buf) {
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
	public boolean stress(Limit limit, Clock clock) throws ConcurrentModification {
		if (!limit.isSpecific()) {
			return limits.stress(limit, clock);
		}
		return alloc(limit, clock);
	}

	@Override
	public boolean alloc(Limit limit, Clock clock) throws ConcurrentModification {
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
