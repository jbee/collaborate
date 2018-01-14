package se.jbee.track.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import se.jbee.track.api.Param.Command;
import se.jbee.track.cache.Cache;
import se.jbee.track.cache.Matches;
import se.jbee.track.db.DB;
import se.jbee.track.engine.Server;
import se.jbee.track.engine.TransitionDenied;
import se.jbee.track.engine.TransitionDenied.Error;
import se.jbee.track.model.Name;
import se.jbee.track.model.Names;
import se.jbee.track.model.Page;
import se.jbee.track.model.Template;
import se.jbee.track.model.User;

public class CachedViewService implements ViewService {

	private final Server server; 
	private final DB db;
	private final Cache cache;
	private final Map<String, User> sessions = new ConcurrentHashMap<>();

	public CachedViewService(Server server, DB db, Cache cache) {
		this.server = server;
		this.db = db;
		this.cache = cache;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends View> T run(Params request, Class<T> response)
			throws ViewNotAvailable {
		Command cmd = request.value(Param.command, Command.list);
		switch (cmd) {
		case list:  if (response == ListView.class) return (T)list(request); 
		case sample:if (response == SampleView.class) return (T)sample(request);
		default:
			throw new ViewNotAvailable(request, response);
		}
	}
	
	private User user(String id) {
		return User.ANONYMOUS;
	}
	
	private SampleView sample(Params request) {
		User actor = user(request.get(Param.actor));
		expectAdmin(actor);
		Names outputs = request.names(Param.output);
		Names areas = request.names(Param.area);
		Names users = request.names(Param.role);
		//TODO sample change itself can be a Change, pass the converted params (names and so forth)
		return new SampleView(actor, server.clock.time());
	}

	private void expectAdmin(User actor) {
		if (!server.isAdmin(actor)) 
			throw new TransitionDenied(Error.E25_ADMIN_REQUIRED, server.admin());
	}
	
	private ListView list(Params request) {
		
		return new ListView(new User(1), System.currentTimeMillis(), new Page[0], new Page(1, Name.as("prod"), Name.as("area"), Name.as("xyz"), Template.template("Hello")), new Matches[0]);
	}
	
}
