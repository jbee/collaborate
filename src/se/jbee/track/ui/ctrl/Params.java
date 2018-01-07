package se.jbee.track.ui.ctrl;

import static se.jbee.track.ui.ctrl.Param.command;
import static se.jbee.track.ui.ctrl.Param.serial;
import static se.jbee.track.ui.ctrl.Param.task;
import static se.jbee.track.ui.ctrl.Param.version;
import static se.jbee.track.ui.ctrl.Param.viewed;

import java.util.EnumMap;

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
	 *  /user/{alias}/{site}/
	 *  /user/{alias}/{site}/as/{alias}
	 *  /product/{name}/
	 *  /product/{name}/* /{site}
	 *  /product/{name}/{area}/
	 *  /product/{name}/{area}/{site}/
	 *  /product/{name}/{area}/{site}/as/{alias}
	 *  /product/{name}/v/{version}/
	 *  /product/{name}/{idn}
	 *  /product/{name}/{area}/{serial}
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
				params.set(command, Action.list.name());
				params.set(viewed, segments.length >= 2 ? segments[1] : "@");
				if (segments.length >= 3) { params.set(Param.site, segments[2]); }
				break;
			case "product":
				if (segments.length >= 2) { params.set(Param.product, segments[1]); }
				if (segments.length >= 3) { 
					String s2 = segments[2];
					if ("v".equals(s2)) { 
						params.set(version, segments[3]);
						params.set(command, Action.version.name());
					} else if (s2.matches("\\d+")) {
						params.set(task, s2);
						params.set(command, Action.details.name());
					} else { 
						params.set(Param.area, s2);
						if (segments.length >= 4) {
							String s3 = segments[3];
							if (s3.matches("\\d+")) {
								params.set(serial, s3);
								params.set(command, Action.details.name());
							} else {
								params.set(command, Action.list.name());
								params.set(Param.site, s3);
							}
						 } else {
								params.set(command, Action.list.name());
						 }
					} 
				} else {
					params.set(Param.command, Action.list.name());
				}
			}
			if (segments.length >= 2 && "as".equals(segments[segments.length-2])) {
				params.set(Param.role, segments[segments.length-1]);
			}
		}
		return params;
	}
	
}