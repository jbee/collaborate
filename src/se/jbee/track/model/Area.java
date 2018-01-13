package se.jbee.track.model;

/**
 * The {@link Area} of {@link Task} can only be assigned by {@link #maintainers}
 *
 * @author jan
 */
public final class Area extends Entity<Area> {

	public Name output;
	public Name name;
	/**
	 * The 'parent' area. Something that contains this area in some way.
	 */
	public Name basis;
	public Names maintainers;
	
	public Name category;
	
	/**
	 * Do only maintainers get to stress?
	 */
	public boolean exclusive;
	/**
	 * An informational flag to mark areas that are not actively maintained any longer.
	 */
	public boolean abandoned;

	/**
	 * Only allow links to integrations.
	 */
	public boolean safeguarded;
	
	/**
	 * {@link Poll}s have a {@link Poll#serial} that is unique within the affected {@link Area}.
	 */
	public int polls;

	// board areas:
	/**
	 * A per area counter that is used in case of {@link #board} areas.
	 */
	public int tasks;
	
	/**
	 * Any registered user may add tasks.
	 * However, no task created with this area can be moved to other areas.
	 * Instead a sequel would be created if necessary.
	 */
	public boolean board;
	public Motive motive; // fix motive and purpose for the board
	public Purpose purpose;

	public Area(int version) {
		super(version);
	}

	/*
	 * An example for an entrance area would be RFCs. The area would be named "RFC".
	 * Tasks created would become "RFC-1" and so on.
	 */
	
	/**
	 * Open areas allow everyone to report new tasks.
	 * 
	 * NB. {@link Name#UNKNOWN} is not an entrance since it does not dictate motive and goal.
	 * 
	 * @return true in case anyone can report {@link Task}s in this {@link Area}, else false.
	 */
	public boolean isOpen() {
		return name.isUnknown() || board;
	}

	@Override
	public ID computeID() {
		return ID.areaId(output, name);
	}
	
	@Override
	public Name output() {
		return output;
	}
}
