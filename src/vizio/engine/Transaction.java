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
import java.util.Map.Entry;
import java.util.Set;

import vizio.engine.DB.Tx;
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

public class Transaction implements Tx, Limits {

	public static void run(Change set, LimitControl lc, DB db) {
		Transaction tx = new Transaction(lc, db);
		try {
			set.apply(new Tracker(lc.clock, tx), tx);
			tx.commit();
		} catch (Exception e) {
			tx.rollback();
		}
	}
	
	private final HashMap<ID, Entity<?>> after = new HashMap<>();
	private final HashMap<ID, Entity<?>> before = new HashMap<>();
	private final Set<Limit> allocated = new HashSet<>();
	
	private final LimitControl lc;
	private final DB db;
	
	public Transaction(LimitControl lc, DB db) {
		super();
		this.lc = lc;
		this.db = db;
	}

	@SuppressWarnings("unchecked")
	private <T extends Entity<T>> T load(ID id, BinaryConversion<Tx, T> reader) {
		Object res = reused(id);
		if (res != null)
			return (T) res;		
		T e = reader.convert(this, db.get(id));
		before.put(id, e);
		return e;
	}
	
	private Object reused(ID id) {
		Object res = after.get(id);
		return res != null ? res : before.get(id);
	}
	
	@Override
	public void put(Entity<?> e) {
		ID id = e.uniqueID();
		if (e != reused(id)) { // only do real updates
			// but "auto"-update fields with updates
			if (e instanceof Poll) {
				put(((Poll) e).area);
			} else if (e instanceof Task) {
				Task t = (Task) e;
				put(t.product);
				put(t.area);
				put(t.base);
			}
			after.put(id, e);
		}
	}
	
	@Override
	public User user(Name user) {
		return load(userId(user), bin2user);
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
	
	public void commit() {
		try {
			ByteBuffer buf = ByteBuffer.allocateDirect(1024);
			for (Entry<ID,Entity<?>> e : after.entrySet()) {
				ID id = e.getKey();
				Entity<?> val = e.getValue();
				switch (id.type) {
				case Area: store(id, (Area)val, area2bin, buf); break;
				case Poll: store(id, (Poll)val, poll2bin, buf); break;
				case Site: store(id, (Site)val, site2bin, buf); break;
				case Task: store(id, (Task)val, task2bin, buf); break;
				case User: store(id, (User)val, user2bin, buf); break;
				case Product: store(id, (Product)val, product2bin, buf); break;
				case Version: store(id, (Version)val, version2bin, buf); break;
				default: throw new UnsupportedOperationException("Cannot store entities of type: "+id);
				}
			}
		} finally {
			freeAllocatedLimits();
		}
	}
	
	private <T> void store(ID id, T e, BinaryConversion<T, ByteBuffer> writer, ByteBuffer buf) {
		writer.convert(e, buf).flip();
		db.put(id, writer.convert(e, buf));
	}
	
	public void rollback() {
		freeAllocatedLimits();
	}

	private void freeAllocatedLimits() {
		for (Limit l : allocated) {
			lc.free(l);
		}
		allocated.clear();
	}
	
	@Override
	public boolean stress(Limit limit) throws IllegalStateException {
		if (!limit.isSpecific()) {
			return lc.stress(limit);
		}
		if (lc.alloc(limit)) {
			allocated.add(limit);
			return true;
		}
		return false;
	}

}
