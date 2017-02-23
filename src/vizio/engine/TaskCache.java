package vizio.engine;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import vizio.db.DB;
import vizio.model.Name;
import vizio.model.Task;

public class TaskCache implements Cache {

	private Task[] tasks; // by ID within the product
	
	private final Name product;
	private final DB db;
	
	private final ArrayBlockingQueue<Query> queries = new ArrayBlockingQueue<>(20);
	
	public TaskCache(Name product, DB db) {
		super();
		this.product = product;
		this.db = db;
	}

	@Override
	public Future<Tasks> tasks(Constraints constraints) {
		Query res = new Query(constraints);
		if (!queries.offer(res)) {
			throw new IllegalStateException("Too many queries in progress...");
		}
		return res;
	}
	
	@Override
	public void invalidate(Changelog log) {
		// TODO Auto-generated method stub
		
	}
	
	private NameIndex<NameIndex<TaskIndex>> idxProductVersion;
	private NameIndex<NameIndex<TaskIndex>> idxProductArea;
	private NameIndex<NameIndex<TaskIndex>> idxProductUser;
	private NameIndex<EnumIndex<TaskIndex>> idxProductMotive;
	private NameIndex<EnumIndex<TaskIndex>> idxProductPurpose;
	private NameIndex<EnumIndex<TaskIndex>> idxProductStatus;
	private NameIndex<EnumIndex<TaskIndex>> idxUserStatus;
	private NameIndex<EnumIndex<TaskIndex>> idxWatcherStatus;

	static class NameIndex<T> {
		
	}
	
	static class EnumIndex<T> {
		
	}
	
	static class TaskIndex {
		// basically a unsorted list of Tasks that fall into a particular category
	}
	
	static final class Query implements Future<Tasks> {

		private static final Tasks CANCELLED = new Tasks(new Task[0], -1);
		
		private final AtomicReference<Tasks> response = new AtomicReference<>(null);
		private final Constraints request;

		Query(Constraints request) {
			super();
			this.request = request;
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			if (response.compareAndSet(null, CANCELLED)) {
				synchronized (this) {
					notifyAll();
				}
				return true;
			}
			return false;
		}

		@Override
		public boolean isCancelled() {
			return response.get() == CANCELLED;
		}

		@Override
		public boolean isDone() {
			return response.get() != null;
		}

		@Override
		public Tasks get() throws InterruptedException {
			Tasks res = response.get();
			while (res == null)  {
				synchronized (this) {
					wait();
				}
				res = response.get();
			}
			return isCancelled() ? null : res;
		}

		@Override
		public Tasks get(long timeout, TimeUnit unit) throws InterruptedException {
			Tasks res = response.get();
			if (res == null) {
				synchronized (this) {
					wait(unit.toNanos(timeout));
				}
			}
			return isCancelled() ? null : res;
		}

		void complete(Tasks res) {
			if (response.compareAndSet(null, res)) {
				synchronized (this) {
					notifyAll();
				}
			}
		}
	}
}
