package se.jbee.track.model;

public final class Site extends Entity<Site> {

	public Name name; // of the site itself
	public Name owner;
	public Template template;

	public Site(int version, Name owner, Name name, Template template) {
		super(version);
		this.owner = owner;
		this.name = name;
		this.template = template;
	}

	@Override
	public ID computeID() {
		return ID.siteId(owner, name);
	}
	
	@Override
	public Name product() {
		return Name.ORIGIN;
	}
}
