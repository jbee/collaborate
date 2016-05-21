package vizio;

import static vizio.Date.date;

/**
 * A {@link Cluster} holds the data of an actual {@link Tracker} instance.
 *
 * A {@link Tracker} spans multiple {@link Product}s, each having {@link Task}s,
 * {@link Version}s, {@link Area}s and {@link Poll}s.
 *
 * All {@link User}s are common to a {@link Cluster} or {@link Tracker}
 * instance.
 *
 * A {@link Cluster}'s "counters" change when new {@link Product}s or
 * {@link Area} are created.
 *
 * @author jan
 */
public class Cluster {

	private static final long EXTENTION_DELAY = 30000; // ms = 30sec
	private static final int EXTENSIONS_PER_DAY = 100;
	private static final long REGISTRATION_DELAY = 30000;// ms = 30sec
	private static final int UNCONFIRMED_REGISTRATIONS_PER_DAY = 100;

	/**
	 * When registering the client just sends the MD5 of the pass-phrase to the
	 * server where it is salted and hashed again. When logging in the
	 * pass-phrase is send plain to the server, where it is hashed, salted and
	 * hashed again before it is compared. This way the hashes stored are less
	 * likely to match known hashes. As long as the salt is unknown the
	 * protection is quite strong.
	 */
	public String salt;

	// protection against to many products and areas are created
	public long millisExtended;
	public int extensionsToday;

	// protection against registering users but never activating them
	public long millisRegistered;
	public int unconfirmedRegistrationsToday;

	//TODO maybe track IPs of unconfirmed additions to block them?
	// or at least reduce unconfirmed additions to 1 per IP/User

	public boolean canExtend(long now) {
		return now - millisExtended > EXTENTION_DELAY
			&& (extensionsToday < EXTENSIONS_PER_DAY || date(now).after(date(millisExtended)));
	}

	public void extended(long now) {
		if (date(now).after(date(millisExtended))) {
			extensionsToday = 1;
		} else {
			extensionsToday++;
		}
		millisExtended = now;
	}

	public boolean canRegister(long now) {
		return now - millisRegistered > REGISTRATION_DELAY
			&& (unconfirmedRegistrationsToday < UNCONFIRMED_REGISTRATIONS_PER_DAY || date(now).after(date(millisRegistered)));
	}

	public void registered(long now) {
		if (date(now).after(date(millisRegistered))) {
			unconfirmedRegistrationsToday = 1;
		} else {
			unconfirmedRegistrationsToday++;
		}
		millisRegistered = now;
	}

}
