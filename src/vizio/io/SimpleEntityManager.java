package vizio.io;

import java.lang.reflect.Array;
import java.util.Arrays;

import vizio.Motive;
import vizio.Name;
import vizio.Names;
import vizio.Purpose;
import vizio.Status;
import vizio.Task;

/**
 * Paths:
 * <pre>
 * Product /<product>/product.dat
 * Area    /<product>/area/<area>.dat
 * Poll    /<product>/poll/<area>/<matter>/<affected>.dat
 * Version /<product>/version/<version>.dat
 * Task    /<product>/task/<IDN>.dat
 * </pre>
 *
 * @author jan
 */
public class SimpleEntityManager {

	// FIX (keys are data that cannot change)
	// INDEX B: product, motive
	// INDEX C: product, purpose
	// INDEX E: product, version, status

	// INDEX A: product, status
	// INDEX D: product, area, status
	// INDEX U: product, user

	// INDEX F: user (involved), status
	// INDEX G: user (watch), status

	// queries that do not limit by user or by product are illegal for now
	// other filters and sorts are done on linearly on the set extracted from index by binary search

	// use copy on write? copy the objects before modification - also important for updates so see what has changed
	// as a protection against forgetting to copy the store can compare its cached reference with the given instance
	// if they are equal the cached one is reload from deep storage and a failure is thrown
	// that would mean all entities should support Cloneable

	// use an index with only one row per key pair (values in a separate array)
	// Object[] keys
	// Task[][] values
	// optionally one can test additional start index
	// int[] starts; // end is simply indicated by null values
	// or if values are unsorted one can always just append.

	static interface Key<K> {
		K key(Task task);
	}

	static final Key<Name> product       = (Task task) -> task.product.name;
	static final Key<Name> area          = (Task task) -> task.area.name;
	static final Key<Name> version       = (Task task) -> task.base.name;
	static final Key<Names> enlistedBy   = (Task task) -> task.enlistedBy;
	static final Key<Names> approachedBy = (Task task) -> task.approachedBy;
	static final Key<Names> watchedBy    = (Task task) -> task.watchedBy;
	static final Key<Status> status      = (Task task) -> task.status;
	static final Key<Motive> motive      = (Task task) -> task.motive;
	static final Key<Purpose> purpose    = (Task task) -> task.purpose;

	/**
	 * Creates an new fresh index from all existing tasks.
	 */
	static <K1,K2> Index<K1, K2> index(Key<K1> key1, Key<K2> key2, Task[] tasks) {

		return null;
	}

	/**
	 * Creates an new fresh index from all existing tasks.
	 */
	static <K2> Index<Name,K2> multiIndex(Key<Names> key1, Key<K2> key2, Task[] tasks) {

		return null;
	}

	static final class Index<K1,K2> {

		/**
		 * First order keys
		 */
		final K1[] keys1;
		/**
		 * Second order keys
		 */
		final K2[] keys2;
		/**
		 * A *unsorted* set of values (no particular order) having the common
		 * keys 1 and 2 at the same index. The array may have null values at the
		 * end. The first null value indicates the end of the set. New values
		 * are simply "appended". If the array is full it is copied to a new
		 * larger one with fresh space for new values.
		 */
		final Task[][] values;

		Index(K1[] keys1, K2[] keys2, Task[][] values) {
			super();
			this.keys1 = keys1;
			this.keys2 = keys2;
			this.values = values;
		}

		Index<K1,K2> index(K1 key1, K2 key2, Task value) {
			int idx = search(key1, key2);
			if (idx >= 0) {
				values[idx] = join(values[idx], value);
				return this;
			}
			int len = values.length+1;
			K1[] ks1 = (K1[]) Array.newInstance(key1.getClass(), len);
			K2[] ks2 = (K2[]) Array.newInstance(key2.getClass(), len);
			Task[][] vs = new Task[len][];
			idx = -(idx+1);
			if (idx > 0) {
				System.arraycopy(keys1, 0, ks1, 0, idx);
				System.arraycopy(keys2, 0, ks2, 0, idx);
				System.arraycopy(values, 0, vs, 0, idx);
			}
			// TODO returns a new index object if the key pair did not exist already
			// otherwise returns same index with updated values for the key pair
			return new Index<K1,K2>(ks1, ks2, vs);
		}

		private static Task[] join(Task[] tasks, Task task) {
			//TODO prevent duplicates (maybe sort tasks by IDN so that check is cheap)
			Task[] appended = Arrays.copyOf(tasks, tasks.length+1);
			appended[appended.length-1] = task;
			return appended;
		}

		/**
		 * A binary search for the index/position for the key pair.
		 *
		 * @return the index of the search key pair, if it is contained in the
		 *         array; otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>.
		 */
		int search(K1 key1, K2 key2) {

			return -1;
		}
	}
}
