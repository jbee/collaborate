package vizio.db;

import java.nio.ByteBuffer;

import vizio.model.ID;

public interface DB {

	TxR read();
	
	TxW write();
	
	interface TxR extends AutoCloseable {
		
		ByteBuffer get(ID key);
		
		@Override
		public void close();
	}
	
	interface TxW extends AutoCloseable {
		
		void put(ID key, ByteBuffer value);
		
		void commit();
		
		@Override
		public void close();
	}
}
