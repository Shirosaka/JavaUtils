package eu.derzauberer.javautils.service;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

import eu.derzauberer.javautils.events.LoggingEvent;

/**
 * The logger collects messages from the {@link #log(String)} methods and send
 * the output with date, type and message to the console and writes it in a
 * logging file if enabled. You can enable the options with
 * {@link #setSystemOutput(boolean)} and {@link #setFileOutput(boolean)}. The
 * use of one of the {@link #log(String)} functions triggers a
 * {@link LoggingEvent}.
 */
public class LoggingService {
	
	/**
	 * Represents the type of logging outputs.
	 */
	public enum LogType {
		INFO,
		WARN,
		ERROR,
		DEBUG
	}
	
	private final String prefix;
	private boolean systemOutput = true;
	private boolean fileOutput = true;
	private boolean enableOutputInformation = true;
	private Path fileDirectory = Path.of("logs");
	private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
	private Consumer<LoggingEvent> loggingAction;

	/**
	 *  Creates a new logging service without a prefix.
	 */
	public LoggingService() {
		this.prefix = "";
	}
	
	/**
	 * Creates a new logging service with a prefix. The prefix will be displayed
	 * between the logging type and the message.
	 * 
	 * @param prefix the prefix that will be displayed between the logging type and
	 *               the message
	 */
	public LoggingService(String  prefix) {
		this.prefix = prefix + ": ";
	}
	
	/**
	 * Creates a new logging service with the name of the class as prefix. The prefix will be displayed
	 * between the logging type and the message.
	 * 
	 * @param classType the class of which the name will be displayed as prefix between the logging type and
	 *               the message
	 */
	public LoggingService(Class<?> classType) {
		prefix = classType.getName() + ": ";
	}
	
	/**
	 * Sends an information message to the logger. The output will not be displayed
	 * if the type is {@link LogType#DEBUG} and the java virtual machine does not run in
	 * debug mode. The default type, if no one is given, is {@link LogType#INFO}.
	 * 
	 * @param message the message to send
	 */
	public void log(String message) {
		log(LogType.INFO, message);
	}
	
	/**
	 * Sends an information message to the logger. The output will not be displayed
	 * if the type is {@link LogType#DEBUG} and the java virtual machine does not
	 * run in debug mode. The default type, if no one is given, is
	 * {@link LogType#INFO}.
	 * 
	 * @param message the message to send
	 * @param args    arguments to insert in the message with the build in
	 *                {@link String#format(String, Object...)} method
	 */
	public void log(String message, Object... args) {
		log(LogType.INFO, String.format(message, args));
	}
	
	/**
	 * Sends a message to the logger. The output will not be displayed
	 * if the type is {@link LogType#DEBUG} and the java virtual machine does not
	 * run in debug mode.
	 * 
	 * @param type    the type of the message
	 * @param message the message to send
	 */
	public void log(LogType type, String message) {
		if (type == LogType.DEBUG && !isDebugEnabled()) return;
		final LocalDateTime timeStamp = LocalDateTime.now();
		final String output = enableOutputInformation ?  "[" + timeStamp.format(dateTimeFormatter) + " " + type + "] " + prefix + message: message;
		if (loggingAction != null) loggingAction.accept(new LoggingEvent(this, type, message, timeStamp, output));
		if (systemOutput) System.out.println(output);
		if (fileOutput) {
			try {
				final Path outputFile = Path.of(fileDirectory.toString(), "log-" + timeStamp.format(DateTimeFormatter.ISO_LOCAL_DATE) + ".txt");
				if (!Files.exists(fileDirectory)) Files.createDirectories(fileDirectory);
				if (!Files.exists(outputFile)) Files.createFile(outputFile);
				Files.writeString(outputFile, output + "\n", StandardOpenOption.APPEND);
			} catch (IOException exception) {
				exception.printStackTrace();
			}
		}
	}
	
