package vizio.model;

public abstract class Entity<T extends Entity<T>> implements Cloneable, Comparable<T> {

	/**
	 * @return A database unique identifier. That means the type of the entity
	 *         is also encoded in the value. Two different entities can never
	 *         have the same ID.
	 */
	public abstract ID uniqueID();
	
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
}
