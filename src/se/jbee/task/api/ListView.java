package se.jbee.task.api;

import se.jbee.task.cache.Matches;
import se.jbee.task.model.Page;
import se.jbee.task.model.Template;
import se.jbee.task.model.User;

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
	//TODO replace this with a list of rendable items derived by running the page

	public ListView(User actor, long now, Page[] menu, Page page, Matches... results) {
		super(actor, now);
		this.menu = menu;
		this.page = page;
		this.results = results;
	}

}