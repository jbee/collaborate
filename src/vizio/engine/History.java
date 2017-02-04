package vizio.engine;

import vizio.model.Entity;
import vizio.model.ID;

/**
 * A history keep track of the transformations of {@link Entity}s over time. 
 */
public final class History {

	public final ID entity;
	public final long[] events;
	
	public History(ID entity, long[] events) {
		super();
		this.entity = entity;
		this.events = events;
	}

	@Override
	public String toString() {
		return entity.toString();
	}
}
