package se.jbee.task.util;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class Log {

	private final Logger logger;

	public Log(Logger logger) {
		this.logger = logger;
	}

	public static Log forClass(Class<?> owner) {
		return new Log(Logger.getLogger(owner.getName()));
	}

	public boolean logsWarn() {
		return logger.isLoggable(Level.WARNING);
	}

	public boolean logsInfo() {
		return logger.isLoggable(Level.INFO);
	}

	public boolean logsDebug() {
		return logger.isLoggable(Level.FINE);
	}

	public boolean logsError() {
		return logger.isLoggable(Level.FINE);
	}

	public void debug(String msg) {
		logger.fine(msg);
	}

	public void warn(String msg) {
		logger.warning(msg);
	}

	public void info(String msg) {
		logger.info(msg);
	}

	public void error(String msg) {
		logger.severe(msg);
	}
}
