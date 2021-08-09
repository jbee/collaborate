package se.jbee.task.model;

/**
 * It explains why a {@link Task} created?
 *
 * This is subjective for the reporter but helps to put it in perspective for contributors.
 */
@UseCode("fadi")
public enum Cause {

	/**
	 * The reporter identified a problem and asks for correction.
	 *
	 * This means something is broken, wrong. A change is required to make it work as intended.
	 *
	 * This is special because of its urgency.
	 */
	finding,

	/**
	 * The reporter wants to put a reminder for a thought that might spawn action.
	 */
	aid,

	/**
	 * The reporter wants give direction e.g. by pointing out a goal, law, cornerstone or quality.
	 *
	 * In contrast to a plan and planing the direction just expresses that
	 * something particular should happen or be reached, is required or needs to be taken into account.
	 * The happening or allowance of it is important, not to predict the moment or plan steps to get there.
	 */
	direction,

	/**
	 * The reporter wants to propose a specific change or action.
	 */
	idea,
}
