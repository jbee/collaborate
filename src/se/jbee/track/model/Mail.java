package se.jbee.track.model;


public final class Mail {

	@UseCode
	public static enum Subject { confirmation, information }
	
	@UseCode
	public static enum Delivery { promptly, shortly, hourly, daily, weekly, never }
	
	public final Delivery method;
	public final Email to;
	public final Subject subject;
	public final String text;
	
	public Mail(Delivery method, Email to, Subject subject, String text) {
		super();
		this.method = method;
		this.to = to;
		this.subject = subject;
		this.text = text;
	}
	
}
