package vizio.ui.ctrl;

import vizio.engine.Constraints;
import vizio.model.Name;
import vizio.model.Task;
import vizio.model.User;
import vizio.ui.view.Menu;
import vizio.ui.view.View;

public interface Controller {

	// DATA

	Task[] tasks(Constraints selection, Context ctx);

	User user(Name user);

	// UI

	Menu[] menus(Context ctx);

	View view(Context ctx);

}
