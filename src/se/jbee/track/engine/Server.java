package se.jbee.track.engine;

import static se.jbee.track.engine.Server.Switch.LOCKDOWN;
import static se.jbee.track.engine.Server.Switch.OPEN;
import static se.jbee.track.engine.Server.Switch.PRIVATE;

import java.util.EnumSet;

import se.jbee.track.model.Email;
import se.jbee.track.model.Output;
import se.jbee.track.model.User;

/**
 * The {@link Server} describes general configuration of the system. It is
 * derived once at startup from command line arguments (or property settings or
 * the like). It cannot be changed during run. To change it the server has to be
 * restarted. This data is non persistent. It is not stored anywhere.
 *
 * @author jan
 */
public final class Server {

	public static enum Switch {
		/**
		 * If set any register {@link User} may create {@link Output}s otherwise
		 * only the {@link Server#admin} can.
		 */
		OPEN,

		/**
		 * If set no user can log in except the {@link Server#admin}. Also all
		 * limits are disabled.
		 *
		 * This is used to correct installations after major problems or before
		 * first use.
		 */
		LOCKDOWN,

		/**
		 * If set user may see/get the admins email so that they can contact the
		 * admin in case it is needed.
		 */
		PRIVATE,

	}

	/**
	 * This is the user that is considered as an administrator. Only the
	 * administrator may create new {@link Output}s on a server that is not
	 * {@link Switch#OPEN}
	 */
	public final Email admin;
	public final Clock clock;
	public final Limits limits;

	private final EnumSet<Switch> switches;

	//TODO add mail settings, maybe as general key value settings?

	public Server(Email admin, Clock clock, Limits limits, Switch...switches) {
		this(admin, clock, limits, switches.length == 0 ? EnumSet.noneOf(Switch.class) : EnumSet.of(switches[0], switches));
	}

	public Server(Email admin, Clock clock, Limits limits, EnumSet<Switch> switches) {
		super();
		this.admin = admin;
		this.clock = clock;
		this.limits = limits;
		this.switches = switches;
	}

	public boolean isOpen() {
		return !isOnLockdown() && switches.contains(OPEN);
	}

	public boolean isOnLockdown() {
		return switches.contains(LOCKDOWN);
	}

	public boolean isAdmin(User user) {
		return user.email.equalTo(admin);
	}

	public Server with(Clock clock, Limits limits) {
		return new Server(admin, clock, limits, switches);
	}

	public Server with(Switch s) {
		if (switches.contains(s))
			return this;
		EnumSet<Switch> switches = this.switches.clone();
		switches.add(s);
		return new Server(admin, clock, limits, switches);
	}

	public Email admin() {
		return switches.contains(PRIVATE) ? admin : null;
	}

}
