package se.jbee.task.model;

import static java.lang.Integer.signum;
import static java.util.Arrays.asList;
import static se.jbee.task.model.Criteria.Operator.asc;
import static se.jbee.task.model.Criteria.Operator.desc;
import static se.jbee.task.model.Criteria.Operator.eq;
import static se.jbee.task.model.Criteria.Operator.ge;
import static se.jbee.task.model.Criteria.Operator.gt;
import static se.jbee.task.model.Criteria.Operator.in;
import static se.jbee.task.model.Criteria.Operator.le;
import static se.jbee.task.model.Criteria.Operator.lt;
import static se.jbee.task.model.Criteria.Operator.neq;
import static se.jbee.task.model.Criteria.Operator.nin;
import static se.jbee.task.model.Criteria.ValueType.date;
import static se.jbee.task.model.Criteria.ValueType.flag;
import static se.jbee.task.model.Criteria.ValueType.name;
import static se.jbee.task.model.Criteria.ValueType.number;
import static se.jbee.task.model.Criteria.ValueType.property;
import static se.jbee.task.model.Criteria.ValueType.text;
import static se.jbee.task.util.Array.any;
import static se.jbee.task.util.Array.refine;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A data structure to describe what {@link Task}s to select and how to present
 * them by a list of {@link Criterium}s.
 *
 * A {@link Criterium} is given in a human/machine friendly syntax:
 *
 * <pre>
 * [property operator value]
 * </pre>
 *
 * There is a fix set of {@link Property}s and {@link Operator}s. Also the
 * possible combinations are constraint statically.
 *
 * As the combination of {@link Property} and {@link Operator} determines the
 * kind of semantic of a value there does not need to be a syntax to distinguish
 * between numbers, property names or texts.
 *
 * Some combinations require or allow value sets. A set is specified using curly
 * brackets and commas to separate elements.
 *
 * Some examples for valid constraints:
 *
 * <pre>
 * [reporter = Frank]
 * [age > 20]
 * [maintainers ~ {Frank, Peter}]
 * [order >> {name, age}]
 * [first = 15]
 * [color = heat]
 * </pre>
 *
 * This would list all task reported by Frank older than 20 days in an area
 * where Frank or Peter are maintainer. The result would be ordered by name
 * first, age second and start with the 15 match and would be colored using the
 * heat of the tasks.
 *
 * The example for <code>order</code> shows, that the set is sometimes used as a
 * list too, that is to say order of elements matters.
 *
 * If in/nin are used the values are alternatives (OR).
 * To test for AND use multiple {@link Criterium}s.
 * <pre>
 * [user ~ Frank]
 * [user ~ Paul]
 * </pre>
 *
 * While the uniform way of describing constraints can be a bit lengthy it
 * allows for simple parsing and remembering.
 */
public final class Criteria implements Iterable<Criteria.Criterium>, Bindable {

	/**
	 * To trigger indexing of a specific {@link Output} in a cache we build a
	 * request that only filters on the {@link Output} and on nothing else.
	 */
	public static Criteria index(Name output) {
		return new Criteria(new Criterium(Property.output, eq, output));
	}

	private final Criterium[] criteria;
	public final boolean unbound;

	public Criteria(Criterium... criteria) {
		this.criteria = criteria;
		this.unbound = any(criteria, c -> c.unbound);
	}

	@Override
	public Criteria bindTo(Map<Property, Name> context) {
		if (!unbound) {
			throw new IllegalStateException("Already bound!");
		}
		return new Criteria(refine(criteria, c -> c.bindTo(context)));
	}

	public boolean isIndexRequest() {
		return criteria.length == 1 && criteria[0].left == Property.output && criteria[0].op == eq;
	}

	public int count() {
		return criteria.length;
	}

	public Criterium get(int index) {
		return criteria[index];
	}

	@Override
	public Iterator<Criterium> iterator() {
		return asList(criteria).iterator();
	}

	@Override
	public String toString() {
		return join(criteria, "");
	}

	public boolean contains(Property p) {
		return indexOf(p) >= 0;
	}

	public int indexOf(Property p) {
		return indexOf(p, 0);
	}

