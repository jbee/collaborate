package se.jbee.track.api;

import static java.lang.Integer.parseInt;
import static java.util.Collections.singletonMap;
import static se.jbee.track.cache.Matches.matches;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import se.jbee.track.api.Param.Command;
import se.jbee.track.cache.Cache;
import se.jbee.track.cache.Matches;
import se.jbee.track.db.DB;
import se.jbee.track.engine.Changes;
import se.jbee.track.engine.Limits;
import se.jbee.track.engine.Sample;
import se.jbee.track.engine.Server;
import se.jbee.track.engine.Transaction;
import se.jbee.track.engine.TransitionDenied;
import se.jbee.track.engine.TransitionDenied.Error;
import se.jbee.track.model.Criteria;
import se.jbee.track.model.Criteria.Property;
import se.jbee.track.model.Email;
import se.jbee.track.model.Name;
import se.jbee.track.model.Names;
import se.jbee.track.model.Page;
import se.jbee.track.model.Template;
import se.jbee.track.model.User;
import se.jbee.track.model.User.AuthState;

public class CachedViewService implements ViewService {

	private final Server server;
	private final DB db;
	private final Cache cache;
	private final Map<String, User> sessions = new ConcurrentHashMap<>();

	public CachedViewService(Server server, DB db, Cache cache) {
		this.server = server.with(Email.email("peter@example.com")); // for now
		this.db = db;
		this.cache = cache;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends View> T run(Params request, Class<T> response)
			throws ViewNotAvailable {
		Command cmd = request.value(Param.command, Command.query);
		switch (cmd) {
		case query:  if (response == ListView.class) return (T)list(request);
		case sample: if (response == SampleView.class) return (T)sample(request);
		default:
			throw new ViewNotAvailable(request, response);
		}
	}

	private User user(String id) {
		User res = new User(0);
		res.email = server.admin;
		res.alias = Name.as("peter");
		res.authState = AuthState.authenticated;
		res.authenticated = 1;
		if (true)
			return res;
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
		int tasks = parseInt(request.get(Param.task));
		Changes changes = Transaction.run(Sample.sample(users, outputs, versions, areas, categories, tasks, actor.alias), db, server.with(Limits.NONE), cache::invalidate);
		return new SampleView(actor, changes);
	}

	private void expectAdmin(User actor) {
		if (!server.isAdmin(actor))
			throw new TransitionDenied(Error.E25_ADMIN_REQUIRED, server.admin());
	}

	private ListView list(Params request) {
		User actor = user(request.get(Param.actor));
		Name output = request.name(Param.output);
		Matches indexing = matches(cache.matchesFor(actor, Criteria.index(output)));
		System.out.println(indexing);
		Matches matches = matches(cache.matchesFor(actor, Criteria.parse("[output=@][length=5][offset=0]").bindTo(singletonMap(Property.output, output))));
		System.out.println(matches);
		return new ListView(new User(1), System.currentTimeMillis(), new Page[0], new Page(1, Name.as("prod"), Name.as("area"), Name.as("xyz"), Template.template("Hello\n[output=@]\n")), matches);
	}



}
