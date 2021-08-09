package se.jbee.task.http;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.session.HashSessionIdManager;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.server.session.SessionHandler;

import se.jbee.task.api.Param;
import se.jbee.task.api.Params;
import se.jbee.task.api.UserInterface;

public class JettyHttpServer extends AbstractHandler {

	public static Server create(se.jbee.task.engine.Server config, UserInterface ui) {
		Server httpServer = new Server(config.port);
		HandlerList handlers = new HandlerList();
		handlers.addHandler(staticContentHandler(config));
		handlers.addHandler(dynamicContentHandler(ui));
		httpServer.setHandler(handlers);
		httpServer.setSessionIdManager(new HashSessionIdManager());
		return httpServer;
	}

	private static Handler dynamicContentHandler(UserInterface ui) {
		JettyHttpServer app = new JettyHttpServer(ui);
		HashSessionManager manager = new HashSessionManager();
		SessionHandler sessions = new SessionHandler(manager);
		sessions.setHandler(app);
		return sessions;
	}

	private static Handler staticContentHandler(se.jbee.task.engine.Server config) {
		ResourceHandler statics = new ResourceHandler();
		if (config.isTemporary) {
			statics.setCacheControl("no-store,no-cache,must-revalidate");
		}
		statics.setDirectoriesListed(true);
		statics.setResourceBase("web");
		ContextHandler staticsContext = new ContextHandler("/static");
		staticsContext.setHandler(statics);
		return staticsContext;
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
			Params params = Params.fromPath(target, !request.getMethod().equalsIgnoreCase("GET"));
			params.set(Param.actor, baseRequest.getSession(true).getId());
			if (params.getOrDefault(Param.viewed, "").equals("@")) {
				params.set(Param.viewed, params.get(Param.actor));
			}
			long nsStart = System.nanoTime();
			response.setStatus(ui.respond(params, response.getWriter()));
			System.out.println((System.nanoTime() - nsStart)/1000000L+" ms");
		}
		baseRequest.setHandled(true);
	}

}
