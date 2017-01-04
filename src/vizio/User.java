package vizio;

import static java.lang.Math.max;
import static vizio.Date.date;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class User extends Entity<User> {

	private static final int MINIMUM_WATCHES = 20;

	public Name name;
	// account
	public String email;
	public byte[] md5;
	public boolean activated;
	// user data
	public Names sites;
	public int watches;
	/* merged properties ...*/

	// activity statistics
	public long millisLastActive;
	public int xp;
	public int absolved;
	public int resolved;
	public int dissolved;
	// supporting tasks
	public long millisEmphasised;
	public int emphasisedToday;
	//TODO maybe add a lock? user reporting to much are locked ...

	public boolean isAnonymous() {
		return name.isEmail();
	}

	public int emphDelay() {
		return max(60000, (int)( 3600000f / (1f+(xp/50f))));
	}

	public int emphPerDay() {
		return 10 + (xp/5);
	}

	public boolean canEmphasise(long now) {
		return activated
			&&	now - millisEmphasised > emphDelay()
			&& (emphasisedToday < emphPerDay() || date(now).after(date(millisEmphasised)));
	}

	public void emphasised(long now) {
		if (date(now).after(date(millisEmphasised))) {
			emphasisedToday = 1;
		} else {
			emphasisedToday++;
		}
		millisEmphasised = now;
	}

	public boolean hasSite(Name name) {
		return sites.contains(name);
	}

	public boolean canWatch() {
		return watches < MINIMUM_WATCHES + (xp / 10);
	}

	@Override
	public String toString() {
		return name != null ? name.toString() : email.toString();
	}

}
