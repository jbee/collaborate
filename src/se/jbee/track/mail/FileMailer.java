package se.jbee.track.mail;

import static se.jbee.track.model.Mail.Objective.confirmation;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLongArray;

import org.eclipse.jetty.util.ConcurrentHashSet;

import se.jbee.track.model.Mail;
import se.jbee.track.model.Mail.Delivery;

public class FileMailer implements Mailer {

	private static final Set<File> runningMailers = new ConcurrentHashSet<>();

	private final Mailer delegate;
	private final ArrayBlockingQueue<Mail> confirmations = new ArrayBlockingQueue<>(20);
	private final ArrayBlockingQueue<Mail> informations = new ArrayBlockingQueue<>(20);
	private final AtomicLongArray oldestFilesForDelivery = new AtomicLongArray(Delivery.values().length);
	private final File mailDir;
	private final File ageDir;
	private final File lockDir;
	private final AtomicBoolean run = new AtomicBoolean(true);

	public FileMailer(File dataDir, Mailer delegate) {
		checkDir(dataDir);
		runningMailers.add(dataDir);
		this.delegate = delegate;
		this.mailDir = new File(dataDir, "mail");
		this.ageDir  = new File(dataDir, "age");
		this.lockDir = new File(dataDir, "lock");
		mkdir(mailDir);
		mkdir(lockDir);
		mkdir(ageDir);
		clearLocks(lockDir);
		new Thread(this::mail2file).start();
		new Thread(this::file2mail).start();
	}

	private static void checkDir(File dataDir) {
		if (runningMailers.contains(dataDir))
			throw new IllegalStateException("Mailer already running for directory "+dataDir);
		for (File dir : runningMailers) {
			if (dir.getAbsolutePath().startsWith(dataDir.getAbsolutePath()))
				throw new IllegalStateException("Cannot start a mailer in a subdirectory of another mailer");
		}
	}

	@Override
	protected void finalize() {
		run.set(false);
		runningMailers.remove(mailDir.getParentFile());
	}

	private static void mkdir(File dir) {
		if (!dir.exists())
			dir.mkdirs();
	}

	private static void clearLocks(File dir) {
		for (File f : dir.listFiles()) {
			if (f.isFile() && f.getName().endsWith(".lock"))
				f.delete();
		}
	}

	/**
	 * Step 1: add files to input queues.
	 *
	 * (This will be called by worker threads that process requests)
	 */
	@Override
	public boolean deliver(Mail mail) {
		if (mail.objective == confirmation) {
			try {
				confirmations.put(mail);
				return true;
			} catch (InterruptedException e) {
				return false;
			}
		}
		try {
			return informations.offer(mail, 1, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			return false;
		}
	}

	/**
	 * Step 2: Main loop of the thread that writes incoming mails to files.
	 */
	private void mail2file() {
		while (run.get()) {

		}
	}

	/**
	 * Step 3: Main loop of the thread that reads files to send them (outgoing mails).
	 */
	private void file2mail() {
		while (run.get()) {

		}
	}

	private static boolean tryToLock(File lock) {
		try {
			return lock.createNewFile();
		} catch (IOException e) {
			return false;
		}
	}

	private static boolean unlock(File lock) {
		return lock.delete();
	}

	private void appendToFile(Mail mail) {
		try {
			Files.write(mailFileFor(mail).toPath(),
					mail.text.getBytes(StandardCharsets.UTF_8),
					StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private File mailFileFor(Mail mail) {
		return new File(mailDir, mail.to.toString()+".mail");
	}

	private File lockFileFor(Mail mail) {
		return new File(lockDir, mail.to.toString()+".lock");
	}

	private File ageFileFor(Mail mail) {
		return new File(ageDir, mail.to.toString()+".age");
	}

}
