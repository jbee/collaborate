package vizio;

import static java.util.Arrays.copyOfRange;

import java.util.Arrays;
import java.util.regex.Pattern;

public final class Name implements CharSequence, Comparable<Name> {

	public static Name fromBytes(byte[] name) {
		return new Name(name);
	}

	private static final Pattern VALID = Pattern.compile("@?[a-zA-Z]+(?:(?:-?[a-zA-Z]|[.@][a-zA-Z0-9])?[a-zA-Z0-9]*)*");

	public static final Name ANONYMOUS = as("@anonymous");
	public static final Name MY = as("@my");
	public static final Name MASTER = as("@master");

	public static final Name ORIGIN = new Name(new byte[] {'*'});
	public static final Name UNKNOWN = new Name(new byte[] {'~'});

	private final byte[] symbols;

	private Name(byte[] symbols) {
		super();
		this.symbols = symbols;
	}

	public static Name limit(String name) {
		return as("@limit."+name);
	}

	public static Name limit(String type, Name name) {
		return limit(type+"."+name);
	}

	public static Name as(String name) {
		if ("*".equals(name))
			return ORIGIN;
		if ("~".equals(name))
			return UNKNOWN;
		final int len = name.length();
		if (VALID.matcher(name).matches() && len > 1
			&& (len <= 16 || (len <= 32 && name.indexOf('@') > 0))) {
			return new Name(name.getBytes());
		}
		throw new IllegalArgumentException("Not a valid name: "+name);
	}

	/**
	 * @return internal names cannot be created by user but they might exist,
	 *         e.g. <code>@my</code> to manage common pages.
	 */
	public boolean isInternal() {
		return !isExternal();
	}

	/**
	 * @return external names can be created by users.
	 */
	public boolean isExternal() {
		return indexOf('@') < 0;
	}

	private int indexOf(char c) {
		for (int i = 0; i < symbols.length; i++) {
			if (symbols[i] == c)
				return i;
		}
		return -1;
	}

	public boolean isUnknown() {
		return symbols[0] == '~';
	}

	public boolean isOrigin() {
		return symbols[0] == '*';
	}

	public Name display() {
		// this is not the same as external, internal names like @my are ok to display while emails are not.
		int idx = indexOf('@');
		return idx == 0 ? subSequence(1, symbols.length) : idx > 0 ? ANONYMOUS : this;
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

	public byte[] bytes() {
		return symbols;
	}
}
