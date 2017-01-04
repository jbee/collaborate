package vizio.http;

import static vizio.Date.date;

import java.io.PrintWriter;

import vizio.Motive;
import vizio.Name;
import vizio.Names;
import vizio.Purpose;
import vizio.Site;
import vizio.Status;
import vizio.Task;
import vizio.Heat;
import vizio.Tracker;
import vizio.User;
import vizio.ctrl.Action;
import vizio.view.Coloring;
import vizio.view.Menu;
import vizio.view.Page;
import vizio.view.View;
import vizio.view.View.Silo;
import vizio.view.Widget;

public class HTMLRenderer {

	private final PrintWriter out;
	private final long now;
	private final User viewer;

	public HTMLRenderer(PrintWriter out, User viewer) {
		super();
		this.out = out;
		this.viewer = viewer;
		this.now = System.currentTimeMillis();
	}

	public void render(Page page) {
		out.append("<!DOCTYPE html>");
		out.append("<head><link rel='stylesheet' href='/static/vizio.css'></head><body>");
		renderStaticMenu();
		render(page.menus);
		render(page.view, page.data);

		out.append("<div class='footer'><div class='column'>");
		renderTable(Coloring.motive, Motive.class);
		renderTable(Coloring.goal, Purpose.class);
		renderTable(Coloring.status, Status.class);
		renderTable(Coloring.temp, Heat.class);
		out.append("</div></div>");
		out.append("</body>");
	}

	private void renderStaticMenu() {
		out.append("<div class='menu'><h1>LiTTr</h1><span class='group'></span>");
		// Liberated Task Coordination
		// Reality Tracker (track what really is not would we believe or want to be)

		out.append("</div>");
	}

	public void renderTable(Coloring scheme, Class<? extends Enum<?>> type) {
		out.append("<table class='legend scheme-").append(scheme.name()).append("'>");
		out.append("<tr><th>").append(scheme.name()).append("</th></td>");
		for (Enum<?> v : type.getEnumConstants()) {
			out.append("<tr class='").append(scheme.name()).append("-").append(v.name()).append("'><td>").append(v.name()).append("</td></tr>");
		}
		out.append("</table>");
	}

	public void render(Menu[] menus) {
		for (Menu menu : menus) {
			render(menu);
		}
	}

	private void render(Menu menu) {
		out.append("<div class='menu'><span class='group'>").append(menu.label).append("</span><ul>");
		for (Site site : menu.entries) {
			out.append("<li>");
			switch (menu.action) {
			case my: out.append("<a href='/my/").append(site.name).append("/'>").append(site.name.display()).append("</a>"); break;
			default:
			case user:
			case view:
				out.append("<a href='/").append(menu.action.name()).append("/").append(site.owner).append("/");
				if (!site.name.equalTo(Name.as("@home"))) {
					out.append(site.name).append("/");
				}
				out.append("'>").append(site.name.display()).append("</a>"); break;
			}
			out.append("</li>");
		}
		out.append("</ul></div>");
	}

	public void render(View view, Task[][][] data) {
		for (int i = 0; i < view.silos.length; i++) {
			render(view.silos[i], data[i]);
		}
	}

	private void render(Silo silo, Task[][] data) {
		out.append("<div class='column'>");
		out.append("<h2>").append(silo.title).append("</h2>");
		for (int i = 0; i < silo.widgets.length; i++) {
			render(silo.widgets[i], data[i]);
		}
		out.append("</div>");
	}

	public void render(Widget widget, Task[] tasks) {
		out.append("<h3>").append(widget.caption).append("</h3>");
		out.append(" (by ").append(widget.scheme.name()).append(")");
		//TODO render a link "scheme", when clicked turns itself into a dropdown, that is just changing the table next to it
		// once selected the dropdown turns into the color link again
		// this is done with JS on client side
		out.append("<table class='list scheme-").append(widget.scheme.name()).append("'>");
		for (Task task : tasks) {
			render(task);
		}
		out.append("</table>");
	}

	private void render(Task task) {
		out.append("<tr");
		renderCssClasses(task);
		renderDataAttributes(task);
		out.append(">");
		out.append("<td>");
		renderTaskLink(task);
		if (viewer.canEmphasise(now) && task.canBeEmphasisedBy(viewer.name)) {
			renderStressLink(task);
		}
		out.append("</td>");
		out.append("<td><h5>");
		if (task.isVisibleTo(viewer.name)) {
			out.append(task.gist);
		} else {
			out.append("<i>(protected)</i>");
		}
		out.append("</h5>");
		renderUsersList(task);
		if (viewer.activated) {
			if (task.enlistedBy.contains(viewer) || task.approachedBy.contains(viewer)) {
				renderTaskActionLink(task, "btn", Action.abandon, "&minus;");
			} else {
				renderTaskActionLink(task, "btn", Action.enlist, "&plus;");
			}
		}
		out.append("</td><td>");
		if (task.area != null) {
			renderAreaLink(task);
			out.append("<span title='").append(task.area.maintainers.toString()).append("'>'").append(String.valueOf(task.area.maintainers.count())).append("</span>");
		}
		if (task.base != null) {
			out.append("<div>");
			renderVersionLink(task);
			out.append("</div>");
		}
		out.append("</td>");
		out.append("</tr>");
	}

	private void renderStressLink(Task task) {
		renderTaskActionLink(task, "stress btn", Action.stress,"!");
	}

	private void renderTaskActionLink(Task task, String cssClasses, Action action, String label) {
		out.append("<a class='").append(cssClasses).append("' href='/").append(action.name()).append("/").append(task.product.name).append("/").append(task.id.toString()).append("/'>").append(label).append("</a>");
	}

	private void renderDataAttributes(Task task) {
		out.append(" data-heat='").append(String.valueOf(task.heat)).append("'");
	}

	private void renderUsersList(Task task) {
		if (task.involvedUsers() > 0) {
			if (task.enlistedBy.count() > 0) {
				out.append("<b>[...</b>");
				renderUsersLinks(task.enlistedBy);
				out.append(" <b>]</b>");
			}
			renderUsersLinks(task.approachedBy);
		}
	}

	private void renderUsersLinks(Names users) {
		for (Name user : users) {
			renderUserLink(user);
		}
	}

	private void renderVersionLink(Task task) {
		out.append("<a href='/view/").append(task.product.name).append("/v/").append(task.base.name).append("/'>").append(task.base.name).append("</a>");
	}

	private void renderUserLink(Name user) {
		if (user.isRegular()) {
			out.append(" <a href='/user/").append(user.display()).append("/'>").append(user).append("</a>");
		} else {
			out.append(" <i>").append(user.display()).append("</i>");
		}
	}

	private void renderAreaLink(Task task) {
		out.append("<a href='/view/").append(task.product.name).append("/").append(task.area.name).append("/'>").append(task.area.name).append("</a>");
	}

	private void renderTaskLink(Task task) {
		renderTaskActionLink(task, "idn", Action.view, "#"+task.id);
	}

	private void renderCssClasses(Task task) {
		out.append(" class='");
		out.append(" status-").append(task.status.name());
		out.append(" goal-").append(task.purpose.name());
		out.append(" motive-").append(task.motive.name());
		out.append(" temp-").append(task.heat(date(now)).name());
		if (task.exploitable) {
			out.append(" exploitable");
		}
		out.append("'");
	}

}
