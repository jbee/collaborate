package vizio.model;

/**
 * A util to work with bytes. 
 */
public final class Bytes {

	public static byte[] asciiBytes(String ascii) {
		byte[] res = new byte[ascii.length()];
		for (int i = 0; i < res.length; i++) {
			res[i] = (byte) ascii.charAt(i);
		}
		return res;
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
}
