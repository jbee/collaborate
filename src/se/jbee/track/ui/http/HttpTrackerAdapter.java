package se.jbee.track.ui.http;

import java.io.PrintWriter;
import java.net.HttpURLConnection;

import se.jbee.track.ui.ctrl.Ctrl;
import se.jbee.track.ui.ctrl.Ctrl.DynamicPage;
import se.jbee.track.ui.ctrl.Params;

/**
 * Connects the HTTP world with the general {@link Controller} abstraction. Its
 * task is also to make the implementation independent from a specific HTTP
 * server implementation and to allow for testing as if making HTTP request
 * without actually running a HTTP server.
 */
public class HttpTrackerAdapter implements HttpAdapter {

	private final Ctrl ctrl;

	public HttpTrackerAdapter(Ctrl ctrl) {
		super();
		this.ctrl = ctrl;
	}

	@Override
	public int respond(Params params, PrintWriter out) {
		DynamicPage page = ctrl.query(params);
		HTMLRenderer renderer = new HTMLRenderer(out, page.actor);
		//TODO render page
		if (true)
			return HttpURLConnection.HTTP_OK;
		return HttpURLConnection.HTTP_NOT_FOUND;
	}

}
