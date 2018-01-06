package se.jbee.track.ui.ctrl;

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

	// /user/<alias>/
	// /user/<alias>/<site>/
	// /user/<alias>/<site>/as/<alias>
	// => there are 3 user references
	// 1. you (the user authorized)
	// 2. viewed (the user we look at)
	// 3. viewer (the user substituted for @)
	// 1 substitutes 2 and 3 if missing
	// /user/ => you
	// /user/<alias>/<site> => as you
	// if site is missing the @home page is used#
	
	// /product/<name>/
	// /product/<name>/*/<site>  (area is ORIGIN)
	// /product/<name>/<area>/
	// /product/<name>/<area>/<site>/
	// /product/<name>/<area>/<site>/as/<alias>
	
	// /product/<name>/v/<name>/		(version)
	// /product/<name>/<idn>			(task)
	// /product/<name>/<area>/<serial>	(board task by serial) 		
	
	public static Params fromPath(String path) {
		if (path.startsWith("/"))
			path = path.substring(1);
		Params params = new Params();
		if (!path.isEmpty()) {
			String[] segments = path.split("[/?]");
			switch (segments[0]) {
			case "user":
				params.set(Param.viewed, segments.length >= 2 ? segments[1] : "@");
				if (segments.length >= 3) { params.set(Param.site, segments[2]); }
				break;
			case "product":
				if (segments.length >= 2) { params.set(Param.product, segments[1]); }
				if (segments.length >= 3) { 
					String s2 = segments[2];
					if ("v".equals(s2)) { 
						params.set(Param.version, segments[3]);
					} else if (s2.matches("\\d+")) {
						params.set(Param.task, s2);
					} else { 
						params.set(Param.area, s2);
						if (segments.length >= 4) {
							String s3 = segments[3];
							if (s3.matches("\\d+")) {
								params.set(Param.serial, s3);
							} else {
								params.set(Param.site, s3);
							}
						 }
					} 
				}
			}
			if (segments.length >= 2 && "as".equals(segments[segments.length-2])) {
				params.set(Param.role, segments[segments.length-1]);
			}
		}
		return params;
	}
	
}