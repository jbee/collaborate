package se.jbee.track.cache;

import se.jbee.track.model.Names;
import se.jbee.track.model.Task;

public final class Matches {
	
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