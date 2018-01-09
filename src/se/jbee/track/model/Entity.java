package se.jbee.track.model;

public abstract class Entity<T extends Entity<T>> implements Cloneable, Comparable<T> {

	public final transient int initalVersion;

	private int version;
	private boolean corrupted = false;
	private transient ID uniqueId;
	
	protected abstract ID computeID();
	
	/**
	 * @return name of the {@link Product} this entity is related to or
	 *         {@link Name#ORIGIN} if the entity is not related to any
	 *         particular product like a user or a user site.
	 */
	public abstract Name product();
	
	public Entity(int initalVersion) {
		super();
		this.initalVersion = initalVersion;
		this.version = initalVersion;
	}
	
	public final int version() {
		return version;
	}
	
	public final boolean isModified() {
		return version > initalVersion;
	}
	
	public final boolean isCurrupted() {
		return corrupted;
	}
	
	/**
	 * This is just visible at all to allow the user to increment its version
	 * without cloning it.
	 */
	final void modified() {
		version++;
	}
	
	/**
	 * This is a way to lift the other instance to the same version as this
	 * version of the very same entity.
	 * 
	 * If another or an older entity is passed no change is done. If the other
	 * instance is changed it also is flagged as {@link #corrupted}. This way it
	 * can be updated to a higher version but only by simultaneously making it
	 * illegal to store this updated entity as we do not know if its state is
	 * 100% identical. We do this in the assumption that its state is a 100%
	 * identical but a programming mistake could render this false and we do not
	 * want to rely on that.
	 * 
	 * Also the update will only be done if this entity itself is not yet
	 * {@link #corrupted}.
	 */
	@SuppressWarnings("unchecked")
	public final void update(Entity<T> other) {
		if (!isCurrupted() && isMoreRecent((T) other)) {
			other.corrupted = true; // most often the other already is corrupted
			other.version = version;
		}
	}

	/**
	 * @return A database unique identifier. That means the type of the entity
	 *         is also encoded in the value. Two different entities can never
	 *         have the same ID.
	 */
	public final ID uniqueID() {
		if (uniqueId != null)
			return uniqueId;
		uniqueId = computeID();
		return uniqueId;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public final T clone() {
		try {
			T res = (T) super.clone();
			res.modified();
			return res;
		} catch (CloneNotSupportedException e) {
			// should never happen
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public final int compareTo(T other) {
		if (this == other)
			return 0;
		return uniqueID().compareTo(other.uniqueID());
	}
	
	@Override
	public final int hashCode() {
		return uniqueID().hashCode();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public final boolean equals(Object obj) {
		return obj != null && obj.getClass() == getClass() && equalTo((T) obj);
	}

	public final boolean equalTo(T other) {
		return uniqueID().equalTo(other.uniqueID());
	}
	
	/**
	 * @return two instances are the same if they have the same {@link ID} AND
	 *         are of the same {@link #version()}. This does not compare them
	 *         field by field!
	 */
	public final boolean sameAs(T other) {
		return equalTo(other) && version == other.version();
	}
	
	public final boolean isMoreRecent(T other) {
		return equalTo(other) && version > other.version();
	}
	
	@Override
	public final String toString() {
		return uniqueID().toString()+":"+version+":"+initalVersion;
	}
}
