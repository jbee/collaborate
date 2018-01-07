package se.jbee.track.ui.http;

import java.io.PrintWriter;
import java.net.HttpURLConnection;

import se.jbee.track.ui.ctrl.Ctrl;
import se.jbee.track.ui.ctrl.Ctrl.ListPage;
import se.jbee.track.ui.ctrl.Params;

/**
 * Connects the HTTP world with the general {@link Controller} abstraction. Its
 * task is also to make the implementation independent from a specific HTTP
 * server implementation and to allow for testing as if making HTTP request
 * without actually running a HTTP server.
 */
public class TrackerHttpUI implements HttpUI {

	private final Ctrl ctrl;

	public TrackerHttpUI(Ctrl ctrl) {
		super();
		this.ctrl = ctrl;
	}

	@Override
	public int respond(Params params, PrintWriter out) {
		ListPage page = ctrl.list(params);
		HTMLRenderer renderer = new HTMLRenderer(out, page.actor);
		renderer.render(page);
		//TODO render page
		if (true)
			return HttpURLConnection.HTTP_OK;
		return HttpURLConnection.HTTP_NOT_FOUND;
	}

}
