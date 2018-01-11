package se.jbee.track.ui.ctrl;

import se.jbee.track.cache.Matches;
import se.jbee.track.model.Site;
import se.jbee.track.model.Template;
import se.jbee.track.model.User;

/**
 * For each type of page there will be a method accepting input parameters.
 *  
 * @author jan
 */
public interface Ctrl {
	// this could also be: interface Controller<P> { P run(Params params); }
	// and than 1 class per type of page/command
	// also there could be a renderer per page type
	
	ListPage list(Params params);
	
	/**
	 * The content of a {@link ListPage} is controlled by the {@link #page}'s
	 * {@link Template} and the queries contained in it.
	 */
	class ListPage {  
		/**
		 * The user looking at the {@link #page}.
		 * This is always the user that has authorized himself. 
		 * Not the "as" viewer. 
		 */
		public final User actor;
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
		
		public ListPage(User actor, Site[] menu, Site page, Matches[] results) {
			this.actor = actor;
			this.menu = menu;
			this.page = page;
			this.results = results;
		}
		
	}
}
