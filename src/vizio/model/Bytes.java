package vizio.model;

import java.util.regex.Pattern;

public abstract class Bytes {

	public static final String FULL_TEXT_REGEX = "[-+*/a-zA-Z0-9\\s\\\\_\\$\\^:,;.?!#<>=%&`\"'~\\pL\\pN\\(\\)\\[\\]\\{\\}]+";
	public static final Pattern FULL_TEXT_ONLY = Pattern.compile("^"+FULL_TEXT_REGEX+"$");
	
	public static final String BASIC_TEXT_REGEX = "[-+*/a-zA-Z0-9\\s_:,;.?!#<>=%&`\"'~\\pL\\pN\\(\\)]+";
	public static final Pattern BASIC_TEXT_ONLY = Pattern.compile("^"+BASIC_TEXT_REGEX+"$");
	
	public abstract byte[] bytes();
	
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
	
	public static int compare(byte[] a, byte[] b) {
		if (b.length != a.length)
			return Integer.compare(a.length, b.length);
		for (int i = 0; i < b.length; i++) {
			int res = Byte.compare(a[i], b[i]);
			if (res != 0)
				return res;
		}
		return 0;		
	}
}
