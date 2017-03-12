package vizio.mail;

import java.util.concurrent.ArrayBlockingQueue;

import vizio.model.Mail;

public class FileMailer {

	private ArrayBlockingQueue<Mail> queries = new ArrayBlockingQueue<>(20);
}
