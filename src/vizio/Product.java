package vizio;

public class Product {

	public Name name;
	public Area star;
	
	public int tasks;
	public int unconfirmedTasks;

	public boolean allowsAnonymousReports() {
		return unconfirmedTasks < 10;
	}
}
