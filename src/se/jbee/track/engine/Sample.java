package se.jbee.track.engine;

import static se.jbee.track.engine.Change.Operation.sample;
import static se.jbee.track.model.Email.email;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import se.jbee.track.engine.Change.Tx;
import se.jbee.track.model.Area;
import se.jbee.track.model.Gist;
import se.jbee.track.model.Mail;
import se.jbee.track.model.Name;
import se.jbee.track.model.Names;
import se.jbee.track.model.Output;
import se.jbee.track.model.User;
import se.jbee.track.model.Version;

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
	
	public static Change sample(Names outputs, Names areas, Names users, int tasks, Name actor) {
		return (t, tx) -> { 
			
			User admin = user(t, tx, actor);
			User[] ux = users(t, tx, users);
			Output[] ox = outputs(t, tx, outputs, admin);
			Area[] ax = areas(t, tx, ox, areas, ux);
			Map<Name, Output> omap = new HashMap<>();
			for (Output o : ox)
				omap.put(o.name, o);
			for (int i = 0; i < tasks; i++) {
				Area a = ax[RND.nextInt(ax.length)];
				User reporter = ux[RND.nextInt(ux.length)];
				Output o = omap.get(a.output);
				Version version = null; //FIXME
				if (a.board) {
					t.reportRequest(o, randomGist(), reporter, a);
				} else {
					if (RND.nextInt(100) < 40) {
						t.reportDefect(o, randomGist(), reporter, a, version, RND.nextBoolean());
					} else {
					}
				}
			}
		};
	}

	private static Gist randomGist() {
		return Gist.gist("");
	}

	private static Area[] areas(Tracker t, Tx tx, Output[] outputs, Names areas, User[] users) {
		Area[] ax = new Area[areas.count()];
		int i = 0;
		for (Name area : areas)
			ax[i++] = area(t, tx, outputs, area, users, ax);
		//TODO to have a difference on sub-area maintainers need to join before all areas are done...
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
		User actor = users[RND.nextInt(users.length)];
		Output output = outputs[RND.nextInt(outputs.length)];
		Area basis = basis(output, bases);
		if (bases == null) {
			return t.compart(output, area, actor);
		}
		return t.compart(basis, area, actor, RND.nextBoolean());
	}
	
	private static Area basis(Output output, Area[] bases) {
		if (RND.nextInt(100) < 15)
			return null; // about 15% top level areas
		List<Area> candidates = new ArrayList<>();
		for (Area a : bases)
			if (a != null && a.output.equalTo(output.name) && !a.board)
				candidates.add(a);
		return candidates.isEmpty() ? null : candidates.get(RND.nextInt(candidates.size()));
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
			res = t.register(res, alias, email(alias.toString()+"@example.com"));
			res = t.authenticate(res, res.otp);
			res = t.configure(res, NO_MAILS);
			tx.put(sample, res);
		}
		return res;
	}

	private static Output[] outputs(Tracker t, Tx tx, Names outputs, User actor) {
		Output[] ox = new Output[outputs.count()];
		int i = 0;
		for (Name output : outputs)
			ox[i++] = output(t, tx, output, actor);
		return ox;
	}

	private static Output output(Tracker t, Tx tx, Name output, User actor) {
		Output res = tx.outputOrNull(output);
		if (res == null) {
			res = t.envision(output, actor); 
			tx.put(sample, res);
		}
		return res;
	}

}
