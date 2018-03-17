package se.jbee.track.cache;

import java.util.concurrent.Future;

import se.jbee.track.model.Names;
import se.jbee.track.model.Output;
import se.jbee.track.model.Task;

public final class Matches {

	public static Matches matches(Future<Matches> matches) {
		try {
			return matches.get();
		} catch (Exception e) {
			e.printStackTrace();
			return Matches.none();
		}
	}

	public static Matches none() {
		return new Matches(new Task[0], 0);
	}

	public final Task[] tasks;
	public final int total;
	/**
	 * The set of outputs that were not included in the matches even though
	 * the user has an affiliation with them. Usually the reason is that a
	 * {@link Output} is not indexed yet. This has to be requested by the user
	 * explicitly.
	 */
	public final Names excludedOutputs;

	public Matches(Task[] matches, int totalMatches) {
		this(matches, totalMatches, Names.empty());
	}
	private Matches(Task[] matches, int totalMatches, Names excludedOutputs) {
		super();
		this.tasks = matches;
		this.total = totalMatches;
		this.excludedOutputs = excludedOutputs;
	}

	public Matches exlcuded(Names outputs) {
		return new Matches(tasks, total, outputs);
	}

	@Override
	public String toString() {
		return tasks.length+"/"+total+" not including "+excludedOutputs+"";
	}
}