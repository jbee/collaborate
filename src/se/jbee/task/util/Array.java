package se.jbee.task.util;

import static java.lang.System.arraycopy;
import static java.util.Arrays.copyOf;
import static java.util.Arrays.copyOfRange;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;


public final class Array {

	public static int nextPowerOf2(int num) {
		return num <= 1 ? 1 : Integer.highestOneBit(num - 1) << 1;
	}

	public static <T> int indexOf(T[] arr, T e, BiPredicate<T, T> eq) {
		for (int i = 0; i < arr.length; i++)
			if (eq.test(arr[i], e))
				return i;
		return -1;
	}

	public static <T> int indexOf(T[] arr, Predicate<T> eq) {
		for (int i = 0; i < arr.length; i++)
			if (eq.test(arr[i]))
				return i;
		return -1;
	}

	public static <T> boolean any(T[] arr, Predicate<T> eq) {
		return indexOf(arr, eq) >= 0;
	}

	public static <T, V> int indexOf(T[] arr, BiPredicate<T, V> swap, V init) {
		V candidate = init;
		int res = -1;
		for (int i = 0; i < arr.length; i++)
			if (swap.test(arr[i], candidate))
				res = i;
		return res;
	}

	public static <T> boolean contains(T[] arr, T e, BiPredicate<T, T> eq) {
		return indexOf(arr, e, eq) >= 0;
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
			if (idx == 0)
				return copyOfRange(arr, 1, arr.length);
			T[] res = copyOf(arr, arr.length-1);
			if (idx < arr.length-1)
				arraycopy(arr, idx+1, res, idx, arr.length-idx-1);
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
			for (T e : a)
				if (indexOf(b, e, eq) < 0)
					return 1;
			return 0;
		}
		return res;
	}

	public static <A> A[] fold(A[][] a) {
		int len = 0;
		for (A[] e : a)
			len += e.length;
		@SuppressWarnings("unchecked")
		A[] res = (A[]) java.lang.reflect.Array.newInstance(a[0].getClass().getComponentType(), len);
		int s = 0;
		for (A[] e : a) {
			System.arraycopy(e, 0, res, s, e.length);
			s+=e.length;
		}
		return res;
	}

	public static <A> A[] refine(A[] a, Function<A, A> f) {
		return a.length == 0 ? a : map(a, f);
	}

	public static <A, B> B[] map(A[] a, Function<A, B> f) {
		B b0 = f.apply(a[0]);
		@SuppressWarnings("unchecked")
		B[] b = (B[]) java.lang.reflect.Array.newInstance(b0.getClass(), a.length);
		b[0] = b0;
		for (int i = 1; i < a.length; i++)
			b[i] = f.apply(a[i]);
		return b;
	}

	public static <A, B> B[] map(Collection<A> a, Function<A, B> f) {
		Iterator<A> iter = a.iterator();
		if (!iter.hasNext())
			throw new IllegalArgumentException("Source must not be empty");
		B b0 = f.apply(iter.next());
		@SuppressWarnings("unchecked")
		B[] b = (B[]) java.lang.reflect.Array.newInstance(b0.getClass(), a.size());
		b[0] = b0;
		int i = 1;
		while (iter.hasNext())
			b[i++] = f.apply(iter.next());
		return b;
	}

	public static int hashCode(byte[] a, int offset, int length) {
		if (a == null)
			return 0;
		int res = 1;
		for (int i = offset; i < length; i++)
			res = 31 * res + a[i];
		return res;
	}
}
