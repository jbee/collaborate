package se.jbee.track.html;

import se.jbee.track.api.Page;

public interface HtmlPageRenderer<P extends Page> {

	void render(P page, HtmlWriter out);
}
