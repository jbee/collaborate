package vizio.model;

public class Site extends Entity<Site> {

	public Name name; // of the site itself
	public Name owner;
	public String template;

	public Site(Name owner, Name name, String template) {
		super();
		this.owner = owner;
		this.name = name;
		this.template = template;
	}

	@Override
	public ID uniqueID() {
		return ID.siteId(owner, name);
	}
}
