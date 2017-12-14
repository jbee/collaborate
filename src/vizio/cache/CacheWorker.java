package vizio.cache;

import static vizio.util.Array.nextPowerOf2;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import vizio.cache.Criteria.Criterium;
import vizio.cache.Criteria.Property;
import vizio.db.DB;
import vizio.db.DB.TxR;
import vizio.engine.Changes;
import vizio.engine.DAO;
import vizio.engine.Repository;
import vizio.model.Date;
import vizio.model.IDN;
import vizio.model.Motive;
import vizio.model.Name;
import vizio.model.Product;
import vizio.model.Purpose;
import vizio.model.Status;
import vizio.model.Task;

/**
 * Each worker is responsible for a single {@link Product}.
 * 
 * This way there is only 1 thread working with the data what makes it trivial
 * to not have inconsistent states leaving the cache while updating the cached
 * entities in place.
 */
final class CacheWorker implements Cache {

	private final Name product;
	private final ExecutorService es;

	/**
	 * The {@link IDN} order is also the order by reported {@link Date}.
	 * This can be used to narrow down with related date ranges.
	 */
	private Task[] byIDN;
	private int usage; // last index in use
	
	// caches: best to worst filtering
	private Map<Name, TaskSet> byUser = new HashMap<>();
	private Map<Name, TaskSet> byMaintainer = new HashMap<>();
	private Map<Name, TaskSet> bySolver = new HashMap<>();
	private Map<Name, TaskSet> byReporter = new HashMap<>();
	private Map<Name, TaskSet> byWatcher = new HashMap<>();
	private Map<Name, TaskSet> byArea = new HashMap<>();
	
	private EnumMap<Status, TaskSet> byStatus = new EnumMap<>(Status.class);
	private EnumMap<Purpose, TaskSet> byPurpose = new EnumMap<>(Purpose.class);
	private EnumMap<Motive, TaskSet> byMotive = new EnumMap<>(Motive.class);
	
	public CacheWorker(Name product, DB db) {
		super();
		this.product = product;
		this.es = Executors.newSingleThreadExecutor(this::factory);
		this.byIDN = new Task[128]; // initial capacity
		init(db);
	}

	private Thread factory(Runnable target) {
		Thread t = new Thread(target);
		t.setDaemon(true);
		t.setName("cache-worker:"+product);
		return t;
	}
	
	@Override
	public String toString() {
		return "cache:"+product.toString()+"["+usage+"]";
	}
	
	private void init(DB db) {
		try (TxR tx = db.read()) {
			try (Repository rep = new DAO(tx)) {
				rep.tasks(product, (t) -> { index(t); return true; });
			}
		}
	}
	
	private void index(Task t) {
		int idn = t.id.num;
		if (idn >= byIDN.length) {
			Task[] tmp = new Task[nextPowerOf2(idn)];
			System.arraycopy(tmp, 0, tmp, 0, usage+1);
			byIDN = tmp;
			usage = idn;
		}
		byIDN[idn] = t;
		for (Name n : t.engagedBy)
			getOrInit(n, byUser).init(t.id);
		for (Name n : t.pursuedBy)
			getOrInit(n, byUser).init(t.id);
		for (Name n : t.area.maintainers)
			getOrInit(n, byMaintainer).init(t.id);
		getOrInit(t.reporter, byReporter).init(t.id);
		if (t.isSolved())
			getOrInit(t.solver, bySolver).init(t.id);
		for (Name n : t.watchedBy)
			getOrInit(n, byWatcher).init(t.id);
		getOrInit(t.area.name, byArea).init(t.id);
		getOrInit(t.status, byStatus).init(t.id);
		getOrInit(t.purpose, byPurpose).init(t.id);
		getOrInit(t.motive, byMotive).init(t.id);
	}
	
	private static <K> TaskSet getOrInit(K key, Map<K, TaskSet> map) {
		TaskSet set = map.get(key);
		if (set == null) {
			set = new TaskSet();
			map.put(key, set);
		}
		return set;
	}

	@Override
	public Future<Tasks> tasks(Criteria criteria) {
		return es.submit(() -> lookup(criteria));
	}
	
	@Override
	public Future<Void> invalidate(Changes changes) {
		return es.submit(() -> { update(changes); return null; } );
	}
	
	/**
	 * As soon the set of potential hits has been narrowed down to less then
	 * about 100 we use the remaining criteria to filter that list of potential
	 * matches. Merging different sets of potential matches is most likely more
	 * expensive. In any case it creates more short lived objects => garbage.
	 */
	private Tasks lookup(Criteria criteria) {
		Criterium selector = criteria.topSelector(EnumSet.of(Property.user, Property.id));
		if (selector == null) {
			//TODO :( filter only
		}
		return null;
	}
	
	private void update(Changes changes) {
		
	}
	
	/**
	 * A unsorted set of ids stored as shorts for compaction of data. Some cells
	 * might be zero. These are blanked after {@link #remove(IDN)} for later
	 * usage by {@link #init(IDN)}. When iterating the set one has to iterate
	 * from 0 to usage (inclusive) and ignore all zeros.
	 * 
	 * The set grows by power of 2 when set if full.
	 * 
	 * The set is designed for high write thoughput and low memory footprint.
	 */
	static class TaskSet {
		// could have multiple impls. for "a set of tasks" mostly because we can refer to them by their ID and IDs are 1..n - for smaller n's short[] is sufficient
		private short[] members = new short[8];
		private int usage = -1;
		private int gaps = 0;
		private int gap0 = -1; // last known gap index (if not reused already) this speeds up remove/add cycles
		
		/**
		 * In contrast to {@link #add(IDN)} we are sure each task is just
		 * initiated once so that we do not have to search for task already
		 * being in the set.
		 */
		void init(IDN task) {
			if (usage >= members.length-1) {
				short[] tmp = new short[members.length * 2];
				System.arraycopy(members, 0, tmp, 0, members.length);
				members = tmp;
			}
			members[++usage] = (short) task.num;
		}
		
		boolean contains(IDN task) {
			final int num = task.num;
			for (int i = 0; i <= usage; i++) {
				if (num == members[i])
					return true;
			}
			return false;
		}
		
		void add(IDN task) {
			if (!contains(task)) {
				if (gaps > 0) {
					if (gap0 >= 0) {
						members[gap0] = (short) task.num;
						gap0 = -1;
					} else {
						int i = 0;
						while (members[i] != 0) i++;
						members[i] = (short) task.num;
					}
					gaps--;
				} else {
					init(task);
				}
			}
		}
		
		void remove(IDN task) {
			if (contains(task)) {
				int num = task.num;
				int i = 0;
				while (members[i] != num) i++;
				members[i] = 0;
				gaps++;
				gap0 = i;
			}
		}
	}
	
	private static void sort(Task[] tasks, final Property[] orders, final Date today) {
		Arrays.sort(tasks, new Comparator<Task>() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public int compare(Task a, Task b) {
				for (int i = 0; i < orders.length; i++) {
					Property order = orders[i];
					Comparable ca = order.access(a, today);
					Comparable cb = order.access(b, today);
					int cmp = ca.compareTo(cb);
					if (cmp != 0)
						return cmp;
				}
				return 0;
			}
		});
	}
}
