package se.jbee.track.engine;

import static se.jbee.track.engine.Bincoder.bin2area;
import static se.jbee.track.engine.Bincoder.bin2event;
import static se.jbee.track.engine.Bincoder.bin2poll;
import static se.jbee.track.engine.Bincoder.bin2product;
import static se.jbee.track.engine.Bincoder.bin2site;
import static se.jbee.track.engine.Bincoder.bin2task;
import static se.jbee.track.engine.Bincoder.bin2user;
import static se.jbee.track.engine.Bincoder.bin2version;
import static se.jbee.track.model.ID.areaId;
import static se.jbee.track.model.ID.pollId;
import static se.jbee.track.model.ID.productId;
import static se.jbee.track.model.ID.siteId;
import static se.jbee.track.model.ID.userId;
import static se.jbee.track.model.ID.versionId;
import static se.jbee.track.model.Name.as;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;

import se.jbee.track.db.DB;
import se.jbee.track.db.DB.TxR;
import se.jbee.track.model.Area;
import se.jbee.track.model.Entity;
import se.jbee.track.model.ID;
import se.jbee.track.model.IDN;
import se.jbee.track.model.Name;
import se.jbee.track.model.Poll;
import se.jbee.track.model.Product;
import se.jbee.track.model.Site;
import se.jbee.track.model.Task;
import se.jbee.track.model.User;
import se.jbee.track.model.Version;

/**
 * Reading {@link Entity}s from a {@link DB} via a {@link Repository} abstraction.  
 */
public class DAO implements Repository {

	protected final HashMap<ID, Entity<?>> loaded = new HashMap<>();
	
	private final DB.TxR txr;
	
	public DAO(TxR txr) {
		super();
		this.txr = txr;
	}

	@Override
	public final void close() {
		txr.close();
	}
	
	@SuppressWarnings("unchecked")
	private final <T extends Entity<T>> T load(ID id, Bincoder<Repository, T> reader) {
		Object res = transactionObject(id);
		if (res != null)
			return (T) res;		
		T e = loadObject(id, reader);
		loaded.put(id, e);
		return e;
	}

	private <T> T loadObject(ID id, Bincoder<Repository, T> reader) {
		return reader.convert(this, read(id));
	}
	
	/**
	 * Override this to return an object already changed in the running
	 * transaction.
	 * 
	 * @return returns the object as it is already present in the running
	 *         transaction. Returns null if the object has not been loaded yet.
	 */
	protected Object transactionObject(ID id) {
		return loaded.get(id);
	}
	
	@Override
	public Site site(Name product, Name user, Name site) {
		return load(siteId(product, user, site), bin2site);
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
	
	@Override
	public User user(Name user) {
		return load(userId(user), bin2user);
	}
	
	@Override
	public Event event(long timestamp) throws UnknownEntity {
		return loadObject(ID.eventId(timestamp), bin2event);
	}
	
	@Override
	public History history(ID entity) throws UnknownEntity {
		ID id = ID.historyId(entity);
		return Bincoder.bin2history.convert(id, read(id));
	}

	private ByteBuffer read(ID id) throws UnknownEntity {
		ByteBuffer buf = txr.get(id);
		if (buf == null)
			throw new Repository.UnknownEntity(id);
		return buf;
	}
	
	@Override
	public void tasks(Name product, Predicate<Task> consumer) {
		range(bin2task, ID.taskId(product, IDN.ZERO), 
				(t) -> t.product.name.equalTo(product) && consumer.test(t));
	}
	
	@Override
	public Product[] products() {
		return range(bin2product, new Product[0], ID.productId(as("0")), 
				(p) -> true);
	}
	
	@Override
	public Site[] sites(Name product, Name menu) {
		return range(bin2site, new Site[0], ID.siteId(product, menu, as("0")),
				(s) -> s.menu.equalTo(menu));
	}
	
	@Override
	public Poll[] polls(Name product, Name area) {
		return range(bin2poll, new Poll[0], ID.pollId(product, area, IDN.ZERO), 
				(p) -> p.area.name.equalTo(area));
	}
	
	private <T> void range(Bincoder<Repository, T> reader, ID start, Predicate<T> filter) {
		txr.range(start, (k,v) -> filter.test(transactionObjectOrDecode(reader, k, v)));
	}
	
	private <T> T[] range(Bincoder<Repository, T> reader, T[] empty, ID start, Predicate<T> filter) {
		List<T> res = new ArrayList<>();
		txr.range(start, (k,v) -> {
			T e = transactionObjectOrDecode(reader, k, v);
			return filter.test(e) && res.add(e);
		});
		return res.toArray(empty);
	}
	
	@SuppressWarnings("unchecked")
	private <T> T transactionObjectOrDecode(Bincoder<Repository, T> reader, ID k, ByteBuffer v) {
		Object et = transactionObject(k);
		return (et != null) ? (T)et : reader.convert(this, v);
	}
}
