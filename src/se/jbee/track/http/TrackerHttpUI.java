package se.jbee.track.http;

import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.util.Map;

import se.jbee.track.api.ListView;
import se.jbee.track.api.View;
import se.jbee.track.api.ViewService;
import se.jbee.track.api.Params;
import se.jbee.track.html.HtmlRenderer;
import se.jbee.track.html.HtmlWriter;

/**
 * Connects the HTTP world with the general {@link Controller} abstraction. Its
 * task is also to make the implementation independent from a specific HTTP
 * server implementation and to allow for testing as if making HTTP request
 * without actually running a HTTP server.
 */
public class TrackerHttpUI implements HttpUI {

	private final ViewService ps;
	private final Map<Class<?>, HtmlRenderer<?>> renderers;

	public TrackerHttpUI(ViewService ps, Map<Class<?>, HtmlRenderer<?>> renderers) {
		super();
		this.ps = ps;
		this.renderers = renderers;
	}

	@Override
	public int respond(Params params, PrintWriter out) {
		runAndRender(ListView.class, params, out);
		//TODO render page
		if (true)
			return HttpURLConnection.HTTP_OK;
		return HttpURLConnection.HTTP_NOT_FOUND;
	}

	private <T extends View> void runAndRender(Class<T> pageType, Params params, PrintWriter out) {
		T page = ps.run(params, pageType);
		@SuppressWarnings("unchecked")
		HtmlRenderer<T> renderer = (HtmlRenderer<T>) renderers.get(pageType);
		renderer.render(page, new HtmlWriter(out));
	}
	
}
