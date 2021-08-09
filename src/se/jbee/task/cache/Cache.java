package se.jbee.task.cache;

import java.util.concurrent.Future;

import se.jbee.task.engine.ChangeLog;
import se.jbee.task.model.Criteria;
import se.jbee.task.model.Output;
import se.jbee.task.model.Task;
import se.jbee.task.model.User;

/**
 * The {@link Cache} is a fast lookup mechanism for computing lists of
 * {@link Task}s with certain properties described by {@link Criteria}.
 *
 * Given a list of {@link Criteria} the cache delivers a result set of
 * {@link Matches}. The data is consistent but not necessarily updated with all
 * changes already persisted.
 *
 * The {@link #invalidate(ChangeLog)} method is used to update the cache with
 * changes that already have occurred (are persisted).
 */
public interface Cache extends AutoCloseable {

	/**
	 * Note: To cause indexing of a specific {@link Output} use
	 * {@link Criteria#index(se.jbee.task.model.Name)}.
	 */
	Future<Matches> matchesFor(User actor, Criteria criteria);

	Future<Void> invalidate(ChangeLog changes);

	/**
	 * Does not throw an {@link Exception}.
	 */
	@Override
	void close();
}
