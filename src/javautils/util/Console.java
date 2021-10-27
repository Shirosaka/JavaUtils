package javautils.util;

import java.io.File;
import java.util.Scanner;
import javauitls.events.ConsoleInputEvent;
import javauitls.events.ConsoleOutputEvent;
import javautils.action.ConsoleOutputAction;
import javautils.handler.CommandHandler;
import javautils.handler.EventHandler;
import javautils.handler.FileHandler;

public class Console implements Runnable {

	private Thread thread;
	private String prefix;
	private File directory;
	private ConsoleOutputAction outputAction;
	
	
	public Console() {
		this(true);
	}
	
	public Console(boolean start) {
		if (start) startConsole();
		directory = FileHandler.getJarDirectory();
		prefix = "";
		outputAction = output -> System.out.println(output);
	}
	
	public Console(String prefix) {
		this(prefix, true);
	}
	
	public Console(String prefix, boolean start) {
		if (start) startConsole();
		directory = FileHandler.getJarDirectory();
		this.prefix = prefix;
		outputAction = output -> System.out.println(output);
	}
	
	public void startConsole() {
		thread = new Thread(this);
		thread.start();
	}

	public void stopConsole() {
		thread.interrupt();
	}
	
	@SuppressWarnings("resource")
	@Override
	public void run() {
		Scanner scanner = new Scanner(System.in);
		String input;
		while (!thread.isInterrupted()) {
			System.out.print(prefix + "~ ");
			input = scanner.nextLine();
			sendInput(input);
		}
	}

	public  void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public String getPrefix() {
		return prefix;
	}
	
	public File getDirectory() {
		return directory;
	}
	
	public void setDirectory(File directory) {
		this.directory = directory;
	}
	
	public void setOutputAction(ConsoleOutputAction outputAction) {
		this.outputAction = outputAction;
	}
	
	public void sendInput(String string) {
		ConsoleInputEvent event = new ConsoleInputEvent(this, string);
		EventHandler.executeEvent(ConsoleInputEvent.class, event);
		if(!event.isCancled()) {
			CommandHandler.executeCommand(event.getConsole(), event.getInput());
		}
	}
	
	private void sendOutput(String output) {
		ConsoleOutputEvent event = new ConsoleOutputEvent(this, output);
		EventHandler.executeEvent(ConsoleOutputEvent.class, event);
		if(!event.isCancled()) {
			outputAction.onAction(event.getOutput());
		}
	}
	
	public void sendMessage(Object object) {
		sendOutput(object.toString());
	}

	public void sendInfoMessage(Object object) {
		sendOutput("[INFO] " + object.toString());
	}

	public void sendWarningMessage(Object object) {
		sendOutput("[WARNING] " + object.toString());
	}

	public void sendErrorMessage(Object object) {
		sendOutput("[ERROR] " + object.toString());
	}
	
	

}
