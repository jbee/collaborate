package vizio;

import static java.lang.System.currentTimeMillis;

public final class Date {

	public final int daysSinceEra;

	public Date(int daysSinceEra) {
		super();
		this.daysSinceEra = daysSinceEra;
	}

	static Date today() {
		return date(currentTimeMillis());
	}

	public static Date date(long millisSinceEra) {
		return new Date( (int) (millisSinceEra / (1000*60*60*24)));
	}

	public boolean after(Date other) {
		return daysSinceEra > other.daysSinceEra;
	}

	public int daysSince(Date other) {
		return daysSinceEra - other.daysSinceEra;
	}

}
