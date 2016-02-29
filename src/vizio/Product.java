package vizio;

public class Product {

	public Name name;
	/**
	 * The area used to manage a product's areas and versions.
	 *
	 * <pre>*</pre>
	 */
	public Area origin;
	/**
	 * The area tasks are assigned to as long as it is unclear to which area
	 * they belong.
	 *
	 * <pre>?</pre>
	 */
	public Area somewhere;

	public Version somewhen;

	public int tasks;
	public int unconfirmedTasks;

	public boolean allowsAnonymousReports() {
		return unconfirmedTasks < 10;
	}
}
