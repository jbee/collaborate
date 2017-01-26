package vizio.engine;

import vizio.engine.Change.Type;
import vizio.model.ID;
import vizio.model.Name;

public final class LogEntry {

	/**
	 * A little "trick" needs to be done to ensure no two entries happen in same moment (timestamp).
	 * 
	 * That is to make sure time at least increases by 1 ms each time a new entry is written.
	 * 
	 * The neat by-effect of this trick is that keys are chronological order and time-slices can
	 * easily be extracted using cursors.
	 */
	public final long timestamp; // also the key
	public final Name user; 
	public final Changes[] entityChanges;
	
	public LogEntry(long timestamp, Name user, Changes... entityChanges) {
		super();
		this.timestamp = timestamp;
		this.user = user;
		this.entityChanges = entityChanges;
	}

	/**
	 * Changes applied to the entity in order of occurance.  
	 */
	public static final class Changes {
		public final ID entity;
		public final Change.Type[] changes;
		
		public Changes(ID entity, Type... changes) {
			super();
			this.entity = entity;
			this.changes = changes;
		}
	}
	
	
}
