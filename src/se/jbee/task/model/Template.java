package se.jbee.task.model;

import static java.nio.charset.StandardCharsets.UTF_16BE;
import static java.util.Arrays.asList;
import static se.jbee.task.util.Array.refine;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import se.jbee.task.model.Criteria.Property;

public final class Template extends Text<Template> implements Iterable<Bindable>, Bindable {

	private static final String TEMPLATE_CHAR_GROUP = "-+*a-zA-Z0-9@_\\s\\\\\\$\\^:,;.?!#>=%&`\\\"'~\\pL\\pN\\p{Sc}\\(\\)\\[\\]\\{\\}";
	private static final String TEMPLATE_TEXT_REGEX = "(?:["+TEMPLATE_CHAR_GROUP+"]+|<[^a-zA-Z/]|/[^>])+[</]?";
	private static final Pattern TEMPLATE_TEXT = Pattern.compile("^"+TEMPLATE_TEXT_REGEX+"$");
	private static final Pattern TEMPLATE_ILLEGAL = Pattern.compile("[^"+TEMPLATE_CHAR_GROUP+"]");

	public static boolean isTemplateText(String s) {
		return TEMPLATE_TEXT.matcher(s).matches();
	}

	private static final Bindable[] NO_ITEMS = new Bindable[0];

	public static final Template BLANK_PAGE = new Template(new byte[0], NO_ITEMS);

	public static Template parseTemplate(String template) {
		if (template.isEmpty())
			return BLANK_PAGE;
		checkText(template);
		byte[] bytes = template.getBytes(UTF_16BE);
		if (bytes.length > 8000)
			throw new IllegalArgumentException("Text is too long.");
		return new Template(bytes, split(bytes));
	}

	static void checkText(String template) {
		if (!isTemplateText(template)) {
			Matcher m = TEMPLATE_ILLEGAL.matcher(template);
			m.find();
			throw new IllegalArgumentException("Template contains illegal characters: "+m.group());
		}
	}

	public static Template fromBytes(byte[] template) {
		return new Template(template, split(template));
	}

	private final Bindable[] items;

	private Template(byte[] template, Bindable[] items) {
		super(template, 0, template.length);
		this.items = items;
	}

	public int items() {
		return items.length;
	}

	public Bindable item(int index) {
		return items[index];
	}

	@Override
	public Iterator<Bindable> iterator() {
		return asList(items).iterator();
	}

	@Override
	public Template bindTo(Map<Property, Name> context) {
		return new Template(utf16symbols, refine(items, i -> i.bindTo(context)));
	}

	private static Bindable[] split(byte[] template) {
		int len = template.length;
		if (len == 0) {
			return NO_ITEMS;
		}
		List<Bindable> parts = new ArrayList<>();
		int sl = 0; // start of next line
		int sb = 0; // start if current block
		while (sl < len) {
			while (sl+2 < len && template[sl] == 0 && template[sl+1] == '[') {
				while (sl+2 < len && template[sl] == 0 && template[sl+1] != '\n') sl+=2; // skip to next line
				sl+=2;
			}
			if (sl > sb) {
				parts.add(Criteria.parse(new Paragraph(template, sb, sl)));
				sb = sl;
			}
			while (sl+2 < len && template[sl] == 0 && template[sl+1] != '[') {
				int sl0 = sl;
				while (sl+2 < len && template[sl] == 0 && template[sl+1] != '\n') sl+=2; // skip to next line
				sl+=2;
				if (isBlank(template, sl0, sl)) {
					//TODO
				}
				//TODO make blank lines also break pars
			}
			if (sl > sb) {
				parts.add(new Paragraph(template, sb, sl));
				sb = sl;
			}
		}
		return parts.toArray(NO_ITEMS);
	}

	private static boolean isBlank(byte[] template, int start, int end) {
		// TODO Auto-generated method stub
		return false;
	}

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
