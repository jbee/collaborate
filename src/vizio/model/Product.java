package vizio.model;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link Product}'s "counter" change when new {@link Task}s for that product
 * are created.
 *
 * @author jan
 */
public class Product extends Entity<Product> {

	public Name name;
	/**
	 * The area used to manage a product's areas and versions.
	 *
	 * <pre>*</pre>
	 */
	public Area origin;
	/**
	 * The area tasks are assigned to as long as it is unclear to which
	 * {@link Area} they belong.
	 *
	 * <pre>
	 * ~
	 * </pre>
	 */
	public Area somewhere;
	/**
	 * The version tasks are assigned to as long as it is unclear to which
	 * {@link Version} they belong.
	 *
	 * <pre>
	 * ~
	 * </pre>
	 */
	public Version somewhen;

	public int tasks;

}
