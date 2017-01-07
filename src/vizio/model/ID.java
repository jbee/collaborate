package vizio.model;

import java.util.Arrays;

/**
 * A (database wide) unique identifier.
 * 
 * @author jan
 */
public final class ID implements Comparable<ID> {

	public enum Type {
	
		User, Site, Product, Area, Version, Task, Poll
	}

	public final Type type;
	private final byte[] symbols;
	
	private ID(Type type, byte[] symbols) {
		super();
		this.symbols = symbols;
		this.type = type;
	}

	private static ID id(Type type, Name name, Name... names) {
		String id = "."+type.name()+"."+name;
		for (Name n : names) {
			id += "."+n;
		}
		return new ID(type, id.getBytes());
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

	public static ID productId(Name product) {
		return id(ID.Type.Product, product);
	}

	public static ID pollId(Name product, Name area, IDN serial) {
		return id(ID.Type.Poll, product, area, serial.asName());
	}

	public static ID siteId(Name owner, Name name) {
		return id(ID.Type.Site, owner, name);
	}

	public static ID userId(Name name) {
		return id(ID.Type.User, name);
	}

	public static ID areaId(Name product, Name area) {
		return id(ID.Type.Area, product, area);
	}

	public static ID versionId(Name product, Name version) {
		return id(ID.Type.Version, product, version);
	}

	public static ID taskId(Name product, IDN id) {
		return ID.id(ID.Type.Task, product, id.asName());
	}
}
