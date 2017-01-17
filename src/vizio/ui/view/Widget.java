package vizio.ui.view;

import static java.lang.Integer.parseInt;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import vizio.io.Criteria;
import vizio.io.Criteria.Filter;
import vizio.io.Criteria.Operator;
import vizio.io.Criteria.Property;
import vizio.io.Criteria.Range;

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
		query.orders = parseProperties(orders);
		query.filters = parseFilters(filters);
		query.range = parseRange(range);
		return query;
	}

	public static Range parseRange(String range) {
		if (range.isEmpty())
			return new Range(0,-1);
		String[] startEnd = range.split("\\s+");
		String start = startEnd[0];
		String end = startEnd[1];
		return new Range("*".equals(start) ? -1 : parseInt(start), "*".equals(end) ? -1 : parseInt(end));
	}

	public static Filter[] parseFilters(String filters) {
		List<Filter> res = new ArrayList<>();
		Matcher m = FILTER_SPLIT.matcher(filters);
		while (m.find()) {
			res.add( new Filter(Property.valueOf(m.group(1)), Operator.forSymbol(m.group(2)), parseValue(m.group(3))));
		}
		return res.toArray(new Filter[0]);
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