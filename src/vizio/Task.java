package vizio;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class Task {

	// creating a task
	public Name product;
	public IDN id;
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
	public Name version;
	public Names usersMarked;
	public Names usersStarted;
	public boolean confirmed;
	// resolving a task
	public Date end;
	public String disclosure; 

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
		return usersMarked.count() + usersStarted.count();
	}
	
	public boolean isVisibleTo(Name user) {
		return !exploitable || reporter.equalTo(user) || area.maintainers.contains(user);
	}
	
	@Override
	public String toString() {
		return id.toString();
	}
}
