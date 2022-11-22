package eu.derzauberer.javautils.observable;

import eu.derzauberer.javautils.events.Event;

/**
 * The observer is called whenever an update happens for the object in
 * which the observer is implemented.
 */
public interface Observer {
	
	/**
	 * The update function of the observer is called whenever an update
	 * happens for the object in which the observer is implemented.
	 */
	void update(Event event);

}
