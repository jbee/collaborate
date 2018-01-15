package se.jbee.track.api;

import static se.jbee.track.engine.Server.Switch.LOCKDOWN;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import se.jbee.track.api.Param.Command;
import se.jbee.track.cache.Cache;
import se.jbee.track.cache.Matches;
import se.jbee.track.db.DB;
import se.jbee.track.engine.Changes;
import se.jbee.track.engine.Sample;
import se.jbee.track.engine.Server;
import se.jbee.track.engine.Transaction;
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
		Names versions = request.names(Param.version);
		Names categories = request.names(Param.category);
		// this is run as if we are on lockdown so that limits won't apply - also this makes sure again one has to be admin
		Changes changes = Transaction.run(Sample.sample(users, outputs, versions, areas, categories, 50, actor.alias), db, server.with(LOCKDOWN));
		cache.invalidate(changes);
		return new SampleView(actor, changes);
	}

	private void expectAdmin(User actor) {
		if (!server.isAdmin(actor)) 
			throw new TransitionDenied(Error.E25_ADMIN_REQUIRED, server.admin());
	}
	
	private ListView list(Params request) {
		
		return new ListView(new User(1), System.currentTimeMillis(), new Page[0], new Page(1, Name.as("prod"), Name.as("area"), Name.as("xyz"), Template.template("Hello")), new Matches[0]);
	}
	
}
