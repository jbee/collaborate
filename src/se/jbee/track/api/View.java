package se.jbee.track.api;

import se.jbee.track.model.User;

public abstract class View {
	/**
	 * The user looking at the {@link #query}.
	 * This is always the user that has authorized himself.
	 * Not the "as" viewer.
	 */
	public final User actor;

	public final long now;

	public View(User actor, long now) {
		this.actor = actor;
		this.now = now;
	}

}