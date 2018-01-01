package se.jbee.track.ui.ctrl;

import se.jbee.track.cache.Criteria;
import se.jbee.track.cache.Matches;
import se.jbee.track.db.DB;
import se.jbee.track.model.Name;
import se.jbee.track.model.User;
import se.jbee.track.ui.view.Menu;
import se.jbee.track.ui.view.View;

public class DBController implements Controller {

	private final DB db;

	public DBController(DB db) {
		this.db = db;
	}

	@Override
	public Matches tasks(Criteria criteria, Context ctx) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public User user(Name user) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Menu[] menus(Context ctx) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public View view(Context ctx) {
		// TODO Auto-generated method stub
		return null;
	}

}
