package vizio.view;

import static java.lang.Integer.parseInt;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import vizio.Task;
import vizio.io.TQuery;
import vizio.io.TQuery.Filter;
import vizio.io.TQuery.Operator;
import vizio.io.TQuery.Property;

public class Widget {

	// ui
	public String caption;
	public Coloring scheme;
	// data
	public TQuery query;
	public Task[] list;

	public Widget() {
	}

	public Widget(String caption, Coloring scheme, TQuery query) {
		super();
		this.caption = caption;
		this.scheme = scheme;
		this.query = query;
	}

	private static final Pattern PARTS = Pattern.compile(
		"(?:\"([^\"]+)\")?"+		// caption
		"(?:\\{(\\d+-\\d+)\\})?"+	// range
		"((?:\\[[^\\]]+\\])+)"+		// filters
		"(?:<([^>]+)>)?"+			// orders
		"(?:#([-a-z]+))?"			// coloring
	);

	public static Widget parse(String widget) {
		Matcher m = PARTS.matcher(widget);
		if (m.matches()) {
			String caption = m.group(1);
			String range = m.group(2);
			String filters = m.group(3);
			String orders = m.group(4);
			String scheme = m.group(5);
			Coloring coloring = scheme == null ? Coloring.temp : Coloring.valueOf(scheme);
			return new Widget(caption, coloring, parseQuery(range, filters, orders));
		}
		throw new IllegalArgumentException(widget);
	}

	public static TQuery parseQuery(String range, String filters, String orders) {
		TQuery query = new TQuery();
		query.orders = parseProperties(orders);
		query.filters = parseFilters(filters);
		String[] rvals = range.split("-");
		query.offset = parseInt(rvals[0]);
		query.length = parseInt(rvals[1]) - query.offset + 1;
		return query;
	}

	private static final Pattern FILTERS = Pattern.compile("(\\[[^]]+\\])+");

	public static Filter[] parseFilters(String filters) {
		Matcher m = FILTERS.matcher(filters);
		if (m.matches()) {
			Filter[] res = new Filter[m.groupCount()];
			for (int i = 1; i <= m.groupCount(); i++) {
				res[i-1] = parseFilter(m.group(i));
			}
			return res;
		}
		throw new IllegalArgumentException(filters);
	}

	private static final Pattern FILTER = Pattern.compile(
		"\\["+
			"([-a-z]+)"+
			"\\s*(=|!=|>=|<=|>|<|~|!~|/|!/)\\s*"+
			"(?:\\{?(?:([:]?[-a-z0-9_]+)\\s*)*\\}?)"+
		"\\]");

	public static Filter parseFilter(String filter) {
		Matcher m = FILTER.matcher(filter);
		if (m.matches()) {
			String prop = m.group(1);
			String op = m.group(2);
			String[] vals = new String[m.groupCount()-2];
			for (int i = 3; i <= m.groupCount(); i++) {
				vals[i-3] = m.group(i);
			}
			return new Filter(Property.valueOf(prop), Operator.forSymbol(op), vals);
		}
		throw new IllegalArgumentException(filter);
	}

	public static Property[] parseProperties(String props) {
		String[] names = props.split("\\s+");
		Property[] res = new Property[names.length];
		for (int i = 0; i < names.length; i++) {
			res[i] = Property.valueOf(names[i]);
		}
		return res;
	}
}
