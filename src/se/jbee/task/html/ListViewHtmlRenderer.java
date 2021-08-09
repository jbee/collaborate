package se.jbee.task.html;

import static se.jbee.task.model.Date.date;

import se.jbee.task.api.ListView;
import se.jbee.task.api.Param.Command;
import se.jbee.task.api.View;
import se.jbee.task.cache.Matches;
import se.jbee.task.model.Criteria;
import se.jbee.task.model.Criteria.Coloration;
import se.jbee.task.model.Criteria.Property;
import se.jbee.task.model.Name;
import se.jbee.task.model.Names;
import se.jbee.task.model.Page;
import se.jbee.task.model.Task;

public class ListViewHtmlRenderer implements HtmlRenderer<ListView> {

	@Override
	public void render(ListView view, HtmlWriter out) {
		out.header();
		renderMenu(view.menu, out);
		renderList(view, view.results, out);

		out.legend();
		out.footer();
	}

	public void renderMenu(Page[] pages, HtmlWriter out) {
		out.append("<div class='menu'><span class='group'>").append("<h1>collaborate!</h1>").append("</span><ul>");
		for (Page page : pages) {
			out.append("<li>");
			out.append("<a href='/").append(page.menu).append("/").append(page.name).append("/");
			if (!page.name.equalTo(Name.as("@home"))) {
				out.append(page.name).append("/");
			}
			out.append("'>").append(page.name.display()).append("</a>");
			out.append("</li>");
		}
		out.append("</ul></div>");
	}

	public void renderList(ListView page, Matches[] matches, HtmlWriter out) {
		out.append("<div class='column'>");
		int i = 0;
		for (Object e : page.page.template) {
			if (e instanceof Criteria) {
				out.append("<h2>").append("TODO use last name").append("</h2>");
				render(page, (Criteria) e, matches[i++], out);
			} else {
				out.append(e.toString());
				//TODO open new columns on --------
			}
		}
		out.append("</div>");
	}

	public void render(ListView page, Criteria criteria, Matches matches, HtmlWriter out) {
		out.append("<code>").append(criteria.toString()).append("</code>");
		Coloration scheme = Coloration.heat;
		if (criteria.indexOf(Property.coloration) > 0) {
			scheme = (Coloration) criteria.get(criteria.indexOf(Property.coloration)).rvalues[0];
		}
		out.append(" (by ").append(scheme.name()).append(")");
		//TODO render a link "scheme", when clicked turns itself into a dropdown, that is just changing the table next to it
		// once selected the dropdown turns into the color link again
		// this is done with JS on client side
		out.append("<table class='list scheme-").append(scheme.name()).append("'>");
		for (Task task : matches.tasks) {
			render(page, task, out);
		}
		out.append("</table>");
	}

	private void render(ListView page, Task task, HtmlWriter out) {
		out.append("<tr");
		renderCssClasses(page, task, out);
		out.append(" data-heat='").append(String.valueOf(task.emphasis)).append("'");
		out.append(">");
		out.append("<td>");
		if (page.actor.canEmphasise(page.now) && task.canBeEmphasisedBy(page.actor.alias)) {
			out.stressLink(task);
		}
		out.append("</td>");
		out.append("<td>");
		out.append("<h5>");
			out.taskLink(task);
		out.append(" ");
		if (task.isVisibleTo(page.actor.alias)) {
			out.append(task.gist.toString());
		} else {
			out.append("<i>(protected)</i>");
		}
		out.append("</h5>");
		if (task.area != null) {
			out.areaLink(task.area);
			if (false) {
				out.append("<span title='").append(task.area.maintainers.toString()).append("'>'").append(String.valueOf(task.area.maintainers.count())).append("</span>");
			}
		}
		out.append("&emsp;");
		if (task.base != null) {
			out.versionLink(task);
		}
		out.append("</td><td>");
		if (page.actor.isAuthenticated()) {
			if (task.aspirants.contains(page.actor) || task.participants.contains(page.actor)) {
				out.commandLink(task, "btn", Command.abandon, "&minus;");
			} else {
				out.commandLink(task, "btn", Command.enlist, "&plus;");
			}
		}
		renderUsersList(task, out);
		out.append("</td>");
		out.append("</tr>");
	}

	private void renderUsersList(Task task, HtmlWriter out) {
		if (task.supporterCount() > 0) {
			if (task.aspirants.count() > 0) {
				out.append("<b>[...</b>");
				renderUsersLinks(task.aspirants, out);
				out.append(" <b>]</b>");
			}
			renderUsersLinks(task.participants, out);
		}
	}

	private void renderUsersLinks(Names users, HtmlWriter out) {
		for (Name user : users) {
			out.userLink(user);
		}
	}

	private void renderCssClasses(View page, Task task, HtmlWriter out) {
		out.append(" class='");
		out.append(" status-").append(task.status.name());
		out.append(" goal-").append(task.goal.name());
		out.append(" cause-").append(task.cause.name());
		out.append(" heat-").append(task.heat(date(page.now)).name());
		if (task.exploitable) {
			out.append(" exploitable");
		}
		out.append("'");
	}

}
