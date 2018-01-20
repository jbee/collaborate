package se.jbee.track.http;

import java.io.IOException;

import javax.servlet.ServletException;
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

import se.jbee.track.Application;
import se.jbee.track.api.Param;
import se.jbee.track.api.Params;
import se.jbee.track.api.UserInterface;

public class JettyHttpServer extends AbstractHandler {

	public static void main(String[] args) throws Exception {
		se.jbee.track.Server config = se.jbee.track.Server.parse(args);
		UserInterface ui = Application.createHttpUserInterface(config);

		Server httpServer = new Server(8080);
		ResourceHandler resource_handler = new ResourceHandler();
		if (config.isTemporary) {
			resource_handler.setCacheControl("no-store,no-cache,must-revalidate");
		}
		// Configure the ResourceHandler. Setting the resource base indicates where the
		// files should be served out of.
		// In this example it is the current directory but it can be configured to
		// anything that the jvm has access to.
		resource_handler.setDirectoriesListed(true);
		resource_handler.setResourceBase("web");
		HandlerList handlers = new HandlerList();

		ContextHandler contextHandler = new ContextHandler("/static");
		contextHandler.setHandler(resource_handler);
		handlers.addHandler(contextHandler);
		httpServer.setSessionIdManager(new HashSessionIdManager());
		JettyHttpServer app = new JettyHttpServer(ui);
		HashSessionManager manager = new HashSessionManager();
		SessionHandler sessions = new SessionHandler(manager);
		sessions.setHandler(app);
		handlers.addHandler(sessions);
		httpServer.setHandler(handlers);
		httpServer.start();
		httpServer.join();
	}

	private final UserInterface ui;

	public JettyHttpServer(UserInterface ui) {
		this.ui = ui;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		if ("/favicon.ico".equals(target)) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		} else {
			response.setContentType("text/html; charset=utf-8");
			Params params = Params.fromPath(target);
			params.set(Param.actor, baseRequest.getSession(true).getId());
			if (params.getOrDefault(Param.viewed, "").equals("@")) {
				params.set(Param.viewed, params.get(Param.actor));
			}
			long nsStart = System.nanoTime();
			response.setStatus(ui.respond(params, response.getWriter()));
			System.out.println((System.nanoTime() - nsStart)/1000L+" us");
		}
		baseRequest.setHandled(true);
	}

}