	public int indexOf(Property p, int start) {
		for (int i = start; i < criteria.length; i++)
			if (criteria[i].left == p)
				return i;
		return -1;
	}

	public Task[] filter(Iterator<Task> tasks, Date today) {
		List<Task> res = new ArrayList<>();
		while (tasks.hasNext()) {
			Task t = tasks.next();
			if (matches(t, today))
				res.add(t);
		}
		return res.toArray(new Task[0]);
	}

	public boolean matches(Task t, Date today) {
		for (Criterium c : criteria)
			if (!c.matches(t, today))
				return false;
		return true;
	}

	public Criteria without(Property p, Property...more) {
		EnumSet<Property> excluded = EnumSet.of(p, more);
		Criterium[] res = new Criterium[criteria.length];
		int i = 0;
		for (int j = 0; j < criteria.length; j++) {
			if (!excluded.contains(criteria[j].left))
				res[i++] = criteria[j];
		}
		return new Criteria(Arrays.copyOf(res, i));
	}

	public <T,V> T collect(T v0, Class<V> elemType, BiFunction<T, V, T> merge, Property p, Operator...ops) {
		EnumSet<Operator> included = EnumSet.of(ops[0], ops);
		T res = v0;
		for (Criterium ct : criteria) {
			if (ct.left == p && included.contains(ct.op))
				for (Object v : ct.rvalues)
					res = merge.apply(res, elemType.cast(v));
		}
		return res;
	}

	private static final Pattern CRITERIUM = Pattern.compile("\\s*\\[([a-z]+)\\s*([=<>?!~]{1,2})\\s*([^\\]]+)\\]", Pattern.MULTILINE);

	public static Criteria parse(CharSequence s) throws CriteriumMalformed {
		Matcher m = CRITERIUM.matcher(s);
		List<Criterium> res = new ArrayList<>();
		while (m.find()) {
			String p = m.group(1);
			String o = m.group(2);
			String v = m.group(3);
			Property prop = Property.property(p.toLowerCase());
			Operator op = Operator.forSymbol(o);
			if (!prop.ops.contains(op)) {
				throw new CriteriumMalformed("Property `"+prop+"` does not support operation `"+op+"`, supported are: "+prop.ops.toString());
			}
			String[] val = parseValue(v);
			if (val.length == 1) {
				// just to get rid of those special cases right away
				if (!prop.isSetValue()) {
					if (op == in)
						op = eq;
					if (op == nin)
						op = neq;
				}
			}
			if (val.length > 1 && !op.allowsMultipleArguments) {
				throw new CriteriumMalformed("Operation `"+op+"` was used with set value `"+Arrays.toString(val)+"` but requires a simple value, use e.g. `"+val[0]+"`");
			}
			if (prop.type == date && val[0].length() < 10) {
				parseDate(prop, op, val, res);
			} else {
				res.add(criterium(prop, op, val));
			}
		}
		Criterium[] arr = res.toArray(new Criterium[0]);
		Arrays.sort(arr);
		return new Criteria(arr);
	}

	private static void parseDate(Property prop, Operator op, String[] val, List<Criterium> res) {
		String date = val[0];
		switch (op) {
		case eq:
			res.add(criterium(prop, ge, startOf(date)));
			res.add(criterium(prop, le, endOf(date)));
			return;
		case gt: res.add(criterium(prop, gt, endOf(date))); return;
		case ge: res.add(criterium(prop, ge, startOf(date))); return;
		case lt: res.add(criterium(prop, lt, startOf(date))); return;
		case le: res.add(criterium(prop, le, endOf(date))); return;
		case in:
			if (sequentialYearsOrMonths(val)) {
				res.add(criterium(prop, ge, startOf(val[0])));
				res.add(criterium(prop, le, endOf(val[val.length-1])));
				return;
			}
		}
		throw new CriteriumMalformed("Date property "+prop+" does not support value: "+Arrays.toString(val));
	}

