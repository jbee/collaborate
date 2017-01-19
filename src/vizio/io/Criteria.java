package vizio.io;

import static vizio.io.Criteria.Operator.*;
import static vizio.io.Criteria.ValueType.SET;
import static vizio.io.Criteria.ValueType.SIMPLE;

import java.util.EnumSet;

import vizio.model.Date;
import vizio.model.Task;

public final class Criteria {

	public final Clause[] clauses;
	
	public Criteria(Clause... clauses) {
		super();
		this.clauses = clauses;
	}

	@Override
	public String toString() {
		return join(clauses, "");
	}
	
	public static final class Clause {

		public Property prop;
		public Operator op;
		public String[] value;

		public Clause(Property prop, Operator op, String[] value) {
			super();
			this.prop = prop;
			this.op = op;
			this.value = value;
		}

		@Override
		public String toString() {
			String val = value.length == 1 ? value[0] : "{"+join(value, " ")+"}";
			return "["+prop.name()+" "+op+" "+val+"]";
		}
	}
	
	/**
	 * Properties a task can be filtered by
	 */
	public static enum Property {
		// general properties
		first(eq, ge, le, gt, lt), 
		last(eq, ge, le, gt, lt), 
		length(eq),
		
		//TODO ops below
		
		// task properties
		emphasis, heat, status, purpose, motive,
		version, start, end,
		exploitable,
		board, // serial is set
		age, id, origin, basis, serial,
		reporter, solver, 
		users, maintainers, watchers, pursued_by, engaged_by,
		area, product,
		gist, conclusion;
		
		public final EnumSet<Operator> supported;
		
		private Property(Operator... supported) {
			this.supported = EnumSet.of(supported[0], supported);
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
			case board: return t.area.board;
			case age: t.age(today);
			default:
			case id: return t.id;
			case origin: return t.origin;
			case basis: return t.basis;
			case serial: return t.serial;
			case reporter: return t.reporter;
			case solver: return t.solver;
			case users: return t.engagedBy; // TODO not quite...
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
	
	public static enum ValueType { SIMPLE, SET }

	public static enum Operator {
		eq("=", SIMPLE, SET), neq("!=", SIMPLE, SET), 
		gt(">", SIMPLE), lt("<", SIMPLE), ge(">=", SIMPLE), le("<=", SIMPLE), 
		in("~", SET), nin("!~", SET), any("/", SET), nany("!/", SET),
		ord("=>", SIMPLE)
		;

		public final String symbol;
		public final EnumSet<ValueType> supported;

		private Operator(String symbol, ValueType...supported) {
			this.symbol = symbol;
			this.supported = EnumSet.of(supported[0], supported);
		}

		public static Operator forSymbol(String symbol) {
			for (Operator op : values()) {
				if (op.symbol.equals(symbol))
					return op;
			}
			throw new IllegalArgumentException(symbol);
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
}
