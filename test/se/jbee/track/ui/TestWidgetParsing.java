package se.jbee.track.ui;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import se.jbee.track.ui.view.Widget;

public class TestWidgetParsing {

	@Test
	public void onlyFilterIsMandatory() {
		verifyWidget("[users=x]",
				"\"\"{0 *}[users=x]<>#heat#");
	}

	@Test
	public void valueCanBeTheEmptySet() {
		verifyWidget("[users={}]",
				"\"\"{0 *}[users={}]<>#heat#");
	}

	@Test
	public void valueCanBeASingletonSet() {
		verifyWidget("[users={x}]",
				"\"\"{0 *}[users=x]<>#heat#");
	}

	@Test
	public void valueCanBeASet() {
		verifyWidget("[users={x y}]",
				"\"\"{0 *}[users={x y}]<>#heat#");
	}

	@Test
	public void twoComponentsTwoOrders() {
		verifyWidget("\"caption\"{2 3}[users=bar users!={}]<users status>#heat#", "");
	}

	private static void verifyWidget(String widget, String expected) {
		if (expected.isEmpty()) {
			expected = widget;
		}
		Widget w = Widget.parse(widget);
		assertEquals(expected, w.toString());
		Widget ww = Widget.parse(w.toString());
		assertEquals(expected, ww.toString());
	}
}
