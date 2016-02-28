package vizio;

/**
 * The {@link Area} of {@link Task} can only be assigned by {@link #maintainers}.
 *  
 * @author jan
 */
public class Area {

	public Name name;
	public Names maintainers;
	public Name product;
	public boolean exclusive;
}
