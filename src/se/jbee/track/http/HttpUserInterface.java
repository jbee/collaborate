package se.jbee.track.http;

import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.util.Map;

import se.jbee.track.api.ListView;
import se.jbee.track.api.Param;
import se.jbee.track.api.Param.Command;
import se.jbee.track.api.Params;
import se.jbee.track.api.SampleView;
import se.jbee.track.api.UserInterface;
import se.jbee.track.api.View;
import se.jbee.track.api.ViewService;
import se.jbee.track.html.HtmlRenderer;
import se.jbee.track.html.HtmlWriter;

/**
 * Connects the HTTP world with the general {@link UserInterface} abstraction.
 */
public class HttpUserInterface implements UserInterface {

	private final ViewService views;
	private final Map<Class<?>, HtmlRenderer<?>> renderers;

	public HttpUserInterface(ViewService views, Map<Class<?>, HtmlRenderer<?>> renderers) {
		this.views = views;
		this.renderers = renderers;
	}

	@Override
	public int respond(Params params, PrintWriter out) {
		if (Command.sample.name().equals(params.get(Param.command))) {
			runAndRender(SampleView.class, params, out);
		} else {
			runAndRender(ListView.class, params, out);
		}
		//TODO render page
		if (true)
			return HttpURLConnection.HTTP_OK;
		return HttpURLConnection.HTTP_NOT_FOUND;
	}

	private <T extends View> void runAndRender(Class<T> pageType, Params params, PrintWriter out) {
		T page = views.run(params, pageType);
		@SuppressWarnings("unchecked")
		HtmlRenderer<T> renderer = (HtmlRenderer<T>) renderers.get(pageType);
		renderer.render(page, new HtmlWriter(out));
	}

}
