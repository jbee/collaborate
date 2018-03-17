package se.jbee.track.model;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class Task extends Entity<Task> {

	// creating a task
	public Output output; // needs to be a object since it changes when new task is created
	public IDN id;
	public IDN serial = IDN.ZERO; // within an board
	// opening record
	public Name reporter;
	public Date reported;
	public Gist gist;
	public Gist originalGist; //TODO actually use this - also add op for changing gist that keeps original in this field

	public Motive motive;
	public Purpose purpose;
	public Status status;
	public Names baseVersions;
	public boolean exploitable;
	public boolean disclosed;

	// working with a task (data that might change)
	public IDN basis = IDN.ZERO; // direct predecessor
	public IDN origin = IDN.ZERO; // initial "impulse" that lead to this task
	public int emphasis;
	public Area area;
	public Attachments attachments;

	/**
	 * The {@link Version} is usually the (already released) version that is the
	 * basis of a modification. Such a modification is later released under one
	 * or more version that include this base version in their change-set. Only
	 * for such release tasks this field holds a new version that isn't the base
	 * but the newly released version.
	 */
	public Version base;
	/**
	 * Users that plan to work with this task.
	 */
	public Names aspirants;
	/**
	 * Users actively working with this task.
	 */
	public Names participants;
	/**
	 * Both {@link #aspirants} and {@link #participants()} are users of this task.
	 * This field is used to avoid recomputing the union again and again.
	 */
	private transient Names users;
	/**
	 * Users that want to follow progress of this task.
	 */
	public Names watchers; // OBS! this is the only real dynamic length field...

	// resolving a task (closing record)
	public Name solver;
	public Date resolved;
	public Gist conclusion;

	// archiving
	public boolean archived;

	public Task(int version) {
		super(version);
	}

	@Override
	public ID computeID() {
		return ID.taskId(output.name, id);
	}

	@Override
	public Name output() {
		return output.name;
	}

	/**
	 * {@link Heat} is aggregated temperature. When {@link User} vote on
	 * {@link Task}s the task receives some heat. The actual amount depends on
	 * the current heat of the {@link Task} and its age.
	 */
	public void emphasise(Date today) {
		int age = age(today);
		int left = 100 * age - emphasis;
		emphasis += max(age, left/2);
	}

	public int temperature(Date today) {
		return min(100, emphasis / age(today));
	}

	public Heat heat(Date today) {
		return Heat.valueOf(temperature(today));
	}

	public int age(Date today) {
		return today.daysSince(reported) + 1;
	}

	public int participants() {
		return aspirants.count() + participants.count();
	}

	public boolean isVisibleTo(Name user) {
		return !exploitable || reporter.equalTo(user) || area.maintainers.contains(user);
	}

	public boolean canBeEmphasisedBy(Name user) {
		return (!area.exclusive || area.maintainers.contains(user));
	}

	public boolean isSolved() {
		return status != Status.unsolved;
	}

	public Names users() {
		if (users == null)
			users = participants.union(aspirants);
		return users;
	}

	public void changed() {
		users = null;
	}
}
