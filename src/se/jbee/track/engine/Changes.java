package se.jbee.track.engine;

import static java.util.Arrays.asList;

import java.util.Iterator;

import se.jbee.track.model.Entity;
import se.jbee.track.model.ID.Type;

public final class Changes implements Iterable<Changes.Entry<?>>{

	public static final Changes EMPTY = new Changes(0, new Entry[0]);
	
	public static final class Entry<T extends Entity<T>> {
		
		public final T before;
		public final Change.Operation[] transitions;
		public final T after;
		
		public Entry(T before, Change.Operation[] transitions, T after) {
			super();
			this.transitions = transitions;
			this.before = before;
			this.after = after;
		}
		
		public boolean isCreation() {
			return before == null;
		}
		
		public boolean isUpdate() {
			return before != null;
		}
		
		public Type type() {
			return after.uniqueID().type;
		}
	}
	
	public final long timestamp;
	private final Entry<?>[] log;
	
	public Changes(long timestamp, Entry<?>[] log) {
		super();
		this.timestamp = timestamp;
		this.log = log;
	}
	
	public Entry<?> get(int index) {
		return log[index];
	}
	
	public int length() {
		return log.length;
	}
	
	@Override
	public Iterator<Entry<?>> iterator() {
		return asList(log).iterator();
	}
}
