package vizio.ctrl;

import vizio.Name;
import vizio.Task;
import vizio.User;
import vizio.store.Selection;
import vizio.view.Menu;
import vizio.view.View;

public interface Controller {

	// DATA
	
	Task[] tasks(Selection selection, Context ctx);

	User user(Name user);

	// UI
	
	Menu[] menus(Context ctx);
	
	View view(Name space, Name site);

}
