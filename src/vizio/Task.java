package vizio;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class Task {

	// creating a task
	public Product product;
	public IDN id;
	// opening record
	public Name reporter;
	public Date start;
	public String summary;

	public Motive motive;
	public Goal goal;
	public Status status;
	public Names changeset;
	public boolean exploitable;
	// working with a task (data that might change)
	public IDN cause;
	public IDN origin;
	public int heat;
	public Area area;
	public Version version;
	public Names targetedBy;
	public Names approachedBy;
	public boolean confirmed;
	// resolving a task (closing record)
	public Name solver;
	public Date end;
	public String conclusion;

	/**
	 * {@link Heat} is aggregated temperature. When {@link User} vote on
	 * {@link Task}s the task receives some heat. The actual amount depends on
	 * the current heat of the {@link Task} and its age.
	 */
	public void heat(Date today) {
		int age = age(today);
		int left = 100 * age - heat;
		heat += max(age, left/2);
	}

	public int temp(Date today) {
		return min(100, heat / age(today));
	}

	public Temp temerature(Date today) {
		return Temp.fromNumeric(temp(today));
	}

	public int age(Date today) {
		return today.daysSince(start) + 1;
	}

	public int users() {
		return targetedBy.count() + approachedBy.count();
	}

	public boolean isVisibleTo(Name user) {
		return !exploitable || reporter.equalTo(user) || area.maintainers.contains(user);
	}

	@Override
	public String toString() {
		return id.toString();
	}

	public boolean canBeStressedBy(Name user) {
		return (!area.exclusive || area.maintainers.contains(user));
	}
}
