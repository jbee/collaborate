package se.jbee.track.engine;

import static se.jbee.track.engine.Server.Switch.LOCKDOWN;
import static se.jbee.track.engine.Server.Switch.OPEN;

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
		LOCKDOWN 
		
	}
	
	/**
	 * This is the user that is considered as an administrator. Only the
	 * administrator may create new {@link Output}s on a server that is not
	 * {@link Switch#OPEN}
	 */
	public final Email admin;

	private final EnumSet<Switch> switches;
	
	public final Clock clock;
	
	public final Limits limits;
	
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
	
}
