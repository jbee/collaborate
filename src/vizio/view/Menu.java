package vizio.view;

import vizio.ctrl.Action;
import vizio.model.Site;

public class Menu {

	public final String label;
	public final Action action;
	public final Site[] entries;

	public Menu(String label, Action action, Site... entries) {
		super();
		this.label = label;
		this.action = action;
		this.entries = entries;
	}

}
