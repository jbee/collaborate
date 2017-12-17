package se.jbee.track.model;


/**
 * A {@link Product}'s "counter" change when new {@link Task}s for that product
 * are created.
 *
 * @author jan
 */
public final class Product extends Entity<Product> {

	public Name name;
	public Integration[] integrations;
	
	/**
	 * The area used to manage a product's areas and versions.
	 * 
	 * If the origin is abandoned the product is abandoned. 
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
	
	public Product(int version) {
		super(version);
	}
	
	@Override
	public ID computeID() {
		return ID.productId(name);
	}
	
	@Override
	public Name product() {
		return name;
	}
	
	public static final class Integration {
		public final Name name;
		public final URL base;
		
		public Integration(Name name, URL base) {
			super();
			this.name = name;
			this.base = base;
		}
		
		public boolean equalTo(Integration other) {
			return name.equalTo(other.name);
		}
		
		public boolean same(Integration other) {
			return equalTo(other) && base.equalTo(other.base);
		}
	}
}
