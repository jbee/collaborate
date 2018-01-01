package se.jbee.track.model;

import static java.nio.charset.StandardCharsets.UTF_16BE;

public final class Template extends Bytes implements Comparable<Template> {

	public static final Template BLANK_PAGE = new Template(new byte[0]);
	
	public static Template template(String template) {
		if (!isText(template)) {
			throw new IllegalArgumentException("Template contains illegal characters.");
		}
		return new Template(template.getBytes(UTF_16BE));
	}
	
	public static Template fromBytes(byte[] template) {
		return new Template(template);
	}
	
	private final byte[] template;
	
	private Template(byte[] template) {
		super();
		this.template = template;
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

}
