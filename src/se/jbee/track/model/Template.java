package se.jbee.track.model;

import static java.nio.charset.StandardCharsets.UTF_16BE;

import java.util.ArrayList;
import java.util.List;

public final class Template extends Bytes implements Comparable<Template> {

	public static final Template BLANK_PAGE = new Template(new byte[0]);
	
	public static Template template(String template) {
		if (template.isEmpty())
			return BLANK_PAGE;
		if (!isText(template)) {
			throw new IllegalArgumentException("Template contains illegal characters.");
		}
		return new Template(template.getBytes(UTF_16BE));
	}
	
	public static Template fromBytes(byte[] template) {
		return new Template(template);
	}
	
	private final byte[] template;
	
	private transient volatile Object[] parsed;
	
	private Template(byte[] template) {
		super();
		this.template = template;
	}
	
	public boolean isEmpty() {
		return template.length == 0;
	}

	@Override
	public byte[] bytes() {
		return template;
	}

	@Override
	public int compareTo(Template other) {
		return this == other ? 0 : compare(template, other.template);
	}
	
	@Override
	public String toString() {
		return new String(template, UTF_16BE);
	}

	public Object[] elements() {
		if (parsed != null)
			return parsed;
		parsed = parseTemplate();
		return parsed;
	}

	private synchronized Object[] parseTemplate() {
		if (parsed != null)
			return parsed;
		String e = "";
		boolean wasQuery = false;
		List<Object> parts = new ArrayList<>();
		String[] lines = toString().split("\n");
		for (String line : lines) {
			boolean isQuery = line.startsWith("[");
			if (isQuery == wasQuery) {
				if (isQuery) {
					e += line;
				} else {
					if (line.matches("^\\s*$")) {
						if (!e.endsWith("\n"))
							e+="\n";
						parts.add(e);
						e = "";
					} else if (line.matches("^\\s*[-]{3,}\\s*$")) {
						if (!e.endsWith("\n"))
							e+="\n";
						parts.add(e);
						parts.add(line);
						e = "";
					} else {
						e += line+ "\n";
					}
				}
			} else {
				parts.add(wasQuery ? Criteria.parse(e) : e);
				e = line;
			}
			wasQuery=isQuery;
		}
		if (!e.isEmpty()) {
			parts.add(wasQuery ? Criteria.parse(e) : e);
		}
		return parts.toArray();
	}
	
	//TODO less formal. A template can contain any text. We do a simple markup there. If a line starts with [ it is considered part of a query
	
	/* so one can write
	 * 
	 * *** My Title ***
	 * 
	 * Some text here.
	 * In multiple lines.
	 * 
	 * My oldies
	 * [user=@]
	 * [age>20]
	 * 
	 * Hot just now
	 * [output=@][temperature > 90]
	 * 
	 * *** Next Silo ***
	 * ...
	 */
	
	// also there could be simple markups for a 
	// * search form => ???
	// * current users polls => !!!
	// * 
}
