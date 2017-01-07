package vizio.engine;

import java.util.Arrays;

import vizio.model.Name;

public final class Limit implements Comparable<Limit> {

	private final byte[] symbols;
	
	private Limit(String symbols) {
		super();
		this.symbols = symbols.getBytes();
	}

	public static Limit limit(String type, Name name) {
		return new Limit(".limit."+type+"."+name);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Limit && equalTo((Limit) obj);
	}

	public boolean equalTo(Limit other) {
		return this == other || Arrays.equals(symbols, other.symbols);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(symbols);
	}
	
	public int factor() {
		byte last = symbols[symbols.length-1];
		if (last == '*')
			return 10;
		if (last == '~')
			return 5;
		return 1;
	}
	
	public boolean isSpecific() {
		return factor() == 1;
	}

	@Override
	public int compareTo(Limit other) {
		if (this == other)
			return 0;
		return new String(symbols).compareTo(new String(other.symbols));
	}

	@Override
	public String toString() {
		return new String(symbols);
	}
}
