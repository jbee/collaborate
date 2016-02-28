package vizio;

import static vizio.Date.date;

public class Cluster {

	private static final long EXTENTION_DELAY = 30000; // ms = 30sec
	private static final int EXTENSIONS_PER_DAY = 100;
	
	// protection against to many products and areas are created
	public long millisExtended;
	public int extensionsToday;
	
	public boolean canExtend(long now) {
		return now - millisExtended > EXTENTION_DELAY
			&& (extensionsToday < EXTENSIONS_PER_DAY || date(now).after(date(millisExtended)));
	}
	
	public void extended(long now) {
		if (date(now).after(date(millisExtended))) {
			extensionsToday =1;
		} else {
			extensionsToday++;
		}
		millisExtended = now;
	}
}
