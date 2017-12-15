package se.jbee.track.ui.ctrl;

import se.jbee.track.cache.Criteria;
import se.jbee.track.model.Name;
import se.jbee.track.model.Task;
import se.jbee.track.model.User;
import se.jbee.track.ui.view.Menu;
import se.jbee.track.ui.view.View;

public interface Controller {

	// DATA

	Task[] tasks(Criteria selection, Context ctx);

	User user(Name user);

	// UI

	Menu[] menus(Context ctx);

	View view(Context ctx);

}