	private static boolean sequentialYearsOrMonths(String[] val) {
		int len = val[0].length();
		for (int i = 1; i < val.length; i++) {
			if (val[i].length() != len)
				return false;
			LocalDate a = LocalDate.parse(startOf(val[i-1]));
			LocalDate b = LocalDate.parse(startOf(val[i]));
			if (len == 4 && !b.minusYears(1).isEqual(a)
				|| len == 7 && !b.minusMonths(1).isEqual(a))
				return false;
		}
		return true;
	}

	private static String endOf(String date) {
		return extend(date, "-12", "-31");
	}

	private static String startOf(String date) {
		return extend(date, "-01", "-01");
	}

	private static String extend(String val, String on4, String on7) {
		if (val.length() == 4)
			val+=on4;
		if (val.length() == 7)
			val+=on7;
		return val;
	}

	private static Criterium criterium(Property p, Operator op, String... val) {
		return new Criterium(p, op, typed(p, val));
	}

	private static Object[] typed(Property p, String[] val) {
		Object[] res = new Object[val.length];
		for (int i = 0; i < val.length; i++) {
			try {
				res[i] = typed(p, val[i]);
			} catch (RuntimeException e) {
				throw new CriteriumMalformed("Failed to parse property "+p+" value: "+val[i], e);
			}
		}
		return res;
	}

	private static final Object UNBOUND = "@";

	private static Object typed(Property p, String val) {
		if ("@".equals(val))
			return UNBOUND;
		if (val.startsWith("@")) {
			return Property.valueOf(val.toLowerCase().substring(1));
		}
		switch (p.type) {
		case number   : return Integer.valueOf(val);
		case date     : return Date.parse(val);
		case name     : return Name.as(val);
		case property : return p.value(val);
		case text     : return Gist.gist(val);
		case flag     : return "true|yes|on|1".matches(val) ? Boolean.TRUE : Boolean.FALSE;
		default       : throw new CriteriumMalformed("Unsupported value type: "+p.type);
		}
	}

	private static String[] parseValue(String value) {
		if ("{}".equals(value) || value == null || value.isEmpty())
			return new String[0];
		if (value.charAt(0) == '{') {
			value = value.substring(1, value.lastIndexOf('}'));
			return value.split("\\s*,\\s*");
		}
		return new String[] { value };
	}

	public static final class Criterium implements Comparable<Criterium> {

		public final Property left;
		public final Operator op;
		public final Object[] rvalues;
		public final Property right;
		public final boolean unbound;

		public Criterium(Property prop, Operator op, Object... values) {
			this.left = prop;
			this.op = op;
			this.rvalues = values;
			this.right = prop != Property.order && values.length > 0 && values[0] instanceof Property ? (Property)values[0] : null;
			this.unbound = any(rvalues, v -> v == UNBOUND);
		}

		public Criterium bindTo(Map<Property, Name> context) {
			if (!unbound)
				return this;
			Object[] values = rvalues.clone();
			for (int i = 0; i < values.length; i++)
				if (values[i] == UNBOUND)
					values[i] = context.get(left);
			return new Criterium(left, op, values);
		}

		public int intValue(int def) {
			return left.type != number ? def : ((Number)rvalues[0]).intValue();
		}

		public boolean isPropertyComparison() {
			return right != null;
		}

		public boolean matches(Task t, Date today) {
			if (left.isResultProperty())
				return true; // basically we ignore these as filter
			Comparable<?> val = left.access(t, today);
			if (isPropertyComparison()) {
				Comparable<?> val2 = right.access(t, today);
				switch (op) {
				case in:  return contains(val, val2);
				case eq:  return equals(val, val2);
				case nin: return !contains(val, val2);
				case neq: return !equals(val, val2);
				case lt:  return cmp(val, val2) <  0;
				case le:  return cmp(val, val2) <= 0;
				case gt:  return cmp(val, val2) >  0;
				case ge:  return cmp(val, val2) >= 0;
				default:  return false;
				}
			}
			switch (op) {
			// set comparisons
			case eq:  return equals(val, rvalues);
			case in:  return contains1(val, rvalues);
			case neq: return !equals(val, rvalues);
			case nin: return !contains1(val, rvalues);
			// non set comparisons
			case lt:  return cmp(val, rvalues[0]) <  0;
			case le:  return cmp(val, rvalues[0]) <= 0;
			case gt:  return cmp(val, rvalues[0]) >  0;
			case ge:  return cmp(val, rvalues[0]) >= 0;
			default:  return false;
			}
		}

