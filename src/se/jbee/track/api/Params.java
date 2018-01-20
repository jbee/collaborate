package se.jbee.track.api;

import static se.jbee.track.api.Param.command;
import static se.jbee.track.api.Param.serial;
import static se.jbee.track.api.Param.task;
import static se.jbee.track.api.Param.version;
import static se.jbee.track.api.Param.viewed;

import java.util.EnumMap;

import se.jbee.track.api.Param.Command;
import se.jbee.track.model.Name;
import se.jbee.track.model.Names;

/**
 * A set of {@link Param} key-value pairs.
 */
public final class Params extends EnumMap<Param, String> {

	public Params() { super(Param.class); }

	public Params set(Param p, String val) {
		put(p, val);
		return this;
	}

	public Params set(Param p, Enum<?> val) {
		return set(p, val.name());
	}

	public <E extends Enum<E>> E value(Param p, E def) {
		String v = get(p);
		return v == null ? def : Enum.valueOf(def.getDeclaringClass(), v);
	}

	public Names names(Param p) {
		Names res = Names.empty();
		String v = getOrDefault(p, "");
		if (!v.isEmpty())
			for (String n : v.split("\\s*,\\s*|\\s+"))
				res = res.add(Name.as(n));
		return res;
	}

	// URLs

	/**
	 * Dissects a path into a set of parameters.
	 *
	 * Paths examples:
	 * <pre>
	 *  /user/{alias}/
	 *  /user/{alias}/{page}/
	 *  /user/{alias}/{page}/as/{alias}
	 *  /{output}/
	 *  /{output}/* /{page}
	 *  /{output}/{area}/
	 *  /{output}/{area}/{page}/
	 *  /{output}/{area}/{page}/as/{alias}
	 *  /{output}/v/{version}/
	 *  /{output}/{idn}
	 *  /{output}/{area}/{serial}
	 * </pre>
	 * All POST/PUT URLs use <code>/do/</code> as first segement.
	 */
	public static Params fromPath(String path) {
		if (path.startsWith("/"))
			path = path.substring(1);
		Params params = new Params();
		if (!path.isEmpty()) {
			String[] segments = path.split("[/?]");
			String s0 = segments[0];
			if ("user".equals(s0) || "*".equals(s0)) {
				params.set(command, Command.query);
				params.set(viewed, segments.length >= 2 ? segments[1] : "@");
				if (segments.length >= 3) { params.set(Param.page, segments[2]); }
			} else if ("do".equals(s0)) {
				//TODO just for now
				params.set(command, Command.sample)
				.set(Param.actor, "peter")
				.set(Param.role, "peter")
				.set(Param.output, "c11, java, python")
				.set(Param.area, "foo, bar, baz")
				.set(Param.version, "0.1, 0.2")
				.set(Param.category, "example")
				.set(Param.task, "10");
			} else {
				params.set(Param.output, s0);
				if (segments.length >= 2) {
					String s1 = segments[1];
					if ("v".equals(s1)) {
						params.set(version, segments[2]);
						params.set(command, Command.oversee);
					} else if (s1.matches("\\d+")) {
						params.set(task, s1);
						params.set(command, Command.examine);
					} else {
						params.set(Param.area, s1);
						if (segments.length >= 3) {
							String s2 = segments[2];
							if (s2.matches("\\d+")) {
								params.set(serial, s2);
								params.set(command, Command.examine);
							} else {
								params.set(command, Command.query);
								params.set(Param.page, s2);
							}
						} else {
							if (s1.matches("^.+-\\d+$")) {
								params.set(command, Command.examine);
								params.set(Param.area, s1.substring(0, s1.lastIndexOf('-')));
								params.set(Param.serial, s1.substring(s1.lastIndexOf('-')+1));
							} else {
								params.set(command, Command.query);
							}
						}
					}
				} else {
					params.set(Param.command, Command.query);
				}
			}
			if (segments.length >= 2 && "as".equals(segments[segments.length-2])) {
				params.set(Param.role, segments[segments.length-1]);
			}
		} else {
			//TODO product overview page
		}
		return params;
	}

}