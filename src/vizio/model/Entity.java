package vizio.model;

public abstract class Entity<T> implements Cloneable {

	@Override
	@SuppressWarnings("unchecked")
	public T clone() {
		try {
			return (T) super.clone();
		} catch (CloneNotSupportedException e) {
			// should never happen
			throw new RuntimeException(e);
		}
	}
}
