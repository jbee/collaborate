package vizio.ui;

import static java.lang.System.currentTimeMillis;
import static vizio.Name.named;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;

import vizio.Area;
import vizio.Goal;
import vizio.IDN;
import vizio.Motive;
import vizio.Product;
import vizio.Task;
import vizio.Tracker;
import vizio.User;
import vizio.view.Coloring;

public class TrackerServer extends AbstractHandler {

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
        handlers.addHandler(new TrackerServer());
        server.setHandler(handlers);
        server.start();
        server.join();
    }

	private Tracker tracker = new Tracker(() -> currentTimeMillis());
	private Task[] tasks = new Task[5];
	private User user;
	private Product product;
	private Area area;

	private TrackerServer() {
		user = tracker.register("test@example.com");
		product = tracker.introduce(named("vizio"), user);
		area = tracker.structure(product.name, named("core"), user);
		for (int i = 0; i < tasks.length; i++) {
			tasks[i] = makeTask(i);
		}
	}

	private Task makeTask(int i) {
		Task task = tracker.track(Motive.values()[i % Motive.values().length], Goal.clarification, "This is issue no. "+i, user);
		task.id = new IDN(i);
		tracker.relocate(task, area, user);
		return task;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		response.setContentType("text/html; charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);

        PrintWriter out = response.getWriter();
        out.append("<!DOCTYPE html>");
        out.append("<head><link rel='stylesheet' href='/static/vizio.css'></head><body>");
        new HTMLRenderer(out).render(tasks, Coloring.motive);
        out.append("</body>");

        baseRequest.setHandled(true);
	}

}
