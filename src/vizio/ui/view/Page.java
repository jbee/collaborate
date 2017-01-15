package vizio.ui.view;

import vizio.model.Task;

public class Page {

	public final Menu[] menus;
	public final View view;
	public final Task[][][] data;
	
	public Page(Menu[] menus, View view, Task[][][] data) {
		super();
		this.menus = menus;
		this.view = view;
		this.data = data;
	}
	
}
