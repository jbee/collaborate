package se.jbee.track.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static se.jbee.track.model.ID.userId;
import static se.jbee.track.model.Name.as;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import se.jbee.track.db.DB.TxRW;
import se.jbee.track.model.ID;

/**
 * Tests the basic correctness of the {@link HeapMapDB} implementation of a {@link DB}.
 */
public class TestHeapMapDB {

	private DB db = HeapMapDB.create(false);

	@Test
	public void onlyOneWriteTransationAtATimeIsPossible() throws InterruptedException {
		try (TxRW tx = db.write()) {
			AtomicReference<TxRW> tx2 = new AtomicReference<>();
			Thread w2 = new Thread(() -> {
				tx2.set(db.write());
			});
			w2.start();
			Thread.sleep(20);
			assertNull(tx2.get());
		}
	}

	@Test
	public void putValuesAreAvailableAfterCommittedTransaction() {
		ID key = userId(as("xy"));
		ByteBuffer value = value("foo");
		try (TxRW tx = db.write()) {
			tx.put(key, value);
			tx.commit();
		}
		assertEquals(value, db.read().get(key));
	}

	@Test
	public void putValuesAreNotAvailableAfterUncommittedTransaction() {
		ID key = userId(as("xy"));
		ByteBuffer value = value("foo");
		try (TxRW tx = db.write()) {
			tx.put(key, value);
		}
		assertNull(db.read().get(key));
	}

	@Test
	public void putValuesCanBeReplacedByACommittedTransaction() {
		ID key = userId(as("xy"));
		ByteBuffer value = value("foo");
		try (TxRW tx = db.write()) {
			tx.put(key, value);
			tx.commit();
		}
		assertEquals(value, db.read().get(key));

		ByteBuffer newValue = value("bar");
		try (TxRW tx = db.write()) {
			tx.put(key, newValue);
			tx.commit();
		}
		assertEquals(newValue, db.read().get(key));
	}

	@Test
	public void putValuesAreNotReplacedByAnUncommittedTransaction() {
		ID key = userId(as("xy"));
		ByteBuffer value = value("foo");
		try (TxRW tx = db.write()) {
			tx.put(key, value);
			tx.commit();
		}
		assertEquals(value, db.read().get(key));

		ByteBuffer newValue = value("bar");
		try (TxRW tx = db.write()) {
			tx.put(key, newValue);
		}
		assertEquals(value, db.read().get(key));
	}

	private static ByteBuffer value(String val) {
		return ByteBuffer.wrap("bar".getBytes());
	}
}
