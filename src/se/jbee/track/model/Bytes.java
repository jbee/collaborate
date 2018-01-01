package se.jbee.track.model;

import java.util.regex.Pattern;

public abstract class Bytes {

	private static final String TEXT_REGEX = "(?:[-+*a-zA-Z0-9@_\\s\\\\\\$\\^:,;.?!#>=%&`\"'~\\pL\\pN\\(\\)\\[\\]\\{\\}]+|<[^a-zA-Z/]|/[^>])+[</]?";
	private static final Pattern TEXT_ONLY = Pattern.compile("^"+TEXT_REGEX+"$");
	
	public static final String BASIC_TEXT_REGEX = "(?:[-+*a-zA-Z0-9@_\\s:,;.?!#>=%&`\"'~\\pL\\pN\\(\\)]+|<[^a-zA-Z/]|/[^>])+[</]?";
	private static final Pattern BASIC_TEXT_ONLY = Pattern.compile("^"+BASIC_TEXT_REGEX+"$");

	private static final String URL_REGEX = "[0-9a-zA-Z$-_.+!*'(),;/?:@=&#%]+";
	private static final Pattern URL = Pattern.compile("^"+URL_REGEX+"$");

	private static final String EMAIL_REGEX = "[0-9a-zA-Z$-_.+!*'(),;/?:=&#%]+";
	private static final Pattern EMAIL = Pattern.compile("^"+EMAIL_REGEX+"@"+EMAIL_REGEX+"$");
	
	public static boolean isText(String s) {
		return TEXT_ONLY.matcher(s).matches();
	}
	
	public static boolean isBasicText(String s) {
		return BASIC_TEXT_ONLY.matcher(s).matches();
	}
	
	public static boolean isURL(String s) {
		return URL.matcher(s).matches();
	}
	
	public static boolean isEmail(String s) {
		return EMAIL.matcher(s).matches();
	}
	
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
	
	public static byte[] longBytes(long val) {
	    byte[] result = new byte[8];
	    for (int i = 7; i >= 0; i--) {
	        result[i] = (byte)(val & 0xFF);
	        val >>= 8;
	    }
	    return result;
	}

	public static long toLong(byte[] bytes) {
	    long result = 0;
	    for (int i = 0; i < 8; i++) {
	        result <<= 8;
	        result |= (bytes[i] & 0xFF);
	    }
	    return result;
	}	
	
}
