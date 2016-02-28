package vizio;

public class Version {

	public static final Version UNKNOWN = new Version(Name.UNKNOWN);
	
	public Name name;
	public Names changeset;
	
	public Version(Name name) {
		super();
		this.name = name;
	}
}
