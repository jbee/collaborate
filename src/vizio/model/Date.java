package vizio.model;

import static java.lang.Integer.compare;

import java.time.LocalDate;

public final class Date implements Comparable<Date> {

	private static final int MILLIS_PER_DAY = 1000*60*60*24;

	public final int epochDay;

	public Date(int epochDay) {
		super();
		this.epochDay = epochDay;
	}
	
	public static Date parse(String yyyymmdd) {
		return new Date((int) LocalDate.parse(yyyymmdd).toEpochDay());
	}

	public static Date date(long millisSinceEpoch) {
		return new Date( (int) (millisSinceEpoch / MILLIS_PER_DAY));
	}

	public boolean after(Date other) {
		return epochDay > other.epochDay;
	}

	public int daysSince(Date other) {
		return epochDay - other.epochDay;
	}

	@Override
	public int compareTo(Date other) {
		return compare(epochDay, other.epochDay);
	}

	public Date plusDays(int days) {
		return new Date(epochDay+days);
	}
	
	public Date minusDays(int days) {
		return new Date(epochDay-days);
	}

	public static Date today() {
		return date(System.currentTimeMillis());
	}
	
	@Override
	public String toString() {
		return LocalDate.ofEpochDay(epochDay).toString();
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof Date && equalTo((Date) obj);
	}

	public boolean equalTo(Date other) {
		return epochDay == other.epochDay;
	}
	
	@Override
	public int hashCode() {
		return epochDay;
	}

}
