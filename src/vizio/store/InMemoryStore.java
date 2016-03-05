package vizio.store;

public class InMemoryStore {

	// INDEX A: product, status
	// INDEX B: product, area, status, user
	// INDEX C: product, version, status, user
	
	// INDEX D: user, status
	// INDEX E: user, motive
	// INDEX F: user, goal
	
	// queries that do not limit by user or by product are illegal for now
	// other filters and sorts are done on linearly on the set extracted from index by binary search
}
