package se.jbee.track.model;

import static java.lang.Math.max;
import static se.jbee.track.model.Date.date;

import java.util.EnumMap;

public final class User extends Entity<User> {

	/**
	 * The idea of a fine grained setting is to reduce mails or mail handling by
	 * allowing users to configure away messages by using
	 * {@link Mail.Delivery#never} and at the same time give the user a way to
	 * adopt mails to their usage pattern.
	 * 
	 * In general one get mails for all tasks involved or watched. So watching
	 * is mostly a way to trigger notifications for task one isn't involved in.
	 */
	@UseCode
	public static enum Notification {
		// user
		authenticated(Mail.Delivery.never), // a login occurred
		// product
		constituted(Mail.Delivery.daily),
		// area
		opened(Mail.Delivery.daily),    // for user that are origin maintainers
		left(Mail.Delivery.daily),      // by a maintainer (to other maintainers)
		// version
		tagged(Mail.Delivery.daily),    // for user that are origin maintainers
		// poll
		polled(Mail.Delivery.hourly),   // in an area the user is maintainer (can vote)
		voted(Mail.Delivery.hourly),     // for a poll where user can vote (is maintainer)
		// task
		reported(Mail.Delivery.hourly), // new tasks (in maintained area)
		developed(Mail.Delivery.daily), // a task the user is involved in has been updated or segmented
		moved(Mail.Delivery.daily),     // where user is involved
		solved(Mail.Delivery.hourly),   // where user is involved
		extended(Mail.Delivery.hourly)  // where user is involved
		;
		
		public final Mail.Delivery def;
		
		Notification(Mail.Delivery def) {
			this.def = def;
		}
	}
	
	private static final int INITIAL_WATCH_LIMIT = 20;

	public Name alias;
	// account
	public Email email;
	public int authenticated; // count
	public transient byte[] otp; // mem only
	public byte[] encryptedOtp; // persisted
	public long millisOtpExprires;
	
	// user data
	public int watches; // n tasks
	public EnumMap<Notification,Mail.Delivery> notificationSettings;
	//TODO preferred page size?
	
	// change log
	public long millisLastActive;
	
	// activity statistics
	public int xp;
	public int absolved;
	public int resolved;
	public int dissolved;
	public int abandoned;
	public Names contributesToProducts;
	
	// voting tasks
	public long millisEmphasised;
	public int emphasisedToday;

	public User(int version) {
		super(version);
	}
	
	/**
	 * User entity is not cloned to but changed in place an explicitly modified
	 * by touching.
	 */
	public void touch(long now) {
		millisLastActive = now;
		modified();
	}
	
	@Override
	public ID computeID() {
		return ID.userId(alias);
	}
	
	@Override
	public Name product() {
		return Name.ORIGIN;
	}
	
	public boolean isAnonymous() {
		return alias.isEmail();
	}

	public int emphDelay() {
		return max(60000, (int)( 3600000f / (1f+(xp/50f))));
	}

	public int emphPerDay() {
		return 10 + (xp/5);
	}

	public boolean canEmphasise(long now) {
		return isAuthenticated()
			&&	now - millisEmphasised > emphDelay()
			&& (emphasisedToday < emphPerDay() || date(now).after(date(millisEmphasised)));
	}

	public boolean isAuthenticated() {
		//TODO make this session dependent - the user should have authenticated in this session
		// maybe reset a users auth when a register/confirm occurs 
		return authenticated > 0 && alias.isEditable();
	}

	public void emphasised(long now) {
		if (date(now).after(date(millisEmphasised))) {
			emphasisedToday = 1;
		} else {
			emphasisedToday++;
		}
		millisEmphasised = now;
	}

	public boolean canWatch() {
		return watches < INITIAL_WATCH_LIMIT + (xp / 10);
	}

}
