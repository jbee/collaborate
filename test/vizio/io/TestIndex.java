package vizio.io;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static vizio.model.Name.as;

import org.junit.Test;

import vizio.model.IDN;
import vizio.model.Name;
import vizio.model.Status;
import vizio.model.Task;

public class TestIndex {

	@Test
	public void testInsert() {
		assertArrayEquals(taskSet(1,2), Index.insert(taskSet(2), task(1)));
		assertArrayEquals(taskSet(1,2), Index.insert(taskSet(1), task(2)));
		assertArrayEquals(taskSet(1,2,4), Index.insert(taskSet(1,2), task(4)));
		assertArrayEquals(taskSet(1,2,4), Index.insert(taskSet(1,4), task(2)));
		assertArrayEquals(taskSet(1,2,3,4), Index.insert(taskSet(1,2,3), task(4)));
		assertArrayEquals(taskSet(1,2,4), Index.insert(taskSet(1,4), task(2)));
		assertArrayEquals(taskSet(1,2,3,4), Index.insert(taskSet(1,2,4), task(3)));
		assertArrayEquals(taskSet(1,2,4,5), Index.insert(taskSet(1,2,4), task(5)));
		assertArrayEquals(taskSet(1,2,3,4,5), Index.insert(taskSet(1,2,4,5), task(3)));
		Task e = task(2);
		Task[] set = Index.insert(taskSet(1), e);
		assertSame(set[1], e);
	}
	
	@Test
	public void testRemove() {
		assertArrayEquals(taskSet(), Index.cutout(taskSet(1), 0));
		assertArrayEquals(taskSet(1), Index.cutout(taskSet(1,2), 1));
		assertArrayEquals(taskSet(2), Index.cutout(taskSet(1,2), 0));
		assertArrayEquals(taskSet(1,2), Index.cutout(taskSet(1,2), -1));
		assertArrayEquals(taskSet(1,3), Index.cutout(taskSet(1,2,3), 1));
	}
	
	@Test
	public void testIndex() {
		Index<Status> idx = Index.init();
		Task t1 = task(1);
		Name p1 = as("prod1");
		idx = idx.add(p1, Status.absolved, t1);
		assertArrayEquals(taskSet(1), idx.tasks(p1, Status.absolved));
		Task t2 = task(2);
		idx = idx.add(p1, Status.absolved, t2);
		assertArrayEquals(taskSet(1,2), idx.tasks(p1, Status.absolved));
		assertNull(idx.tasks(p1, Status.dissolved));
		idx = idx.add(p1, Status.absolved, t2);
		assertArrayEquals(taskSet(1,2), idx.tasks(p1, Status.absolved));
		idx = idx.remove(p1, Status.absolved, t1);
		assertArrayEquals(taskSet(2), idx.tasks(p1, Status.absolved));
		idx = idx.remove(p1, Status.absolved, t2);
		assertNull(idx.tasks(p1, Status.absolved));
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
		Task task = new Task(1);
		task.id = new IDN(num);
		return task;
	}
}
