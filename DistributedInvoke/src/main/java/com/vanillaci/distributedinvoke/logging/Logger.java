package com.vanillaci.distributedinvoke.logging;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.util.logging.Level;

/**
 * User: Joel Johnson
 * Date: 3/1/13
 * Time: 11:40 PM
 */
public class Logger {
	private final java.util.logging.Logger logger;

	public static Logger forClass(@NotNull Class cls) {
		return new Logger(java.util.logging.Logger.getLogger(cls.getCanonicalName()));
	}

	private Logger(java.util.logging.Logger logger) {
		this.logger = logger;
	}

	public void error(@NotNull String message, @Nullable Throwable e) {
		if(logger.isLoggable(Level.SEVERE)) {
			String stackTrace = e == null ? "" : "\n" + ExceptionUtils.getFullStackTrace(e);
			logger.severe(message + stackTrace);
		}
	}

	public void error(Throwable e) {
		if(logger.isLoggable(Level.SEVERE)) {
			String stackTrace = e == null ? "" : ExceptionUtils.getFullStackTrace(e);
			logger.severe(stackTrace);
		}
	}

	public void error(String message) {
		logger.severe(message);
	}

	public void warn(String s) {
		logger.warning(s);
	}

	public void warn(String message, Throwable e) {
		if(logger.isLoggable(Level.WARNING)) {
			String stackTrace = e == null ? "" : "\n" + ExceptionUtils.getFullStackTrace(e);
			logger.warning(message + "\n" + stackTrace);
		}
	}

	public void warn(Throwable e) {
		if(logger.isLoggable(Level.WARNING)) {
			String stackTrace = e == null ? "" : ExceptionUtils.getFullStackTrace(e);
			logger.warning(stackTrace);
		}
	}

	public void info(String message) {
		logger.info(message);
	}

	public void infop(String format, Object... args) {
		if(logger.isLoggable(Level.INFO)) {
			info(String.format(format, args));
		}
	}

	public void info(String message, Throwable e) {
		if(logger.isLoggable(Level.INFO)) {
			String stackTrace = e == null ? "" : "\n" + ExceptionUtils.getFullStackTrace(e);
			logger.info(message + "\n" + stackTrace);
		}
	}
}
