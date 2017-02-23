package vizio.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static vizio.engine.Constraints.ValueType.text;

import org.junit.Test;

import vizio.engine.Constraints.Operator;
import vizio.engine.Constraints.Property;
import vizio.model.Date;
import vizio.model.Name;

public class TestContraints {

	@Test
	public void textValuesCanBeURLs() {
		assertTrue(text.isValid("http://localhost:8080/"));
		assertTrue(text.isValid("https://www.youtube.com/watch?v=uhU_Uw3Yazo"));
		assertTrue(text.isValid("https://mail.google.com/mail/u/0/#inbox"));
		assertTrue(text.isValid("http://asserttrue.blogspot.se/2010/01/how-to-iimplement-custom-paint-in-50.html#"));
	}
	
	@Test
	public void test() {
		Constraints constraints = Constraints.parse("[color=heat][length=20][users~{foo,bar}]");
		assertEquals(3, constraints.count());
		assertEquals("[color = heat][length = 20][users ~ {foo, bar}]", constraints.toString());
		assertSame(Name.class, constraints.get(0).value[0].getClass());
		assertSame(Integer.class, constraints.get(1).value[0].getClass());
		assertSame(Name.class, constraints.get(2).value[0].getClass());
	}
	
	@Test
	public void test2() {
		Constraints constraints = Constraints.parse("[url={jira, GRP-001}][url=https://mail.google.com/mail/u/0/#inbox]");
		assertEquals(2, constraints.count());
	}
	
	@Test
	public void test3() {
		Constraints constraints = Constraints.parse("[order>>heat]");
		assertEquals(1, constraints.count());
		assertSame(Property.heat, constraints.get(0).value[0]);
	}
	
	@Test
	public void dateYYYYexpandsToRange() {
		Constraints constraints = Constraints.parse("[reported=2016]");
		assertEquals(2, constraints.count());
		assertEquals(Operator.ge, constraints.get(0).op);
		assertEquals(Date.parse("2016-01-01"), constraints.get(0).value[0]);
		assertEquals(Operator.le, constraints.get(1).op);
		assertEquals(Date.parse("2016-12-31"), constraints.get(1).value[0]);
	}
}
