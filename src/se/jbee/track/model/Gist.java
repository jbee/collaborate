package se.jbee.track.model;

import static java.nio.charset.StandardCharsets.UTF_16BE;

import java.util.Arrays;
import java.util.regex.Pattern;

public final class Gist implements ByteSequence<Gist> {

	static final String GIST_TEXT_REGEX = "(?:[-+*a-zA-Z0-9@_\\s:,;.?!#>=%&`\"'~\\pL\\pN\\(\\)]+|<[^a-zA-Z/]|/[^>])+[</]?";
	private static final Pattern GIST_TEXT = Pattern.compile("^"+GIST_TEXT_REGEX+"$");

	public static boolean isGistText(String s) {
		return GIST_TEXT.matcher(s).matches();
	}

	public static final Gist EMPTY = new Gist(new byte[0]);

	private final byte[] text;

	public static Gist gist(String gist) {
		if (gist == null || gist.isEmpty())
			return EMPTY;
		if (gist.length() >= 256)
			throw new IllegalArgumentException("Gist is too long, maximal 256 characters: "+gist);
		if (!isGistText(gist))
			throw new IllegalArgumentException("Gist can only use letters, digits, space and common punctuation marks: "+gist);
		return new Gist(gist.getBytes(UTF_16BE));
	}

	public static Gist fromBytes(byte[] utf16Symbols) {
		if (utf16Symbols == null || utf16Symbols.length == 0)
			return EMPTY;
		if (utf16Symbols.length >= 512)
			throw new IllegalArgumentException("Gist is too long, maximal 256 characters: "+utf16Symbols);
		return new Gist(utf16Symbols);
	}

	private Gist(byte[] utf16Symbols) {
		super();
		this.text = utf16Symbols;
	}

	@Override
	public byte[] readonlyBytes() {
		return text;
	}

	@Override
	public String toString() {
		return new String(text, UTF_16BE);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(text);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Gist && equalTo((Gist) obj);
	}

	public boolean contains(Gist section) {
		return ByteSequence.contains(this, section);
	}
}