		private boolean equals(Comparable<?> lv, Comparable<?> rv) {
			if (left.type == name) {
				boolean leftIsSet = left.isSetValue();
				boolean rightIsSet = right.isSetValue();
				if (leftIsSet != rightIsSet) {
					return leftIsSet
							? ((Names) lv).contains((Name)rv)
							: ((Names) rv).contains((Name)lv);
				}
			}
			return lv.equals(rv);
		}

		private boolean contains(Comparable<?> lv, Comparable<?> rv) {
			if (left.type == name) {
				if (right.type == text) {
					Gist text = (Gist) rv;
					return text.contains(Gist.gist(lv.toString()));
				}
				boolean leftIsSet = left.isSetValue();
				boolean rightIsSet = right.isSetValue();
				if (leftIsSet != rightIsSet) {
					return leftIsSet
							? ((Names) lv).contains((Name)rv)
							: ((Names) rv).contains((Name)lv);
				}
				if (leftIsSet && rightIsSet) {
					for (Name n : (Names)lv)
						if (((Names)rv).contains(n))
							return true;
					return false;
				}
			}
			return lv.equals(rv);
		}

		private boolean equals(Comparable<?> lv, Object[] rvs) {
			if (left.type == name && left.isSetValue()) {
				Names set = (Names) lv;
				if (set.count() != rvs.length)
					return false;
				for (int i = 0; i< rvs.length; i++) {
					if (!set.contains((Name)rvs[i]))
						return false;
				}
				return true;
			}
			return rvs.length == 1 && rvs[0].equals(lv);
		}

		private boolean contains1(Comparable<?> val, Object[] anyOf) {
			if (left.type == name && left.isSetValue()) {
				Names set = (Names) val;
				for (int i = 0; i< anyOf.length; i++) {
					if (set.contains((Name)anyOf[i]))
						return true;
				}
				return false;
			}
			if (left.type == text) {
				Gist g = (Gist) val;
				for (int i = 0; i < anyOf.length; i++)
					if (g.contains((Gist)anyOf[i]))
						return true;
				return false;
			}
			if (anyOf.length == 1)
				return anyOf[0].equals(val);
			for (int i = 0; i < anyOf.length; i++)
				if (anyOf[i].equals(val))
					return true;
			return false;
		}

		@SuppressWarnings("unchecked")
		private static <T> int cmp(Comparable<T> a, Object b) {
			if (a.getClass() != b.getClass())
				return -1;
			return a.compareTo((T) b);
		}

		@Override
		public String toString() {
			String val = rvalues.length == 1 ? rvalues[0].toString() : "{"+join(rvalues, ", ")+"}";
			return "["+left.name()+" "+op+" "+(isPropertyComparison()? "@": "")+val+"]";
		}

		@Override
		public int compareTo(Criterium other) {
			if (left.isResultProperty() != other.left.isResultProperty())
				return left.isResultProperty() ? 1 : -1;
			int res = op.compareTo(other.op);
			if (res != 0)
				return res;
			return signum(other.left.selectivity - left.selectivity);
		}

	}

	public static enum Coloration {

		heat, status, goal, cause,
	}

	/**
	 * Pages not using a {@link #list} {@link Layout} usually only use a single
	 * query in the {@link Page} {@link Template} as these layouts require
	 * horizontal space.
	 *
	 * @author jan
	 */
	public static enum Layout {
		/**
		 * The usual list of task
		 */
		list,
		/**
		 * A board is grouping the results in columns given by a the
		 * {@link Property#group}. The columns are usually the constants of an
		 * enum. Special boards for version, area or user would work too; user
		 * gives the classic "unassigned", "planed", "in progress", "done")
		 */
		board,
		/**
		 * Is a overview table only showing a specific property of the matching
		 * task to give a overall impression or indication about an aspect.
		 *
		 * Tasks are the rows. Columns could be users (this makes very much
		 * sense as multiple users can work on a task). It helps to understand
		 * how well task are covered.
		 *
		 * Another tableau could be using areas as rows and showing
		 * accumulations in the columns. Like using Status as columns would show
		 * how many tasks of a type in each state exist. Or maybe even links to
		 * em.
		 */
		tableau,
		/**
		 * Tree is used in connection with filter on {@link Property#basis} or {@link Property#origin}.
		 * The tasks are shown as a tree with the origin being the root.
		 */
		tree

	}

