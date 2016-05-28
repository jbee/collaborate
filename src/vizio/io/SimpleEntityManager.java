package vizio.io;

import java.util.Arrays;

import vizio.IDN;
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

	/**
	 * An {@link Index} always has two key levels. 
	 * The {@link #values} for a particular key-pair are themselves a set sorted by {@link Task#id}.
	 * 
	 * Both the arrays for keys and those for the value-sets of key-pairs are growing with power of two.
	 * Unused slots are null values.
	 */
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
		 * A set of values (sorted by {@link IDN}) having the common
		 * keys 1 and 2 at the same index. The array may have null values at the
		 * end. The first null value indicates the end of the set.
		 */
		final Task[][] values;

		Index(K1[] keys1, K2[] keys2, Task[][] values) {
			super();
			this.keys1 = keys1;
			this.keys2 = keys2;
			this.values = values;
		}

		Index<K1,K2> index(K1 key1, K2 key2, Task value) {

			return this;
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
	
	/**
	 * Adds the element to the set. 
	 * The set is sorted by {@link Task#id}. 
	 * The set might have empty (null) slots at the end for later use. 
	 */
	static <T extends Comparable<T>> T[] insert(T[] set, T e) {
		return insert(set, e, indexOf(set, e));
	}
	
	static <T> T[] insert(T[] set, T e, int idx) {		
		if (idx >= 0) { // found
			set[idx] = e; // update to be sure
			return set;
		}
		idx = -++idx; // undo negative pos
		int last = set.length-1;
		int len = last-idx;
		if (set[last] != null) { // allocate
			set = Arrays.copyOf(set, set.length*2);
			len++;
		} 
		if (len > 0) { // move backwards
			System.arraycopy(set, idx, set, idx+1, len);
		}
		set[idx] = e;
		return set;
	}
	
	static <T extends Comparable<T>> int indexOf(T[] set, T e) {
		return search(set, 0, set.length, e);
	}
	
	private static <T extends Comparable<T>> int search(T[] a, int fromIndex, int toIndex, T e) {
		int low = fromIndex;
		int high = toIndex - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;
			T midVal = a[mid];
			int cmp = midVal == null ? 1 : midVal.compareTo(e);

			if (cmp < 0)
				low = mid + 1;
			else if (cmp > 0)
				high = mid - 1;
			else
				return mid; // key found
		}
		return -(low + 1);  // key not found.
	}
}
