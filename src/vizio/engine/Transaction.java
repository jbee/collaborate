package vizio.engine;

import static vizio.engine.BinaryConversion.area2bin;
import static vizio.engine.BinaryConversion.bin2area;
import static vizio.engine.BinaryConversion.bin2poll;
import static vizio.engine.BinaryConversion.bin2product;
import static vizio.engine.BinaryConversion.bin2site;
import static vizio.engine.BinaryConversion.bin2task;
import static vizio.engine.BinaryConversion.bin2user;
import static vizio.engine.BinaryConversion.bin2version;
import static vizio.engine.BinaryConversion.poll2bin;
import static vizio.engine.BinaryConversion.product2bin;
import static vizio.engine.BinaryConversion.site2bin;
import static vizio.engine.BinaryConversion.task2bin;
import static vizio.engine.BinaryConversion.user2bin;
import static vizio.engine.BinaryConversion.version2bin;
import static vizio.model.ID.areaId;
import static vizio.model.ID.pollId;
import static vizio.model.ID.productId;
import static vizio.model.ID.siteId;
import static vizio.model.ID.userId;
import static vizio.model.ID.versionId;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;

import vizio.db.DB;
import vizio.db.DB.TxW;
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

public class Transaction implements Tx, Limits, AutoCloseable {

	public static boolean run(Change set, LimitControl lc, DB db) throws ConcurrentModification {
		try (Transaction tx = new Transaction(lc, db)) {
			try {
				set.apply(new Tracker(lc.clock, tx), tx);
				return tx.commit();
			} finally {
				tx.freeAllocatedLimits();
			}
		}
	}
	
	private final LinkedHashMap<ID, Entity<?>> changed = new LinkedHashMap<>();
	private final HashMap<ID, Entity<?>> loaded = new HashMap<>();
	private final HashMap<User, Integer> loadedUserVersions = new HashMap<>();
	private final Set<Limit> allocated = new HashSet<>();
	
	private final LimitControl lc;
	private final DB db;
	private final DB.TxR txr;
	
	public Transaction(LimitControl lc, DB db) {
		super();
		this.lc = lc;
		this.db = db;
		this.txr = db.read();
	}
	
	@Override
	public void close() {
		txr.close();
	}

	@SuppressWarnings("unchecked")
	private <T extends Entity<T>> T load(ID id, BinaryConversion<Tx, T> reader) {
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
	public void put(Entity<?> e) {
		ID id = e.uniqueID();
		if (e != possiblyChanged(id)) { // only do real updates
			// but "auto"-update fields with updates
			if (e instanceof Poll) {
				put(((Poll) e).area);
			} else if (e instanceof Task) {
				Task t = (Task) e;
				put(t.product);
				put(t.area);
				put(t.base);
			}
			changed.put(id, e);
		}
	}
	
	@Override
	public User user(Name user) {
		User res = load(userId(user), bin2user);
		loadedUserVersions.put(res, res.version);
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
	
	private boolean commit() {
		txr.close(); // no more writing
		if (changed.isEmpty())
			return false;
		// add touched users to changeset
		for (Entry<User, Integer> u : loadedUserVersions.entrySet()) {
			if (u.getKey().version > u.getValue().intValue()) {
				changed.put(u.getKey().uniqueID(), u.getKey());
			}
		}
		loadedUserVersions.clear();
		ByteBuffer buf = ByteBuffer.allocateDirect(1024);
		try (TxW tx = db.write()) {
			for (Entry<ID,Entity<?>> e : changed.entrySet()) {
				ID id = e.getKey();
				Entity<?> val = e.getValue();
				switch (id.type) {
				case Area: store(tx, id, (Area)val, area2bin, buf); break;
				case Poll: store(tx, id, (Poll)val, poll2bin, buf); break;
				case Site: store(tx, id, (Site)val, site2bin, buf); break;
				case Task: store(tx, id, (Task)val, task2bin, buf); break;
				case User: store(tx, id, (User)val, user2bin, buf); break;
				case Product: store(tx, id, (Product)val, product2bin, buf); break;
				case Version: store(tx, id, (Version)val, version2bin, buf); break;
				default: throw new UnsupportedOperationException("Cannot store entities of type: "+id);
				}
				buf.clear();
			}
			tx.commit();
			return true;
		}
	}
	
	private static <T> void store(TxW tx, ID id, T e, BinaryConversion<T, ByteBuffer> writer, ByteBuffer buf) {
		writer.convert(e, buf).flip();
		tx.put(id, buf);
		buf.clear();
	}
	
	private void freeAllocatedLimits() {
		for (Limit l : allocated) {
			lc.free(l);
		}
		allocated.clear();
	}
	
	@Override
	public boolean stress(Limit limit) throws ConcurrentModification {
		if (!limit.isSpecific()) {
			return lc.stress(limit);
		}
		if (allocated.contains(limit))
			return true;
		if (lc.alloc(limit)) {
			allocated.add(limit);
			return true;
		}
		return false;
	}

}
