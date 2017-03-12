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
	/**
	 * Index 0 always contains the very first (oldest) event.
	 * Index 1 and onwards contain the history of events limited to some length.
	 * This is called compaction of events.
	 * On first event index 1 holds same event as index 0.
	 * When compaction occurs the oldest event (index 1) is replaced first.
	 * Order is always maintained from oldest to newest (most recent) event.
	 */
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
	
	/**
	 * Compaction means that at least one event has been dropped to not have the history grow limit-less.
	 */
	public boolean isCompacted() {
		return events[0] != events[1];
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
