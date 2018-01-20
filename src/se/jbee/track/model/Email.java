package se.jbee.track.model;


public final class Email extends Identifier<Email>  {

	/**
	 * A constant used as the administrators email in case no one should be the
	 * administrator.
	 *
	 * This object is an illegal email that cannot be created using
	 * {@link #email(String)} so no user email can be {@link #equalTo(Email)} it
	 * and thereby nobody is considered the administrator.
	 */
	public static final Email NO_ADMIN = fromBytes("@".getBytes());

	public static Email email(String email) {
		if (!isEmail(email))
			throw new IllegalArgumentException(email);
		return new Email(asciiBytes(email));
	}

	public static Email fromBytes(byte[] email) {
		return email == null ? null : new Email(email);
	}

	private Email(byte[] email) {
		super(email);
	}

}
