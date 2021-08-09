package se.jbee.task.cache;

import static se.jbee.task.util.Array.indexOf;

import java.util.concurrent.Future;

import se.jbee.task.model.Name;
import se.jbee.task.model.Names;
import se.jbee.task.model.Output;
import se.jbee.task.model.Task;

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
	public final Names includedOutputs;
	/**
	 * The set of outputs that were not included in the matches even though
	 * the user has an affiliation with them. Usually the reason is that a
	 * {@link Output} is not indexed yet. This has to be requested by the user
	 * explicitly.
	 */
	public final Names excludedOutputs;
	public final Names erroneousOutputs;

	public Matches(Task[] matches, int totalMatches) {
		this(matches, totalMatches, Names.empty(), Names.empty(), Names.empty());
	}
	private Matches(Task[] matches, int totalMatches, Names includedOutputs, Names excludedOutputs, Names erroneousOutputs) {
		this.tasks = matches;
		this.total = totalMatches;
		this.includedOutputs = includedOutputs;
		this.excludedOutputs = excludedOutputs;
		this.erroneousOutputs = erroneousOutputs;
	}

	public Matches inContext(Names includedOutputs, Names excludedOutputs, Names erroneousOutputs) {
		return new Matches(tasks, total, includedOutputs, excludedOutputs, erroneousOutputs);
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