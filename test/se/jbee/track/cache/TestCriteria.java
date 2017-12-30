package se.jbee.track.cache;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static se.jbee.track.cache.Criteria.ValueType.text;
import static se.jbee.track.model.Name.as;

import java.util.EnumMap;

import org.junit.Test;

import se.jbee.track.cache.Criteria.Coloring;
import se.jbee.track.cache.Criteria.Operator;
import se.jbee.track.cache.Criteria.Property;
import se.jbee.track.model.Date;
import se.jbee.track.model.Name;

public class TestCriteria {

	@Test
	public void textValuesCanBeURLs() {
		assertTrue(text.isValid("http://localhost:8080/"));
		assertTrue(text.isValid("https://www.youtube.com/watch?v=uhU_Uw3Yazo"));
		assertTrue(text.isValid("https://mail.google.com/mail/u/0/#inbox"));
		assertTrue(text.isValid("http://asserttrue.blogspot.se/2010/01/how-to-iimplement-custom-paint-in-50.html#"));
	}
	
	@Test
	public void valuesAreTyped() {
		Criteria criteria = Criteria.parse("[color=heat][length=20][user~{foo,bar}]");
		assertEquals(3, criteria.count());
		assertEquals("[user ~ {foo, bar}][color = heat][length = 20]", criteria.toString());
		assertSame(Name.class, criteria.get(0).values[0].getClass());
		assertSame(Coloring.class, criteria.get(1).values[0].getClass());
		assertSame(Integer.class, criteria.get(2).values[0].getClass());
	}
	
	@Test
	public void count() {
		Criteria criteria = Criteria.parse("[url={jira:GRP-001}][url=https://mail.google.com/mail/u/0/#inbox]");
		assertEquals(2, criteria.count());
	}
	
	@Test
	public void valuesCanBeEnumConstants() {
		Criteria criteria = Criteria.parse("[order>>heat]");
		assertEquals(1, criteria.count());
		assertSame(Property.heat, criteria.get(0).values[0]);
	}
	
	@Test
	public void dateYYYYexpandsToRange() {
		Criteria criteria = Criteria.parse("[reported=2016]");
		assertEquals(2, criteria.count());
		assertEquals(Operator.ge, criteria.get(0).op);
		assertEquals(Date.parse("2016-01-01"), criteria.get(0).values[0]);
		assertEquals(Operator.le, criteria.get(1).op);
		assertEquals(Date.parse("2016-12-31"), criteria.get(1).values[0]);
	}
	
	@Test
	public void parsingOrdersListOfCriteriumByItsImportanceForLookup() {
		Criteria criteria = Criteria.parse("[length=5][user~{Tom,Frank}][reporter=Tom][age<10][solver=Max]");
		assertEquals(5, criteria.count());
		assertEquals(Property.solver, criteria.get(0).prop);
		assertEquals(Property.reporter, criteria.get(1).prop);
		assertEquals(Property.user, criteria.get(2).prop);
		assertEquals(Property.age, criteria.get(3).prop);
		assertEquals(Property.length, criteria.get(4).prop);
	}
	
	@Test
	public void parsingReplacesActualProduct() {
		Criteria criteria = Criteria.parse("[product=@]", singletonMap(Property.product, as("foo")));
		assertEquals("[product = foo]", criteria.toString());
	}

	@Test
	public void parsingReplacesActualArea() {
		Criteria criteria = Criteria.parse("[area=@]", singletonMap(Property.area, as("foo")));
		assertEquals("[area = foo]", criteria.toString());
	}

	@Test
	public void parsingReplacesActualUser() {
		EnumMap<Property, Name> context = new EnumMap<>(Property.class);
		context.put(Property.user, as("foo"));
		context.put(Property.reporter, as("bar"));
		Criteria criteria = Criteria.parse("[user=@][reporter=@]", context);
		assertEquals("[reporter = bar][user = foo]", criteria.toString());
	}

}
