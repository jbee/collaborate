package vizio.engine;

import java.util.Iterator;

import vizio.model.Entity;
import vizio.model.ID;
import vizio.model.User;

/**
 * A history keep track of the transformations of {@link Entity}s over time. 
 * 
 * If the entity is a {@link User} the events are not the transformations done one the user but by the user.
 */
public final class History implements Iterable<ID> {

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
	
	public int length() {
		return events.length-1;
	}

	@Override
	public Iterator<ID> iterator() {
		return new Iterator<ID>() {
			
			int i = 1;
			@Override
			public boolean hasNext() {
				return i < events.length;
			}

			@Override
			public ID next() {
				return ID.eventId(events[i++]);
			}
		};
	}
}
