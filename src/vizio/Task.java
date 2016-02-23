package vizio;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.System.currentTimeMillis;

public class Task {

	public ID id;
	public String summary;
	public ID chronicle;
	public ID origin;
	public Stimulus stimulus;
	public Goal goal;
	public Status status;
	public int heat;
	public int start; // days from ms
	public int end = -1; // days from ms
	public int product;
	public int area;
	public int version;
	public Users started;
	public Users marked;

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

	public int age() {
		return today() - start;
	}

	private int today() {
		return days(currentTimeMillis());
	}

	private static int days(long millis) {
		return (int) (millis / (1000*60*60*24));
	}
}
