package vizio.db;

import java.nio.ByteBuffer;

import org.lmdbjava.Dbi;
import org.lmdbjava.DbiFlags;
import org.lmdbjava.Env;
import org.lmdbjava.Txn;

import vizio.model.ID;

public class LMDB implements DB {

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