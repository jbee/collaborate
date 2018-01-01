package se.jbee.track.ui.ctrl;

import se.jbee.track.cache.Criteria;
import se.jbee.track.cache.Matches;
import se.jbee.track.model.Name;
import se.jbee.track.model.User;
import se.jbee.track.ui.view.Menu;
import se.jbee.track.ui.view.View;

public interface Controller {

	// DATA

	Matches tasks(Criteria criteria, Context ctx);

	User user(Name user);

	// UI

	Menu[] menus(Context ctx);

	View view(Context ctx);
	
	//TODO maybe change the interface and have the controller return fully assembled pages
	// Page page(Context ctx); // maybe call it a Path or Reference? or just use Site IDs? they miss the "as X" part

}
