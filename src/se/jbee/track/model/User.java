package se.jbee.track.model;

import static java.lang.Math.max;
import static se.jbee.track.model.Date.date;

import java.util.EnumMap;

public final class User extends Entity<User> {

	@UseCode("urca")
	public static enum AuthState { unregistered, registered, confirming, authenticated }

	private static final int INITIAL_WATCH_LIMIT = 20;

	public static final User ANONYMOUS = anonymousUser();

	public Name alias;
	// account
	public Email email;
	public AuthState authState = AuthState.unregistered;
	public int authenticated; // how often
	public transient byte[] otp; // mem only
	public byte[] encryptedOtp; // persisted
	public long millisOtpExprires;

	// user data
	public int watches; // n tasks
	public EnumMap<Mail.Notification,Mail.Delivery> notificationSettings;
	//TODO preferred page size?

	// change log
	public long millisLastActive;

	// activity statistics
	public int xp;
	public int absolved;
	public int resolved;
	public int dissolved;
	public int abandoned;
	public Names contributesToOutputs;

	// voting tasks
	public long millisEmphasised;
	public int emphasisedToday;

	public User(int version) {
		super(version);
	}

	private static User anonymousUser() {
		User a  = new User(0);
		a.alias=Name.ANONYMOUS;
		a.authState=AuthState.unregistered;
		return a;
	}

	/**
	 * User entity is not cloned to but changed in place an explicitly modified
	 * by touching.
	 */
	public void touch(long now) {
		millisLastActive = now;
		if (!isModified())
			modified();
	}

	@Override
	public ID computeID() {
		return ID.userId(alias);
	}

	@Override
	public Name output() {
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
		return authState == AuthState.authenticated && alias.isEditable();
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

	/**
	 * A dubious user has never been confirmed. After the OTP expired another
	 * user can claim this account name and effectively replace the dubious user.
	 */
	public boolean isDubious(long now) {
		return authState == AuthState.registered && now > millisOtpExprires;
	}

}
