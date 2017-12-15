package se.jbee.track.db;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

import se.jbee.track.model.ID;

public interface DB {

	TxR read();
	
	TxRW write();
	
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
