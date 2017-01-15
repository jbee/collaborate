package vizio.model;

import static java.lang.Math.max;
import static vizio.model.Date.date;

public final class User extends Entity<User> {

	private static final int MINIMUM_WATCH_LIMIT = 20;

	public Name name;
	// account
	public String email;
	public byte[] md5;
	public boolean activated;
	
	// user data
	public Names sites;
	public int watches; // n tasks

	// change log
	public long millisLastActive;
	
	// activity statistics
	public int xp;
	public int absolved;
	public int resolved;
	public int dissolved;
	
	// voting tasks
	public long millisEmphasised;
	public int emphasisedToday;

	public User(int version) {
		super(version);
	}
	
	@Override
	public ID computeID() {
		return ID.userId(name);
	}
	
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
		return watches < MINIMUM_WATCH_LIMIT + (xp / 10);
	}

}
