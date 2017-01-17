package vizio.db;

import java.nio.ByteBuffer;
import java.util.function.Predicate;

import org.lmdbjava.CursorIterator;
import org.lmdbjava.CursorIterator.IteratorType;
import org.lmdbjava.Dbi;
import org.lmdbjava.DbiFlags;
import org.lmdbjava.Env;
import org.lmdbjava.Txn;

import vizio.model.ID;

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
	public TxW write() {
		return new LMDB_TxW(env);
	}

	Dbi<ByteBuffer> table(ID id) {
		return tables[id.type.ordinal()];
	}
	
	private final class LMDB_TxR implements TxR {

		private final Txn<ByteBuffer> txn;
		private final ByteBuffer key;
		
		public LMDB_TxR(Env<ByteBuffer> env) {
			super();
			this.txn = env.txnRead();
			this.key = ByteBuffer.allocateDirect(env.getMaxKeySize());
		}

		@Override
		public ByteBuffer get(ID id) {
			key.clear();
			key.put(id.bytes()).flip();
			return table(id).get(txn, key);
		}
		
		@Override
		public void range(ID id, Predicate<ByteBuffer> consumer) {
			if (id.isUnique()) {
				consumer.test(get(id));
				return;
			}
			key.clear();
			key.put(id.bytes());
			key.position(key.position()-2);
			key.flip();
			try (CursorIterator<ByteBuffer> it = table(id).iterate(txn, key, IteratorType.FORWARD)) {
				boolean consume = it.hasNext();
				while (consume) {
					consume = consumer.test(it.next().val()) && it.hasNext(); 
				}
			}
		}
		
		@Override
		public void close() {
			txn.close();
		}
	}

	private class LMDB_TxW implements TxW {

		private final Txn<ByteBuffer> txn;
		private final ByteBuffer key;
		
		public LMDB_TxW(Env<ByteBuffer> env) {
			super();
			this.txn = env.txnWrite();
			this.key = ByteBuffer.allocateDirect(env.getMaxKeySize());
		}

		@Override
		public void put(ID id, ByteBuffer value) {
			key.clear();
			key.put(id.bytes()).flip();
			table(id).put(txn, key, value);
		}

		@Override
		public void commit() {
			txn.commit();			
		}
		
		@Override
		public void close() {
			txn.close();
		}
	}

}
