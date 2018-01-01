package se.jbee.track.model;

import static java.util.Arrays.copyOfRange;
import static se.jbee.track.model.Bytes.join;


/**
 * A {@link Product}'s "counter" change when new {@link Task}s for that product
 * are created.
 *
 * @author jan
 */
public final class Product extends Entity<Product> {

	public Name name;
	public Integration[] integrations;
	
	//TODO there should be hard limits for the amount of versions and area possible to have for a product
	
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
	
	public boolean isIntegrated(URL url) {
		boolean name = url.isIntegrated();
		for (Integration i : integrations) {
			if (name && url.startsWith(i.name) || !name && url.startsWith(i.base))
				return true;
		}
		return false;
	}

	public URL integrate(URL attachment) {
		for (Integration i : integrations)
			if (attachment.startsWith(i.base))
				return URL.fromBytes(join(i.name.bytes(), new byte[] {':'}, copyOfRange(attachment.bytes(), i.base.length(), attachment.length())));
		return attachment;
	}
}
