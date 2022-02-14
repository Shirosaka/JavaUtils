package eu.derzauberer.javautils.events;

import eu.derzauberer.javautils.util.Client;
import eu.derzauberer.javautils.util.Event;

public class ClientDisconnectEvent extends Event {
	
	private Client client;
	
	public ClientDisconnectEvent(Client client) {
		this.client = client;
		execute();
	}
	
	public Client getClient() {
		return client;
	}
	
}