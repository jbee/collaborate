package se.jbee.task.model;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public interface ByteSequence<T extends ByteSequence<T>> extends CharSequence, Comparable<T> {

	/**
	 * @return the underlying raw bytes. These must not be changed. For efficiency
	 *         reasons this is not necessarily a copy or otherwise computed array.
	 */
	byte[] readonlyBytes();

	default Charset charset() {
		return StandardCharsets.US_ASCII;
	}

	default boolean isEmpty() {
		return readonlyBytes().length == 0;
	}

	default int indexOf(char c) {
		byte[] seq = readonlyBytes();
		for (int i = 0; i < seq.length; i++) {
			if (seq[i] == c)
				return i;
		}
		return -1;
	}

	@Override
	default int length() {
		return readonlyBytes().length;
	}

	@Override
	default char charAt(int index) {
		return (char) readonlyBytes()[index];
	}

	@Override
	default ByteSequence<?> subSequence(int start, int end) {
		throw new UnsupportedOperationException(getClass().getSimpleName()+"s cannot be changed!");
	}

	@Override
	default int compareTo(T other) {
		return this == other ? 0 : compare(this, other);
	}

	default boolean equalTo(T other) {
		return this == other || other != null && Arrays.equals(readonlyBytes(), other.readonlyBytes());
	}

	default boolean startsWith(ByteSequence<?> prefix) {
		byte[] textbody = readonlyBytes();
		byte[] searchterm = prefix.readonlyBytes();
		if (searchterm.length > textbody.length)
			return false;
		for (int i = 0; i < searchterm.length; i++) {
			if (textbody[i] != searchterm[i])
				return false;
		}
		return true;
	}

	public static int compare(ByteSequence<?> a, ByteSequence<?> b) {
		byte[] sa = a.readonlyBytes();
		byte[] sb = b.readonlyBytes();
		if (sb.length != sa.length)
			return Integer.compare(sa.length, sb.length);
		for (int i = 0; i < sb.length; i++) {
			int res = Byte.compare(sa[i], sb[i]);
			if (res != 0)
				return res;
		}
		return 0;
	}

	public static boolean contains(ByteSequence<?> a, ByteSequence<?> b) {
		return find(a, b) >= 0;
	}

	public static int find(ByteSequence<?> a, ByteSequence<?> b) {
		byte[] searchterm = b.readonlyBytes();
		byte[] textbody = a.readonlyBytes();
		int ls = searchterm.length;
		int lb = textbody.length;
		if (ls > lb)
			return -1;
		byte first = searchterm[0];
		int i = 0;
		while (i < lb) {
			while (i < lb && textbody[i] != first) {
				if (i+ls > lb)
					return -1;
				i++;
			}
			if (i < lb) {
				int j = 0;
				while (j < ls && i < lb && textbody[i++] == searchterm[j++]);
				if (j == ls && textbody[i-1] == searchterm[j-1])
					return i-searchterm.length;
			}
		}
		return -1;
	}
}
