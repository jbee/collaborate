package se.jbee.task.api;

import java.io.PrintWriter;

/**
 * A interface to decouple the user interface from any particular HTTP
 * server implementation so that one case use a stand alone server like jetty or
 * a servlet based one or other options.
 *
 * This also allows for testing as if making HTTP request without actually
 * running a HTTP server.
 *
 * Also other types of user interfaces are possible like a command line interface.
 *
 * @author jan
 */
@FunctionalInterface
public interface UserInterface {

	/**
	 * Responds the request by writing to output stream.
	 *
	 * @param params user input (what to do)
	 * @param out target for the user interface output
	 * @return result status code (the HTTP status code in case of an HTTP impl)
	 */
	int respond(Params params, PrintWriter out);

}