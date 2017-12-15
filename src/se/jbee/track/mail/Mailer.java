package se.jbee.track.mail;

import se.jbee.track.model.Mail;

/**
 * Abstraction for a "mailing system" (not to confuse with a mail server).
 * 
 * The task of the system is to aggregate mails to the moment when it is fitting to deliver them. 
 */
public interface Mailer {

	/**
	 * Enqueues a new email for delivery as specified. 
	 */
	boolean deliver(Mail mail);
}
