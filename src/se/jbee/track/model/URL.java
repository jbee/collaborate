package se.jbee.track.model;

public final class URL extends Identifier<URL> {

	private URL(byte[] symbols) {
		super(symbols);
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

	/**
	 * @return true if this URL starts with <code>name:</code> for a integration
	 *         instead of being a usual absolute URL.
	 */
	public boolean isIntegrated() {
		byte[] bs = bytes();
		for (int i = 0; i < bs.length; i++) {
			byte c = bs[i];
			if (c == ':' && i > 0)
				return i < bs.length-1 && bs[i+1] != '/';
			if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '_' || c == '-' || c >= '0' && c <= '9'))
				return false;
		}
		return false; 
	}
	
}
