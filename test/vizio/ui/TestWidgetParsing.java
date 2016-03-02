package vizio.ui;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import vizio.view.Widget;

public class TestWidgetParsing {

	@Test
	public void onlyFilterIsMandatory() {
		verifyQuery("[users=x]",
				"\"\"{0 *}[users=x]<>#temp#");
	}

	@Test
	public void valueCanBeTheEmptySet() {
		verifyQuery("[users={}]",
				"\"\"{0 *}[users={}]<>#temp#");
	}

	@Test
	public void valueCanBeASingletonSet() {
		verifyQuery("[users={x}]",
				"\"\"{0 *}[users=x]<>#temp#");
	}

	@Test
	public void valueCanBeASet() {
		verifyQuery("[users={x y}]",
				"\"\"{0 *}[users={x y}]<>#temp#");
	}

	@Test
	public void twoComponentsTwoOrders() {
		verifyQuery("\"caption\"{2 3}[users=bar users!={}]<users status>#age#", "");
	}

	private static void verifyQuery(String query, String expected) {
		if (expected.isEmpty()) {
			expected = query;
		}
		Widget w = Widget.parse(query);
		assertEquals(expected, w.toString());
		Widget ww = Widget.parse(w.toString());
		assertEquals(expected, ww.toString());
	}
}
