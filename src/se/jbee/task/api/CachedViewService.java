package se.jbee.task.api;

import static java.lang.Integer.parseInt;
import static se.jbee.task.cache.Matches.matches;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import se.jbee.task.api.Param.Command;
import se.jbee.task.cache.Cache;
import se.jbee.task.cache.Matches;
import se.jbee.task.db.DB;
import se.jbee.task.engine.ChangeLog;
import se.jbee.task.engine.Limits;
import se.jbee.task.engine.Sample;
import se.jbee.task.engine.Server;
import se.jbee.task.engine.Transaction;
import se.jbee.task.engine.TransitionDenied;
import se.jbee.task.engine.TransitionDenied.Error;
import se.jbee.task.model.Criteria;
import se.jbee.task.model.Criteria.Property;
import se.jbee.task.model.Email;
import se.jbee.task.model.Name;
import se.jbee.task.model.Names;
import se.jbee.task.model.Page;
import se.jbee.task.model.Template;
import se.jbee.task.model.User;
import se.jbee.task.model.User.AuthState;

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
		ChangeLog changes = Transaction.run(Sample.sample(users, outputs, versions, areas, categories, tasks, actor.alias), db, server.with(Limits.NONE), cache::invalidate);
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
		String query = "[output=@][area=@][cause=finding][temperature < 0][length=5][offset=0][coloration=cause]";
		Map<Property, Name> args = new EnumMap<>(Property.class);
		args.put(Property.output, output);
		Name area = request.name(Param.area);
		if (area != Name.UNKNOWN) {
			args.put(Property.area, area);
		}
		Matches matches = matches(cache.matchesFor(actor, Criteria.parse(query).bindTo(args)));
		//TODO the results must replace the generic Criteria with the bound one for rendering
		return new ListView(new User(1), System.currentTimeMillis(), new Page[0], new Page(1, Name.as("prod"), Name.as("area"), Name.as("xyz"), Template.parseTemplate("Hello\n"+query+"\n")), matches);
	}



}
