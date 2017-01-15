package vizio.http;

import static java.lang.Character.isDigit;
import static java.lang.Integer.parseInt;
import static vizio.ctrl.Action.view;
import static vizio.model.ID.Type.Area;
import static vizio.model.ID.Type.Product;
import static vizio.model.ID.Type.Task;
import static vizio.model.ID.Type.User;
import static vizio.model.ID.Type.Version;
import static vizio.model.Name.as;

import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.util.Map;

import vizio.ctrl.Action;
import vizio.ctrl.Context;
import vizio.ctrl.Controller;
import vizio.model.IDN;
import vizio.model.Name;
import vizio.model.Task;
import vizio.model.User;
import vizio.view.Page;
import vizio.view.View;
import vizio.view.Widget;

/**
 * Connects the HTTP world with the general {@link Controller} abstraction. Its
 * task is also to make the implementation independent from a specific HTTP
 * server implementation and to allow for testing as if making HTTP request
 * without actually running a HTTP server.
 */
public class HttpTrackerAdapter implements HttpAdapter {

	private final Controller ctrl;

	public HttpTrackerAdapter(Controller ctrl) {
		super();
		this.ctrl = ctrl;
	}

	@Override
	public int respond(String path, Map<String, String> params, PrintWriter out) {
		Context ctx = map(path, params);
		switch(ctx.action) {
		case my:
		case user:
		case view: return view(ctx, out);
		default:
			return HttpURLConnection.HTTP_NOT_FOUND;
		}
	}

	private int view(Context ctx, PrintWriter out) {
		View view = ctrl.view(ctx);
		User viewer = ctrl.user(ctx.user);
		HTMLRenderer renderer = new HTMLRenderer(out, viewer);
		renderer.render(new Page(ctrl.menus(ctx), view, fetch(view, ctx)));
		return HttpURLConnection.HTTP_OK;
	}

	private Task[][][] fetch(View view, Context ctx) {
		Task[][][] data = new Task[view.silos.length][][];
		for (int i = 0; i < view.silos.length; i++) {
			Widget[] widgets = view.silos[i].widgets;
			data[i] = new Task[widgets.length][];
			for (int j = 0; j < widgets.length; j++) {
				data[i][j] = ctrl.tasks(widgets[j].query, ctx);
			}
		}
		return data;
	}

	/**
	 * Takes the path and parameters (incl. session data) and derives the
	 * application level request {@link Context} from it.
	 */
	private Context map(String path, Map<String, String> params) {
		if (path.startsWith("/"))
			path = path.substring(1);
		Context ctx = new Context();
		String user = params.get(KEY_SESSION_USER);
		ctx.currentUser = user == null ? Name.ANONYMOUS : as(user);
		ctx.action=view;
		if (path.isEmpty() || "/".equals(path))
			return ctx;
		String[] segments = path.split("/");
		Action action = Action.valueOf(segments[0]);
		ctx.action = action;
		switch (action) {
		case view:
			// non-user views
			ctx.type=Product;
			ctx.product=as(segments[1]);
			if (segments.length > 2) {
				if ("v".equals(segments[2])) {
					ctx.type=Version;
					ctx.version=as(segments[3]);
					siteAtIndex(4, segments, ctx);
				} else if (isDigit(segments[2].charAt(0))) {
					ctx.type=Task;
					ctx.task=new IDN(parseInt(segments[2]));
					siteAtIndex(3, segments, ctx);
				} else {
					ctx.type=Area;
					String area = segments[2];
					if (area.matches("^.*?-[0-9]+$")) {
						ctx.area=as(area.substring(0, area.lastIndexOf('-')));
						ctx.serial=new IDN(parseInt(area.substring(area.lastIndexOf('-')+1)));
						siteAtIndex(3, segments, ctx);
					} else {
						ctx.area=as(area);
						if (segments.length > 3) {
							ctx.serial=new IDN(parseInt(segments[3]));
						}
						siteAtIndex(4, segments, ctx);
					}
					if (ctx.serial!=null) {
						ctx.type=Task;
					}
				}
			}
			break;
		case enlist:
		case approach:
		case abandon:
			ctx.type=Task;
			ctx.product=as(segments[1]);
			ctx.task=new IDN(parseInt(segments[2]));
			break;
		case peek: // the semantics are just different later on in the substitution of :me
		case user:
			ctx.type=User;
			if (segments.length > 1) {
				ctx.user=as(segments[1]);
				siteAtIndex(2, segments, ctx);
			}
			break;
		case my: // this is just a shortcut to the sites of the logged in user (or @anonymous sites)
			ctx.type=User;
			ctx.user=ctx.currentUser;
			siteAtIndex(1, segments, ctx);
			break;
		}
		return ctx;
	}

	private static void siteAtIndex(int idx, String[] segments, Context ctx) {
		if (idx < segments.length) {
			ctx.site=as(segments[idx]);
		}
	}

}
