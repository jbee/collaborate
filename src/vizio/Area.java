package vizio;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * The {@link Area} of {@link Task} can only be assigned by {@link #maintainers}
 *
 * @author jan
 */
public class Area {

	public Name product;
	public Name name;
	/**
	 * The 'parent' area. Something that contains this area in some way.
	 */
	public Name basis;
	public Names maintainers;
	/**
	 * Do only maintainers get to stress?
	 */
	public boolean exclusive;

	/**
	 * {@link Poll}s have a {@link Poll#serial} that is unique within the affected {@link Area}.
	 */
	public AtomicInteger polls;

	// entrance areas:
	/**
	 * Any registered user may add tasks.
	 * However, no task created with this area can be moved to other areas.
	 * Instead a sequel would be created if necessary.
	 */
	public boolean entrance;
	public Motive motive;
	public Purpose purpose;
	/**
	 * A per area counter that is used in case of {@link #entrance} areas.
	 */
	public AtomicInteger tasks;

	/*
	 * An example for an entrance area would be RFCs. The area would be named "RFC".
	 * Tasks created would become "RFC-1" and so on.
	 */
}
