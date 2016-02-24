package vizio;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static vizio.Date.today;

public class Task {

	public IDN id;
	public IDN chronicle;
	public IDN origin;
	public ID creator;
	public String summary;
	public Stimulus stimulus;
	public Goal goal;
	public Status status;
	public int heat;
	public Date start = Date.today();
	public Date end;
	public Product product;
	public Area area;
	public int version;
	public Users marked;
	public Users started;
	public boolean exploitable;

	/**
	 * {@link Heat} is aggregated temperature. When {@link User} vote on
	 * {@link Task}s the task receives some heat. The actual amount depends on
	 * the current heat of the {@link Task} and its age.
	 */
	public void heat() {
		int age = age();
		int left = 100 * age - heat;
		heat += max(age, left/2);
	}

	public int temp() {
		return min(100, heat / age());
	}

	public Temp temerature() {
		return Temp.fromNumeric(temp());
	}

	public int age() {
		return today().daysSince(start) + 1;
	}
}
