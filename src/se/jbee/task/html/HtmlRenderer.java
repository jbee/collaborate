package se.jbee.task.html;

import se.jbee.task.api.View;

public interface HtmlRenderer<V extends View> {

	void render(V view, HtmlWriter out);
}
