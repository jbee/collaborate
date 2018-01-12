package se.jbee.track.api;

import se.jbee.track.cache.Matches;
import se.jbee.track.model.Site;
import se.jbee.track.model.Template;
import se.jbee.track.model.User;

/**
 * The content of a {@link ListPage} is controlled by the {@link #page}'s
 * {@link Template} and the queries contained in it.
 */
public final class ListPage extends Page {  

	/**
	 * Related sites shown as menu
	 */
	public final Site[] menu;
	/**
	 * The template shown
	 */
	public final Site page;
	/**
	 * Query results in order of occurrence in the {@link Template} of the
	 * {@link #page}.
	 */
	public final Matches[] results;
	
	public ListPage(User actor, long now, Site[] menu, Site page, Matches[] results) {
		super(actor, now);
		this.menu = menu;
		this.page = page;
		this.results = results;
	}
	
}