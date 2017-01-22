package vizio.io;

import vizio.engine.Constraints;
import vizio.model.Task;

public interface Cache {

	Task[] tasks(Constraints constraints);
	
	// Properties of the cache:
	// - cache per product
	// - no cross product searches
	// - values for a key are stored in an array growing with power of 2, using a length field to keep track of how many slots are in use
	// - values for a key are not sorted
	// - insert adds to first empty (null) slot (might inc length if all slots are taken; no remove occured)
	// - remove simply sets a slot to empty (null)
	// - update changes the object (task) not the array slot
}