	/**
	 * Properties a task can be filtered by.
	 */
	public static enum Property {
		// result properties
		offset(number, 0, eq, ge, le, gt, lt),
		length(number, 0, eq, le, lt),
		order(property, 0, Property.class, asc, desc),
		group(property, 0, Property.class, eq),
		coloration(property, 0, Coloration.class, eq),
		layout(property, 0, Layout.class, eq),

		// task properties
		emphasis(number, 10, eq, ge, le, gt, lt),
		temperature(number, 10, eq, ge, le, gt, lt),
		heat(property, 6, Heat.class, eq, neq, in, nin, lt, gt, ge, le),
		status(property, 5, Status.class, eq, neq, in, nin),
		goal(property, 4, Goal.class, eq, neq, in, nin),
		cause(property, 3, Cause.class, eq, neq, in, nin),
		version(name, 10, eq, neq, in, nin),
		reported(date, 50, eq, ge, le, gt, lt, in),
		resolved(date, 50, eq, ge, le, gt, lt, in),
		exploitable(flag, 1, eq, neq),
		archived(flag, 1, eq, neq),
		age(number, 15, eq, ge, le, gt, lt),
		id(number, 100, eq, ge, le, gt, lt, in, nin),
		origin(number, 20, eq, ge, le, gt, lt, in, nin),
		basis(number, 25, eq, ge, le, gt, lt, in, nin),
		serial(number, 90, eq, ge, le, gt, lt, in, nin),
		reporter(name, 50, eq, neq, in, nin),
		solver(name, 60, eq, neq, in, nin),
		supporter(name, 40, eq, neq, in, nin), // means either a aspirant or participant
		maintainer(name, 30, eq, neq, in, nin),
		watcher(name, 25, eq, neq, in, nin),
		aspirant(name, 45, eq, neq, in, nin),
		participant(name, 45, eq, neq, in, nin),
		area(name, 20, eq, neq, in, nin),
		category(name, 8, eq, neq, in, nin),
		output(name, 1, eq, neq, in, nin),
		url(text, 70, eq, gt, lt, in, nin), //TODO when eq, gt or lt is used and the value is not an URL (starts with http) then we somehow have to know what kind of integration URL is meant and look for that
		gist(text, 70, eq, gt, lt, in, nin),
		conclusion(text, 70, eq, gt, lt, in, nin);

		/**
		 * The operations supported by the property
		 */
		public final EnumSet<Operator> ops;

		/**
		 * The type usually used with the property.
		 * When an {@link Operator} is used that just is just with other types those are also accepted.
		 * If an {@link Operator} is used with the {@link #type} this has to be used.
		 */
		public final ValueType type;
		/**
		 * A range from 0 (no selecting at all) to 100 (unique selection).
		 *
		 * A measure of how good the property is suited to reduce possible
		 * matching tasks to a small set of tasks.
		 */
		public final int selectivity;

		private final Class<? extends Enum<?>> propertyType;
		private Enum<?>[] values;

		private Property(ValueType type, int selectivity, Operator... ops) {
			this(type, selectivity, null, ops);
		}
		private Property(ValueType type, int selectivity, Class<? extends Enum<?>> propertyType, Operator... ops) {
			this.propertyType = propertyType;
			this.selectivity = selectivity;
			this.ops = EnumSet.of(ops[0], ops);
			this.type = type;
		}

		public boolean isResultProperty() {
			return ordinal() <= coloration.ordinal();
		}

		public boolean isSetValue() {
			return ordinal() >= supporter.ordinal() && ordinal() <= participant.ordinal();
		}

		public Enum<?> value(String name) {
			if (values == null) {
				values = propertyType.getEnumConstants(); // need to do this outside of constructor as Property itself can be the enum
			}
			for (Enum<?> v : values) {
				if (v.name().equalsIgnoreCase(name))
					return v;
			}
			throw new CriteriumMalformed("No such value: `"+name+"` valid values are: "+Arrays.toString(values));
		}

