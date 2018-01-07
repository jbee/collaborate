package se.jbee.track.model;

@UseCode
public enum Motive {

	/**
	 * In contrast to a plan and planing the intention just expresses that
	 * something particular should happen. The event itself is important, not to
	 * predict the moment or plan the way.
	 */
	necessity,
	
	/**
	 * To a thought that might be something to do.
	 */
	reminder,
	
	/**
	 * A specific change of how things should be is proposed.
	 */
	proposal,
	
	/**
	 * Something is broken, wrong. Change is required to make something work as
	 * described.
	 */
	defect
}
