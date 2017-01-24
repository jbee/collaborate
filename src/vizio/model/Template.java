package vizio.model;

import java.nio.charset.StandardCharsets;

public final class Template extends Bytes implements Comparable<Template> {

	public static final Template BLANK_PAGE = new Template(new byte[0]);
	
	public static Template template(String template) {
		if (!FULL_TEXT_ONLY.matcher(template).matches()) {
			throw new IllegalArgumentException("Template contains illegal characters.");
		}
		return new Template(template.getBytes(StandardCharsets.UTF_16));
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
		return new String(template, StandardCharsets.UTF_16);
	}

}
