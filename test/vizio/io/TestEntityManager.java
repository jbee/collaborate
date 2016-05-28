package vizio.io;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertSame;
import static vizio.io.SimpleEntityManager.insert;

import org.junit.Test;

import vizio.IDN;
import vizio.Task;

public class TestEntityManager {

	@Test
	public void insertCorrectness() {
		assertArrayEquals(taskSet(1,2), insert(taskSet(2), task(1)));
		assertArrayEquals(taskSet(1,2), insert(taskSet(1), task(2)));
		assertArrayEquals(taskSet(1,2,4,0), insert(taskSet(1,2), task(4)));
		assertArrayEquals(taskSet(1,2,4,0), insert(taskSet(1,4), task(2)));
		assertArrayEquals(taskSet(1,2,3,4), insert(taskSet(1,2,3,0), task(4)));
		assertArrayEquals(taskSet(1,2,4,0), insert(taskSet(1,4,0,0), task(2)));
		assertArrayEquals(taskSet(1,2,3,4), insert(taskSet(1,2,4,0), task(3)));
		assertArrayEquals(taskSet(1,2,4,5), insert(taskSet(1,2,4,0), task(5)));
		assertArrayEquals(taskSet(1,2,3,4,5,0,0,0), insert(taskSet(1,2,4,5), task(3)));
		Task e = task(2);
		Task[] set = insert(taskSet(1), e);
		assertSame(set[1], e);
	}
	
	private Task[] taskSet(int...ids) {
		Task[] set = new Task[ids.length];
		for (int i = 0; i < ids.length; i++) {
			if (ids[i] > 0) {
				set[i] = task(ids[i]);
			}
		}
		return set;
	}

	private Task task(int num) {
		Task task = new Task();
		task.id = new IDN(num);
		return task;
	}
}
