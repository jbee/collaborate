package vizio.ui;

import java.io.PrintWriter;

import vizio.Task;
import vizio.view.Coloring;

public class HTMLRenderer {

	private final PrintWriter out;

	public HTMLRenderer(PrintWriter out) {
		super();
		this.out = out;
	}

	public void render(Task[] list, Coloring scheme) {
		out.append("<table class='").append(scheme.name()).append("'>");
		for (Task task : list) {
			render(task, scheme);
		}
		out.append("</table>");
	}

	private void render(Task task, Coloring scheme) {
		out.append("<tr class=' scheme-").append(scheme.name());
		out.append(" status-").append(task.status.name());
		out.append(" goal-").append(task.goal.name());
		out.append(" stimulus-").append(task.stimulus.name());
		out.append(" temp-").append(task.temerature().name());
		out.append("'");
		out.append(" data-heat='").append(String.valueOf(task.heat)).append("'");
		out.append(">");
		String num = task.num.toString();
		out.append("<td><a href='/task/").append(num).append("/'>#").append(num).append("</a></td>");
		out.append("<td>").append(task.summary).append("</td>");
		out.append("<td><a href='").append(task.product.name).append("/").append(task.area.name).append("/'>").append(task.area.name).append("</a></td>");
		out.append("</tr>");
	}

}
