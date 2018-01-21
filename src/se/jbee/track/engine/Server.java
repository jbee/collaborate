package se.jbee.track.engine;

import static java.lang.Integer.parseInt;
import static java.lang.Short.parseShort;
import static se.jbee.track.engine.Server.Switch.DEDICATED;
import static se.jbee.track.engine.Server.Switch.LOCKDOWN;
import static se.jbee.track.engine.Server.Switch.OPEN;

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
	 * -b LIMIT activity limit base (default 5)
	 * -o       open: allow users to create outputs
	 * -l       lock-down: only the admin user may log in
	 * -d       dedicated: allow user to see admin's email
	 * </pre>
	 *
	 * @param args
	 *            the command line args (OPTIONs) as described above
	 * @return a {@link Server} configuration
	 * @throws IOException
	 *             in case DB path is not accessible
	 */
	public static Server parse(String...args) throws IOException {
		Server res = new Server();
		int i = 0;
		while (i  < args.length) {
			String option = args[i++];
			if (!option.startsWith("-") || option.length() <= 1)
				throw new IllegalArgumentException("Expected option; Unknown option: "+args[i-1]);
			switch (option.charAt(1)) {
			case 'f': res = res.with(new File(args[i++])); break;
			case 's': res = res.with(parseShort(args[i++])); break;
			case 'a': res = res.with(Email.email(args[i++])); break;
			case 'b': res = res.with(new LinearLimits(parseInt(args[i++]))); break;
			case 'p': res = res.with(parseInt(args[i++])); break;
			case 'o': res = res.with(Switch.OPEN); break;
			case 'l': res = res.with(Switch.LOCKDOWN); break;
			case 'd': res = res.with(Switch.DEDICATED); break;
			case 'h': System.out.println("Usage: java -jar collaborate.jar [OPTION...]"); System.exit(0); break;
			default:
				throw new IllegalArgumentException("Unknown option: "+args[i-1]);
			}
		}
		return res;
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
		DEDICATED,

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
	public final boolean isTemporary;

	public final Clock clock;
	public final Limits limits;
	private final EnumSet<Switch> switches;
	public final int port;

	public Server() {
		this(Email.NO_ADMIN,
				new File(System.getProperty("java.io.tmpdir") + "/collaborate-"+Date.today()+"/"), 1014L * 1024L * 10L, 8080,
				() -> System.currentTimeMillis(), new LinearLimits(5), EnumSet.noneOf(Switch.class));
	}

	private Server(Email admin, File pathDB, long sizeDB, int port, Clock clock, Limits limits, EnumSet<Switch> switches) {
		super();
		this.admin = admin;
		this.pathDB = pathDB;
		this.sizeDB = sizeDB;
		this.port = port;
		this.clock = clock;
		this.limits = limits;
		this.switches = switches;
		this.isTemporary = pathDB.getPath().startsWith(System.getProperty("java.io.tmpdir"));
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
		return new Server(admin, pathDB, sizeDB, port, clock, limits, switches);
	}

	public Server with(Limits limits) {
		return new Server(admin, pathDB, sizeDB, port, clock, limits, switches);
	}

	public Server with(Email admin) {
		return new Server(admin, pathDB, sizeDB, port, clock, limits, switches);
	}

	public Server with(Switch...switches) {
		return new Server(admin, pathDB, sizeDB, port, clock, limits,
				switches.length == 0
				? EnumSet.noneOf(Switch.class)
				: EnumSet.of(switches[0], switches));
	}

	public Server with(File pathDB) throws IOException {
		if (!pathDB.exists() && !pathDB.mkdirs()) {
			throw new IOException("Database folder does not exist and cannot be created: "+pathDB);
		}
		if (!pathDB.isDirectory()) {
			throw new IllegalArgumentException("Please provide the folder the database is located, not a file like: "+pathDB);
		}
		return new Server(admin, pathDB, sizeDB, port, clock, limits, switches);
	}

	public Server with(short sizeDB) {
		return new Server(admin, pathDB, 1014L * 1024L * sizeDB, port, clock, limits, switches);
	}

	public Server with(int port) {
		return new Server(admin, pathDB, sizeDB, port, clock, limits, switches);
	}

	public Server with(Switch s) {
		if (switches.contains(s))
			return this;
		EnumSet<Switch> switches = this.switches.clone();
		switches.add(s);
		return new Server(admin, pathDB, sizeDB, port, clock, limits, switches);
	}

	public Email admin() {
		return switches.contains(DEDICATED) ? admin : null;
	}

}
