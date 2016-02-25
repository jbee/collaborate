package vizio;

import static java.util.Arrays.copyOfRange;

import java.util.Arrays;
import java.util.regex.Pattern;

public final class Name implements CharSequence, Comparable<Name> {

	private static final Pattern VALID = Pattern.compile("[a-zA-Z]+(?:[-.](a-zA-Z)+)*");

	private final byte[] symbols;

	private Name(byte[] symbols) {
		super();
		this.symbols = symbols;
	}

	public static Name named(String name) {
		if (VALID.matcher(name).matches()) {
			return new Name(name.getBytes());
		}
		throw new IllegalArgumentException("Not a valid name: "+name);
	}

	@Override
	public int length() {
		return symbols.length;
	}

	@Override
	public char charAt(int index) {
		return (char) symbols[index];
	}

	@Override
	public Name subSequence(int start, int end) {
		return new Name(copyOfRange(symbols, start, end));
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Name && equalTo((Name) obj);
	}

	public boolean equalTo(Name other) {
		return this == other || Arrays.equals(symbols, other.symbols);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(symbols);
	}

	@Override
	public int compareTo(Name other) {
		if (this == other)
			return 0;
		return new String(symbols).compareTo(new String(other.symbols));
	}

	@Override
	public String toString() {
		return new String(symbols);
	}
}
