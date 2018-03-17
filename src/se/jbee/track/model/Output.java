package se.jbee.track.model;

/**
 * A {@link Output}'s "counter" change when new {@link Task}s for that
 * {@link Output} are created.
 *
 * @author jan
 */
public final class Output extends Entity<Output> {

	public Name name;
	public Integration[] integrations;

	//TODO there should be hard limits for the amount of versions and area possible to have

	/**
	 * The area used to manage a {@link Output}'s areas and versions.
	 *
	 * If the origin is abandoned the {@link Output} is abandoned.
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

	/**
	 * A set of suggested categories.
	 */
	public Names categories;

	public int tasks;

	public Output(int version) {
		super(version);
	}

	@Override
	public ID computeID() {
		return ID.outputId(name);
	}

	@Override
	public Name output() {
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
				return attachment.integrateAs(i.base, i.name);
		return attachment;
	}

	public Names integrations() {
		Names res = Names.empty();
		for (Integration i : integrations)
			res = res.add(i.name);
		return res;
	}
}
