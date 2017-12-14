package vizio.cache;

import static java.util.Arrays.asList;
import static vizio.cache.Criteria.Operator.asc;
import static vizio.cache.Criteria.Operator.desc;
import static vizio.cache.Criteria.Operator.eq;
import static vizio.cache.Criteria.Operator.ge;
import static vizio.cache.Criteria.Operator.gt;
import static vizio.cache.Criteria.Operator.in;
import static vizio.cache.Criteria.Operator.le;
import static vizio.cache.Criteria.Operator.lt;
import static vizio.cache.Criteria.Operator.neq;
import static vizio.cache.Criteria.Operator.nin;
import static vizio.cache.Criteria.ValueType.date;
import static vizio.cache.Criteria.ValueType.flag;
import static vizio.cache.Criteria.ValueType.name;
import static vizio.cache.Criteria.ValueType.number;
import static vizio.cache.Criteria.ValueType.property;
import static vizio.cache.Criteria.ValueType.text;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import vizio.model.Bytes;
import vizio.model.Date;
import vizio.model.Gist;
import vizio.model.Motive;
import vizio.model.Name;
import vizio.model.Purpose;
import vizio.model.Status;
import vizio.model.Task;

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
 * While the uniform way of describing constraints can be a bit lengthy it
 * allows for simple parsing and remembering.
 */
public final class Criteria implements Iterable<Criteria.Criterium> {

	private final Criterium[] criteria;
	
	public Criteria(Criterium... criteria) {
		super();
		this.criteria = criteria;
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
		for (int i = 0; i < criteria.length; i++)
			if (criteria[i].prop == p)
				return i;
		return -1;
	}
	
	public Task[] filter(Iterable<Task> tasks, Date today) {
		List<Task> res = new ArrayList<>();
		for (Task t : tasks) {
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
	
	public Criterium topSelector(EnumSet<Property> properties) {
		int res = -1;
		int max = 0;
		for (int i = 0; i < criteria.length; i++) {
			Criterium criterium = criteria[i];
			if (       properties.contains(criterium.prop) 
					&& criterium.op.isSelector 
					&& criterium.prop.selectivity > max) {
				res = i;
				max = criterium.prop.selectivity;
			}
		}
		return res < 0 ? null : get(res);
	}
	
	public Criteria without(Property p, Property...more) {
		EnumSet<Property> excluded = EnumSet.of(p, more);
		Criterium[] res = new Criterium[criteria.length];
		int i = 0;
		for (int j = 0; j < criteria.length; j++) {
			if (!excluded.contains(criteria[j].prop))
				res[i++] = criteria[j];
		}
		return new Criteria(Arrays.copyOf(res, i));
	}
	
	public <T,V> T collect(T v0, Class<V> elemType, BiFunction<T, V, T> merge, Property p, Operator...ops) {
		EnumSet<Operator> included = EnumSet.of(ops[0], ops);
		T res = v0;
		for (Criterium ct : criteria) {
			if (ct.prop == p && included.contains(ct.op))
				for (Object v : ct.value)
					res = merge.apply(res, elemType.cast(v));
		}
		return res;
	}
	
	private static final Pattern CONSTRAINT = Pattern.compile("\\s*\\[([a-z]+)\\s*([=<>?!~]{1,2})\\s*([^\\]]+)\\]");
	
	public static Criteria parse(String s) throws MalformedConstraint {
		return parse(s, new HashMap<Criteria.Property, Name>());
	}
	
	public static Criteria parse(String s, Map<Property, Name> context) throws MalformedConstraint {
		Matcher m = CONSTRAINT.matcher(s);
		List<Criterium> res = new ArrayList<>();
		while (m.find()) {
			String p = m.group(1);
			String o = m.group(2);
			String v = m.group(3);
			Property prop = Property.property(p.toLowerCase());
			Operator op = Operator.forSymbol(o);
			if (!prop.ops.contains(op)) {
				throw new MalformedConstraint("Property `"+prop+"` does not support operation `"+op+"`, supported are: "+prop.ops.toString());
			}
			String[] val = parseValue(v);
			if (val.length == 1) {
				// just to get rid of those special cases right away
				if (op == in)
					op = eq;
				if (op == nin)
					op = neq;
				// @ can be used to refer the name for that property given by the context (useful for all properties of type name)
				if (prop.type == name && "@".equals(val[0])) {
					if (!context.containsKey(prop))
						throw new MalformedConstraint("No context substitution known for property: "+prop);
					val[0] = context.get(prop).toString();
				}
			}
			if (val.length > 1 && !op.setOp) {
				throw new MalformedConstraint("Operation `"+op+"` was used with set value `"+Arrays.toString(val)+"` but requires a simple value, use e.g. `"+val[0]+"`");
			}
			if (prop.type == date && val[0].length() < 10) {
				parseDate(prop, op, val, res);
			} else {
				res.add(criterium(prop, op, val));
			}
		}
		return new Criteria(res.toArray(new Criterium[0]));
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
		throw new MalformedConstraint("Date property "+prop+" does not support value: "+Arrays.toString(val));
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
				throw new MalformedConstraint("Failed to parse property "+p+" value: "+val[i], e);
			}
		}
		return res;
	}
	
