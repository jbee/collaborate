package se.jbee.track.model;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class Task extends Entity<Task> {

	// creating a task
	public Product product; // needs to be a object since it changes when new task is created
	public IDN id;
	public IDN serial; // within an board
	// opening record
	public Name reporter;
	public Date reported;
	public Gist gist;

	public Motive motive;
	public Purpose purpose;
	public Status status;
	public Names changeset;
	public boolean exploitable;
	// working with a task (data that might change)
	public IDN basis; // direct predecessor
	public IDN origin; // initial "impulse" that lead to this task
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
	public Names pursuedBy;
	public Names engagedBy;
	public Names watchedBy; // OBS! this is the only real dynamic length field...
	// resolving a task (closing record)
	public Name solver;
	public Date resolved;
	public Gist conclusion;

	public Task(int version) {
		super(version);
	}
	
	@Override
	public ID computeID() {
		return ID.taskId(product.name, id);
	}
	
	@Override
	public Name product() {
		return product.name;
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
		return pursuedBy.count() + engagedBy.count();
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

}
