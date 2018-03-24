package se.jbee.track.engine;

import static java.util.Arrays.asList;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

import se.jbee.track.model.Entity;
import se.jbee.track.model.ID.Type;

public final class Changes implements Iterable<Changes.Entry<?>>{

	public static final Changes EMPTY = new Changes(0, -1, new Entry[0]);

	/**
	 * Each {@link Changes} change-set get a incrementing serial attached.
	 * This serial is unique within a run of the application. It is not
	 * persisted but it helps further processing to reorder {@link Changes}
	 * if necessary as they can identify (and wait) missing sets.
	 */
	private static final AtomicLong SERIAL = new AtomicLong();

	public static final class Entry<T extends Entity<T>> {

		public final T before;
		public final Change.Operation[] transitions;
		public final T after;

		public Entry(T before, Change.Operation[] transitions, T after) {
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

		@Override
		public String toString() {
			return after.toString()+Arrays.toString(transitions);
		}
	}

	public final long timestamp;
	private final Entry<?>[] log;
	/**
	 * A non-persistent monotonically increasing number that is unique during
	 * application lifetime. A larger serial happened after a smaller one. A
	 * serial that is a direct successor happened directly after that one
	 * (speaking order not time).
	 */
	public final long serial;

	public static Changes changes(long timestamp, Entry<?>[] log) {
		return log.length == 0 ? EMPTY : new Changes(timestamp, SERIAL.incrementAndGet(), log);
	}

	public static long latestSerial() {
		return SERIAL.get();
	}

	private Changes(long timestamp, long serial, Entry<?>[] log) {
		this.timestamp = timestamp;
		this.serial = serial;
		this.log = log;
	}

	public Entry<?> get(int index) {
		return log[index];
	}

	public int length() {
		return log.length;
	}

	public boolean isEmpty() {
		return length() == 0;
	}

	@Override
	public Iterator<Entry<?>> iterator() {
		return asList(log).iterator();
	}
}
