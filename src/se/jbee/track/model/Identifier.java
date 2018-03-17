package se.jbee.track.model;

import java.util.Arrays;

/**
 * Base class for all the different types of identifiers.
 *
 * The assumption is that on ASCII characters are used!
 *
 * @param <T> actual type of the identifier
 */
public abstract class Identifier<T extends Identifier<T>> implements ByteSequence<T>  {

	private final byte[] symbols;

	protected Identifier(byte[] symbols) {
		super();
		if (symbols == null || symbols.length == 0)
			throw new IllegalArgumentException("must not be empty");
		this.symbols = symbols;
	}

	@Override
	@SuppressWarnings("unchecked")
	public final boolean equals(Object obj) {
		return obj != null && obj.getClass() == getClass() && equalTo((T) obj);
	}

	@Override
	public final int hashCode() {
		return Arrays.hashCode(symbols);
	}

	@Override
	public String toString() {
		return new String(symbols, charset());
	}

	@Override
	public final byte[] readonlyBytes() {
		return symbols;
	}

	public static byte[] join(byte[]...bs) {
		int len = 0;
		for (int i = 0; i < bs.length; i++) {
			len += bs[i].length;
		}
		byte[] res = new byte[len];
		int s = 0;
		for (int i = 0; i < bs.length; i++) {
			final byte[] arr = bs[i];
			final int al = arr.length;
			if (al > 1) {
				System.arraycopy(arr, 0, res, s, al);
			} else {
				res[s] = arr[0];
			}
			s += al;
		}
		return res;
	}

	public static byte[] asciiBytes(String ascii) {
		byte[] res = new byte[ascii.length()];
		for (int i = 0; i < res.length; i++) {
			res[i] = (byte) ascii.charAt(i);
		}
		return res;
	}

}
