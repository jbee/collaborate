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
	public byte[] md5;
	public boolean activated;
	// activity statistics
	public Date lastActive;
	public int xp;
	public int absolved;
	public int resolved;
	public int dissolved;
	// supporting tasks
	public long millisEmphasized;
	public int emphasizedToday;
	// reporting tasks (protection against compromised accounts or abuse of anonymous reports)
	public long millisReported;
	public int reportedToday;
	//TODO maybe add a lock? user reporting to much a locked ...
	
	public int emphasisDelay() {
		return max(60000, (int)( 3600000f / (1f+(xp/50f))));
	}

	public int emphasisPerDay() {
		return 10 + (xp/5);
	}

	public boolean canEmphasize(long now) {
		return activated
			&&	now - millisEmphasized > emphasisDelay()
			&& (emphasizedToday < emphasisPerDay() || date(now).after(date(millisEmphasized)));
	}

	public void emphasized(long now) {
		if (date(now).after(date(millisEmphasized))) {
			emphasizedToday = 1;
		} else {
			emphasizedToday++;
		}
		millisEmphasized = now;
	}
	
	public boolean canReport(long now) {
		return activated
			&&	now - millisReported > REPORT_DELAY
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
