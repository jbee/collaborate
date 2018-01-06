package se.jbee.track.ui.ctrl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import se.jbee.track.cache.Cache;
import se.jbee.track.db.DB;
import se.jbee.track.engine.Clock;
import se.jbee.track.engine.Limits;
import se.jbee.track.model.Name;
import se.jbee.track.model.User;

public class DBController implements Ctrl {

	private final DB db;
	private final Cache cache;
	private final Map<Name, User> sessions = new ConcurrentHashMap<>(); 

	public DBController(DB db, Cache cache, Clock clock, Limits limits) {
		this.db = db;
		this.cache = cache;
	}

	@Override
	public DynamicPage query(Params params) {
		// TODO Auto-generated method stub
		return null;
	}

	
}
