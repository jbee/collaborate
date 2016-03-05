package vizio.http;

import static java.lang.Character.isDigit;
import static java.lang.Integer.parseInt;
import static vizio.Name.MY;
import static vizio.Name.UNKNOWN;
import static vizio.Name.as;

import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.util.Map;

import vizio.IDN;
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
public class HttpAdapter {

	private final Controller ctrl;

	public HttpAdapter(Controller ctrl) {
		super();
		this.ctrl = ctrl;
	}

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
		View view = ctrl.view(ctx.space, ctx.site);
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
		String[] segments = path.split("/");
		Action action = Action.valueOf(segments[0]);
		Context ctx = new Context();
		ctx.action = action;
		ctx.user = UNKNOWN;
		//TODO get user from params (session)
		switch (action) {
		case view:
			// non-user views
			// :me is substituted with the ctx.user (viewer)
			ctx.product=as(segments[1]);
			ctx.space=as("@product."+segments[1]); // each product has its own common views
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
				// a product's home page, initially copied from @my:@product
				ctx.site=as("@home");
			}
			break;
		case target:
		case approach:
		case abandon:
			ctx.product=as(segments[1]);
			ctx.task=new IDN(parseInt(segments[2]));
			break;
		case user:
			if (segments.length==1) {
				ctx.space=MY;
				ctx.site=as("@users");
			} else {
				// these are the views made by the users themselves
				// here :me is substituted with ctx.space (not viewing user)
				ctx.space=as(segments[1]);
				// a third menu shows the sites in space (in case the viewer isn't that user himself)
				if (segments.length > 2) {
					ctx.site=as(segments[2]);
				} else {
					// this site is customized by the user (since space is the user's space)
					// initially it is copied from @my:@home
					ctx.site=as("@home");
				}
			}
			// TODO how to use a site created by the viewer or a common one for a specific user?
			break;
		case my:
			// these are common views
			// :me is substituted with ctx.user (viewer)
			ctx.space=MY; // belongs to all and nobody in particular (make this a system config? what users are registered to "product" @my)
			ctx.site=as(segments[1]); // these are custom sites created, like "dashboard"
		}
		return ctx;
	}

}
