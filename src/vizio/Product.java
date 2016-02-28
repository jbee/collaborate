package vizio;

public class Product {

	public Name name;
	public int tasks;
	public int unconfirmedTasks;

	public boolean allowsAnonymousReports() {
		return unconfirmedTasks < 10;
	}
}
