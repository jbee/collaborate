package vizio;

import static java.lang.Math.max;
import static vizio.Date.date;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class User {

	private static final int MINIMUM_WATCHES = 20;

	public Name name;
	// account
	public String email;
	public byte[] md5;
	public boolean activated;
	// user data
	public Site[] sites;
	public AtomicInteger watches;
	/* merged properties ...*/

	// activity statistics
	public long millisLastActive;
	public int xp;
	public int absolved;
	public int resolved;
	public int dissolved;
	// supporting tasks
	public long millisStressed;
	public int stressedToday;
	//TODO maybe add a lock? user reporting to much are locked ...

	public void mergeWith(User instance) {
		if (!name.equalTo(instance.name)
			|| !email.equals(instance.email)
			|| !Arrays.equals(md5, instance.md5)
			|| activated != instance.activated
			|| sites.length != instance.sites.length
			) {
			throw new IllegalArgumentException("Cannot merge user instances!");
		}
		millisLastActive = max(millisLastActive, instance.millisLastActive);
		xp = max(xp, instance.xp);
		absolved = max(absolved, instance.absolved);
		resolved = max(resolved, instance.resolved);
		dissolved = max(dissolved, instance.dissolved);
		millisStressed = max(millisStressed, instance.millisStressed);
		stressedToday = max(stressedToday, instance.stressedToday);
	}

	public int stressDelay() {
		return max(60000, (int)( 3600000f / (1f+(xp/50f))));
	}

	public int stressesPerDay() {
		return 10 + (xp/5);
	}

	public boolean canStress(long now) {
		return activated
			&&	now - millisStressed > stressDelay()
			&& (stressedToday < stressesPerDay() || date(now).after(date(millisStressed)));
	}

	public void stressed(long now) {
		if (date(now).after(date(millisStressed))) {
			stressedToday = 1;
		} else {
			stressedToday++;
		}
		millisStressed = now;
	}

	public boolean hasSite(Name name) {
		for (Site s : sites) {
			if (s.name.equalTo(name))
				return true;
		}
		return false;
	}

	public boolean canWatch() {
		return watches.get() < MINIMUM_WATCHES + (xp / 10);
	}

	@Override
	public String toString() {
		return name != null ? name.toString() : email.toString();
	}

}
