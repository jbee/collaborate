package vizio.view;

import vizio.Site;

public class Menu {

	public final String label;
	public final Site[] entries;
	
	public Menu(String label, Site... entries) {
		super();
		this.label = label;
		this.entries = entries;
	}
	
}
