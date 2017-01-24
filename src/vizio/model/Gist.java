package vizio.model;

import java.nio.charset.StandardCharsets;

public final class Gist extends Bytes implements Comparable<Gist> {

	private final byte[] text;

	public static Gist gist(String gist) {
		if (gist.length() >= 256) {
			throw new IllegalArgumentException("Gist is too long, maximal 256 characters: "+gist);
		}
		if (!BASIC_TEXT_ONLY.matcher(gist).matches()) {
			throw new IllegalArgumentException("Gist can only use letters, digits, space and common punctuation marks: "+gist);
		}
		return new Gist(gist.getBytes(StandardCharsets.UTF_16));
	}
	
	public static Gist gist(byte[] gist) {
		if (gist == null)
			return null;
		if (gist.length >= 512) {
			throw new IllegalArgumentException("Gist is too long, maximal 256 characters: "+gist);
		}
		return new Gist(gist);
	}
	
	private Gist(byte[] utf16Symbols) {
		super();
		this.text = utf16Symbols;
	}

	@Override
	public String toString() {
		return new String(text, StandardCharsets.UTF_16);
	}

	@Override
	public byte[] bytes() {
		return text;
	}

	@Override
	public int compareTo(Gist other) {
		return compare(text, other.text);
	}
}