	private static Object typed(Property p, String val) {
		switch (p.type) {
		case number   : return Integer.valueOf(val);
		case date     : return Date.parse(val);
		case name     : return Name.as(val);
		case property : return p.value(val);
		case text     : return Gist.gist(val);
		case flag     : return "true|yes|on|1".matches(val) ? Boolean.TRUE : Boolean.FALSE;
		default       : throw new MalformedConstraint("Unsupported value type: "+p.type);
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
	
	public static final class Criterium {

		public Property prop;
		public Operator op;
		public Object[] value;

		public Criterium(Property prop, Operator op, Object... value) {
			super();
			this.prop = prop;
			this.op = op;
			this.value = value;
		}

		public boolean matches(Task t, Date today) {
			if (prop.isResultProperty())
				return true; // basically we ignore these as filter
			Comparable<?> val = prop.access(t, today);
			switch (op) {
			case eq:  
			case in:  return isValue(val);
			case neq: 
			case nin: return !isValue(val);
			case lt:  return cmp(val, value[0]) <  0;
			case le:  return cmp(val, value[0]) <= 0;
			case gt:  return cmp(val, value[0]) >  0;
			case ge:  return cmp(val, value[0]) >= 0;
			default:  return false;
			}
		}
		
		public boolean isValue(Object other) {
			if (value.length == 1)
				return value[0].equals(other);
			for (int i = 0; i < value.length; i++)
				if (value[i].equals(other))
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
			String val = value.length == 1 ? value[0].toString() : "{"+join(value, ", ")+"}";
			return "["+prop.name()+" "+op+" "+val+"]";
		}
		
	}
	
	/**
	 * Properties a task can be filtered by
	 */
	public static enum Property {
		// result properties
		first(number, 0, eq, ge, le, gt, lt),
		last(number, 0, eq, ge, le, gt, lt),
		length(number, 0, eq, le, lt),
		order(property, 0, Property.class, asc, desc),
		color(name, 0, eq),
		
		// task properties
		emphasis(number, 10, eq, ge, le, gt, lt),
		heat(number, 10, eq, ge, le, gt, lt),
		status(property, 5, Status.class, eq, neq, in, nin),
		purpose(property, 4, Purpose.class, eq, neq, in, nin),
		motive(property, 3, Motive.class, eq, neq, in, nin),
		version(name, 10, eq, neq, in, nin),
		reported(date, 50, eq, ge, le, gt, lt, in),
		resolved(date, 50, eq, ge, le, gt, lt, in),
		exploitable(flag, 1, eq, neq),
		age(number, 15, eq, ge, le, gt, lt),
		id(number, 100, eq, ge, le, gt, lt, in, nin),
		origin(number, 20, eq, ge, le, gt, lt, in, nin),
		basis(number, 25, eq, ge, le, gt, lt, in, nin),
		serial(number, 90, eq, ge, le, gt, lt, in, nin),
		reporter(name, 50, eq, neq, in, nin),
		solver(name, 60, eq, neq, in, nin),
		user(name, 40, eq, neq, in, nin),
		maintainer(name, 30, eq, neq, in, nin),
		watcher(name, 25, eq, neq, in, nin),
		pursued_by(name, 45, eq, neq, in, nin),
		engaged_by(name, 45, eq, neq, in, nin),
		area(name, 20, eq, neq, in, nin),
		product(name, 0, eq, neq, in, nin),
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
			return ordinal() <= color.ordinal();
		}
		
		public Enum<?> value(String name) {
			if (values == null) {
				values = propertyType.getEnumConstants(); // need to do this outside of constructor as Property itself can be the enum
			}
			for (Enum<?> v : values) {
				if (v.name().equalsIgnoreCase(name))
					return v;
			}
			throw new MalformedConstraint("No such value: `"+name+"` valid values are: "+Arrays.toString(values));
		}

		public static Property property(String name) {
			try {
				return valueOf(name);
			} catch (IllegalArgumentException e) {
				throw new MalformedConstraint("No such property: `"+name+"`, valid properties are: "+ Arrays.toString(values()));
			}
		}

		public Comparable<?> access(Task t, Date today) {
			switch (this) {
			case emphasis: return t.emphasis;
			case heat: return t.heat(today);
			case status: return t.status;
			case purpose: return t.purpose;
			case motive : return t.motive;
			case version: return t.base.name;
			case reported: return t.reported;
			case resolved: return t.resolved;
			case exploitable : return t.exploitable;
			case age: t.age(today);
			default:
			case id: return t.id;
			case origin: return t.origin;
			case basis: return t.basis;
			case serial: return t.serial;
			case reporter: return t.reporter;
			case solver: return t.solver;
			case pursued_by: return t.pursuedBy;
			case engaged_by: return t.engagedBy;
			case user: return t.engagedBy;
			case maintainer: return t.area.maintainers;
			case watcher: return t.watchedBy;
			case area: return t.area.name;
			case product: return t.product.name;
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
		text(Bytes.BASIC_TEXT_REGEX);
		
		public final Pattern value;

		private ValueType(String value) {
			this.value = Pattern.compile("^"+value+"$");
		}
		
		public boolean isValid(String val) {
			return value.matcher(val).matches();
		}
	}

	public static enum Operator {
		eq("=", true, true), 
		neq("!=", true, false), 
		gt(">", false, false), 
		lt("<", false, false), 
		ge(">=", false, false), 
		le("<=", false, false), 
		in("~", true, true), 
		nin("!~", true, false), 
		asc(">>", true, false),
		desc("<<", true, false)
		;

		public final String symbol;
		public final boolean setOp;
		public final boolean isSelector;

		private Operator(String symbol, boolean set, boolean selector) {
			this.symbol = symbol;
			this.setOp = set;
			this.isSelector = selector;
		}

		public static Operator forSymbol(String symbol) {
			for (Operator op : values()) {
				if (op.symbol.equals(symbol))
					return op;
			}
			throw new MalformedConstraint("No such operator `"+symbol+"`, valid operators are "+Arrays.toString(Operator.values()));
		}
		
		public boolean isFilter() {
			return !isSelector;
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

	public static class MalformedConstraint extends IllegalArgumentException {

		public MalformedConstraint(String message) {
			super(message);
		}

		public MalformedConstraint(String message, Throwable cause) {
			super(message, cause);
		}
		
	}

}
