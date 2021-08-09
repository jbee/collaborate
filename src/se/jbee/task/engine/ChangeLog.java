package se.jbee.task.engine;

import static java.util.Arrays.asList;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

import se.jbee.task.model.Entity;
import se.jbee.task.model.ID.Type;

public final class ChangeLog implements Iterable<ChangeLog.Entry<?>>{

	public static final ChangeLog EMPTY = new ChangeLog(0, -1, new Entry[0]);

	/**
	 * Each {@link ChangeLog} change-set get a incrementing serial attached.
	 * This serial is unique within a run of the application. It is not
	 * persisted but it helps further processing to reorder {@link ChangeLog}
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
	private final Entry<?>[] entries;
	/**
	 * A non-persistent monotonically increasing number that is unique during
	 * application lifetime. A larger serial happened after a smaller one. A
	 * serial that is a direct successor happened directly after that one
	 * (speaking order not time).
	 */
	public final long serial;

	public static ChangeLog changes(long timestamp, Entry<?>[] log) {
		return log.length == 0 ? EMPTY : new ChangeLog(timestamp, SERIAL.incrementAndGet(), log);
	}

	public static long latestSerial() {
		return SERIAL.get();
	}

	private ChangeLog(long timestamp, long serial, Entry<?>[] entries) {
		this.timestamp = timestamp;
		this.serial = serial;
		this.entries = entries;
	}

	public Entry<?> get(int index) {
		return entries[index];
	}

	public int length() {
		return entries.length;
	}

	public boolean isEmpty() {
		return entries.length == 0;
	}

	@Override
	public Iterator<Entry<?>> iterator() {
		return asList(entries).iterator();
	}
}
