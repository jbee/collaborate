package se.jbee.task.model;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static se.jbee.task.model.Criteria.ValueType.text;
import static se.jbee.task.model.Name.as;

import java.util.EnumMap;

import org.junit.Test;

import se.jbee.task.model.Criteria;
import se.jbee.task.model.Date;
import se.jbee.task.model.Name;
import se.jbee.task.model.Criteria.Coloration;
import se.jbee.task.model.Criteria.Operator;
import se.jbee.task.model.Criteria.Property;

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
		Criteria criteria = Criteria.parse("[coloration=heat][length=20][user~{foo,bar}]");
		assertEquals(3, criteria.count());
		assertEquals("[user ~ {foo, bar}][coloration = heat][length = 20]", criteria.toString());
		assertSame(Name.class, criteria.get(0).rvalues[0].getClass());
		assertSame(Coloration.class, criteria.get(1).rvalues[0].getClass());
		assertSame(Integer.class, criteria.get(2).rvalues[0].getClass());
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
		assertSame(Property.heat, criteria.get(0).rvalues[0]);
	}

	@Test
	public void dateYYYYexpandsToRange() {
		Criteria criteria = Criteria.parse("[reported=2016]");
		assertEquals(2, criteria.count());
		assertEquals(Operator.ge, criteria.get(0).op);
		assertEquals(Date.parse("2016-01-01"), criteria.get(0).rvalues[0]);
		assertEquals(Operator.le, criteria.get(1).op);
		assertEquals(Date.parse("2016-12-31"), criteria.get(1).rvalues[0]);
	}

	@Test
	public void parsingOrdersListOfCriteriumByItsImportanceForLookup() {
		Criteria criteria = Criteria.parse("[length=5][user~{Tom,Frank}][reporter=Tom][age<10][solver=Max]");
		assertEquals(5, criteria.count());
		assertEquals(Property.solver, criteria.get(0).left);
		assertEquals(Property.reporter, criteria.get(1).left);
		assertEquals(Property.supporter, criteria.get(2).left);
		assertEquals(Property.age, criteria.get(3).left);
		assertEquals(Property.length, criteria.get(4).left);
	}

	@Test
	public void parsingReplacesActualOutput() {
		Criteria criteria = Criteria.parse("[output=@]").bindTo(singletonMap(Property.output, as("foo")));
		assertEquals("[output = foo]", criteria.toString());
	}

	@Test
	public void parsingReplacesActualArea() {
		Criteria criteria = Criteria.parse("[area=@]").bindTo(singletonMap(Property.area, as("foo")));
		assertEquals("[area = foo]", criteria.toString());
	}

	@Test
	public void parsingReplacesActualUser() {
		EnumMap<Property, Name> context = new EnumMap<>(Property.class);
		context.put(Property.supporter, as("foo"));
		context.put(Property.reporter, as("bar"));
		Criteria criteria = Criteria.parse("[user=@][reporter=@]").bindTo(context);
		assertEquals("[reporter = bar][user = foo]", criteria.toString());
	}

	@Test
	public void propertyCanBeUsedAsRightOperand() {
		Criteria criteria = Criteria.parse("[reporter~@user][user~@reporter]");
		assertEquals("[reporter = @user][user ~ @reporter]", criteria.toString());
	}

}
