package se.jbee.track.model;

import static java.nio.charset.StandardCharsets.UTF_16BE;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class Template implements ByteSequence<Template> {

	private static final String TEMPLATE_TEXT_REGEX = "(?:[-+*a-zA-Z0-9@_\\s\\\\\\$\\^:,;.?!#>=%&`\"'~\\pL\\pN\\(\\)\\[\\]\\{\\}]+|<[^a-zA-Z/]|/[^>])+[</]?";
	private static final Pattern TEMPLATE_TEXT = Pattern.compile("^"+TEMPLATE_TEXT_REGEX+"$");

	public static boolean isTemplateText(String s) {
		return TEMPLATE_TEXT.matcher(s).matches();
	}

	public static final Template BLANK_PAGE = new Template(new byte[0]);

	public static Template template(String template) {
		if (template.isEmpty())
			return BLANK_PAGE;
		if (!isTemplateText(template))
			throw new IllegalArgumentException("Template contains illegal characters.");
		byte[] bytes = template.getBytes(UTF_16BE);
		if (bytes.length > 8000)
			throw new IllegalArgumentException("Text is too long.");
		return new Template(bytes);
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

	@Override
	public byte[] readonlyBytes() {
		return template;
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
