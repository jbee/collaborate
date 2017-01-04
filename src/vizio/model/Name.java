package vizio.model;

import static java.util.Arrays.copyOfRange;

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Valid editable names:
 * <pre>
 * x1
 * simple
 * two-part
 * three-part-word
 * me42
 * </pre>
 * 
 * Illegal editable names:
 * <pre>
 * x
 * </pre>
 * 
 * Valid non editable names:
 * <pre>
 * *
 * ~
 * @foo
 * @foo.*
 * @foo.~
 * </pre>
 * 
 * @author jan
 *
 */
public final class Name implements CharSequence, Comparable<Name> {

	public static Name fromBytes(byte[] name) {
		return new Name(name);
	}

	private static final Pattern VALID_NON_EDITABLE = Pattern.compile("(?:[@.][-a-zA-Z0-9_]+)+(?:[.][*~])?");
	private static final Pattern VALID_EDITABLE = Pattern.compile("(?:\\d+(?:[.]\\d+)*)?(?:[-_a-zA-Z0-9]+)?");

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

	public static Name limit(String type, Name name) {
		return as("@limit."+type+"."+name);
	}

	public static Name as(String name) {
		if ("*".equals(name))
			return ORIGIN;
		if ("~".equals(name))
			return UNKNOWN;
		final int len = name.length();
		if (len <= 16 && VALID_EDITABLE.matcher(name).matches()) {
			return new Name(name.getBytes());
		}
		if (len <= 32 && VALID_NON_EDITABLE.matcher(name).matches()) {
			return new Name(name.getBytes());
		}
		throw new IllegalArgumentException("Not a valid name: "+name);
	}
	
	private int indexOf(char c) {
		for (int i = 0; i < symbols.length; i++) {
			if (symbols[i] == c)
				return i;
		}
		return -1;
	}

	/**
	 * @return not editable names cannot be created by user but they might exist,
	 *         e.g. <code>@my</code> to manage common pages.
	 */
	public boolean isNonEditable() {
		return indexOf('@') == 0 || symbols.length == 1;
	}
	
	public boolean isEmail() {
		return indexOf('@') > 0;
	}

	/**
	 * @return editable names can be created by users.
	 */
	public boolean isEditable() {
		return !isNonEditable();
	}

	public boolean isRegular() {
		return isEditable() && !isEmail();
	}

	public boolean isUnknown() {
		return symbols[0] == '~';
	}

	public boolean isOrigin() {
		return symbols[0] == '*';
	}

	/**
	 * @return this is not the same as external, internal names like @my are ok to display while emails are not.
	 */
	public Name display() {
		return isEmail() ? ANONYMOUS : this;
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
