package se.jbee.track.html;

import java.util.Arrays;

import se.jbee.track.api.SampleView;
import se.jbee.track.engine.Changes;

public class SampleViewHtmlRenderer implements HtmlRenderer<SampleView> {

	@Override
	public void render(SampleView view, HtmlWriter out) {
		Changes changes = view.changes;
		out.header();
		out.append("<h2>Created</h2>");
		for (Changes.Entry<?> e : changes) {
			out.append(e.type().name());
			out.append(" ");
			out.append(e.after.uniqueID());
			out.append(" ");
			out.append(String.valueOf(e.after.version()));
			out.append(" ");
			out.append(Arrays.toString(e.transitions));
			out.append(" ");
			out.append(String.valueOf(e.before == null));
			out.append("<br/>");
		}
		out.footer();
	}

}
