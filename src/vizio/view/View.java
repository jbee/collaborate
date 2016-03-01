package vizio.view;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class View {

	public String title;
	public Column[] columns;

	public View(String title, Widget... widgets) {
		this(title, new Column[] { new Column(widgets) } );
	}

	public View(String title, Column... columns) {
		super();
		this.title = title;
		this.columns = columns;
	}

	private static final Pattern PARTS = Pattern.compile(
		"\\s*(===.*?===)"+ // title
		"(?:\\s*\\|([^|]+)\\|)+", // columns
		Pattern.MULTILINE
	);

	public static View parse(String template) {
		Matcher m = PARTS.matcher(template);
		if (m.matches()) {
			String title = m.group(1);
			Column[]columns = new Column[m.groupCount()-1];
			for (int i = 2; i < m.groupCount(); i++) {
				columns[i-2] = new Column(widgets( m.group(i) ));
			}
			return new View(title, columns);
		}
		throw new IllegalArgumentException(template);
	}

	private static Widget[] widgets(String column) {
		String[] lines = column.split("\n");
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
