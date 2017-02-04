package vizio.engine;

import static java.util.Arrays.asList;

import java.util.Iterator;

import vizio.model.Entity;

public final class Changelog implements Iterable<Changelog.Entry<?>>{

	public static final Changelog EMPTY = new Changelog(new Entry[0]);
	
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
	}
	
	private final Entry<?>[] log;
	
	public Changelog(Entry<?>[] log) {
		super();
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
