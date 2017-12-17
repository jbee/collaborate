package se.jbee.track.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static se.jbee.track.cache.Criteria.ValueType.text;

import org.junit.Test;

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
	public void test() {
		Criteria criteria = Criteria.parse("[color=heat][length=20][user~{foo,bar}]");
		assertEquals(3, criteria.count());
		assertEquals("[color = heat][length = 20][user ~ {foo, bar}]", criteria.toString());
		assertSame(Name.class, criteria.get(0).values[0].getClass());
		assertSame(Integer.class, criteria.get(1).values[0].getClass());
		assertSame(Name.class, criteria.get(2).values[0].getClass());
	}
	
	@Test
	public void test2() {
		Criteria criteria = Criteria.parse("[url={jira, GRP-001}][url=https://mail.google.com/mail/u/0/#inbox]");
		assertEquals(2, criteria.count());
	}
	
	@Test
	public void test3() {
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
}