package vizio.ctrl;

import vizio.io.Criteria;
import vizio.model.Name;
import vizio.model.Task;
import vizio.model.User;
import vizio.view.Menu;
import vizio.view.View;

public interface Controller {

	// DATA

	Task[] tasks(Criteria selection, Context ctx);

	User user(Name user);

	// UI

	Menu[] menus(Context ctx);

	View view(Context ctx);

}
