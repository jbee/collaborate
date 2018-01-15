package se.jbee.track.api;

import se.jbee.track.engine.Changes;
import se.jbee.track.model.User;

/**
 * Result of creation sample data.
 *  
 * @author jan
 */
public final class SampleView extends View {

	public final Changes changes;
	
	public SampleView(User actor, Changes changes) {
		super(actor, changes.timestamp);
		this.changes = changes;
	}

}
