package vizio.model;

import com.sun.java_cup.internal.runtime.Symbol;


/**
 * A (database wide) unique identifier.
 */
public final class ID extends Identifier<ID> {

	private static final byte[] DIVIDER = {'#'};

	public enum Type {
	
		User, Site, Product, Area, Version, Task, Poll;
		
		final byte[] symbol;

		private Type() {
			this.symbol = new byte[] { (byte) name().toLowerCase().charAt(0) };
		}

		public static Type fromSymbol(byte s) {
			for (Type t : values()) {
				if (t.symbol[0] == s)
					return t;
			}
			throw new IllegalArgumentException("No type for symbol: "+(char)s);
		}
		
	}

	public final Type type;
	
	private ID(Type type, byte[] symbols) {
		super(symbols);
		this.type = type;
	}

	private static ID id(Type type, Name name, Name... names) {
		byte[] id = join(type.symbol, DIVIDER, name.bytes());
		for (Name n : names) {
			id = join(id, DIVIDER, n.bytes());
		}
		return new ID(type, id);
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
	
	public static ID fromBytes(byte[] bytes) {
		return new ID(Type.fromSymbol(bytes[0]), bytes);
	}

	public boolean isUnique() {
		return charAt(length()-1) != '*';
	}
}
