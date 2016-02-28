package vizio;

import static java.lang.Math.max;
import static vizio.Date.date;

public class User {

	/**
	 * The delay is not based on XP since this protection assumes that a users
	 * account has been compromised an is abused to mass report bullsh*t.
	 */
	private static final long REPORT_DELAY = 60000L; //ms = 1min
	/**
	 * A user can report once per hour on average.
	 */
	private static final int REPORTS_PER_DAY = 24;
	
	public Name name;
	// account
	public String email;
	public boolean confirmed;
	// activity statistics
	public Date lastActive;
	public int xp;
	public int absolved;
	public int resolved;
	public int dissolved;
	// supporting tasks
	public long millisSupported;
	public int supportedToday;
	// reporting tasks (protection against compromised accounts or abuse of anonymous reports)
	public long millisReported;
	public int reportedToday;
	//TODO maybe add a lock? user reporting to much a locked ...
	
	public int supportDelay() {
		return max(60000, (int)( 3600000f / (1f+(xp/50f))));
	}

	public int supportsPerDay() {
		return 10 + (xp/5);
	}

	public boolean canSupport(long now) {
		return now - millisSupported > supportDelay()
			&& (supportedToday < supportsPerDay() || date(now).after(date(millisSupported)));
	}

	public void supports(long now) {
		if (date(now).after(date(millisSupported))) {
			supportedToday = 1;
		} else {
			supportedToday++;
		}
		millisSupported = now;
	}
	
	public boolean canReport(long now) {
		return now - millisReported > REPORT_DELAY
			&& (reportedToday < REPORTS_PER_DAY || date(now).after(date(millisReported)));
	}
	
	public void reports(long now) {
		if (date(now).after(date(millisReported))) {
			reportedToday =1;
		} else {
			reportedToday++;
		}
		millisReported = now;
	}
	
	@Override
	public String toString() {
		return name != null ? name.toString() : email.toString();
	}
}
