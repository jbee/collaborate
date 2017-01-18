package vizio.model;

import java.nio.charset.StandardCharsets;

public final class Gist extends Bytes implements Comparable<Gist> {

	private final byte[] string;

	public static Gist gist(String gist) {
		if (gist.length() >= 256) {
			throw new IllegalArgumentException("Gist is too long, maximal 256 characters: "+gist);
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
		this.string = utf16Symbols;
	}

	@Override
	public String toString() {
		return new String(string, StandardCharsets.UTF_16);
	}

	@Override
	public byte[] bytes() {
		return string;
	}

	@Override
	public int compareTo(Gist other) {
		return 0;
	}
}
