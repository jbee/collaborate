package vizio.http;

import java.io.IOException;
import java.util.HashMap;
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

import vizio.ctrl.DummyController;

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
		handlers.addHandler(new HttpTrackerServer());
		server.setHandler(handlers);
		server.start();
		server.join();
	}

	private HttpAdapter adapter = new HttpAdapter(new DummyController());

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		if ("/favicon.ico".equals(target)) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		} else {
			response.setContentType("text/html; charset=utf-8");
			response.setStatus(HttpServletResponse.SC_OK);

			Map<String, String> params = new HashMap<String, String>();
			Cookie[] cookies = request.getCookies();
			params.put("product", value(cookies, "TRACKER_CUR_PRODUCT"));
			adapter.respond(target, params, response.getWriter());
		}

		baseRequest.setHandled(true);
	}

	private String value(Cookie[] cookies, String name) {
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
