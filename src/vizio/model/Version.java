package vizio.model;

public final class Version extends Entity<Version> {

	public Name product;
	public Name name;
	/**
	 * The names of all versions that are included (but had not been included previously).
	 */
	public Names changeset;
	
	/**
	 * A version that is not yet published is only a blank name.
	 * 
	 * @return true, if this version is not yet released.
	 */
	public boolean isPublished() {
		return changeset.count() > 0;
	}

	public Version(int version) {
		super(version);
	}
	
	@Override
	public ID computeID() {
		return ID.versionId(product, name);
	}
}
