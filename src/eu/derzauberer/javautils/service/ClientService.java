package eu.derzauberer.javautils.service;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.function.Consumer;
import eu.derzauberer.javautils.events.ClientConnectEvent;
import eu.derzauberer.javautils.events.ClientDisconnectEvent;
import eu.derzauberer.javautils.events.ClientDisconnectEvent.DisconnectCause;
import eu.derzauberer.javautils.events.ClientMessageReceiveEvent;
import eu.derzauberer.javautils.events.ClientMessageSendEvent;
import eu.derzauberer.javautils.util.Sender;

/**
 * This client socket can send and receive messages from
 * a server socket, for example the {@link ServerService}.
 */
public class ClientService implements Sender, Closeable {

	private final Socket socket;
	private final ServerService server;
	private final InputStream input;
	private final OutputStream output;
	private Charset charset;
	private DisconnectCause cause;
	private boolean isDisconnected;
	private boolean nextLineIgnored;
	private Consumer<ClientMessageReceiveEvent> messageReceiveAction;
	private Consumer<ClientMessageSendEvent> messageSendAction;
	private Consumer<ClientConnectEvent> connectAction;
	private Consumer<ClientDisconnectEvent> disconnectAction;

	/**
	 * Creates a new client based on a host address and port.
	 * 
	 * @param host the address of the host server
	 * @param port the port of the host server
	 * @throws UnknownHostException if no connection can be established between
	 *                              client and server, because the host can't be
	 *                              found.
	 * @throws IOException          if an I/O exception occurs
	 */
	public ClientService(String host, int port) throws UnknownHostException, IOException {
		this(new Socket(host, port));
	}

	/**
	 * Creates a new client based on an existing socket.
	 * 
	 * @param socket the existing socket
	 * @throws IOException if an I/O exception occurs
	 */
	public ClientService(Socket socket) throws IOException {
		this(socket, null);
	}

	/**
	 * Creates a new client based on an existing socket and a part of a server.
	 * 
	 * @param socket the existing socket
	 * @param server the server if the socket is a part of a server
	 * @throws IOException if an I/O exception occurs
	 */
	public ClientService(Socket socket, ServerService server) throws IOException {
		this.server = server;
		this.socket = socket;
		input = socket.getInputStream();
		output = socket.getOutputStream();
		charset = Charset.defaultCharset();
		isDisconnected = false;
		nextLineIgnored = false;
		final ClientConnectEvent event = new ClientConnectEvent(this);
		if (connectAction != null) connectAction.accept(event);
		if (isPartOfServer() && server.getConnectAction() != null) server.getConnectAction().accept(event);
		if (!isPartOfServer()) new Thread(this::inputLoop).start();
	}

