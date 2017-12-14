package vizio.ui.ctrl;

import vizio.cache.Criteria;
import vizio.model.Name;
import vizio.model.Task;
import vizio.model.User;
import vizio.ui.view.Menu;
import vizio.ui.view.View;

public interface Controller {

	// DATA

	Task[] tasks(Criteria selection, Context ctx);

	User user(Name user);

	// UI

	Menu[] menus(Context ctx);

	View view(Context ctx);

}
