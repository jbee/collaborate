package se.jbee.track.cache;

import static se.jbee.track.util.Array.indexOf;

import java.util.concurrent.Future;

import se.jbee.track.model.Name;
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

	//TODO includedOutputs

	public Matches(Task[] matches, int totalMatches) {
		this(matches, totalMatches, Names.empty());
	}
	private Matches(Task[] matches, int totalMatches, Names excludedOutputs) {
		this.tasks = matches;
		this.total = totalMatches;
		this.excludedOutputs = excludedOutputs;
	}

	public Matches exlcuded(Names outputs) {
		return new Matches(tasks, total, outputs);
	}

	public Output latestOutput() {
		return tasks.length == 0 ? null : latestOutput(tasks[0].output());
	}

	public Output latestOutput(Name output) {
		int idx = indexOf(tasks, (t, max) -> t.output.name.equalTo(output) && t.output.version() > max, 0);
		return idx < 0 ? null : tasks[idx].output;
	}

	@Override
	public String toString() {
		return tasks.length+"/"+total+" not including "+excludedOutputs+"";
	}
}