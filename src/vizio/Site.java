package vizio;

public class Site {

	public Name owner;
	public Name name; // of the site itself
	public String template;

	public Site(Name owner, Name name, String template) {
		super();
		this.owner = owner;
		this.name = name;
		this.template = template;
	}

}
