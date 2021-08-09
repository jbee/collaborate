package se.jbee.task.model;

import java.util.Map;

import se.jbee.task.model.Criteria.Property;

public interface Bindable {

	Bindable bindTo(Map<Property, Name> context);

}