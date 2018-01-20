package se.jbee.track.engine;

import static se.jbee.track.engine.Bincoder.bin2area;
import static se.jbee.track.engine.Bincoder.bin2event;
import static se.jbee.track.engine.Bincoder.bin2output;
import static se.jbee.track.engine.Bincoder.bin2page;
import static se.jbee.track.engine.Bincoder.bin2poll;
import static se.jbee.track.engine.Bincoder.bin2task;
import static se.jbee.track.engine.Bincoder.bin2user;
import static se.jbee.track.engine.Bincoder.bin2version;
import static se.jbee.track.model.ID.areaId;
import static se.jbee.track.model.ID.outputId;
import static se.jbee.track.model.ID.pageId;
import static se.jbee.track.model.ID.pollId;
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
import se.jbee.track.model.Output;
import se.jbee.track.model.Page;
import se.jbee.track.model.Poll;
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
	private final <T extends Entity<T>> T load(ID id, Bincoder<Repository, T> decoder) {
		Object res = transactionObject(id);
		if (res != null)
			return (T) res;		
		T e = loadObject(id, decoder);
		loaded.put(id, e);
		return e;
	}

	private <T> T loadObject(ID id, Bincoder<Repository, T> decoder) {
		return decoder.convert(this, read(id));
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
	public Page page(Name output, Name user, Name page) {
		return load(pageId(output, user, page), bin2page);
	}

	@Override
	public Poll poll(Name output, Name area, IDN serial) {
		return load(pollId(output, area, serial), bin2poll);
	}

	@Override
	public Output output(Name output) {
		return load(outputId(output), bin2output);
	}

	@Override
	public Area area(Name output, Name area) {
		return load(areaId(output, area), bin2area);
	}

	@Override
	public Version version(Name output, Name version) {
		return load(versionId(output, version), bin2version);
	}

	@Override
	public Task task(Name output, IDN id) {
		return load(ID.taskId(output, id), bin2task);
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
	public void tasks(Name output, Predicate<Task> consumer) {
		range(bin2task, ID.taskId(output, IDN.ZERO), 
				(t) -> t.output.name.equalTo(output) && consumer.test(t));
	}
	
	@Override
	public Output[] outputs() {
		return range(bin2output, new Output[0], ID.outputId(as("0")), 
				(p) -> true);
	}
	
	@Override
	public Page[] pages(Name output, Name menu) {
		return range(bin2page, new Page[0], ID.pageId(output, menu, as("0")),
				(s) -> s.menu.equalTo(menu));
	}
	
	@Override
	public Poll[] polls(Name output, Name area) {
		return range(bin2poll, new Poll[0], ID.pollId(output, area, IDN.ZERO), 
				(p) -> p.area.name.equalTo(area));
	}
	
	private <T> void range(Bincoder<Repository, T> decoder, ID start, Predicate<T> filter) {
		txr.range(start, (k,v) -> filter.test(transactionObjectOrDecode(decoder, k, v)));
	}
	
	private <T> T[] range(Bincoder<Repository, T> decoder, T[] empty, ID start, Predicate<T> filter) {
		List<T> res = new ArrayList<>();
		txr.range(start, (k,v) -> {
			T e = transactionObjectOrDecode(decoder, k, v);
			return filter.test(e) && res.add(e);
		});
		return res.toArray(empty);
	}
	
	@SuppressWarnings("unchecked")
	private <T> T transactionObjectOrDecode(Bincoder<Repository, T> decoder, ID k, ByteBuffer v) {
		Object et = transactionObject(k);
		return (et != null) ? (T)et : decoder.convert(this, v);
	}
}
