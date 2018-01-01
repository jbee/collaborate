package se.jbee.track.db;

import java.nio.ByteBuffer;
import java.util.function.BiPredicate;

import se.jbee.track.model.ID;

public interface DB extends AutoCloseable {

	TxR read();
	
	TxRW write();
	
	@Override
	public void close();
	
	interface TxR extends AutoCloseable {
		
		ByteBuffer get(ID key);
		
		@Override
		public void close();
		
		void range(ID first, BiPredicate<ID, ByteBuffer> consumer);
		
	}
	
	interface TxRW extends TxR {
		
		void put(ID key, ByteBuffer value);
		
		void commit();
		
	}
}
