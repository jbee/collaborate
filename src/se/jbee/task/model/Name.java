package se.jbee.task.model;

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

	private static final Pattern VALID_INTERNAL = Pattern.compile("(?:[@.][-a-zA-Z0-9_]+)+(?:[.][*~])?");
	private static final Pattern VALID_EXTERNAL = Pattern.compile("(?:\\d+(?:[.]\\d+)*)?(?:[a-zA-Z][-_a-zA-Z0-9]+)?");

	public static final Name ANONYMOUS = as("@anonymous");

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
		if (len <= 16 && VALID_EXTERNAL.matcher(name).matches()) {
			return new Name(asciiBytes(name));
		}
		if (len <= 32 && VALID_INTERNAL.matcher(name).matches()) {
			return new Name(asciiBytes(name));
		}
		if (Email.isEmail(name)) {
			return new Name(asciiBytes(name));
		}
		throw new IllegalArgumentException("Not a valid name: "+name);
	}

	/**
	 * @return internal names cannot be created by user but they might exist,
	 *         e.g. <code>@my</code> to manage common pages.
	 */
	public boolean isInternal() {
		return indexOf('@') == 0 || length() == 1;
	}

	/**
	 * @return external names can be created by users. An email is an external name as well.
	 */
	public boolean isExternal() {
		return !isInternal();
	}

	public boolean isEmail() {
		return indexOf('@') > 0;
	}

	public boolean isRegular() {
		return isExternal() && !isEmail() && !isVersion();
	}

	public boolean isUnknown() {
		return charAt(0) == '~';
	}

	public boolean isOrigin() {
		return charAt(0) == '*';
	}

	public boolean isVersion() {
		return charAt(0) >= '0' && charAt(0) <= '9';
	}

	/**
	 * @return this is not the same as external, internal names like @my are ok to display while emails are not.
	 */
	public Name display() {
		return isEmail() ? ANONYMOUS : this;
	}

}
