package se.jbee.track.ui.http;

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
import org.lmdbjava.Env;

import se.jbee.track.db.DB;
import se.jbee.track.db.LMDB;
import se.jbee.track.ui.ctrl.Ctrl;
import se.jbee.track.ui.ctrl.DBController;
import se.jbee.track.ui.ctrl.Params;

public class HttpTrackerServer extends AbstractHandler {

	public static void main( String[] args ) throws Exception
	{
		Server server = new Server(8080);

		ResourceHandler resource_handler = new ResourceHandler();
		// Configure the ResourceHandler. Setting the resource base indicates where the files should be served out of.
		// In this example it is the current directory but it can be configured to anything that the jvm has access to.
		resource_handler.setDirectoriesListed(true);
		resource_handler.setResourceBase("web");
		HandlerList handlers = new HandlerList();
		ContextHandler contextHandler = new ContextHandler("/static");
		contextHandler.setHandler(resource_handler);
		handlers.addHandler(contextHandler);
		DB db = createDB(args);
		handlers.addHandler(new HttpTrackerServer(new DBController(db, null, null, null)));
		server.setHandler(handlers);
		server.start();
		server.join();
	}

	private static DB createDB(String[] args) throws IOException {
		String path = System.getProperty("java.io.tmpdir") + "/db"+System.currentTimeMillis()+"/";
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
	
	private final HttpAdapter adapter;
	
	public HttpTrackerServer(Ctrl crtl) {
		super();
		this.adapter = new HttpTrackerAdapter(crtl);
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		if ("/favicon.ico".equals(target)) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		} else {
			response.setContentType("text/html; charset=utf-8");
			response.setStatus(HttpServletResponse.SC_OK);
			//Cookie[] cookies = request.getCookies();
			//TODO use session to fill data
			adapter.respond(Params.fromPath(target), response.getWriter());
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
