package se.jbee.track.model;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Base class for all the different types of identifiers.
 * 
 * The assumption is that on ASCII characters are used!
 *  
 * @param <T> actual type of the identifier
 */
public abstract class Identifier<T extends Identifier<T>> extends Bytes implements CharSequence, Comparable<T>  {

	private final byte[] symbols;
	
	protected Identifier(byte[] symbols) {
		super();
		if (symbols == null || symbols.length == 0)
			throw new IllegalArgumentException("must not be empty");
		this.symbols = symbols;
	}

	public final int indexOf(char c) {
		for (int i = 0; i < symbols.length; i++) {
			if (symbols[i] == c)
				return i;
		}
		return -1;
	}
	
	@Override
	public final int length() {
		return symbols.length;
	}
	
	@Override
	public final char charAt(int index) {
		return (char) symbols[index];
	}
	
	@Override
	public final CharSequence subSequence(int start, int end) {
		throw new UnsupportedOperationException("Identifiers cannot be changed!");
	}

	@Override
	@SuppressWarnings("unchecked")
	public final boolean equals(Object obj) {
		return obj != null && obj.getClass() == getClass() && equalTo((T) obj);
	}
	
	public final boolean equalTo(T other) {
		return this == other || Arrays.equals(symbols, other.bytes());
	}
	
	@Override
	public final int hashCode() {
		return Arrays.hashCode(symbols);
	}
	
	@Override
	public final int compareTo(T other) {
		return this == other ? 0 : Bytes.compare(symbols, other.bytes());
	}
	
	@Override
	public String toString() {
		return new String(symbols, StandardCharsets.US_ASCII);
	}
	
	@Override
	public final byte[] bytes() {
		return symbols;
	}
	
}
