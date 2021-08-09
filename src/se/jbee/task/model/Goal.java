package se.jbee.task.model;

/**
 * What is the goal of a {@link Task}?
 *
 * What does the reporter or creator of it want to happen?
 */
@UseCode("eacvp")
public enum Goal {
	/**
	 * To clarify or detail something before further steps (usually of {@link #adaptation}).
	 *
	 * E.g. confirming a defect, localising the area, exploring ideas
	 */
	elaboration,
	/**
	 * To change or derive something from the existing
	 *
	 * E.g. a modification or manipulation or processing
	 */
	adaptation,
	/**
	 * To ensure the acquisition of something currently missing.
	 *
	 * E.g. to provide a resource, to appropriate something
	 */
	contribution,
	/**
	 * To ensure a certain quality in a {@link #contribution} or {@link #adaptation}
	 */
	verification,
	/**
	 * To make results usable or put the to use
	 *
	 * E.g. book publication, software deployment, transports of goods
	 */
	propagation,
}
