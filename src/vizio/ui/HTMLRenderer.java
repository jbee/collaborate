package vizio.ui;

import java.io.PrintWriter;

import vizio.Date;
import vizio.Task;
import vizio.view.Coloring;

public class HTMLRenderer {

	private final PrintWriter out;
	private final Date today;

	public HTMLRenderer(PrintWriter out) {
		super();
		this.out = out;
		this.today = Date.date(System.currentTimeMillis());
	}

	public void render(Task[] list, Coloring scheme) {
		out.append("<table class='scheme-").append(scheme.name()).append("'>");
		for (Task task : list) {
			render(task);
		}
		out.append("</table>");
	}

	private void render(Task task) {
		out.append("<tr class='");
		out.append(" status-").append(task.status.name());
		out.append(" goal-").append(task.goal.name());
		out.append(" stimulus-").append(task.motive.name());
		out.append(" temp-").append(task.temerature(today).name());
		out.append("'");
		out.append(" data-heat='").append(String.valueOf(task.heat)).append("'");
		out.append(">");
		String num = task.id.toString();
		out.append("<td><a href='/task/").append(num).append("/'>#").append(num).append("</a></td>");
		out.append("<td>").append(task.summary).append("</td>");
		out.append("<td><a href='").append(task.product).append("/").append(task.area.name).append("/'>").append(task.area.name).append("</a></td>");
		out.append("</tr>");
	}

}
