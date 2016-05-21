package vizio.io;

public class SimplePersistenceManager {

	// INDEX A: product, status
	// INDEX B: product, motive
	// INDEX C: product, goal
	// INDEX D: product, area, status
	// INDEX E: product, version, status

	// INDEX F: user (involved), status
	// INDEX G: user (watch), status

	// queries that do not limit by user or by product are illegal for now
	// other filters and sorts are done on linearly on the set extracted from index by binary search

	// use copy on write? copy the objects before modification - also important for updates so see what has changed
	// as a protection against forgetting to copy the store can compare its cached reference with the given instance
	// if they are equal the cached one is reload from deep storage and a failure is thrown
	// that would mean all entities should support Cloneable
}
