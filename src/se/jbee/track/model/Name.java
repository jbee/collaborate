package se.jbee.track.model;

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
 */
public final class Name extends Identifier<Name> {

	public static Name fromBytes(byte[] name) {
		return name == null ? null : new Name(name);
	}

	private static final Pattern VALID_NON_EDITABLE = Pattern.compile("(?:[@.][-a-zA-Z0-9_]+)+(?:[.][*~])?");
	private static final Pattern VALID_EDITABLE = Pattern.compile("(?:\\d+(?:[.]\\d+)*)?(?:[-_a-zA-Z0-9]+)?");

	public static final Name ANONYMOUS = as("@anonymous");
	public static final Name MY = as("@my");
	public static final Name MASTER = as("@master");

	public static final Name ORIGIN = new Name(new byte[] {'*'});
	public static final Name UNKNOWN = new Name(new byte[] {'~'});

	private Name(byte[] symbols) {
		super(symbols);
	}

	public static Name as(String name) {
		if ("*".equals(name))
			return ORIGIN;
		if ("~".equals(name))
			return UNKNOWN;
		final int len = name.length();
		if (len <= 16 && VALID_EDITABLE.matcher(name).matches()) {
			return new Name(asciiBytes(name));
		}
		if (len <= 32 && VALID_NON_EDITABLE.matcher(name).matches()) {
			return new Name(asciiBytes(name));
		}
		throw new IllegalArgumentException("Not a valid name: "+name);
	}
	
	/**
	 * @return not editable names cannot be created by user but they might exist,
	 *         e.g. <code>@my</code> to manage common pages.
	 */
	public boolean isNonEditable() {
		return indexOf('@') == 0 || length() == 1;
	}
	
	public boolean isEmail() {
		return indexOf('@') > 0;
	}

	/**
	 * @return editable names can be created by users. An email is an editable name as well.
	 */
	public boolean isEditable() {
		return !isNonEditable();
	}

	public boolean isRegular() {
		return isEditable() && !isEmail();
	}

	public boolean isUnknown() {
		return charAt(0) == '~';
	}

	public boolean isOrigin() {
		return charAt(0) == '*';
	}

	/**
	 * @return this is not the same as external, internal names like @my are ok to display while emails are not.
	 */
	public Name display() {
		return isEmail() ? ANONYMOUS : this;
	}

}
