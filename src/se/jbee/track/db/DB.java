package se.jbee.track.db;

import java.nio.ByteBuffer;
import java.util.function.BiPredicate;

import se.jbee.track.model.ID;

/**
 * A key-value store database low level abstraction where keys are represented
 * by {@link ID}s and values by {@link ByteBuffer}s.
 *
 * This usually is a multi reader, single writer database.
 */
public interface DB extends AutoCloseable {

	/**
	 * @return a new read transaction opened
	 */
	Read read();

	/**
	 * @return anew write transaction opened
	 */
	Write write();

	@Override
	public void close();

	/**
	 * A read-only transaction
	 */
	interface Read extends AutoCloseable {

		/**
		 * @param key not null
		 * @return value or null, if no such value exists
		 */
		ByteBuffer get(ID key);

		@Override
		public void close();

		void range(ID first, BiPredicate<ID, ByteBuffer> consumer);

	}

	/**
	 * A read-write transaction.
	 */
	interface Write extends Read {

		void put(ID key, ByteBuffer value);

		void delete(ID key);

		void commit();

	}
}
