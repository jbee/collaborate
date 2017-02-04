package vizio.db;

import java.nio.ByteBuffer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import vizio.model.ID;

public interface DB {

	TxR read();
	
	TxW write();
	
	interface TxR extends AutoCloseable {
		
		ByteBuffer get(ID key);
		
		void range(ID key, Predicate<ByteBuffer> consumer);
		
		default void all(ID key, Consumer<ByteBuffer> consumer) {
			range(key, (b) -> { consumer.accept(b); return true; } );
		}
		
		@Override
		public void close();
	}
	
	interface TxW extends AutoCloseable {
		
		ByteBuffer get(ID key);
		
		void put(ID key, ByteBuffer value);
		
		void commit();
		
		@Override
		public void close();
	}
}
