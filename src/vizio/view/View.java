package vizio.view;

import java.util.ArrayList;
import java.util.List;

public class View {

	public static class Silo {

		public Widget[] widgets;

		public Silo(Widget... widgets) {
			super();
			this.widgets = widgets;
		}
	}

	public String title;
	public Silo[] silos;

	public View(String title, Widget... widgets) {
		this(title, new Silo[] { new Silo(widgets) } );
	}

	public View(String title, Silo... silos) {
		super();
		this.title = title;
		this.silos = silos;
	}

	public static View parse(String template) {
		String title = Widget.section(template, "===", "===");
		template = template.replace("==="+title+"===", "");
		List<Silo> silos = new ArrayList<>();
		int e = 0;
		int s = template.indexOf('|', e);
		while (s >= 0) {
			e = template.indexOf('|', s+1);
			silos.add(new Silo(widgets(template.substring(s+1, e))));
			s=template.indexOf('|', e+1);
		}
		return new View(title, silos.toArray(new Silo[0]));
	}

	private static Widget[] widgets(String silo) {
		String[] lines = silo.split("\n");
		List<Widget> widgets = new ArrayList<>();
		for (String line : lines) {
			line = line.trim();
			if (line.length() >  0) {
				widgets.add(Widget.parse(line));
			}
		}
		return widgets.toArray(new Widget[0]);
	}
}
