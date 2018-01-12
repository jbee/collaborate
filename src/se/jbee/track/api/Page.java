package se.jbee.track.api;

import se.jbee.track.model.User;

public abstract class Page {
	/**
	 * The user looking at the {@link #page}.
	 * This is always the user that has authorized himself. 
	 * Not the "as" viewer. 
	 */
	public final User actor;
	
	public final long now;

	public Page(User actor, long now) {
		super();
		this.actor = actor;
		this.now = now;
	}
	
}