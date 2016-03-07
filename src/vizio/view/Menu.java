package vizio.view;

import vizio.Site;
import vizio.ctrl.Action;

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
