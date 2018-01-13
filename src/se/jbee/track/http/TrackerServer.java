package se.jbee.track.http;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.io.File;
import java.io.IOException;

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

import se.jbee.track.api.Param;
import se.jbee.track.api.Params;
import se.jbee.track.db.DB;
import se.jbee.track.db.LMDB;
import se.jbee.track.model.Date;

public class TrackerServer extends AbstractHandler {

	public static void main( String[] args ) throws Exception
	{
		Server server = new Server(8080);

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
        server.setSessionIdManager(new HashSessionIdManager());
		try (DB db = createDB(args)) {
			TrackerServer app = new TrackerServer(new TrackerHttpUI(null, null));
			HashSessionManager manager = new HashSessionManager();
	        SessionHandler sessions = new SessionHandler(manager);
	        sessions.setHandler(app);
			handlers.addHandler(sessions);
			server.setHandler(handlers);
			server.start();
			server.join();
		} 
	}

	private static DB createDB(String[] args) throws IOException {
		String path = System.getProperty("java.io.tmpdir") + "/collaborate-"+Date.today()+"/";
		if (args.length >= 1) {
			path = args[0];
		}
		int sizeMB = 10;
		if (args.length >= 2) {
			sizeMB = min(100, max(10, Integer.parseInt(args[1])));
		}
		File file = new File(path);
		if (!file.exists() && !file.mkdirs()) {
			throw new IOException("Unable to create DB folder.");
		}
		return new LMDB(Env.create().setMapSize(1014 * 1024 * sizeMB), file);
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
