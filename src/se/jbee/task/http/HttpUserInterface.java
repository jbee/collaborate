package se.jbee.task.http;

import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.util.Map;

import se.jbee.task.api.ListView;
import se.jbee.task.api.Param;
import se.jbee.task.api.Params;
import se.jbee.task.api.SampleView;
import se.jbee.task.api.UserInterface;
import se.jbee.task.api.View;
import se.jbee.task.api.ViewService;
import se.jbee.task.api.Param.Command;
import se.jbee.task.html.HtmlRenderer;
import se.jbee.task.html.HtmlWriter;

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
