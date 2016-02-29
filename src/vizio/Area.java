package vizio;

/**
 * The {@link Area} of {@link Task} can only be assigned by {@link #maintainers}.
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
}
