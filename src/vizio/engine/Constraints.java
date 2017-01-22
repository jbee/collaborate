package vizio.engine;

import static vizio.engine.Constraints.Operator.any;
import static vizio.engine.Constraints.Operator.asc;
import static vizio.engine.Constraints.Operator.desc;
import static vizio.engine.Constraints.Operator.eq;
import static vizio.engine.Constraints.Operator.ge;
import static vizio.engine.Constraints.Operator.gt;
import static vizio.engine.Constraints.Operator.in;
import static vizio.engine.Constraints.Operator.le;
import static vizio.engine.Constraints.Operator.lt;
import static vizio.engine.Constraints.Operator.nany;
import static vizio.engine.Constraints.Operator.neq;
import static vizio.engine.Constraints.Operator.nin;
import static vizio.engine.Constraints.Operator.subst;
import static vizio.engine.Constraints.ValueType.date;
import static vizio.engine.Constraints.ValueType.flag;
import static vizio.engine.Constraints.ValueType.name;
import static vizio.engine.Constraints.ValueType.number;
import static vizio.engine.Constraints.ValueType.property;
import static vizio.engine.Constraints.ValueType.text;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import vizio.model.Date;
import vizio.model.Name;
import vizio.model.Task;

/**
 * A data structure to describe what {@link Task}s to select and how to present them. 
 */
public final class Constraints {

	private final Constraint[] constraints;
	
	public Constraints(Constraint... constraints) {
		super();
		this.constraints = constraints;
	}

	public int count() {
		return constraints.length;
	}
	
	@Override
	public String toString() {
		return join(constraints, "");
	}
	
	private static final Pattern CONSTRAINT = Pattern.compile("\\s*\\[([a-z]+)\\s*([=<>?!~]{1,2})\\s*([^\\]]+)\\]");
	
	public static Constraints parse(String s) throws ContraintParseException {
		Matcher m = CONSTRAINT.matcher(s);
		List<Constraint> res = new ArrayList<>();
		while (m.find()) {
			String p = m.group(1);
			String o = m.group(2);
			String v = m.group(3);
			Property prop = Property.property(p.toLowerCase());
			res.add(new Constraint(prop, Operator.forSymbol(o), parseValue(prop, v)));
		}
		return new Constraints(res.toArray(new Constraint[0]));
	}
	
	private static String[] parseValue(Property prop, String value) {
		if ("{}".equals(value))
			return new String[0];
		if (value.charAt(0) == '{') {
			value = value.substring(1, value.lastIndexOf('}'));
			return value.split("\\s*,\\s*");
		}
		return new String[] { value };
	}
	
	public static final class Constraint {

		public Property prop;
		public Operator op;
		public String[] value;

		public Constraint(Property prop, Operator op, String[] value) {
			super();
			this.prop = prop;
			this.op = op;
			this.value = value;
		}

		@Override
		public String toString() {
			String val = value.length == 1 ? value[0] : "{"+join(value, ", ")+"}";
			return "["+prop.name()+" "+op+" "+val+"]";
		}
	}
	
	/**
	 * Properties a task can be filtered by
	 */
	public static enum Property {
		// result properties
		first(number, eq, ge, le, gt, lt),
		last(number, eq, ge, le, gt, lt),
		length(number, eq, le, lt),
		order(property, asc, desc),
		color(name, eq),
		
		// task properties
		emphasis(number, eq, ge, le, gt, lt),
		heat(number, eq, ge, le, gt, lt),
		status(property, eq, neq, in, nin),
		purpose(property, eq, neq, in, nin),
		motive(property, eq, neq, in, nin),
		version(name, eq, neq, in, nin),
		start(date, eq, ge, le, gt, lt, in, nin),
		end(date, eq, ge, le, gt, lt, in, nin),
		exploitable(flag, eq, neq),
		age(number, eq, ge, le, gt, lt),
		id(number, eq, ge, le, gt, lt, in, nin),
		origin(number, eq, ge, le, gt, lt, in, nin),
		basis(number, eq, ge, le, gt, lt, in, nin),
		serial(number, eq, ge, le, gt, lt, in, nin),
		reporter(name, eq, neq, in, nin),
		solver(name, eq, neq, in, nin),
		users(name, eq, neq, in, nin, any, nany),
		maintainers(name, eq, neq, in, nin, any, nany),
		watchers(name, eq, neq, in, nin, any, nany),
		pursued_by(name, eq, neq, in, nin, any, nany),
		engaged_by(name, eq, neq, in, nin, any, nany),
		area(name, eq, neq, in, nin),
		product(name, eq, neq, in, nin),
		url(text, eq, gt, lt, subst),
		gist(text, eq, gt, lt, any, nany),
		conclusion(text, eq, gt, lt, any, nany);
		
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
		
		private Property(ValueType type, Operator... ops) {
			this.ops = EnumSet.of(ops[0], ops);
			this.type = type;
			
		}

		public static Property property(String name) {
			try {
				return valueOf(name);
			} catch (IllegalArgumentException e) {
				throw new ContraintParseException("No such property: `"+name+"`, valid properties are: "+ Arrays.toString(values()));
			}
		}

		public Comparable<?> access(Task t, Date today) {
			switch (this) {
			case emphasis: return t.emphasis;
			case heat: return t.heatType(today);
			case status: return t.status;
			case purpose: return t.purpose;
			case motive : return t.motive;
			case version: return t.base.name;
			case start: return t.start;
			case end: return t.end;
			case exploitable : return t.exploitable;
			case age: t.age(today);
			default:
			case id: return t.id;
			case origin: return t.origin;
			case basis: return t.basis;
			case serial: return t.serial;
			case reporter: return t.reporter;
			case solver: return t.solver;
			case users: return t.engagedBy;
			case maintainers: return t.area.maintainers;
			case pursued_by: return t.pursuedBy;
			case engaged_by: return t.engagedBy;
			case watchers: return t.watchedBy;
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
		date("\\d\\d\\d\\d-\\d\\d-\\d\\d"),
		/**
		 * essentially true or false but names like "yes"/"no" or such are also acceptable 
		 */
		flag("true|false|yes|no|1|0|on|off"),
		/**
		 * letters, digits, space and punctuation characters are allowed, typical URLs should be accepted.
		 * {@link Operator#gt} is used as "starts with" and {@link Operator#lt} as "ends with".
		 */
		text("[-+*/_:.?!#=%&a-zA-Z0-9\\s\\pL\\pN]+");
		
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
		neq("!=", true, true), 
		gt(">", true, false), 
		lt("<", true, false), 
		ge(">=", true, false), 
		le("<=", true, false), 
		in("~", false, true), 
		nin("!~", false, true), 
		any("?", false, true), 
		nany("!?", false, true),
		asc(">>", true, true),
		desc("<<", true, true),
		subst("~~", false, true)
		;

		public final String symbol;
		public final boolean isBinary;
		public final boolean isSet;

		private Operator(String symbol, boolean binary, boolean set) {
			this.symbol = symbol;
			this.isBinary = binary;
			this.isSet = set;
		}

		public static Operator forSymbol(String symbol) {
			for (Operator op : values()) {
				if (op.symbol.equals(symbol))
					return op;
			}
			throw new ContraintParseException("No such operator `"+symbol+"`, valid operators are "+Arrays.toString(Operator.values()));
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

	public static class ContraintParseException extends IllegalArgumentException {

		public ContraintParseException(String message) {
			super(message);
		}
	}
}
