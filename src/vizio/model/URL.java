package vizio.model;


public final class URL extends Identifier<URL> {

	public static final URL[] NONE = new URL[0];

	private URL(byte[] symbols) {
		super(symbols);
	}

	public static URL fromBytes(byte[] symbols) {
		return symbols == null || symbols.length == 0 ? null : new URL(symbols);
	}

}
