package vizio;

import static java.lang.System.arraycopy;
import static java.util.Arrays.copyOf;
import static java.util.Arrays.copyOfRange;

import java.util.function.BiPredicate;

import vizio.model.URL;


public final class Array {

	public static <T> int indexOf(T[] arr, T e, BiPredicate<T, T> eq) {
		for (int i = 0; i < arr.length; i++) {
			if (eq.test(arr[i], e))
				return i;
		}
		return -1;
	}

	public static <T> T[] add(T[] set, T e, BiPredicate<T, T> eq) {
		return indexOf(set, e, eq) < 0 ? append(set, e) : set;
	}
	
	public static <T> T[] append(T[] arr, T e) {
		T[] res = copyOf(arr, arr.length+1);
		res[arr.length] = e;
		return res;
	}
	
	public static <T> T[] remove(T[] arr, T e, BiPredicate<T, T> eq) {
		int idx = indexOf(arr, e, eq);
		if (idx >= 0) {
			if (idx == 0) {
				return copyOfRange(arr, 1, arr.length);
			}
			T[] res = copyOf(arr, arr.length-1);
			if (idx < arr.length-1) {
				arraycopy(arr, idx+1, res, idx, arr.length-idx-1);
			}
			return res;
		}
		return arr;
	}
	
	public static <T extends Comparable<T>> int compare(T[] a, T[] b) {
		int cmp = Integer.compare(a.length, b.length);
		if (cmp != 0)
			return cmp;
		for (int i = 0; i < a.length; i++) {
			cmp = a[i].compareTo(b[i]);
			if (cmp != 0)
				return cmp;
		}
		return 0;		
	}
	
	public static <T extends Comparable<T>> int compare(T[] a, T[] b, BiPredicate<T, T> eq) {
		int res = a.length - b.length;
		if (res == 0) { // compare as sets
			for (T e : a) {
				if (indexOf(b, e, eq) < 0)
					return 1;
			}
			return 0;
		}
		return res;		
	}
}
