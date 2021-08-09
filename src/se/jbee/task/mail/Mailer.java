package se.jbee.task.mail;

import se.jbee.task.model.Mail;

/**
 * Abstraction for a "mailing system" (not to confuse with a mail server).
 *
 * The task of the system is to aggregate mails to the moment when it is fitting to deliver them.
 */
@FunctionalInterface
public interface Mailer {

	/**
	 * Enqueues a new email for delivery as specified.
	 */
	boolean deliver(Mail mail);
}
