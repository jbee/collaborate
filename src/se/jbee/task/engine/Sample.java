package se.jbee.task.engine;

import static se.jbee.task.model.Email.email;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import se.jbee.task.engine.Change.Tx;
import se.jbee.task.model.Area;
import se.jbee.task.model.Cause;
import se.jbee.task.model.Gist;
import se.jbee.task.model.Mail;
import se.jbee.task.model.Name;
import se.jbee.task.model.Names;
import se.jbee.task.model.Output;
import se.jbee.task.model.User;
import se.jbee.task.model.Version;

/**
 * A {@link Change} operation that creates a bunch of test data.
 *
 * This is no primary operation of the system but shipped with any new system
 * for demonstrating purposes.
 *
 * For this reasons it is not added to the {@link Tracker} but separated in this
 * file.
 *
 * @author jan
 */
public class Sample {

	private static final Random RND = new Random();

	private static final EnumMap<Mail.Notification, Mail.Delivery> NO_MAILS = new EnumMap<>(Mail.Notification.class);

	public static Change sample(Names users, Names outputs, Names versions, Names areas, Names categories, int tasks, Name actor) {
		return (t, tx) -> {
			User admin = user(t, tx, actor);
			User[] ux = users(t, tx, users);
			Output[] ox = outputs(t, tx, outputs, categories, admin);
			Version[] vx = versions(t, tx, ox, versions, ux);
			Area[] ax = areas(t, tx, ox, areas, ux);
			Map<Name, Output> omap = new HashMap<>();
			for (Output o : ox)
				omap.put(o.name, o);
			Map<Name, List<Version>> versionByOutput = new HashMap<>();
			for (Version v : vx)
				versionByOutput.computeIfAbsent(v.output, (name) -> new ArrayList<>()).add(v);
			for (int i = 0; i < tasks; i++) {
				Area a = pick1From(ax);
				User reporter = pick1From(ux);
				Output o = omap.get(a.output);
				List<Version> candidates = versionByOutput.get(a.output);
				if (candidates == null) { // we just register a new version for that product as well
					candidates = Collections.singletonList(version(t, tx, new Output[] {o}, pick1From(versions), ux));
					versionByOutput.put(a.output, candidates);
				}
				Version version = pick1From(candidates);
				if (a.board) {
					Change.request(o.name, randomGist(a.cause), reporter.alias, a.name).apply(t, tx);
				} else {
					if (RND.nextInt(100) < 40) {
						Change.warn(o.name, randomGist(Cause.finding), reporter.alias, a.name, version.name, RND.nextBoolean()).apply(t, tx);
					} else {
						Change.propose(o.name, randomGist(Cause.idea), reporter.alias, a.name).apply(t, tx);
					}
				}
			}
		};
	}

	private static Version[] versions(Tracker t, Tx tx, Output[] outputs, Names versions, User[] users) {
		Version[] vx = new Version[versions.count()];
		int i = 0;
		for (Name version : versions)
			vx[i++] = version(t, tx, outputs, version, users);
		return vx;
	}

	private static Version version(Tracker t, Tx tx, Output[] outputs, Name version, User[] users) {
		for (Output o : outputs) {
			Version res = tx.versionOrNull(o.name, version);
			if (res != null)
				return res;
		}
		// does not exist
		User actor = pick1From(users);
		Output output = pick1From(outputs);
		Change.tag(output.name, version, actor.alias).apply(t, tx);
		return tx.version(output.name, version);
	}

	private static final String[] SUBJECT = {"Mail", "User", "Product", "Article", "Account"};
	private static final String[] SUBJECT_TECH = {"Factory", "Manager", "Proxy", "Impl"};
	private static final String[] DEFECT = {" is not responding.", " throws ", " causes ", " fails because of a "};
	private static final String[] ERROR = {"Error", "Exception"};
	private static final String[] VERB = {" should be ", " should not be ", " could be "};
	private static final String[] ENDING = {" available.", " moved.", "moved back.", "green.", "red."};

	private static Gist randomGist(Cause cause) {
		String gist = pick1From(SUBJECT);
		int suf = RND.nextInt(100);
		while (suf < 30 && suf > 0) {
			gist += pick1From(SUBJECT_TECH);
			suf = RND.nextInt(suf);
		}
		if (cause == Cause.finding) {
			gist += pick1From(DEFECT);
			if (!gist.endsWith(".")) {
				gist += pick1From(SUBJECT)+pick1From(ERROR);
			}
		} else {
			gist += pick1From(VERB)+pick1From(ENDING);
		}
		return Gist.gist(gist);
	}

	private static <T> T pick1From(T[] options) {
		return options[RND.nextInt(options.length)];
	}

	private static Name pick1From(Names options) {
		return options.at(RND.nextInt(options.count()));
	}

	private static <T> T pick1From(List<T> options) {
		return options.isEmpty() ? null : options.get(RND.nextInt(options.size()));
	}

	private static Area[] areas(Tracker t, Tx tx, Output[] outputs, Names areas, User[] users) {
		Area[] ax = new Area[areas.count()];
		int i = 0;
		for (Name area : areas)
			ax[i++] = area(t, tx, outputs, area, users, ax);
		return ax;
	}

	private static Area area(Tracker t, Tx tx, Output[] outputs, Name area, User[] users, Area[] bases) {
		// try all outputs involved...
		for (Output o : outputs) {
			Area res = tx.areaOrNull(o.name, area);
			if (res != null)
				return res;
		}
		// does not exist yet...
		User actor = pick1From(users);
		Output output = pick1From(outputs);
		Area basis = basis(output, bases);
		if (basis == null) {
			Change.compart(output.name, area, actor.alias).apply(t, tx);
		} else {
			Change.compart(output.name, basis.name, area, actor.alias, RND.nextBoolean()).apply(t, tx);
		}
		if (!output.categories.isEmpty()) {
			Change.categorise(output.name, area, pick1From(output.categories), actor.alias).apply(t, tx);
		}
		//TODO to have a difference on sub-area maintainers need to join before all areas are done...
		return tx.area(output.name, area);
	}

	private static Area basis(Output output, Area[] bases) {
		if (RND.nextInt(100) < 15)
			return null; // about 15% top level areas
		List<Area> candidates = new ArrayList<>();
		for (Area a : bases)
			if (a != null && a.output.equalTo(output.name) && !a.board)
				candidates.add(a);
		return pick1From(candidates);
	}

	private static User[] users(Tracker t, Tx tx, Names users) {
		User[] ux = new User[users.count()];
		int i = 0;
		for (Name user : users)
			ux[i++] = user(t, tx, user);
		return ux;
	}

	private static User user(Tracker t, Tx tx, Name alias) {
		User res = tx.userOrNull(alias);
		if (res == null) {
			Change.register(alias, email(alias.toString()+"@example.com")).apply(t, tx);
			Change.authenticate(alias, tx.user(alias).otp).apply(t, tx);
			Change.configure(alias, NO_MAILS).apply(t, tx);
		}
		return tx.user(alias);
	}

	private static Output[] outputs(Tracker t, Tx tx, Names outputs, Names categories, User actor) {
		Output[] ox = new Output[outputs.count()];
		int i = 0;
		for (Name output : outputs)
			ox[i++] = output(t, tx, output, categories, actor);
		return ox;
	}

	private static Output output(Tracker t, Tx tx, Name output, Names categories, User actor) {
		Output res = tx.outputOrNull(output);
		if (res == null) {
			Change.envision(output, actor.alias).apply(t, tx);
			for (Name c : categories)
				Change.suggest(output, c, actor.alias).apply(t, tx);
		}
		return tx.output(output);
	}

}
