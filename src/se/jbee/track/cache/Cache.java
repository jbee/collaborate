package se.jbee.track.cache;

import java.util.concurrent.Future;

import se.jbee.track.engine.Changes;
import se.jbee.track.model.Names;
import se.jbee.track.model.Task;

/**
 * The {@link Cache} is a fast lookup mechanism for computing lists of
 * {@link Task}s with certain properties described by {@link Criteria}.
 * 
 * Given a list of {@link Criteria} the cache delivers a result set of
 * {@link Tasks}. The data is consistent but not necessarily updated with all
 * changes already persisted.
 * 
 * The {@link #invalidate(Changes)} method is used to update the cache with
 * changes that already have occurred (are persisted).
 */
public interface Cache {

	final class Tasks {
		
		public static final Tasks NONE = new Tasks(new Task[0], 0, Names.empty());
		
		public final Task[] matches;
		public final int totalMatches;
		public final Names unindexedProducts;
		public Tasks(Task[] matches, int totalMatches, Names unindexedProducts) {
			super();
			this.matches = matches;
			this.totalMatches = totalMatches;
			this.unindexedProducts = unindexedProducts;
		}
	}

	Tasks CANCELLED = new Tasks(new Task[0], -1, Names.empty());
	
	Future<Tasks> tasks(Criteria constraints);
	
	Future<Void> invalidate(Changes changes);

}
