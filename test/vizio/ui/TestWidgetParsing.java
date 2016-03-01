package vizio.ui;

import org.junit.Test;

import vizio.view.Widget;

public class TestWidgetParsing {

	@Test
	public void test() {
		Widget w = Widget.parse("\"caption\"{2-3}[users=bar]<users>#temp");
		System.out.println(w);
	}
}
