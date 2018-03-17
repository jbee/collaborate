package se.jbee.track.model;

import static java.util.Arrays.copyOfRange;

import java.nio.charset.StandardCharsets;

/**
 * A (database wide) unique identifier.
 */
public final class ID extends Identifier<ID> {

	private static final byte[] DIVIDER = {':'};

	@UseCode("UPOAVTpeh")
	public enum Type {
		// core domain (uses upper case symbols)
		User, Page, Output, Area, Version, Task,

		// support domain (uses lower case symbols)
		poll, event, history;

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

	private static ID id(Type type, Name level1, Name... names) {
		byte[] id = join(type.symbol, DIVIDER, level1.readonlyBytes());
		for (Name n : names) {
			id = join(id, DIVIDER, n.readonlyBytes());
		}
		return new ID(type, id);
	}

	public static ID outputId(Name output) {
		return id(ID.Type.Output, output);
	}

	public static ID pollId(Name output, Name area, IDN serial) {
		return id(ID.Type.poll, output, area, serial.asName());
	}

	/**
	 * A user {@link Page} ID
	 */
	public static ID pageId(Name user, Name name) {
		return pageId(Name.ORIGIN, user, name);
	}

	/**
	 * A area {@link Page} ID
	 */
	public static ID pageId(Name output, Name area, Name name) {
		return id(ID.Type.Page, output, area, name);
	}

	public static ID userId(Name name) {
		return id(ID.Type.User, name);
	}

	public static ID areaId(Name output, Name area) {
		return id(ID.Type.Area, output, area);
	}

	public static ID versionId(Name output, Name version) {
		return id(ID.Type.Version, output, version);
	}

	public static ID taskId(Name output, IDN id) {
		return ID.id(ID.Type.Task, output, id.asName());
	}

	public static ID eventId(long timestamp) {
		// we just use the hex string of the long number - a key without a : is an event
		return new ID(Type.event, Long.toHexString(timestamp).getBytes(StandardCharsets.US_ASCII));
	}

	public static ID historyId(ID entity) {
		return entity.type == Type.history ? entity : new ID(Type.history, join(Type.history.symbol, DIVIDER, entity.readonlyBytes()));
	}

	public static ID fromBytes(byte[] bytes) {
		if (bytes[1] != DIVIDER[0]) {
			return new ID(Type.event, bytes);
		}
		return new ID(Type.fromSymbol(bytes[0]), bytes);
	}

	public ID entity() {
		return type == Type.history ? fromBytes(copyOfRange(readonlyBytes(), 2, readonlyBytes().length)) : this;
	}

	public boolean startsWith(Name name) {
		byte[] a = readonlyBytes();
		byte[] b = name.readonlyBytes();
		if (b.length + 2 < a.length)
			return false;
		for (int i = 0; i < b.length; i++) {
			if (a[i+2] != b[i])
				return false;
		}
		return a.length-2 == b.length || a[b.length+1] == ':';
	}

	@Override
	public String toString() {
		return type == Type.event ? String.valueOf(toLong(readonlyBytes())) : super.toString();
	}

	private static long toLong(byte[] bytes8) {
	    long result = 0;
	    for (int i = 0; i < 8; i++) {
	        result <<= 8;
	        result |= (bytes8[i] & 0xFF);
	    }
	    return result;
	}
}
