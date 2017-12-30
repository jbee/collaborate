package se.jbee.track.cache;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static se.jbee.track.model.IDN.idn;

import org.junit.Test;

import se.jbee.track.cache.CacheWorker.TaskSet;

public class TestTaskSet {

	@Test
	public void contains() {
		TaskSet set = new TaskSet();
		for (int i = 1; i < 100; i+=3)
			set.init(idn(i));
		set.remove(idn(55));
		set.remove(idn(88));
		set.remove(idn(33));
		set.add(idn(150));
		set.add(idn(120));
		
		assertTrue(set.contains(idn(150)));
		assertTrue(set.contains(idn(120)));
		assertTrue(set.contains(idn(1)));
		assertTrue(set.contains(idn(7)));
		assertTrue(set.contains(idn(16)));
		assertFalse(set.contains(idn(100)));
		assertFalse(set.contains(idn(33)));
		assertFalse(set.contains(idn(88)));
		
	}
}