		public static Property property(String name) {
			try {
				return valueOf(name);
			} catch (IllegalArgumentException e) {
				throw new CriteriumMalformed("No such property: `"+name+"`, valid properties are: "+ Arrays.toString(values()));
			}
		}

		public Comparable<?> access(Task t, Date today) {
			switch (this) {
			case emphasis: return t.emphasis;
			case heat: return t.heat(today);
			case status: return t.status;
			case goal: return t.goal;
			case cause : return t.cause;
			case version: return t.base.name;
			case reported: return t.reported;
			case resolved: return t.resolved;
			case exploitable : return t.exploitable;
			case archived: return t.archived;
			case age: t.age(today);
			default:
			case id: return t.id;
			case origin: return t.origin;
			case basis: return t.basis;
			case serial: return t.serial;
			case reporter: return t.reporter;
			case solver: return t.solver;
			case aspirant: return t.aspirants;
			case participant: return t.participants;
			case supporter: return t.supporters();
			case maintainer: return t.area.maintainers;
			case watcher: return t.watchers;
			case area: return t.area.name;
			case output: return t.output.name;
			case gist: return t.gist;
			case conclusion: return t.conclusion;
			}
		}
	}

	public static enum ValueType {
		/**
		 * essentially an <code>int<code>
		 */
		number("\\d+"),
		/**
		 * essentially a enumeration name or constant name like "asc"
		 */
		property("[a-z]+"),
		/**
		 * A {@link Name}
		 */
		name("[-a-zA-Z0-9_@.]+[*~]?"),
		/**
		 * A {@link Date}
		 */
		date("\\d\\d\\d\\d(?:-\\d\\d(?:-\\d\\d))"),
		/**
		 * essentially true or false but names like "yes"/"no" or such are also acceptable
		 */
		flag("true|false|yes|no|1|0|on|off"),
		/**
		 * A {@link Gist} of letters, digits, space and punctuation characters are allowed,
		 * typical URLs should be accepted.
		 * {@link Operator#gt} is used as "starts with" and {@link Operator#lt} as "ends with".
		 */
		text(Gist.GIST_TEXT_REGEX);

		public final Pattern value;

		private ValueType(String value) {
			this.value = Pattern.compile("^"+value+"$");
		}

		public boolean isValid(String val) {
			return value.matcher(val).matches();
		}
	}

	public static enum Operator {
		eq("=", true),
		/**
		 * Is the value one of a set of alternatives?
		 */
		in("~", true),
		gt(">", false),
		lt("<", false),
		ge(">=", false),
		le("<=", false),
		neq("!=", true),
		/**
		 * Is the value not one of a set of alternatives?
		 */
		nin("!~", true),

		asc(">>", true),
		desc("<<", true)
		;
		//OPEN should there be an operator "not exits" to check e.g. for tasks without a origin, or task where solver is not yet defined

		public final String symbol;
		public final boolean allowsMultipleArguments;

		private Operator(String symbol, boolean multi) {
			this.symbol = symbol;
			this.allowsMultipleArguments = multi;
		}

		public static Operator forSymbol(String symbol) {
			for (Operator op : values()) {
				if (op.symbol.equals(symbol))
					return op;
			}
			throw new CriteriumMalformed("No such operator `"+symbol+"`, valid operators are "+Arrays.toString(Operator.values()));
		}

		public boolean isFilter() {
			return !isSelector() && ordinal() <= nin.ordinal();
		}

		public boolean isSelector() {
			return ordinal() <= in.ordinal() ;
		}

		@Override
		public String toString() {
			return symbol;
		}
	}

	static String join(Object[] values, String separator) {
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < values.length; i++) {
			if (i > 0) b.append(separator);
			b.append(values[i]);
		}
		return b.toString();
	}

	public static class CriteriumMalformed extends IllegalArgumentException {

		public CriteriumMalformed(String message) {
			super(message);
		}

		public CriteriumMalformed(String message, Throwable cause) {
			super(message, cause);
		}

	}

}
