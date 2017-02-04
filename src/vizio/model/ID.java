package vizio.model;

import static java.util.Arrays.copyOfRange;
import vizio.util.PersistedData;

/**
 * A (database wide) unique identifier.
 */
public final class ID extends Identifier<ID> {

	private static final byte[] DIVIDER = {'#'};

	public enum Type {
		// core domain
		User, Site, Product, Area, Version, Task, Poll, 
		// meta
		Event, History;

		@PersistedData
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
	
	public static ID eventId(long timestamp) {
		// we just use the long bytes - a 8 bytes long key with no second byte similar to # is an event
		return new ID(Type.Event, longBytes(timestamp));
	}
	
	public static ID historyId(ID entity) {
		return entity.type == Type.History ? entity : new ID(Type.History, join(Type.History.symbol, DIVIDER, entity.bytes()));
	}
	
	public static ID fromBytes(byte[] bytes) {
		if (bytes.length == 8 && bytes[1] != '#') {
			return new ID(Type.Event, bytes);
		}
		return new ID(Type.fromSymbol(bytes[0]), bytes);
	}
	
	public boolean isUnique() {
		return charAt(length()-1) != '*';
	}
	
	public ID entity() {
		return type == Type.History ? fromBytes(copyOfRange(bytes(), 2, bytes().length)) : this;
	}
	
	@Override
	public String toString() {
		return type == Type.Event ? String.valueOf(toLong(bytes())) : super.toString();
	}
}
