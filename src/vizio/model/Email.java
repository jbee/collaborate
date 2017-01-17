package vizio.model;

import java.nio.charset.StandardCharsets;

public final class Email extends Bytes implements Comparable<Email> {

	public static Email email(String email) {
		//TODO check
		return new Email(email.getBytes(StandardCharsets.US_ASCII));
	}
	
	public static Email fromBytes(byte[] email) {
		return email == null ? null : new Email(email);
	}
	
	private final byte[] email;
	
	private Email(byte[] email) {
		super();
		this.email = email;
	}

	@Override
	public byte[] bytes() {
		return email;
	}

	@Override
	public int compareTo(Email other) {
		return this == other ? 0 : compare(email, other.email);
	}
	
	@Override
	public String toString() {
		return new String(email, StandardCharsets.US_ASCII);
	}

}
