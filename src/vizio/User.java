package vizio;

import static java.lang.Math.max;
import static java.lang.System.currentTimeMillis;
import static vizio.Date.date;

public class User {

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
	// lifting tasks
	public long millisLifted;
	public int liftedToday;

	public int liftingDelay() {
		return max(60000, (int)( 3600000f / (1f+(xp/50f))));
	}

	public int liftsPerDay() {
		return 10 + (xp/5);
	}

	public boolean canLift(Date today) {
		return currentTimeMillis() - millisLifted > liftingDelay()
			&& (liftedToday < liftsPerDay() || today.after(date(millisLifted)));
	}

	public void lift(Date today) {
		if (today.after(date(millisLifted))) {
			liftedToday = 1;
		} else {
			liftedToday++;
		}
		millisLifted = currentTimeMillis();
	}
}
