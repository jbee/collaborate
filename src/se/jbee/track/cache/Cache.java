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
		
		public final Task[] list;
		public final int totalMatches;
		public final Names unindexedProducts;
		public Tasks(Task[] list, int totalMatches, Names unindexedProducts) {
			super();
			this.list = list;
			this.totalMatches = totalMatches;
			this.unindexedProducts = unindexedProducts;
		}
	}

	Tasks CANCELLED = new Tasks(new Task[0], -1, Names.empty());
	
	Future<Tasks> tasks(Criteria constraints);
	
	Future<Void> invalidate(Changes changes);
	
	// Properties of the cache:
	// - cache per product
	// - no cross product searches
	// - values for a key are stored in an array growing with power of 2, using a length field to keep track of how many slots are in use
	// - values for a key are not sorted
	// - insert adds to first empty (null) slot (might inc length if all slots are taken; no remove occurred)
	// - remove simply sets a slot to empty (null)
	// - update changes the object (task) in place - not the array slot (writes work with their own instances)
	
	/*
	 * 
	 * 
Cache for Tasks:

Referenced entities:
* Product: can be seen as a constant, maybe change Task to just use a Name ref here
* Area: has to be updated when changed
* Version: has to be updated when changed

All caches are "per product".
When changes are made one sometimes has to update caches.
Is there a situation when one has to know the old value to find the tasks that need update?
It should be that such updates are updates to the task itself in what case the task is updated from DB by setting all fields to new values (in-place) to avoid finding all usages in the different indexes.

changes to a task that require updating that particular task in cache
- solving a task
- attach (an URL)
- relocate
- adding/removing user from one of the tasks user lists (approchedBy, watchedBy, enlistedBy)
- emphasise
- (adding new tasks)

changes that might have an effect on tasks:
- leave (maintainer leaves area)
- releasing (solve a publish task that changes version)
- settling a poll (and thereby adding or removing a maintainer or changing the character of an area)

These potential changes can be ignored as long as this data is not used when showing tasks.
But the maintainers of the area is relevant if the maintainer is the current user as this is used to control if certain actions are possible and therefore how a task is rendered.

Actions that requires to actually change the index (add/remove a task from a certain index list) are:
(all can be identified by looking at old/new of the changed entities)

relocate:
- product-area 

resolve:
- product-status
- user-status (3 lists)

add/remove a user from one of the lists:
- product user 
- user-status (3 lists)

A temperature index has to be refreshed every day. 
Heat can be rephrased as temperature as it is just a range

	 */
}
