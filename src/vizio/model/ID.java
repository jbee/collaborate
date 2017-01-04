package vizio.model;

import java.util.Arrays;

import vizio.model.Entity.Type;

/**
 * A (database wide) unique identifier.
 * 
 * @author jan
 */
public final class ID implements Comparable<ID> {

	private final byte[] symbols;
	
	public ID(byte[] symbols) {
		super();
		this.symbols = symbols;
	}

	public static ID id(Type type, Name name, Name... names) {
		String duid = "."+type.name()+"."+name;
		for (Name n : names) {
			duid += "."+n;
		}
		return new ID(duid.getBytes());
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ID && equalTo((ID) obj);
	}

	public boolean equalTo(ID other) {
		return this == other || Arrays.equals(symbols, other.symbols);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(symbols);
	}

	@Override
	public int compareTo(ID other) {
		if (this == other)
			return 0;
		return new String(symbols).compareTo(new String(other.symbols));
	}

	@Override
	public String toString() {
		return new String(symbols);
	}
}
