package se.jbee.track.engine;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static se.jbee.track.engine.Server.Switch.LOCKDOWN;
import static se.jbee.track.engine.Server.Switch.OPEN;
import static se.jbee.track.engine.Server.Switch.PRIVATE;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;

import se.jbee.track.model.Date;
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

	/**
	 * Creates a {@link Server} configuration from the command line arguments as
	 * passed to the main method of the application.
	 *
	 * Usage:
	 * <pre>
	 * java -jar collaborate.jar [OPTION...]
	 *
	 * -f FILE  database path
	 * -s SIZE  database size in MB (10-100)
	 * -a EMAIL the EMAIL address of the user that has admin rights
	 * -l LIMIT activity limit base (default 5)
	 * -O       open: allow users to create outputs
	 * -L       lock-down: only the admin user may log in
	 * -P       private: allow user to see admin's email
	 * </pre>
	 *
	 * @param args
	 *            the command line args (OPTIONs) as described above
	 * @return a {@link Server} configuration
	 * @throws IOException
	 *             in case DB path is not accessible
	 */
	public static Server parse(String...args) throws IOException {
		String path = System.getProperty("java.io.tmpdir") + "/collaborate-"+Date.today()+"/";
		Email admin = Email.NO_ADMIN;
		int sizeMB = 10;

		if (args.length >= 1) {
			path = args[0];
		}
		if (args.length >= 2) {
			String a1 = args[1];
			if (!a1.startsWith("-")) {
				try {
					sizeMB = min(100, max(10, Integer.parseInt(a1)));
				} catch (NumberFormatException e) {
				}
			} else {

			}
		}
		File file = new File(path);
		if (!file.exists() && !file.mkdirs()) {
			throw new IOException("Unable to create DB folder.");
		}
		return new Server(admin, file, 1014L * 1024L * sizeMB, () -> System.currentTimeMillis(), new LinearLimits(5), EnumSet.noneOf(Switch.class));
	}

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
	/**
	 * Path to the DB file
	 */
	public final File pathDB;

	/**
	 * Maximum size of DB in bytes.
	 */
	public final long sizeDB;

	public final Clock clock;
	public final Limits limits;
	private final EnumSet<Switch> switches;

	public Server() {
		this(Email.NO_ADMIN, null, 0, () -> System.currentTimeMillis(), new LinearLimits(5), EnumSet.noneOf(Switch.class));
	}

	private Server(Email admin, File pathDB, long sizeDB, Clock clock, Limits limits, EnumSet<Switch> switches) {
		super();
		this.admin = admin;
		this.pathDB = pathDB;
		this.sizeDB = sizeDB;
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

	public Server with(Clock clock) {
		return new Server(admin, pathDB, sizeDB, clock, limits, switches);
	}

	public Server with(Limits limits) {
		return new Server(admin, pathDB, sizeDB, clock, limits, switches);
	}

	public Server with(Email admin) {
		return new Server(admin, pathDB, sizeDB, clock, limits, switches);
	}

	public Server with(Switch...switches) {
		return new Server(admin, pathDB, sizeDB, clock, limits,
				switches.length == 0
				? EnumSet.noneOf(Switch.class)
				: EnumSet.of(switches[0], switches));
	}

	public Server with(Switch s) {
		if (switches.contains(s))
			return this;
		EnumSet<Switch> switches = this.switches.clone();
		switches.add(s);
		return new Server(admin, pathDB, sizeDB, clock, limits, switches);
	}

	public Email admin() {
		return switches.contains(PRIVATE) ? admin : null;
	}

}
