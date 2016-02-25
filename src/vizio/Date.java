package vizio;

import static java.lang.Integer.compare;

public final class Date implements Comparable<Date> {

	private static final int MS_PER_DAY = 1000*60*60*24;

	public final int daysSinceEra;

	public Date(int daysSinceEra) {
		super();
		this.daysSinceEra = daysSinceEra;
	}

	public static Date date(long millisSinceEra) {
		return new Date( (int) (millisSinceEra / MS_PER_DAY));
	}

	public boolean after(Date other) {
		return daysSinceEra > other.daysSinceEra;
	}

	public int daysSince(Date other) {
		return daysSinceEra - other.daysSinceEra;
	}

	@Override
	public int compareTo(Date other) {
		return compare(daysSinceEra, other.daysSinceEra);
	}

}
