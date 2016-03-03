package vizio.http;

import static vizio.Date.date;

import java.io.PrintWriter;

import vizio.Name;
import vizio.Names;
import vizio.Task;
import vizio.User;
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

	public void render(View view, Task[][][] data) {
		out.append("<!DOCTYPE html>");
		out.append("<head><link rel='stylesheet' href='/static/vizio.css'></head><body>");
		out.append("<h1>").append(view.title).append("</h1>");
		for (int i = 0; i < view.silos.length; i++) {
			render(view.silos[i], data[i]);
		}
		out.append("</body>");
	}

	private void render(Silo silo, Task[][] data) {
		out.append("<div class='column'>");
		for (int i = 0; i < silo.widgets.length; i++) {
			render(silo.widgets[i], data[i]);
		}
		out.append("</div>");
	}

	public void render(Widget widget, Task[] tasks) {
		out.append("<h3>").append(widget.caption).append("</h3>");
		out.append(" (by ").append(widget.scheme.name()).append(")");
		out.append("<table class='list scheme-").append(widget.scheme.name()).append("'>");
		for (Task task : tasks) {
			render(task);
		}
		out.append("<tr><td colspan='3'>").append("</td></tr>");
		out.append("</table>");
	}

	private void render(Task task) {
		out.append("<tr");
		renderCssClasses(task);
		renderDataAttributes(task);
		out.append(">");
		out.append("<td>");
		renderTaskLink(task);
		if (viewer.canStress(now) && task.canBeStressedBy(viewer.name)) {
			renderStressLink(task);
		}
		out.append("</td>");
		out.append("<td><h5>").append(task.summary).append("</h5>");
		renderUsersList(task);
		if (viewer.activated) {
			if (task.targetedBy.contains(viewer) || task.approachedBy.contains(viewer)) {
				renderTaskActionLink(task, "btn", "drop", "&minus;");
			} else {
				renderTaskActionLink(task, "btn", "target", "&plus;");
			}
		}
		out.append("</td><td>");
		if (task.area != null) {
			renderAreaLink(task);
			out.append("<span title='").append(task.area.maintainers.toString()).append("'>.").append(String.valueOf(task.area.maintainers.count())).append("</span>");
		}
		if (task.version != null) {
			out.append("<div>");
			renderVersionLink(task);
			out.append("</div>");
		}
		out.append("</td>");
		out.append("</tr>");
	}

	private void renderStressLink(Task task) {
		renderTaskActionLink(task, "stress btn", "stress","!");
	}

	private void renderTaskActionLink(Task task, String cssClasses, String action, String label) {
		out.append("<a class='").append(cssClasses).append("' href='/").append(action).append("/").append(task.product.name).append("/").append(task.id.toString()).append("/'>").append(label).append("</a>");
	}

	private void renderDataAttributes(Task task) {
		out.append(" data-heat='").append(String.valueOf(task.heat)).append("'");
	}

	private void renderUsersList(Task task) {
		if (task.users() > 0) {
			if (task.targetedBy.count() > 0) {
				out.append("<b>[...</b>");
				renderUsersLinks(task.targetedBy);
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
		out.append("<a href='/view/").append(task.product.name).append("/v/").append(task.version.name).append("/'>").append(task.version.name).append("</a>");
	}

	private void renderUserLink(Name user) {
		if (user.isExternal()) {
			out.append(" <a href='/user/").append(user).append("/'>").append(user).append("</a>");
		} else {
			out.append(" <i>").append(user.external()).append("</i>");
		}
	}

	private void renderAreaLink(Task task) {
		out.append("<a href='/view/").append(task.product.name).append("/").append(task.area.name).append("/'>").append(task.area.name).append("</a>");
	}

	private void renderTaskLink(Task task) {
		renderTaskActionLink(task, "idn", "view", "#"+task.id);
	}

	private void renderCssClasses(Task task) {
		out.append(" class='");
		out.append(" status-").append(task.status.name());
		out.append(" goal-").append(task.goal.name());
		out.append(" motive-").append(task.motive.name());
		out.append(" temp-").append(task.temerature(date(now)).name());
		if (task.exploitable) {
			out.append(" exploitable");
		}
		out.append("'");
	}

}
