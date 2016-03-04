package vizio.http;

import static java.lang.Character.isDigit;
import static java.lang.Integer.parseInt;
import static vizio.Name.as;
import static vizio.ctrl.Action.view;

import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.util.Map;

import vizio.IDN;
import vizio.Task;
import vizio.User;
import vizio.ctrl.Action;
import vizio.ctrl.Context;
import vizio.ctrl.Controller;
import vizio.view.View;
import vizio.view.Widget;

/**
 * Connects the HTTP world with the general {@link Controller} abstraction.
 *
 * @author jan
 */
public class HttpAdapter {

	private final Controller ctrl;

	public HttpAdapter(Controller ctrl) {
		super();
		this.ctrl = ctrl;
	}

	public int respond(String path, Map<String, String> params, PrintWriter out) {
		Context ctx = map(path, params);
		switch(ctx.action) {
		case view: return view(ctx, out);
		default:
			return HttpURLConnection.HTTP_NOT_FOUND;
		}
	}

	private int view(Context ctx, PrintWriter out) {
		View view = ctrl.show(ctx.space(), ctx.site());
		User viewer = ctrl.user(ctx.user);
		new HTMLRenderer(out, viewer).render(view, fetch(view, ctx));
		return HttpURLConnection.HTTP_OK;
	}

	private Task[][][] fetch(View view, Context ctx) {
		Task[][][] data = new Task[view.silos.length][][];
		for (int i = 0; i < view.silos.length; i++) {
			Widget[] widgets = view.silos[i].widgets;
			data[i] = new Task[widgets.length][];
			for (int j = 0; j < widgets.length; j++) {
				data[i][j] = ctrl.select(widgets[j].query, ctx);
			}
		}
		return data;
	}

	private Context map(String path, Map<String, String> params) {
		String[] segments = path.split("/");
		Action action = Action.valueOf(segments[0]);
		Context ctx = new Context();
		ctx.action = action;
		switch (action) {
		case view:
			ctx.product=as(segments[1]);
			if (segments.length > 2) {
				if ("v".equals(segments[2])) {
					ctx.version=as(segments[3]);
				} else if (isDigit(segments[2].charAt(0))) {
					ctx.task=new IDN(parseInt(segments[2]));
				} else {
					ctx.area=as(segments[2]);
				}
				//TODO serials: both /area-serial/ and /area/serial/ works since in a name minus cannot be followed by a digit
			}
			break;
		case target:
		case approach:
		case abandon:
			ctx.product=as(segments[1]);
			ctx.task=new IDN(parseInt(segments[2]));
			break;
		case user:
			ctx.action=view;
			ctx.user=as(segments[1]);
			if (segments.length > 2) {
				ctx.site=as(segments[2]);
			}
			break;
		case my:
			ctx.action=view;
			ctx.user=as(params.get("user"));
			ctx.site=as(segments[1]);
		}
		return ctx;
	}

}
