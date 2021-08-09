package se.jbee.task.db;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.BiPredicate;

import org.lmdbjava.CursorIterator;
import org.lmdbjava.CursorIterator.IteratorType;
import org.lmdbjava.CursorIterator.KeyVal;
import org.lmdbjava.Dbi;
import org.lmdbjava.DbiFlags;
import org.lmdbjava.Env;
import org.lmdbjava.Env.Builder;

import se.jbee.task.model.ID;

import org.lmdbjava.Txn;

/**
 * A wrapper around the java LMDB library to decouple all code from the library.
 *
 * A {@link LMDB} instance usually is constructed on application startup and
 * used by multiple threads to create {@link Read} or {@link Write} transactions.
 */
public final class LMDB implements DB {

	private volatile Env<ByteBuffer> env;
	private final AtomicReferenceArray<Dbi<ByteBuffer>> collections = new AtomicReferenceArray<>(ID.Type.values().length);

	public LMDB(Builder<ByteBuffer> envBuilder, File path) {
		this.env = envBuilder.setMaxDbs(10).open(path);
		for (ID.Type t : ID.Type.values()) {
			collections.set(t.ordinal(), env.openDbi(t.name(), DbiFlags.MDB_CREATE));
		}
	}

	@Override
	public void close() {
		for (int i = 0; i < collections.length(); i++)
			collections.get(i).close();
		env.close();
	}

	@Override
	public Read read() {
		return new LMDBRead(env);
	}

	@Override
	public Write write() {
		return new LMDBWrite(env);
	}

	Dbi<ByteBuffer> collection(ID.Type type) {
		return collections.get(type.ordinal());
	}

	private class LMDBRead implements Read {

		final Txn<ByteBuffer> txn;
		final ByteBuffer key;

		public LMDBRead(Env<ByteBuffer> env) {
			this(env.txnRead());
		}

		LMDBRead(Txn<ByteBuffer> txn) {
			this.txn = txn;
			this.key = ByteBuffer.allocateDirect(env.getMaxKeySize());
		}

		@Override
		public final ByteBuffer get(ID id) {
			setKey(id);
			return collection(id.type).get(txn, key);
		}

		@Override
		public void range(ID first, BiPredicate<ID, ByteBuffer> consumer) {
			try (CursorIterator<ByteBuffer> it = iterator(first)) {
				while (it.hasNext()) {
					KeyVal<ByteBuffer> e = it.next();
					byte[] k = new byte[e.key().remaining()];
					e.key().get(k);
					if (!consumer.test(ID.fromBytes(k), e.val()))
						return;
				}
			}
		}

		private CursorIterator<ByteBuffer> iterator(ID first) {
			Dbi<ByteBuffer> collection = collection(first.type);
			key.clear();
			key.put(first.readonlyBytes());
			key.position(key.position()-2);
			key.flip();
			return collection.iterate(txn, key, IteratorType.FORWARD);
		}

		@Override
		public final void close() {
			txn.close();
		}

		final void setKey(ID id) {
			key.clear();
			key.put(id.readonlyBytes()).flip();
		}
	}

	private final class LMDBWrite extends LMDBRead implements Write {

		public LMDBWrite(Env<ByteBuffer> env) {
			super(env.txnWrite());
		}

		@Override
		public void put(ID id, ByteBuffer value) {
			setKey(id);
			collection(id.type).put(txn, key, value);
		}

		@Override
		public void delete(ID id) {
			setKey(id);
			collection(id.type).delete(txn, key);
		}

		@Override
		public void commit() {
			txn.commit();
		}

	}

}
