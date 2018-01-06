package se.jbee.track.ui.ctrl;

import se.jbee.track.cache.Matches;
import se.jbee.track.model.Site;
import se.jbee.track.model.Template;
import se.jbee.track.model.User;

public interface Ctrl {
	
	DynamicPage query(Params params);
	
	/**
	 * The content of a dynamic page is controlled by the {@link #page}
	 * {@link Template} and the queries contained in it.
	 */
	class DynamicPage {  
		/**
		 * The user looking at the {@link #page}.
		 * This is always the user that has authorized himself. 
		 * Not the "as" viewer. 
		 */
		public User actor;
		/**
		 * Related sites shown as menu
		 */
		public Site[] menu;
		/**
		 * The template shown
		 */
		public Site page;
		/**
		 * Query results in order of occurrence in the {@link Template} of the
		 * {@link #page}.
		 */
		public Matches[] results; 
	}
}
