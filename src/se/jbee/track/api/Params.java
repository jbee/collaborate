package se.jbee.track.api;

import static se.jbee.track.api.Param.command;
import static se.jbee.track.api.Param.serial;
import static se.jbee.track.api.Param.task;
import static se.jbee.track.api.Param.version;
import static se.jbee.track.api.Param.viewed;

import java.util.EnumMap;

import se.jbee.track.api.Param.Command;

/**
 * A set of {@link Param} key-value pairs. 
 */
public final class Params extends EnumMap<Param, String> {

	public Params() { super(Param.class); }
	
	public Params set(Param p, String val) {
		put(p, val);
		return this;
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
	 *  /output/{name}/
	 *  /output/{name}/* /{page}
	 *  /output/{name}/{area}/
	 *  /output/{name}/{area}/{page}/
	 *  /output/{name}/{area}/{page}/as/{alias}
	 *  /output/{name}/v/{version}/
	 *  /output/{name}/{idn}
	 *  /output/{name}/{area}/{serial}
	 * </pre>
	 */
	public static Params fromPath(String path) {
		if (path.startsWith("/"))
			path = path.substring(1);
		Params params = new Params();
		if (!path.isEmpty()) {
			String[] segments = path.split("[/?]");
			switch (segments[0]) {
			case "user":
				params.set(command, Command.list.name());
				params.set(viewed, segments.length >= 2 ? segments[1] : "@");
				if (segments.length >= 3) { params.set(Param.page, segments[2]); }
				break;
			case "output": //TODO remove output from path - make sure "user" is no logal output name
				if (segments.length >= 2) { params.set(Param.output, segments[1]); }
				if (segments.length >= 3) { 
					String s2 = segments[2];
					if ("v".equals(s2)) { 
						params.set(version, segments[3]);
						params.set(command, Command.version.name());
					} else if (s2.matches("\\d+")) {
						params.set(task, s2);
						params.set(command, Command.details.name());
					} else { 
						params.set(Param.area, s2);
						if (segments.length >= 4) {
							String s3 = segments[3];
							if (s3.matches("\\d+")) {
								params.set(serial, s3);
								params.set(command, Command.details.name());
							} else {
								params.set(command, Command.list.name());
								params.set(Param.page, s3);
							}
						} else {
							if (s2.matches("^.+-\\d+$")) {
								params.set(command, Command.details.name());
								params.set(Param.area, s2.substring(0, s2.lastIndexOf('-')));
								params.set(Param.serial, s2.substring(s2.lastIndexOf('-')+1));
							} else {
								params.set(command, Command.list.name());
							}
						}
					} 
				} else {
					params.set(Param.command, Command.list.name());
				}
			}
			if (segments.length >= 2 && "as".equals(segments[segments.length-2])) {
				params.set(Param.role, segments[segments.length-1]);
			}
		}
		return params;
	}
	
}