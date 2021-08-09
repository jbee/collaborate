package se.jbee.task.api;

import se.jbee.task.engine.ChangeLog;
import se.jbee.task.model.User;

/**
 * Result of creation sample data.
 *  
 * @author jan
 */
public final class SampleView extends View {

	public final ChangeLog changes;
	
	public SampleView(User actor, ChangeLog changes) {
		super(actor, changes.timestamp);
		this.changes = changes;
	}

}
