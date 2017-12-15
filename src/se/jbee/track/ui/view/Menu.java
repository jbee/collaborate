package se.jbee.track.ui.view;

import se.jbee.track.model.Name;
import se.jbee.track.model.Site;
import se.jbee.track.ui.ctrl.Action;

public class Menu {

	public final Name label;
	public final Action action;
	public final Site[] entries;

	public Menu(Name label, Action action, Site... entries) {
		super();
		this.label = label;
		this.action = action;
		this.entries = entries;
	}

}
