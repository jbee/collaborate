package se.jbee.track.cache;

import java.util.concurrent.Future;

import se.jbee.track.engine.Changes;
import se.jbee.track.model.Criteria;
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
}
