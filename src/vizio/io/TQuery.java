package vizio.io;

public class TQuery {

	public static enum Property {
		temp, status, goal, stimulus
        , heat, version, start, end, age
        , users, active_users, passive_users
        , area, product
        , summary
        , exploitable
	}

	public static enum Operator {
		//'=' | '!=' | '>' | '<' | '>=' | '<=' | '~' | '!~' | '/' | '!/'
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
	}

	public int offset;
	public int length;
	public Filter[] filters;
	public Property[] orders;
}
