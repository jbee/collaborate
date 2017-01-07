package vizio.io;

import java.lang.reflect.Array;
import java.util.Arrays;

import vizio.io.SimpleCache.Key;
import vizio.model.IDN;
import vizio.model.Name;
import vizio.model.Names;
import vizio.model.Task;

/**
 * An {@link Index} always has two key levels. 
 * The {@link #values} for a particular key-pair are themselves a set sorted by {@link Task#id}.
 * 
 * Both the arrays for keys and those for the value-sets of key-pairs are growing with power of two.
 * Unused slots are null values.
 */
final class Index<K extends Comparable<K>> {

	/**
	 * Creates an new fresh index from all existing tasks.
	 */
	static <K extends Comparable<K>> Index<K> index(Key<Name> key1, Key<K> key2, Task[] tasks) {

		return null;
	}

	/**
	 * Creates an new fresh index from all existing tasks.
	 */
	static <K extends Comparable<K>> Index<K> multiIndex(Key<Names> key1, Key<K> key2, Task[] tasks) {

		return null;
	}
	
	@SuppressWarnings("unchecked")
	static <K extends Comparable<K>> Index<K> init() {
		return new Index<K>(new Name[0], new TasksIndex[0]);
	}
	
	static final class TasksIndex<K extends Comparable<K>> {
		/**
		 * Second order keys 
		 */
		final K[] keys;
		
		/**
		 * A set of values (sorted by {@link IDN}) having the common
		 * keys 1 and 2 at the same index. The array may have null values at the
		 * end. The first null value indicates the end of the set.
		 */
		final Task[][] values;
		
		TasksIndex(K[] keys, Task[][] values) {
			super();
			this.keys = keys;
			this.values = values;
		}
	
		TasksIndex<K> add(K key, Task task) {
			int idx = indexOf(keys, key);
			if (idx >= 0) { // key found
				// this is "atomic" so no new Index instance is required
				values[idx] = insert(values[idx], task);
				return this;
			}
			return new TasksIndex<K>(
					insert(keys, key, idx), 
					insert(values, new Task[] { task }, idx));
		}
		
		TasksIndex<K> remove(K key, Task task) {
			int idx = indexOf(keys, key);
			if (idx >= 0) { // key found
				Task[] set = cutout(values[idx], task);
				if (set.length == 0) {
					return new TasksIndex<K>(cutout(keys, idx), cutout(values, idx));
				}
				values[idx] = set;
			}
			return this;
		}
		
		Task[] tasks(K key) {
			int idx = indexOf(keys, key);
			return idx < 0 ? null : values[idx];
		}
	}

	/**
	 * First order keys: Index leads to array of second order keys with that common first order key.
	 */
	final Name[] keys;

	final TasksIndex<K>[] values;
	
	private Index(Name[] keys, TasksIndex<K>[] values) {
		super();
		this.keys = keys;
		this.values = values;
	}

	Index<K> add(Name key1, K key2, Task task) {
		int idx = indexOf(keys, key1);
		if (idx >= 0) { // key found
			values[idx] = values[idx].add(key2, task);
			return this;
		}
		@SuppressWarnings("unchecked")
		K[] keys2 = (K[]) Array.newInstance(key2.getClass(), 1);
		keys2[0] = key2;
		return new Index<K>(
				insert(keys, key1, idx), 
				insert(values, new TasksIndex<K>(keys2, new Task[][] {{ task } }), idx));
	}
	
	Index<K> remove(Name key1, K key2, Task task) {
		int idx = indexOf(keys, key1);
		if (idx >= 0) { // key found
			TasksIndex<K> set = values[idx].remove(key2, task);
			if (set.values.length == 0)
				return new Index(cutout(keys, idx), cutout(values, idx));
			values[idx] = set;
		}
		return this;
	}
	
	Task[] tasks(Name key1, K key2) {
		int idx = indexOf(keys, key1);
		return idx < 0 ? null : values[idx].tasks(key2);
	}

	static <T extends Comparable<T>> int search(T[] a, int fromIndex, int toIndex, T e) {
		int low = fromIndex;
		int high = toIndex - 1;
	
		while (low <= high) {
			int mid = (low + high) >>> 1;
			int cmp = a[mid].compareTo(e);
	
			if (cmp < 0)
				low = mid + 1;
			else if (cmp > 0)
				high = mid - 1;
			else
				return mid; // key found
		}
		return -(low + 1);  // key not found.
	}

	static <T extends Comparable<T>> int indexOf(T[] set, T e) {
		return search(set, 0, set.length, e);
	}

	static <T> T[] cutout(T[] arr, int idx) {
		if (idx < 0) // not found
			return arr; // no change
		@SuppressWarnings("unchecked")
		T[] res = (T[]) Array.newInstance(arr.getClass().getComponentType(), arr.length-1);
		if (idx > 0) {
			System.arraycopy(arr, 0, res, 0, idx);
		}
		if (idx < arr.length-1) {
			System.arraycopy(arr, idx+1, res, idx, arr.length-idx-1);
		}
		return res;
	}

	static <T> T[] insert(T[] arr, T e, int idx) {
		if (idx >= 0) { // found
			arr[idx] = e; // update
			return arr;
		}
		idx = -++idx; // undo negative pos
		@SuppressWarnings("unchecked")
		T[] res = (T[]) Array.newInstance(arr.getClass().getComponentType(), arr.length+1);
		if (idx > 0) {
			System.arraycopy(arr, 0, res, 0, idx);
		}
		res[idx] = e;
		if (idx < arr.length) {
			System.arraycopy(arr, idx, res, idx+1, arr.length-idx);
		}
		return res;
	}
	
	static <T extends Comparable<T>> T[] merge(T[] a, T[] b) {
		int ia = 0;
		int ib = 0;
		int ic = 0;
		@SuppressWarnings("unchecked")
		T[] c = (T[]) Array.newInstance(a.getClass().getComponentType(), a.length+b.length);
		while (ia < a.length && ib < b.length) {
			int cmp = a[ia].compareTo(b[ib]);
			if (cmp == 0) {
				c[ic++] = a[ia];
				ia++;
				ib++;
			} else if (cmp < 0) {
				c[ic++] = a[ia++];
			} else {
				c[ic++] = b[ib++];
			}
		}
		if (ia < a.length) {
			System.arraycopy(a, ia, c, ic, a.length-ia);
			ic+=a.length-ia;
		}
		if (ib < b.length) {
			System.arraycopy(b, ia, c, ic, b.length-ib);
			ic+=b.length-ib;
		}
		return ic == c.length ? c : Arrays.copyOf(c, ic); 
	}

	static <T extends Comparable<T>> T[] cutout(T[] set, T e) {
		return cutout(set, indexOf(set,  e));
	}

	/**
	 * Adds the element to the set. 
	 * The set is sorted by {@link Task#id}. 
	 * The set might have empty (null) slots at the end for later use. 
	 */
	static <T extends Comparable<T>> T[] insert(T[] set, T e) {
		return insert(set, e, indexOf(set, e));
	}
}