	/**
	 * Waits for incoming messages from the server socket.
	 */
	protected void inputLoop() {
		String message;
		try {
			while (!isClosed()) {
				message = readLine();
				if (message.equals("null")) break;
				final ClientMessageReceiveEvent event = new ClientMessageReceiveEvent(this, message);
				if (messageReceiveAction != null && !event.isCancelled()) {
					messageReceiveAction.accept(event);
				}
				if (!event.isCancelled() && isPartOfServer() && server.getMessageReceiveAction() != null) {
					server.getMessageReceiveAction().accept(event);
				}
			}
		} catch (SocketTimeoutException exception) {
			cause = DisconnectCause.TIMEOUT;
		} catch (SocketException | NullPointerException exception) {	
		} catch (IOException exception) {
			exception.printStackTrace();
		}
		if (cause == null) cause = DisconnectCause.DISCONNECTED;
		try {
			close();
		} catch (IOException exception) {
			exception.printStackTrace();
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void send(String string) {
		final ClientMessageSendEvent event = new ClientMessageSendEvent(this, string);
		if (messageSendAction != null && !event.isCancelled()) messageSendAction.accept(event);
		if (!event.isCancelled() && isPartOfServer() && server.getMessageSendAction() != null) server.getMessageSendAction().accept(event);
		if (!event.isCancelled()) Sender.super.send(event.getMessage());
	}
	
	/**
	 * Returns the {@link ServerService} if this socket is part of a server,
	 * returns null if not.
	 * 
	 * @return Returns the server of this socket if this socket is part of a server,
	 *         returns null if not
	 */
	public ServerService getServer() {
		return server;
	}
	
	/**
	 * Returns if the client is part of a server.
	 * 
	 * @return if the client is part of a server
	 */
	public boolean isPartOfServer() {
		return server != null;
	}
	
	/**
	 * Returns the address of the client
	 * 
	 * @return the address of the client
	 */
	public InetAddress getAddress() {
		return socket.getInetAddress();
	}

	/**
	 * Returns the port of the client
	 * 
	 * @return the port of the client
	 */
	public int getPort() {
		return socket.getPort();
	}

	/**
	 * Returns the local address of the client
	 * 
	 * @return the local address of the client
	 */
	public InetAddress getLocalAddress() {
		return socket.getLocalAddress();
	}

	/**
	 * Returns the local port of the client
	 * 
	 * @return the local port of the client
	 */
	public int getLocalPort() {
		return socket.getLocalPort();
	}
	
	/**
	 * Sets the timeout of the client. If the client doesn't receive a message
	 * during this time the connection will be closed.
	 * 
	 * @param timeout the timeout of the client in milliseconds
	 */
	public void setTimeout(int timeout) {
		try {
			socket.setSoTimeout(timeout);
		} catch (SocketException exception) {
		}
	}

	/**
	 * Returns the timeout of the client. If the client doesn't receive a message
	 * during this time the connection will be closed.
	 * 
	 * @return the timeout of the client in milliseconds
	 */
	public int getTimeout() {
		try {
			return socket.getSoTimeout();
		} catch (SocketException exception) {
			return 0;
		}
	}
	
	/**
	 * Closes the connection between client and server.
	 * 
	 * @throws IOException if an I/O exception occurs
	 */
	@Override
	public void close() throws IOException {
		if (!isDisconnected) {
			isDisconnected = true;
			socket.close();
			if (cause == null) cause = DisconnectCause.CLOSED;
			final ClientDisconnectEvent event = new ClientDisconnectEvent(this, cause);
		 	if (disconnectAction != null) disconnectAction.accept(event);
		 	if (isPartOfServer() && server.getDisconnectAction() != null) server.getDisconnectAction().accept(event);
			if (isPartOfServer()) server.getClients().remove(this);
		}
	}
	
	/**
	 * Returns if the connection between client and server is closed.
	 * 
	 * @return if the connection between client and server is closed
	 */
	public boolean isClosed() {
		return socket.isClosed();
	}
	
	/**
	 * Sets the charset for the stream that is in use when converting
	 * bytes to strings.
	 * 
	 * @param charset the charset for the stream
	 */
	public void setCharset(Charset charset) {
		this.charset = charset;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Charset getCharset() {
		return charset;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public InputStream getInputStream() {
		return input;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public OutputStream getOutputStream() {
		return output;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setNextLineIgnore(boolean nextLineIgnored) {
		this.nextLineIgnored = nextLineIgnored;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isNextLineIgnored() {
		return nextLineIgnored;
	}

	/**
	 * Sets an action to execute when the socket receives a message.
	 * 
	 * @param messageReceiveAction an action to execute when the socket receives a
	 *                             message
	 */
	public void setMessageReceiveAction(Consumer<ClientMessageReceiveEvent> messageReceiveAction) {
		this.messageReceiveAction = messageReceiveAction;
	}

	/**
	 * Returns an action to execute when the socket receives a message.
	 * 
	 * @return an action to execute when the socket receives a message
	 */
	public Consumer<ClientMessageReceiveEvent> getMessageReceiveAction() {
		return messageReceiveAction;
	}

	/**
	 * Sets an action to execute when the socket sends a message.
	 * 
	 * @param messageSendAction an action to execute when the socket sends a
	 *                             message
	 */
	public void setMessageSendAction(Consumer<ClientMessageSendEvent> messageSendAction) {
		this.messageSendAction = messageSendAction;
	}

	/**
	 * Returns an action to execute when the socket sends a message.
	 * 
	 * @return an action to execute when the socket sends a message
	 */
	public Consumer<ClientMessageSendEvent> getMessageSendAction() {
		return messageSendAction;
	}

	/**
	 * Sets an action to execute when the socket connected to a server.
	 * 
	 * @param connectAction an action to execute when the socket connected to
	 *                             a server
	 */
	public void setConnectAction(Consumer<ClientConnectEvent> connectAction) {
		this.connectAction = connectAction;
	}

	/**
	 * Returns an action to execute when the socket connected to a server.
	 * 
	 * @return an action to execute when the socket connected to a server
	 */
	public Consumer<ClientConnectEvent> getConnectAction() {
		return connectAction;
	}

	/**
	 * Sets an action to execute when the socket disconnected to a server.
	 * 
	 * @param disconnectAction an action to execute when the socket disconnected
	 *                             to a server
	 */
	public void setDisconnectAction(Consumer<ClientDisconnectEvent> disconnectAction) {
		this.disconnectAction = disconnectAction;
	}

	/**
	 * Returns an action to execute when the socket disconnected to a server.
	 * 
	 * @return an action to execute when the socket disconnected to a server
	 */
	public Consumer<ClientDisconnectEvent> getDisconnectAction() {
		return disconnectAction;
	}

}