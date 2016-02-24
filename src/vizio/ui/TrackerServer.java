package vizio.ui;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class TrackerServer extends AbstractHandler {

	public static void main( String[] args ) throws Exception
    {
        Server server = new Server(8080);
        server.setHandler(new TrackerServer());
        server.start();
        server.join();
    }

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		response.setContentType("text/html; charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);

        PrintWriter out = response.getWriter();

        out.println("<h1>Hi"+target+"</h1>");

        baseRequest.setHandled(true);
	}

}
