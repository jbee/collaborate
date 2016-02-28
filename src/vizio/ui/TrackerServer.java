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
import vizio.Product;
import vizio.Task;
import vizio.Tracker;
import vizio.User;
import vizio.Version;
import vizio.view.Coloring;
import vizio.view.Page;
import vizio.view.Widget;

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


	private TrackerServer() {
	}
	
	private long now;
	
	private Task[] testTasks() {
		now = currentTimeMillis();
		Tracker tracker = new Tracker(() -> { now += 70000; return now; } );
		Task[] tasks = new Task[5];
		User user = tracker.register(named("tester"), "test@example.com", "xxx");
		tracker.activate(user);
		Product product = tracker.initiate(named("vizio"), user);
		Area area = tracker.compart(product, named("core"), user);
		Area ui = tracker.compart(product, named("ui"), user);
		Version v0_1= new Version(named("v0.1"));
		tasks[0] = tracker.reportDefect(product, "Something is wrong with...", user, area, Version.UNKNOWN, false);
		tasks[1] = tracker.reportDefect(product, "Regression for 0.1 showed bug...", user, area, v0_1, true);
		tasks[2] = tracker.reportProposal(product, "We should count ...", user, product.origin);
		tasks[3] = tracker.reportIdea(product, "Maybe make everything...", user, product.unknown);
		tasks[4] = tracker.reportProposal(product, "Use bold text for everything important", user, ui);
		tracker.mark(tasks[1], user);
		tracker.start(tasks[2], user);
		tasks[0].heat = 97;
		tasks[1].heat = 78;
		tasks[2].heat = 56;
		tasks[3].heat = 28;
		tasks[4].heat = 14;
		return tasks;
	}


	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		response.setContentType("text/html; charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);

        PrintWriter out = response.getWriter();
        Widget widget = new Widget();
        widget.list = testTasks();
        widget.scheme = Coloring.temp;
        widget.caption = "Assorted tasks";
		new HTMLRenderer(out).render(new Page("Test", widget));

        baseRequest.setHandled(true);
	}

}
