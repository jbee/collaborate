package se.jbee.track.ui.http;

import static se.jbee.track.model.Date.date;

import java.io.PrintWriter;

import se.jbee.track.cache.Matches;
import se.jbee.track.model.Criteria;
import se.jbee.track.model.Criteria.Coloring;
import se.jbee.track.model.Criteria.Property;
import se.jbee.track.model.Heat;
import se.jbee.track.model.Motive;
import se.jbee.track.model.Name;
import se.jbee.track.model.Names;
import se.jbee.track.model.Purpose;
import se.jbee.track.model.Site;
import se.jbee.track.model.Status;
import se.jbee.track.model.Task;
import se.jbee.track.model.User;
import se.jbee.track.ui.ctrl.Action;
import se.jbee.track.ui.ctrl.Ctrl;

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

	public void render(Ctrl.ListPage page) {
		out.append("<!DOCTYPE html><head><title>collaborate!</title>");
		out.append("<link rel='stylesheet' href='/static/vizio.css'></head><body>");
		renderStaticMenu();
		renderMenu(page.menu);
		renderList(page.page, page.results);

		out.append("<div class='footer'><div class='column'>");
		renderTable(Coloring.motive, Motive.class);
		renderTable(Coloring.goal, Purpose.class);
		renderTable(Coloring.status, Status.class);
		renderTable(Coloring.heat, Heat.class);
		out.append("</div></div>");
		out.append("</body>");
	}

	private void renderStaticMenu() {
		
	}

	public void renderTable(Coloring scheme, Class<? extends Enum<?>> type) {
		out.append("<table class='legend scheme-").append(scheme.name()).append("'>");
		out.append("<tr><th>").append(scheme.name()).append("</th></td>");
		for (Enum<?> v : type.getEnumConstants()) {
			out.append("<tr class='").append(scheme.name()).append("-").append(v.name()).append("'><td>").append(v.name()).append("</td></tr>");
		}
		out.append("</table>");
	}

	public void renderMenu(Site[] entries) {
		out.append("<div class='menu'><span class='group'>").append("<h1>collaborate!</h1>").append("</span><ul>");
		for (Site site : entries) {
			out.append("<li>");
			out.append("<a href='/").append(site.menu).append("/").append(site.name).append("/");
			if (!site.name.equalTo(Name.as("@home"))) {
				out.append(site.name).append("/");
			}
			out.append("'>").append(site.name.display()).append("</a>");
			out.append("</li>");
		}
		out.append("</ul></div>");
	}

	public void renderList(Site page, Matches[] matches) {
		Object[] seq = page.template.elements();
		out.append("<div class='column'>");
		int i = 0;
		for (Object e : seq) {
			if (e instanceof Criteria) {
				out.append("<h2>").append("TODO use last name").append("</h2>");
				render((Criteria) e, matches[i++]);
			} else {
				out.append(e.toString());
				//TODO open new columns on --------
			}
		}
		out.append("</div>");
	}

	public void render(Criteria criteria, Matches matches) {
		out.append("<h3>").append(criteria.toString()).append("</h3>");
		Coloring scheme = Coloring.heat;
		if (criteria.indexOf(Property.color) > 0) {
			scheme = (Coloring) criteria.get(criteria.indexOf(Property.color)).rvalues[0];
		}
		out.append(" (by ").append(scheme.name()).append(")");
		//TODO render a link "scheme", when clicked turns itself into a dropdown, that is just changing the table next to it
		// once selected the dropdown turns into the color link again
		// this is done with JS on client side
		out.append("<table class='list scheme-").append(scheme.name()).append("'>");
		for (Task task : matches.tasks) {
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
		if (viewer.canEmphasise(now) && task.canBeEmphasisedBy(viewer.alias)) {
			renderStressLink(task);
		}
		out.append("</td>");
		out.append("<td>");
		out.append("<h5>");
		renderTaskLink(task);
		out.append(" ");
		if (task.isVisibleTo(viewer.alias)) {
			out.append(task.gist.toString());
		} else {
			out.append("<i>(protected)</i>");
		}
		out.append("</h5>");
		if (task.area != null) {
			renderAreaLink(task);
			if (false) {
				out.append("<span title='").append(task.area.maintainers.toString()).append("'>'").append(String.valueOf(task.area.maintainers.count())).append("</span>");
			}
		}
		out.append("&emsp;");
		if (task.base != null) {
			renderVersionLink(task);
		}		
		out.append("</td><td>");
		if (viewer.isAuthenticated()) {
			if (task.aspirants.contains(viewer) || task.participants.contains(viewer)) {
				renderTaskActionLink(task, "btn", Action.abandon, "&minus;");
			} else {
				renderTaskActionLink(task, "btn", Action.enlist, "&plus;");
			}
		}
		renderUsersList(task);
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
		out.append(" data-heat='").append(String.valueOf(task.emphasis)).append("'");
	}

	private void renderUsersList(Task task) {
		if (task.participants() > 0) {
			if (task.aspirants.count() > 0) {
				out.append("<b>[...</b>");
				renderUsersLinks(task.aspirants);
				out.append(" <b>]</b>");
			}
			renderUsersLinks(task.participants);
		}
	}

	private void renderUsersLinks(Names users) {
		for (Name user : users) {
			renderUserLink(user);
		}
	}

	private void renderVersionLink(Task task) {
		out.append("<a class='vn' href='/view/").append(task.product.name).append("/v/").append(task.base.name).append("/'>").append(task.base.name).append("</a>");
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
		renderTaskActionLink(task, "idn", Action.list, "#"+task.id);
	}

	private void renderCssClasses(Task task) {
		out.append(" class='");
		out.append(" status-").append(task.status.name());
		out.append(" goal-").append(task.purpose.name());
		out.append(" motive-").append(task.motive.name());
		out.append(" heat-").append(task.heat(date(now)).name());
		if (task.exploitable) {
			out.append(" exploitable");
		}
		out.append("'");
	}

}