	/**
	 * Sends a message to the logger. The output will not be displayed
	 * if the type is {@link LogType#DEBUG} and the java virtual machine does not
	 * run in debug mode.
	 * 
	 * @param type   the type of the message
	 * @param message the message to send
	 * @param args   arguments to insert in the message with the build in
	 *               {@link String#format(String, Object...)} method
	 */
	public void log(LogType type, String message, Object... args) {
		log(type, String.format(message, args));
	}
	
	/**
	 * Returns if java is currently running in debug mode.
	 * 
	 * @return if java is currently running in debug mode
	 */
	public boolean isDebugEnabled() {
		return ManagementFactory.getRuntimeMXBean().getInputArguments().toString().indexOf("-agentlib:jdwp") > 0;
	}
	
	/**
	 * Sets if the logging output should be printed to the standard output.
	 * 
	 * @param systemOutput if the logging output should be printed to the standard
	 *                     output
	 */
	public void setSystemOutput(boolean systemOutput) {
		this.systemOutput = systemOutput;
	}
	
	/**
	 * Returns if the logging output should be printed to the standard output.
	 * 
	 * @return if the logging output should be printed to the standard output
	 */
	public boolean isSystemOutput() {
		return systemOutput;
	}
	
	/**
	 * Sets if the logging output should be printed to a file. The file directory
	 * can be defined with {@link #setFileDirectory(File)}.
	 * 
	 * @param fileOutput if the logging output should be printed to a file
	 */
	public void setFileOutput(boolean fileOutput) {
		this.fileOutput = fileOutput;
	}
	
	/**
	 * Returns if the logging output should be printed to a file. The file directory
	 * can be requested with {@link #getFileDirectory()}.
	 * 
	 * @return if the logging output should be printed to a file
	 */
	public boolean isFileOutput() {
		return fileOutput;
	}
	
	/**
	 * Defines whether the date, type and prefix should be printed in front of the debug output.
	 * 
	 * @param enableOutputInformation if the date, type and prefix should be printed in front of the debug output
	 */
	public void setOutputInformationEnabled(boolean enableOutputInformation) {
		this.enableOutputInformation = enableOutputInformation;
	}
	
	/**
	 * Returns whether the date, type and prefix should be printed in front of the debug output.
	 * 
	 * @return if the date, type and prefix should be printed in front of the debug output
	 */
	public boolean isOutputInformationEnabled() {
		return enableOutputInformation;
	}
	
	/**
	 * Sets the directory in which the files for the logging output should be in.
	 * 
	 * @param fileDirectory the directory in which the files for the logging output
	 *                      should be in
	 */
	public void setFileDirectory(Path fileDirectory) {
		this.fileDirectory = fileDirectory;
	}
	
	/**
	 * Returns the directory in which the files for the logging output should be in.
	 * 
	 * @return the directory in which the files for the logging output should be in
	 */
	public Path getFileDirectory() {
		return fileDirectory;
	}
	
	/**
	 * Sets the format in which the date should be in front of the logging
	 * messages.
	 * 
	 * @param dateTimeFormatter the format in which the date should be in front
	 *                          of the logging messages
	 */
	public void setDateTimeFormatter(DateTimeFormatter dateTimeFormatter) {
		this.dateTimeFormatter = dateTimeFormatter;
	}
	
	/**
	 * Returns the format in which the date should be in front of the logging
	 * messages.
	 * 
	 * @return the format in which the date should be in front of the logging
	 *         messages
	 */
	public DateTimeFormatter getDateTimeFormatter() {
		return dateTimeFormatter;
	}
	
	/**
	 * Sets an action to execute when something is logged.
	 * 
	 * @param outputAction an action to execute when something is logged
	 */
	public void setLoggingAction(Consumer<LoggingEvent> loggingAction) {
		this.loggingAction = loggingAction;
	}
	
	/**
	 * Returns an action to execute when something is logged.
	 * 
	 * @return an action to execute when something is logged
	 */
	public Consumer<LoggingEvent> getLoggingAction() {
		return loggingAction;
	}
	
}
