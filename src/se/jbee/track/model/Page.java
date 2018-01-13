package se.jbee.track.model;

/**
 * How {@link Page}s work:
 * 
 * {@link User} page   : {@link #menu} = {@link User#alias},  {@link #output} = {@link Name#ORIGIN} 
 * {@link Area} page   : {@link #menu} = {@link Area#name},   {@link #output} = {@link Area#output} 
 * {@link Output} page : {@link #menu} = {@link Name#ORIGIN}, {@link #output} = {@link Output#name}
 * 
 * So a {@link Output} {@link Page} is really just a area {@link Page} for the special 
 * {@link Name#ORIGIN} area. All users can only create and change their own
 * {@link Page}s. Area pages can be created and changed by all actual maintainers.
 * The amount of pages for each menu is limited. 
 * 
 * There are 3 special users to set the default user, area and output pages:
 * <code>@user</code>, <code>@area</code>, <code>@output</code>. The pages of
 * these "special" user accounts make use of replacement variables
 * <code>@</code> in their queries which are replaced with the user, output or
 * area one is currently looking at.
 * 
 * Also one can look at another user's page as a different user, mostly oneself.
 * This allows to see their kind of queries but the results filtered for that
 * other user. The things one can see and do on any page are always dependent on
 * the actual logged in users rights.
 * 
 * @author jan
 */
public final class Page extends Entity<Page> implements Transitory {

	/**
	 * {@link User} pages use {@link Name#ORIGIN}
	 */
	public Name output;
	public Name menu; // a user or area name
	public Name name; // of the page itself
	public Template template;

	public Page(int version, Name output, Name menu, Name name, Template template) {
		super(version);
		this.output = output;
		this.menu = menu;
		this.name = name;
		this.template = template;
	}

	@Override
	public ID computeID() {
		return ID.pageId(output, menu, name);
	}
	
	@Override
	public Name output() {
		return output;
	}
	
	public boolean isUserPage() {
		return Name.ORIGIN.equalTo(output);
	}
	
	public boolean isAreaPage() {
		return !isUserPage();
	}
	
	@Override
	public boolean obsolete() {
		return template.isEmpty();
	}
}
