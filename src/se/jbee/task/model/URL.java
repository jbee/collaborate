package se.jbee.task.model;

import static java.util.Arrays.copyOfRange;

import java.util.regex.Pattern;

public final class URL extends Identifier<URL> {

	private static final String URL_REGEX = "[0-9a-zA-Z$-_.+!*'(),;/?:@=&#%]+";
	private static final Pattern URL = Pattern.compile("^"+URL_REGEX+"$");

	public static boolean isURL(String s) {
		return URL.matcher(s).matches();
	}

	public static URL url(String url) {
		if (!isURL(url)) {
			throw new IllegalArgumentException(url);
		}
		return new URL(asciiBytes(url));
	}

	public static URL fromBytes(byte[] symbols) {
		return symbols == null || symbols.length == 0 ? null : new URL(symbols);
	}

	private URL(byte[] symbols) {
		super(symbols);
	}

	/**
	 * @return true if this URL starts with <code>name:</code> for a integration
	 *         instead of being a usual absolute URL.
	 */
	public boolean isIntegrated() {
		byte[] bs = readonlyBytes();
		for (int i = 0; i < bs.length; i++) {
			byte c = bs[i];
			if (c == ':' && i > 0)
				return i < bs.length-1 && bs[i+1] != '/';
			if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '_' || c == '-' || c >= '0' && c <= '9'))
				return false;
		}
		return false;
	}

	public URL integrateAs(URL base, Name name) {
		if (!startsWith(base))
			throw new IllegalArgumentException("Cannot integrate for different base URL: "+base);
		return fromBytes(join(name.readonlyBytes(), new byte[] {':'}, copyOfRange(readonlyBytes(), base.length(), length())));
	}

}
