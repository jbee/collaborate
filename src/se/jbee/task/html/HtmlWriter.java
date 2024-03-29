package se.jbee.task.html;

import java.io.PrintWriter;

import se.jbee.task.api.Param.Command;
import se.jbee.task.model.Area;
import se.jbee.task.model.Cause;
import se.jbee.task.model.Goal;
import se.jbee.task.model.Heat;
import se.jbee.task.model.Name;
import se.jbee.task.model.Status;
import se.jbee.task.model.Task;
import se.jbee.task.model.Criteria.Coloration;


/**
 * A wrapper around the {@link PrintWriter} that is the HTTP response stream.
 *
 * Here is the place for utility methods, things that are done by multiple
 * pages, like likes and alike.
 *
 * @author jan
 */
public final class HtmlWriter {

	//TODO have a look at https://www.owasp.org/index.php/XSS_(Cross_Site_Scripting)_Prevention_Cheat_Sheet

	private final PrintWriter out;

	public HtmlWriter(PrintWriter out) {
		this.out = out;
	}

	public HtmlWriter append(CharSequence s) {
		out.append(s);
		return this;
	}

	public void versionLink(Task task) {
		out.append("<a class='vn' href='").append(task.output.name).append("/v/").append(task.base.name).append("/'>").append(task.base.name).append("</a>");
	}

	public void userLink(Name user) {
		if (user.isRegular()) {
			out.append(" <a href='/user/").append(user.display()).append("/'>").append(user).append("</a>");
		} else {
			out.append(" <i>").append(user.display()).append("</i>");
		}
	}

	public void areaLink(Area area) {
		out.append("<a href='").append(area.output).append("/").append(area.name).append("/'>").append(area.name).append("</a>");
	}

	public void taskLink(Task task) {
		commandLink(task, "idn", Command.query, "#"+task.id);
	}

	public void stressLink(Task task) {
		commandLink(task, "stress btn", Command.stress,"!");
	}

	public void commandLink(Task task, String cssClasses, Command command, String label) {
		out.append("<a class='").append(cssClasses).append("' href='/").append(command.name()).append("/").append(task.output.name).append("/").append(task.id.toString()).append("/'>").append(label).append("</a>");
	}

	public void legend() {
		out.append("<div class='footer'><div class='column'>");
		renderTable(Coloration.cause, Cause.class);
		renderTable(Coloration.status, Status.class);
		renderTable(Coloration.goal, Goal.class);
		renderTable(Coloration.heat, Heat.class);
		out.append("</div></div>");
	}

	private void renderTable(Coloration scheme, Class<? extends Enum<?>> type) {
		out.append("<table class='legend scheme-").append(scheme.name()).append("'>");
		out.append("<tr><th>").append(scheme.name()).append("</th></td>");
		for (Enum<?> v : type.getEnumConstants()) {
			out.append("<tr class='").append(scheme.name()).append("-").append(v.name()).append("'><td>").append(v.name()).append("</td></tr>");
		}
		out.append("</table>");
	}

	public void footer() {
		out.append("</body>");
	}

	public void header() {
		out.append("<!DOCTYPE html><head><title>collaborate!</title>");
		out.append("<link rel='stylesheet' href='/static/collaborate.css'></head><body>");
	}
}
