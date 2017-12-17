package se.jbee.track.model;

import static java.nio.charset.StandardCharsets.UTF_16BE;

public final class Gist extends Bytes implements Comparable<Gist> {

	private final byte[] text;

	public static Gist gist(String gist) {
		if (gist.length() >= 256) {
			throw new IllegalArgumentException("Gist is too long, maximal 256 characters: "+gist);
		}
		if (!BASIC_TEXT_ONLY.matcher(gist).matches()) {
			throw new IllegalArgumentException("Gist can only use letters, digits, space and common punctuation marks: "+gist);
		}
		return new Gist(gist.getBytes(UTF_16BE));
	}
	
	public static Gist fromBytes(byte[] utf16Symbols) {
		if (utf16Symbols == null)
			return null;
		if (utf16Symbols.length >= 512) {
			throw new IllegalArgumentException("Gist is too long, maximal 256 characters: "+utf16Symbols);
		}
		return new Gist(utf16Symbols);
	}
	
	private Gist(byte[] utf16Symbols) {
		super();
		this.text = utf16Symbols;
	}

	@Override
	public String toString() {
		return new String(text, UTF_16BE);
	}

	@Override
	public byte[] bytes() {
		return text;
	}

	@Override
	public int compareTo(Gist other) {
		return compare(text, other.text);
	}

	public boolean contains(Gist other) {
		byte[] part = other.text;
		int lp = part.length;
		int lt = text.length;
		if (lp > lt)
			return false;
		byte first = part[0];
		int i = 0;
		while (i < lt) {
			while (i < lt && text[i] != first) {
				if (i+lp > lt)
					return false;
				i++;
			}
			if (i < lt) {
				int j = 0;
				while (j < lp && i < lt && text[i++] == part[j++]);
				if (j == lp && text[i-1] == part[j-1])
					return true;
			}
		}
		return false;
	}
}
