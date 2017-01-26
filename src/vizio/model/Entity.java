package vizio.model;

public abstract class Entity<T extends Entity<T>> implements Cloneable, Comparable<T> {

	public final transient int initalVersion;
	public int version;
	
	private transient ID uniqueId;
	
	protected abstract ID computeID();
	
	public Entity(int initalVersion) {
		super();
		this.initalVersion = initalVersion;
		this.version = initalVersion;
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
			res.version++;
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
	
	@Override
	public final String toString() {
		return uniqueID().toString()+":"+version+":"+initalVersion;
	}
}
