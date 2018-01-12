package se.jbee.track.ui.http;

import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.util.Map;

import se.jbee.track.api.ListPage;
import se.jbee.track.api.Page;
import se.jbee.track.api.PageService;
import se.jbee.track.api.Params;
import se.jbee.track.html.HtmlPageRenderer;
import se.jbee.track.html.HtmlWriter;

/**
 * Connects the HTTP world with the general {@link Controller} abstraction. Its
 * task is also to make the implementation independent from a specific HTTP
 * server implementation and to allow for testing as if making HTTP request
 * without actually running a HTTP server.
 */
public class TrackerHttpUI implements HttpUI {

	private final PageService ps;
	private final Map<Class<?>, HtmlPageRenderer<?>> renderers;

	public TrackerHttpUI(PageService ps, Map<Class<?>, HtmlPageRenderer<?>> renderers) {
		super();
		this.ps = ps;
		this.renderers = renderers;
	}

	@Override
	public int respond(Params params, PrintWriter out) {
		runAndRender(ListPage.class, params, out);
		//TODO render page
		if (true)
			return HttpURLConnection.HTTP_OK;
		return HttpURLConnection.HTTP_NOT_FOUND;
	}

	private <T extends Page> void runAndRender(Class<T> pageType, Params params, PrintWriter out) {
		T page = ps.run(params, pageType);
		@SuppressWarnings("unchecked")
		HtmlPageRenderer<T> renderer = (HtmlPageRenderer<T>) renderers.get(pageType);
		renderer.render(page, new HtmlWriter(out));
	}
	
}
