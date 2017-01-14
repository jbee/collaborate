package vizio.model;

public abstract class Entity<T extends Entity<T>> implements Cloneable, Comparable<T> {

	private transient ID uniqueId;
	
	protected abstract ID computeID();
	
	/**
	 * @return A database unique identifier. That means the type of the entity
	 *         is also encoded in the value. Two different entities can never
	 *         have the same ID.
	 */
	public final ID uniqueID() {
		if (uniqueId != null)
			return uniqueId;
		uniqueId = uniqueID();
		return uniqueId;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public final T clone() {
		try {
			return (T) super.clone();
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
	public String toString() {
		return uniqueID().toString();
	}
}
