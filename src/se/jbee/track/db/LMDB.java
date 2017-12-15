package se.jbee.track.db;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import org.lmdbjava.Cursor;
import org.lmdbjava.CursorIterator;
import org.lmdbjava.CursorIterator.IteratorType;
import org.lmdbjava.CursorIterator.KeyVal;
import org.lmdbjava.Dbi;
import org.lmdbjava.DbiFlags;
import org.lmdbjava.Env;
import org.lmdbjava.GetOp;
import org.lmdbjava.Txn;

import se.jbee.track.model.ID;
import se.jbee.track.model.ID.Type;

public final class LMDB implements DB {

	private final Env<ByteBuffer> env;
	@SuppressWarnings("unchecked")
	private final Dbi<ByteBuffer>[] tables = new Dbi[ID.Type.values().length]; 

	public LMDB(Env<ByteBuffer> env) {
		super();
		this.env = env;
		for (ID.Type t : ID.Type.values()) {
			tables[t.ordinal()] = env.openDbi(t.name(), DbiFlags.MDB_CREATE);
		}
	}
	
	@Override
	public TxR read() {
		return new LMDB_TxR(env);
	}
	
	@Override
	public TxRW write() {
		return new LMDB_TxRW(env);
	}

	Dbi<ByteBuffer> table(ID.Type type) {
		return tables[type.ordinal()];
	}
	
	private class LMDB_TxR implements TxR {

		final Txn<ByteBuffer> txn;
		final ByteBuffer key;
		
		public LMDB_TxR(Env<ByteBuffer> env) {
			this(env.txnRead());
		}
		
		LMDB_TxR(Txn<ByteBuffer> txn) {
			this.txn = txn;
			this.key = ByteBuffer.allocateDirect(env.getMaxKeySize());
		}

		@Override
		public final ByteBuffer get(ID id) {
			setKey(id);
			return table(id.type).get(txn, key);
		}
		
		@Override
		public void range(ID first, BiPredicate<ID, ByteBuffer> consumer) {
			try (CursorIterator<ByteBuffer> it = iterator(first)) {
				KeyVal<ByteBuffer> e = it.next();
				byte[] k = new byte[e.key().remaining()];
				e.key().get(k);
				while (it.hasNext() && consumer.test(ID.fromBytes(k), e.val()));
			}
		}
		
		private CursorIterator<ByteBuffer> iterator(ID first) {
			Dbi<ByteBuffer> table = table(first.type);
			key.clear();
			key.put(first.bytes());
			key.position(key.position()-2);
			key.flip();
			return table.iterate(txn, key, IteratorType.FORWARD);			
		}
		
		@Override
		public final void close() {
			txn.close();
		}
		
		final void setKey(ID id) {
			key.clear();
			key.put(id.bytes()).flip();
		}
	}

	private class LMDB_TxRW extends LMDB_TxR implements TxRW {

		public LMDB_TxRW(Env<ByteBuffer> env) {
			super(env.txnWrite());
		}
		
		@Override
		public void put(ID id, ByteBuffer value) {
			setKey(id);
			table(id.type).put(txn, key, value);
		}

		@Override
		public void commit() {
			txn.commit();			
		}

	}

}
