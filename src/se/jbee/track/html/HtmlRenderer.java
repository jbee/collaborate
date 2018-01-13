package se.jbee.track.html;

import se.jbee.track.api.View;

public interface HtmlRenderer<V extends View> {

	void render(V view, HtmlWriter out);
}
