package vizio.view;

import java.util.ArrayList;
import java.util.List;

public class View {

	public static class Silo {

		public final String title;
		public final Widget[] widgets;

		public Silo(String title, Widget... widgets) {
			super();
			this.title = title;
			this.widgets = widgets;
		}
	}

	public final Silo[] silos;

	public View(Silo... silos) {
		super();
		this.silos = silos;
	}

	public static View parse(String template) {
		List<Silo> silos = new ArrayList<>();
		int e = 0;
		int s = template.indexOf('|', e);
		while (s >= 0) {
			e = template.indexOf('|', s+1);
			silos.add(new Silo("NO_TITLE", widgets(template.substring(s+1, e))));
			s=template.indexOf('|', e+1);
		}
		return new View(silos.toArray(new Silo[0]));
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
