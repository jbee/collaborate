package se.jbee.track.ui.view;

import java.util.regex.Pattern;

import se.jbee.track.model.Criteria;
import se.jbee.track.model.Criteria.Coloring;
import se.jbee.track.model.Criteria.Property;

public class Widget {

	private static final Pattern FILTER_SPLIT = Pattern.compile("\\s*\\[?\\s*([-a-z]+)\\s*([!=<>~/]+)\\s*(\\{.*?\\}|[^\\s\\]]+)\\s*\\]?\\s*");
	// ui
	public final String caption;
	public final Coloring scheme;
	// data
	public final Criteria query;

	public Widget(String caption, Coloring scheme, Criteria query) {
		super();
		this.caption = caption;
		this.scheme = scheme;
		this.query = query;
	}

	@Override
	public String toString() {
		return String.format("\"%s\"%s#%s#", caption, query, scheme);
	}

	public static Widget parse(String widget) {
		String filters = widget.substring(widget.indexOf('['), widget.lastIndexOf(']')+1);
		String others = widget.replace(filters, "");
		String caption = section(others, '"', '"');
		String range = section(others, '{', '}');
		String orders = section(others, '<', '>');
		String scheme = section(others, '#', '#');
		Coloring coloring = scheme.isEmpty() ? Coloring.heat : Coloring.valueOf(scheme);
		return new Widget(caption, coloring, parseQuery(range, filters, orders));
	}

	public static String section(String str, char start, char end) {
		int si = str.indexOf(start);
		if (si < 0)
			return "";
		int ei = str.indexOf(end, si+1);
		if (ei < 0)
			return "";
		return str.substring(si+1, ei);
	}

	public static String section(String str, String start, String end) {
		int si = str.indexOf(start);
		if (si < 0)
			return "";
		int ei = str.indexOf(end, si+start.length());
		if (ei < 0)
			return "";
		return str.substring(si+start.length(), ei);
	}

	public static Criteria parseQuery(String range, String filters, String orders) {
		Criteria query = new Criteria();
		return query;
	}

	private static String[] parseValue(String value) {
		if ("{}".equals(value))
			return new String[0];
		if (value.charAt(0) == '{') {
			value = value.substring(1, value.lastIndexOf('}'));
			return value.split("\\s+");
		}
		return new String[] { value };
	}

	public static Property[] parseProperties(String props) {
		if (props.isEmpty())
			return new Property[0];
		String[] names = props.split("\\s+");
		Property[] res = new Property[names.length];
		for (int i = 0; i < names.length; i++) {
			res[i] = Property.valueOf(names[i]);
		}
		return res;
	}
}
