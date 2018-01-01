package se.jbee.track.model;


public final class Email extends Identifier<Email>  {

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
