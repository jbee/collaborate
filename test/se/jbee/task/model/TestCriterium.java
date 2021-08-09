package se.jbee.task.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static se.jbee.task.model.Criteria.Operator.eq;
import static se.jbee.task.model.Criteria.Operator.ge;
import static se.jbee.task.model.Criteria.Operator.gt;
import static se.jbee.task.model.Criteria.Operator.in;
import static se.jbee.task.model.Criteria.Operator.le;
import static se.jbee.task.model.Criteria.Operator.lt;
import static se.jbee.task.model.Criteria.Operator.nin;
import static se.jbee.task.model.Criteria.Property.reported;
import static se.jbee.task.model.Name.as;

import org.junit.Before;
import org.junit.Test;

import se.jbee.task.model.Date;
import se.jbee.task.model.Output;
import se.jbee.task.model.Task;
import se.jbee.task.model.Criteria.Criterium;
import se.jbee.task.model.Criteria.Property;

public class TestCriterium {

	private final Date today = Date.today();
	private final Output p1 = new Output(1);
	private final Task t = new Task(1);

	@Before
	public void setUp() {
		p1.name = as("p1");
		t.output = p1;
		t.reported = today.minusDays(1);
	}
	
	@Test
	public void eq() {
		Criterium c1 = new Criterium(Property.output, eq, p1.name);
		assertTrue(c1.matches(t, today));
		p1.name = as("p2");
		assertFalse(c1.matches(t, today));

		Criterium c2 = new Criterium(reported, eq, today.minusDays(1));
		assertTrue(c2.matches(t, today));
		t.reported = today;
		assertFalse(c2.matches(t, today));
	}
	
	@Test
	public void lt() {
		Criterium c1 = new Criterium(reported, lt, today);
		assertTrue(c1.matches(t, today));

		Criterium c2 = new Criterium(reported, lt, today.minusDays(1));
		assertFalse(c2.matches(t, today));

		Criterium c3 = new Criterium(reported, lt, today.plusDays(1));
		assertTrue(c3.matches(t, today));
		
		Criterium c4 = new Criterium(reported, lt, today.minusDays(2));
		assertFalse(c4.matches(t, today));	
	}
	
	@Test
	public void le() {
		Criterium c1 = new Criterium(reported, le, today);
		assertTrue(c1.matches(t, today));

		Criterium c2 = new Criterium(reported, le, today.minusDays(1));
		assertTrue(c2.matches(t, today));

		Criterium c3 = new Criterium(reported, le, today.plusDays(1));
		assertTrue(c3.matches(t, today));

		Criterium c4 = new Criterium(reported, le, today.minusDays(2));
		assertFalse(c4.matches(t, today));	
	}
	
	@Test
	public void ge() {
		Criterium c1 = new Criterium(reported, ge, today);
		assertFalse(c1.matches(t, today));

		Criterium c2 = new Criterium(reported, ge, today.minusDays(1));
		assertTrue(c2.matches(t, today));

		Criterium c3 = new Criterium(reported, ge, today.plusDays(1));
		assertFalse(c3.matches(t, today));

		Criterium c4 = new Criterium(reported, ge, today.minusDays(2));
		assertTrue(c4.matches(t, today));	
	}	
	
	@Test
	public void gt() {
		Criterium c1 = new Criterium(reported, gt, today);
		assertFalse(c1.matches(t, today));

		Criterium c2 = new Criterium(reported, gt, today.minusDays(1));
		assertFalse(c2.matches(t, today));

		Criterium c3 = new Criterium(reported, gt, today.plusDays(1));
		assertFalse(c3.matches(t, today));
		
		Criterium c4 = new Criterium(reported, gt, today.minusDays(2));
		assertTrue(c4.matches(t, today));
	}
	
	@Test
	public void in() {
		Criterium c1 = new Criterium(Property.output, in, p1.name, as("p2"));
		assertTrue(c1.matches(t, today));
		
		Criterium c2 = new Criterium(Property.output, in, as("p2"), p1.name);
		assertTrue(c2.matches(t, today));
		
		Criterium c3 = new Criterium(Property.output, in, as("p2"), p1.name, as("p3"));
		assertTrue(c3.matches(t, today));
		
		Criterium c4 = new Criterium(Property.output, in, as("p2"), as("p3"));
		assertFalse(c4.matches(t, today));		
	}
	
	@Test
	public void nin() {
		Criterium c1 = new Criterium(Property.output, nin, p1.name, as("p2"));
		assertFalse(c1.matches(t, today));
		
		Criterium c2 = new Criterium(Property.output, nin, as("p2"), p1.name);
		assertFalse(c2.matches(t, today));
		
		Criterium c3 = new Criterium(Property.output, nin, as("p2"), p1.name, as("p3"));
		assertFalse(c3.matches(t, today));
		
		Criterium c4 = new Criterium(Property.output, nin, as("p2"), as("p3"));
		assertTrue(c4.matches(t, today));		
	}
}
