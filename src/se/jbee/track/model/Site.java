package se.jbee.track.model;

/**
 * How {@link Site}s work:
 * 
 * {@link User} site   : {@link #menu} = {@link User#alias},  {@link #product} = {@link Name#ORIGIN} 
 * {@link Area} site   : {@link #menu} = {@link Area#name},   {@link #product} = {@link Area#product} 
 * {@link Product} site: {@link #menu} = {@link Name#ORIGIN}, {@link #product} = {@link Product#name}
 * 
 * So a product site is really just a area site for the special 
 * {@link Name#ORIGIN} area. All users can only create and change their own
 * sites. Area sites can be created and changed by all actual maintainers.
 * The amount of sites for each menu is limited. 
 * 
 * There are 3 special users to set the default user, area and product sites:
 * <code>@user</code>, <code>@area</code>, <code>@product</code>. The sites of
 * these "special" user accounts make use of replacement variables
 * <code>@</code> in their queries which are replaced with the user, product or
 * area one is currently looking at.
 * 
 * Also one can look at another user's site as a different user, mostly oneself.
 * This allows to see their kind of queries but the results filtered for that
 * other user. The things one can see and do on any site are always dependent on
 * the actual logged in users rights.
 * 
 * @author jan
 */
public final class Site extends Entity<Site> implements Transitory {

	/**
	 * {@link User} sites use {@link Name#ORIGIN}
	 */
	public Name product;
	public Name menu; // a user or area name
	public Name name; // of the site itself
	public Template template;

	public Site(int version, Name product, Name menu, Name name, Template template) {
		super(version);
		this.product = product;
		this.menu = menu;
		this.name = name;
		this.template = template;
	}

	@Override
	public ID computeID() {
		return ID.siteId(product, menu, name);
	}
	
	@Override
	public Name product() {
		return product;
	}
	
	public boolean isUserSite() {
		return Name.ORIGIN.equalTo(product);
	}
	
	public boolean isAreaSite() {
		return !isUserSite();
	}
	
	@Override
	public boolean obsolete() {
		return template.isEmpty();
	}
}
