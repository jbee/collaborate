package se.jbee.task.model;

import java.util.Map;

import se.jbee.task.model.Criteria.Property;

public final class Paragraph extends Text<Paragraph> implements Bindable {

	public Paragraph(byte[] utf16symbols, int start, int end) {
		super(utf16symbols, start, end);
	}

	@Override
	public Paragraph bindTo(Map<Property, Name> context) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Paragraph subSequence(int start, int end) {
		return new Paragraph(this.utf16symbols, this.start + (start * 2), this.start + (end * 2));
	}
}
