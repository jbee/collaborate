package vizio.io;

import vizio.Date;
import vizio.Task;

public class Criteria {

	/**
	 * Properties a task can be filtered by
	 */
	public static enum Property {
		temp, heat, status, purpose, motive,
		version, start, end,
		exploitable,
		entrance, // serial is set
		age, id, origin, cause, serial,
		reporter, solver, users, maintainers, enlisted_by, approched_by, watched_by,
		area, product,
		gist, conclusion;
		
		public Comparable<?> access(Task t, Date today) {
			switch (this) {
			case temp: return t.temerature(today);
			case heat: return t.heat;
			case status: return t.status;
			case purpose: return t.purpose;
			case motive : return t.motive;
			case version: return t.base.name;
			case start: return t.start;
			case end: return t.end;
			case exploitable : return t.exploitable;
			case entrance: return t.area.entrance;
			case age: t.age(today);
			default:
			case id: return t.id;
			case origin: return t.origin;
			case cause: return t.cause;
			case serial: return t.serial;
			case reporter: return t.reporter;
			case solver: return t.solver;
			case users: return t.approachedBy; // TODO not quite...
			case maintainers: return t.area.maintainers;
			case enlisted_by: return t.enlistedBy;
			case approched_by: return t.approachedBy;
			case watched_by: return t.watchedBy;
			case area: return t.area.name;
			case product: return t.product.name;
			case gist: return t.gist; 
			case conclusion: return t.conclusion;
			}
		}
	}

	public static enum Operator {
		eq("="), neq("!="), gt(">"), lt("<"), ge(">="), le("<="), in("~"), nin("!~"), any("/"), nany("!/");

		public String symbol;

		private Operator(String symbol) {
			this.symbol = symbol;
		}

		public static Operator forSymbol(String symbol) {
			for (Operator op : values()) {
				if (op.symbol.equals(symbol))
					return op;
			}
			throw new IllegalArgumentException(symbol);
		}
	}

	public static class Filter {

		public Property prop;
		public Operator op;
		public String[] value;

		public Filter(Property prop, Operator op, String[] value) {
			super();
			this.prop = prop;
			this.op = op;
			this.value = value;
		}

		@Override
		public String toString() {
			String vs = value.length == 1 ? value[0] : "{"+join(value)+"}";
			return prop.name()+op.symbol+vs;
		}
	}

	public static class Range {

		public final int start;
		public final int end;

		public Range(int start, int end) {
			super();
			this.start = start;
			this.end = end;
		}

		@Override
		public String toString() {
			String e = end < 0 ? "*" : String.valueOf(end);
			String s  = start < 0 ? "*" : String.valueOf(start);
			return String.format("{%s %s}", s, e);
		}

		public int length() {
			return end-start+1;
		}
	}

	public Range range;
	public Filter[] filters;
	public Property[] orders;

	@Override
	public String toString() {
		return String.format("%s[%s]<%s>", range, join(filters), join(orders));
	}

	static String join(Object[] values) {
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < values.length; i++) {
			if (i > 0) b.append(' ');
			b.append(values[i]);
		}
		return b.toString();
	}
}
