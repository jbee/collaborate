package se.jbee.track.http;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.io.File;
import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.session.HashSessionIdManager;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.server.session.SessionHandler;
import org.lmdbjava.Env;

import se.jbee.track.api.CachedViewService;
import se.jbee.track.api.ListView;
import se.jbee.track.api.Param;
import se.jbee.track.api.Params;
import se.jbee.track.api.SampleView;
import se.jbee.track.cache.CacheCluster;
import se.jbee.track.db.DB;
import se.jbee.track.db.LMDB;
import se.jbee.track.html.HtmlRenderer;
import se.jbee.track.html.ListViewHtmlRenderer;
import se.jbee.track.html.SampleViewHtmlRenderer;
import se.jbee.track.model.Date;
import se.jbee.track.model.Email;

public class TrackerServer extends AbstractHandler {

	public static void main( String[] args ) throws Exception
	{
		se.jbee.track.engine.Server config = se.jbee.track.engine.Server.parse(args);
		
		Server httpServer = new Server(8080);

		ResourceHandler resource_handler = new ResourceHandler();
		resource_handler.setCacheControl("no-store,no-cache,must-revalidate");
		// Configure the ResourceHandler. Setting the resource base indicates where the files should be served out of.
		// In this example it is the current directory but it can be configured to anything that the jvm has access to.
		resource_handler.setDirectoriesListed(true);
		resource_handler.setResourceBase("web");
		HandlerList handlers = new HandlerList();

		ContextHandler contextHandler = new ContextHandler("/static");
		contextHandler.setHandler(resource_handler);
		handlers.addHandler(contextHandler);
        httpServer.setSessionIdManager(new HashSessionIdManager());

        try (DB db = createDB(config)) {
			TrackerServer app = createServer(config, db);
			HashSessionManager manager = new HashSessionManager();
	        SessionHandler sessions = new SessionHandler(manager);
	        sessions.setHandler(app);
			handlers.addHandler(sessions);
			httpServer.setHandler(handlers);
			httpServer.start();
			httpServer.join();
		}
	}

	private static TrackerServer createServer(se.jbee.track.engine.Server config, DB db) throws IOException {
        Map<Class<?>, HtmlRenderer<?>> renderers = new IdentityHashMap<>();
        renderers.put(ListView.class, new ListViewHtmlRenderer());
        renderers.put(SampleView.class, new SampleViewHtmlRenderer());
        config = config.with(Email.email("peter@example.com"));
        return new TrackerServer(new TrackerHttpUI(new CachedViewService(config, db, new CacheCluster(db, config.clock)), renderers));
	}

	private static DB createDB(se.jbee.track.engine.Server config) throws IOException {
		return new LMDB(Env.create().setMapSize(config.sizeDB).setMaxReaders(8), config.pathDB);
	}

	private final HttpUI ui;

	public TrackerServer(HttpUI ui) {
		this.ui = ui;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		if ("/favicon.ico".equals(target)) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		} else {
			response.setContentType("text/html; charset=utf-8");
			//Cookie[] cookies = request.getCookies();
			Params params = Params.fromPath(target);
			params.set(Param.actor, baseRequest.getSession(true).getId());
			if (params.getOrDefault(Param.viewed, "").equals("@")) {
				params.set(Param.viewed, params.get(Param.actor));
			}
			response.setStatus(ui.respond(params, response.getWriter()));
		}
		baseRequest.setHandled(true);
	}

	private static String value(Cookie[] cookies, String name) {
		if (cookies == null)
			return null;
		for (Cookie c : cookies) {
			if (c.getName().equals(name)) {
				return c.getValue();
			}
		}
		return null;
	}

}
