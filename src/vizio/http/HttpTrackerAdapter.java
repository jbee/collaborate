package vizio.http;

import static java.lang.Character.isDigit;
import static java.lang.Integer.parseInt;
import static vizio.Name.as;

import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.util.Map;

import vizio.IDN;
import vizio.Name;
import vizio.Task;
import vizio.User;
import vizio.ctrl.Action;
import vizio.ctrl.Context;
import vizio.ctrl.Controller;
import vizio.view.Page;
import vizio.view.View;
import vizio.view.Widget;

/**
 * Connects the HTTP world with the general {@link Controller} abstraction. Its
 * task is also to make the implementation independent from a specific HTTP
 * server implementation and to allow for testing as if making HTTP request
 * without actually running a HTTP server.
 *
 * @author jan
 */
public class HttpTrackerAdapter implements HttpAdapter {

	private final Controller ctrl;

	public HttpTrackerAdapter(Controller ctrl) {
		super();
		this.ctrl = ctrl;
	}

	/* (non-Javadoc)
	 * @see vizio.http.HttpAdapter#respond(java.lang.String, java.util.Map, java.io.PrintWriter)
	 */
	@Override
	public int respond(String path, Map<String, String> params, PrintWriter out) {
		Context ctx = map(path, params);
		switch(ctx.action) {
		case as:
		case my:
		case user:
		case view: return view(ctx, out);
		default:
			return HttpURLConnection.HTTP_NOT_FOUND;
		}
	}

	private int view(Context ctx, PrintWriter out) {
		View view = ctrl.view(ctx.owner, ctx.site);
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
		ctx.user = user == null ? Name.ANONYMOUS : as(user);
		String[] segments = path.split("/");
		Action action = Action.valueOf(segments[0]);
		ctx.action = action;
		switch (action) {
		case view:
			// non-user views
			// :me is substituted with the ctx.user (viewer)
			ctx.product=as(segments[1]);
			ctx.owner=as("@product."+segments[1]); // each product has its own common views
			if (segments.length > 2) {
				if ("v".equals(segments[2])) {
					ctx.version=as(segments[3]);
					ctx.site=as("@version");
				} else if (isDigit(segments[2].charAt(0))) {
					ctx.task=new IDN(parseInt(segments[2]));
					ctx.site=as("@task");
				} else {
					String area = segments[2];
					if (segments.length > 3) {
						ctx.serial=new IDN(parseInt(segments[3]));
						ctx.site=as("@request");
					} else {
						if (area.matches("^.*?-[0-9]+$")) {
							ctx.area=as(area.substring(0, area.lastIndexOf('-')));
							ctx.serial=new IDN(parseInt(area.substring(area.lastIndexOf('-')+1)));
							ctx.site=as("@request");
						} else {
							ctx.area=as(area);
							ctx.site=as("@area");
						}
					}
				}
			} else { // /view/<product>/
				// a product's home page
				ctx.site=as("@home");
			}
			break;
		case target:
		case approach:
		case abandon:
			ctx.product=as(segments[1]);
			ctx.task=new IDN(parseInt(segments[2]));
			break;
		case as: // almost the same as /user/x/; /as/x/ shows the site of user x but having :me subst with the current user
		case user:
			if (segments.length==1) {
				ctx.owner=Name.MASTER;
				ctx.site=as("@users");
			} else {
				// these are the views made by the users themselves
				// here :me is substituted with ctx.owner (not viewing user)
				ctx.owner=as(segments[1]);
				// a third menu shows the sites in space (in case the viewer isn't that user himself)
				if (segments.length > 2) {
					ctx.site=as(segments[2]);
				} else {
					// this site is customized by the user (since space is the user's space)
					ctx.site=as("@home");
				}
			}
			break;
		case my: // this is just a shortcut to the sites of the logged in user (or @anonymous sites)
			ctx.owner=ctx.user;
			ctx.site=as(segments[1]);

		// case ? TODO
			// these are common views
			// :me is substituted with ctx.user (viewer)
			// belongs to all and nobody in particular

		// TODO how to use a site created by the viewer or a common one for a specific user?

		}
		return ctx;
	}

}
