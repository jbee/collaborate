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
	public Name basis;
	public Names maintainers;
	/**
	 * Do only maintainers get to stress?
	 */
	public boolean exclusive;

	// entrance areas:
	/**
	 * Any registered user may add tasks but no task created with this area can
	 * be moved.
	 */
	public boolean entrance;
	/**
	 * A per area counter that is used in case of {@link #entrance} areas.
	 */
	public AtomicInteger tasks;
	public Motive motive;
	public Goal goal;
}
