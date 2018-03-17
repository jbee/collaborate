package se.jbee.track.api;

import se.jbee.track.cache.Matches;
import se.jbee.track.model.Page;
import se.jbee.track.model.Template;
import se.jbee.track.model.User;

/**
 * The content of a {@link ListView} is controlled by the {@link #page}'s
 * {@link Template} and the queries contained in it.
 */
public final class ListView extends View {

	/**
	 * Related pages shown as menu
	 */
	public final Page[] menu;
	/**
	 * The template shown
	 */
	public final Page page;
	/**
	 * Query results in order of occurrence in the {@link Template} of the
	 * {@link #page}.
	 */
	public final Matches[] results;

	public ListView(User actor, long now, Page[] menu, Page page, Matches... results) {
		super(actor, now);
		this.menu = menu;
		this.page = page;
		this.results = results;
	}

}