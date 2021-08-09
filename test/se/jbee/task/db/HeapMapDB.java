package se.jbee.task.db;

import static java.util.Collections.emptySortedMap;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;

import se.jbee.task.db.DB;
import se.jbee.task.model.ID;

/**
 * A {@link DB} for testing purposes that keeps entries in a memory
 * {@link SortedMap}.
 *
 * It uses a {@link Semaphore} to limit number of active {@link DB#write()}
 * transactions to one at a time.
 *
 * Read transaction operate on the state of the map at the beginning of the
 * {@link DB.Read}. When a {@link DB.Write} is created the {@link SortedMap} is
 * copied and any modifications are only transfered back in case of a
 * {@link Write#commit()}.
 *
 * Consequently write performance is pretty terrible for a larger database as it
 * copies on change.
 */
public final class HeapMapDB implements DB {

	public static DB create(boolean emptyOnClose) {
		return new HeapMapDB(emptyOnClose);
	}

	private final boolean emptyOnClose;
	private final AtomicReference<SortedMap<ID, ByteBuffer>> entities = new AtomicReference<>(emptySortedMap());
	private final Semaphore writeLock = new Semaphore(1);

	public HeapMapDB(boolean emptyOnClose) {
		this.emptyOnClose = emptyOnClose;
	}

	@Override
	public Read read() {
		return new HeapMapRead(entities.get());
	}

	@Override
	public Write write() {
		try {
			writeLock.acquire();
			return new HeapMapWrite(entities.get(), this);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close() {
		if (emptyOnClose) {
			entities.get().clear();
			entities.set(emptySortedMap());
		}
	}

	private static class HeapMapRead implements Read {

		final SortedMap<ID, ByteBuffer> entities;

		HeapMapRead(SortedMap<ID, ByteBuffer> entities) {
			this.entities = entities;
		}

		@Override
		public ByteBuffer get(ID key) {
			return entities.get(key);
		}

		@Override
		public void close() {
			// nothing...
		}

		@Override
		public void range(ID first, BiPredicate<ID, ByteBuffer> consumer) {
			Iterator<Entry<ID, ByteBuffer>> iter = entities.entrySet().iterator();
			Entry<ID, ByteBuffer> e = null;
			while (iter.hasNext() && (e == null || !e.getKey().equalTo(first)))
				e = iter.next();
			if (e != null && e.getKey().equalTo(first)) {
				boolean cont = consumer.test(first, e.getValue());
				while (cont && iter.hasNext())
					cont = consumer.test(first, e.getValue());
			}
		}

	}

	private static class HeapMapWrite extends HeapMapRead implements Write {

		private final HeapMapDB db;
		private boolean committed = false;

		HeapMapWrite(SortedMap<ID, ByteBuffer> entities, HeapMapDB db) {
			super(new TreeMap<>(entities));
			this.db = db;
		}

		@Override
		public void put(ID key, ByteBuffer value) {
			entities.put(key, value);
		}

		@Override
		public void delete(ID key) {
			entities.remove(key);
		}

		@Override
		public void commit() {
			db.entities.set(entities);
			committed = true;
		}

		@Override
		public void close() {
			if (!committed) {
				entities.clear();
			}
			db.writeLock.release();
		}

	}

}
