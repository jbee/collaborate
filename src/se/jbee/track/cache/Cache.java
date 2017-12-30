package se.jbee.track.cache;

import java.util.concurrent.Future;

import se.jbee.track.engine.Changes;
import se.jbee.track.model.Names;
import se.jbee.track.model.Task;
import se.jbee.track.model.User;

/**
 * The {@link Cache} is a fast lookup mechanism for computing lists of
 * {@link Task}s with certain properties described by {@link Criteria}.
 * 
 * Given a list of {@link Criteria} the cache delivers a result set of
 * {@link Matches}. The data is consistent but not necessarily updated with all
 * changes already persisted.
 * 
 * The {@link #invalidate(Changes)} method is used to update the cache with
 * changes that already have occurred (are persisted).
 */
public interface Cache {

	/**
	 * Note: To cause indexing of a specific product use
	 * {@link Criteria#index(se.jbee.track.model.Name)}.
	 */
	Future<Matches> matchesFor(User inquirer, Criteria criteria);
	
	Future<Void> invalidate(Changes changes);

	void shutdown();
	
	final class Matches {
		
		public static Matches none() {
			return new Matches(new Task[0], 0);
		}
		
		public final Task[] tasks;
		public final int total;
		/**
		 * The set of products that were not included in the matches even though
		 * the user has an affiliation with them. Usually the reason is that a
		 * product is not indexed yet. This has to be requested by the user
		 * explicitly.
		 */
		public final Names excludedProducts;
		
		public Matches(Task[] matches, int totalMatches) {
			this(matches, totalMatches, Names.empty());
		}
		private Matches(Task[] matches, int totalMatches, Names excludedProducts) {
			super();
			this.tasks = matches;
			this.total = totalMatches;
			this.excludedProducts = excludedProducts;
		}
		
		public Matches exlcuded(Names products) {
			return new Matches(tasks, total, products);
		}
	}
}
