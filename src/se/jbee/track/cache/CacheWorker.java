package se.jbee.track.cache;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Arrays.asList;
import static java.util.Arrays.copyOfRange;
import static se.jbee.track.model.Criteria.Operator.eq;
import static se.jbee.track.model.Criteria.Property.length;
import static se.jbee.track.util.Array.nextPowerOf2;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;

import se.jbee.track.db.DB;
import se.jbee.track.db.DB.TxR;
import se.jbee.track.engine.Change;
import se.jbee.track.engine.Changes;
import se.jbee.track.engine.Changes.Entry;
import se.jbee.track.engine.DAO;
import se.jbee.track.engine.Event;
import se.jbee.track.engine.History;
import se.jbee.track.engine.Repository;
import se.jbee.track.model.Area;
import se.jbee.track.model.Criteria;
import se.jbee.track.model.Criteria.Criterium;
import se.jbee.track.model.Criteria.Operator;
import se.jbee.track.model.Criteria.Property;
import se.jbee.track.model.Date;
import se.jbee.track.model.IDN;
import se.jbee.track.model.Motive;
import se.jbee.track.model.Name;
import se.jbee.track.model.Names;
import se.jbee.track.model.Output;
import se.jbee.track.model.Page;
import se.jbee.track.model.Poll;
import se.jbee.track.model.Purpose;
import se.jbee.track.model.Status;
import se.jbee.track.model.Task;
import se.jbee.track.model.User;
import se.jbee.track.model.Version;

/**
 * Each worker is responsible for a single {@link Output}.
 *
 * This way there is only 1 thread working with the data what makes it trivial
 * to not have inconsistent states leaving the cache while updating the cached
 * entities in place.
 */
final class CacheWorker implements Cache {

	private final Name output;
	private final Date today;
	private final ExecutorService es;

	/**
	 * The {@link IDN} order is also the order by reported {@link Date}.
	 * This can be used to narrow down with related date ranges.
	 */
	private Task[] byIDN; // fix (growing at the end)
	private int usage; // last index in use

	// caches: best to worst filtering
	private Map<Name, TaskSet> byUser = new HashMap<>(); // almost fix
	private Map<Name, TaskSet> byMaintainer = new HashMap<>(); // almost fix
	private Map<Name, TaskSet> bySolver = new HashMap<>(); // almost fix
	private Map<Name, TaskSet> byReporter = new HashMap<>(); // fix
	private Map<Name, TaskSet> byWatcher = new HashMap<>(); // almost fix
	private Map<Name, TaskSet> byArea = new HashMap<>(); // almost fix
	private Map<Name, TaskSet> byVersion = new HashMap<>(); // almost fix
	private Map<Name, TaskSet> byCategory = new HashMap<>(); // almost fix

	private Map<IDN, TaskSet> byBasis = new HashMap<>(); // fix
	private Map<IDN, TaskSet> byOrigin = new HashMap<>(); // fix
	private Map<IDN, TaskSet> bySerial = new HashMap<>(); // fix

	private EnumMap<Purpose, TaskSet> byPurpose = new EnumMap<>(Purpose.class); // fix
	private EnumMap<Motive, TaskSet> byMotive = new EnumMap<>(Motive.class); // fix
	private EnumMap<Status, TaskSet> byStatus = new EnumMap<>(Status.class); // almost fix

	// special caches:
	private TaskSet[] byTemperature = new TaskSet[100]; // not fix, has to be recomputed every day

	public CacheWorker(Name output, DB db, Date today) {
		this.output = output;
		this.today = today;
		this.byIDN = new Task[128]; // initial capacity
		this.es = Executors.newSingleThreadExecutor(this::factory);
		init(db);
	}

	@Override
	public void shutdown() {
		es.shutdown();
	}

	private Thread factory(Runnable target) {
		Thread t = new Thread(target);
		t.setDaemon(true);
		t.setName("cache-worker:"+output);
		return t;
	}

	@Override
	public String toString() {
		return "cache:"+output.toString()+"["+usage+"]";
	}

	private void init(DB db) {
		try (TxR tx = db.read()) {
			try (Repository rep = new DAO(tx)) {
				rep.tasks(output, (t) -> { index(t, TaskSet::init); return true; });
			}
		}
	}

	private void index(Task t, BiConsumer<TaskSet, IDN> f) {
		int idn = t.id.num;
		if (idn >= byIDN.length) {
			Task[] tmp = new Task[nextPowerOf2(idn)];
			System.arraycopy(tmp, 0, tmp, 0, usage+1);
			byIDN = tmp;
		}
		usage = Math.max(idn, usage);
		byIDN[idn] = t;
		if (!t.archived) {
			for (Name n : t.participants)
				f.accept(getOrInit(n, byUser), t.id);
			for (Name n : t.aspirants)
				f.accept(getOrInit(n, byUser), t.id);
			for (Name n : t.area.maintainers)
				f.accept(getOrInit(n, byMaintainer), t.id);
			f.accept(getOrInit(t.reporter, byReporter), t.id);
			if (t.isSolved())
				f.accept(getOrInit(t.solver, bySolver), t.id);
			for (Name n : t.watchers)
				f.accept(getOrInit(n, byWatcher), t.id);
			f.accept(getOrInit(t.area.name, byArea), t.id);
			f.accept(getOrInit(t.area.category, byCategory), t.id);
			f.accept(getOrInit(t.base.name, byVersion), t.id);
			f.accept(getOrInit(t.status, byStatus), t.id);
			f.accept(getOrInit(t.purpose, byPurpose), t.id);
			f.accept(getOrInit(t.motive, byMotive), t.id);
			f.accept(getOrInit(t.temperature(today), byTemperature), t.id);
			getOrInit(t.serial, bySerial).init(t.id);
			getOrInit(t.basis, byBasis).init(t.basis);
			getOrInit(t.origin, byOrigin).init(t.origin);
		}
	}

	private static <K> TaskSet getOrInit(K key, Map<K, TaskSet> map) {
		TaskSet set = map.get(key);
		if (set == null) {
			set = new TaskSet();
			map.put(key, set);
		}
		return set;
	}

	private static TaskSet getOrInit(int idx, TaskSet[] map) {
		if (map[idx] == null)
			map[idx] = new TaskSet();
		return map[idx];
	}

	@Override
	public Future<Matches> matchesFor(User actor, Criteria criteria) {
		return es.submit(() -> lookup(criteria));
	}

	@Override
	public Future<Void> invalidate(Changes changes) {
		//TODO if there is a gap in serial put the ones coming late in a separate map.
		// the later update will notice the gap again and pull all from the map that are inbetween
		return es.submit(() -> { update(changes); return null; } );
	}

	/**
	 * As soon the set of potential hits has been narrowed down to less then
	 * about 100 we use the remaining criteria to filter that list of potential
	 * matches. Merging different sets of potential matches is most likely more
	 * expensive. In any case it creates more short lived objects => garbage.
	 */
	private Matches lookup(Criteria criteria) {
		// 0. if there is not a single criteria return all
		if (criteria.count() == 0) {
			return new Matches(copyOfRange(byIDN, 1, usage+1), usage+1);
		}
		// 1. find the "eq" selector that yields the smallest set
		TaskSet candidates = null;
		Criterium selector = null;
		int i = 1;
		while (i < criteria.count() && criteria.get(i).op == eq) {
			Criterium c = criteria.get(i);
			Map<?,TaskSet> table = select(c.left);
			if (table != null) {
				TaskSet index = table.get(c.rvalues[0]);
				if (index == null)
					return Matches.none(); // there are no matches for this selector - we are done
				if (candidates == null || index.size() < candidates.size()) {
					selector = c;
					candidates = index;
				}
			}
			i++;
		}
		if (selector != null) {
			return orderAndSlice(filter(candidates, criteria), criteria, today);
		}
		// 2. or (if no eq available) use the first "in" clause

		// 3. or use a range (serial, heat, temperature)

		// 4. or just plain filter every known task ;(
		return orderAndSlice(filter(byIDN, usage+1, criteria), criteria, today);
	}

	static Matches orderAndSlice(Task[] matches, Criteria criteria, Date today) {
		int len = 50;
		int offset = 0;
		if (criteria.contains(length)) {
			len = criteria.get(criteria.indexOf(length)).intValue(len);
		}
		if (criteria.contains(Property.offset)) {
			offset = criteria.get(criteria.indexOf(Property.offset)).intValue(offset);
		}
		int total = matches.length;
		if (offset + len > total) {
			matches = new Task[0];
		} else {
			if (criteria.contains(Property.order)) {
				order(matches, criteria, today);
			}
			if (offset > 0 || offset+total > len) {
				matches = copyOfRange(matches, offset, min(total, offset+len));
			}
		}
		return new Matches(matches, total);
	}


	static void order(Task[] matches, Criteria criteria, Date today) {
		int len = 0;
		int i = criteria.indexOf(Property.order);
		while (i >= 0) {
			len += criteria.get(i).rvalues.length;
			i = criteria.indexOf(Property.order, i+1);
		}
		final Property[] props = new Property[len];
		final int[] factors = new int[len];
		i = criteria.indexOf(Property.order);
		int s = 0;
		while (i >= 0) {
			Criterium criterium = criteria.get(i);
			Object[] orders = criterium.rvalues;
			System.arraycopy(orders, 0, props, s, orders.length);
			Arrays.fill(factors, criterium.op == Operator.asc ? 1 : -1, s, s+orders.length);
			s+=orders.length;
			i = criteria.indexOf(Property.order, i+1);
		}
		Arrays.sort(matches, (a,b) -> {
			for (int k = 0; k < props.length; k++) {
				Property p = props[k];
				int res = cmp(p.access(a, today), p.access(b, today));
				if (res != 0)
					return factors[k] * res;
			}
			return 0;
		});
	}

	@SuppressWarnings("unchecked")
	static <T extends Comparable<T>> int cmp(Comparable<?> a, Comparable<?> b) {
		return ((T)a).compareTo((T)b);
	}

	private Task[] filter(TaskSet set, Criteria criteria) {
		final int size = set.size();
		return criteria.filter(new Iterator<Task>() {

			int i = 0;
			@Override
			public Task next() {
				while (set.members[i] == 0) i++; // skip gaps
				return byIDN[set.members[i++]];
			}

			@Override
			public boolean hasNext() {
				return i < size-1;
			}
		}, today);
	}

	private Task[] filter(Task[] set, int size, Criteria criteria) {
		return criteria.filter(asList(set).subList(1, size).iterator(), today);
	}

	private Map<?,TaskSet> select(Property prop) {
		switch (prop) {
		case aspirant:
		case participant:
		case user: return byUser;
		case reporter: return byReporter;
		case solver: return bySolver;
		case watcher: return byWatcher;
		case maintainer: return byMaintainer;
		case area: return byArea;
		case category: return byCategory;
		case version: return byVersion;
		case purpose: return byPurpose;
		case motive: return byMotive;
		case status: return byStatus;
		case serial: return bySerial;
		case origin: return byOrigin;
		case basis: return byBasis;
		default: return null;
		}
	}

	/**
	 * Changes to take care of:
	 *
	 * - change to the{@link Output}, {@link Area}s and {@link Poll}s.
	 * - {@link Task} changes (obviously)
	 *
	 * Changes to ignore:
	 *
	 * - change to {@link User}, {@link Page} and {@link Version}
	 * - new {@link Event}s or {@link History}
	 */
	@SuppressWarnings("unchecked")
	private void update(Changes changes) {
		for (Changes.Entry<?> e : changes) {
			switch (e.type()) {
			case Area: updateArea((Entry<Area>) e); break;
			case Output: updateOutput((Entry<Output>) e); break;
			case Task: updateTask((Entry<Task>) e); break;
			default: // just ignore those changes
			}
		}
	}

	/**
	 * The updates required to incooperate changes is mostly minimal. This is
	 * the main idea behind this domains specific caching that takes advantage
	 * of domain knowledge to minimize the work required to keep the cache up to
	 * date even for a entity that is modified quite often.
	 *
	 * A classic design of "throw out" and "reload" from DB would basically
	 * constantly reload stuff and thereby not be that helpful.
	 */
	@SuppressWarnings("null")
	private void updateTask(Entry<Task> e) {
		final IDN idn = e.after.id;
		final Task before = e.before;
		final Task after = e.after;
		final Task task = idn.num >= byIDN.length ? before : byIDN[idn.num];
		if (task != null) {
			after.update(task);
			task.changed();
		}
		for (Change.Operation op : e.transitions) {
			switch (op) {
			case emphasise: // emphasis up/down
				if (before.temperature(today) != after.temperature(today)) {
					byTemperature[before.temperature(today)].remove(idn);
					getOrInit(after.temperature(today), byTemperature).add(idn);
				}
				task.emphasis = after.emphasis;
				break;
			case resolve: // solving
			case absolve:
			case dissolve:
				getOrInit(before.status, byStatus).remove(idn);
				getOrInit(after.status, byStatus).add(idn);
				getOrInit(after.solver, bySolver).add(idn);
				removeMissing(before.participants, after.participants, byUser, idn);
				removeMissing(before.aspirants, after.aspirants, byUser, idn);
				task.status = after.status;
				break;
			case relocate: // change of area
				getOrInit(before.area.name, byArea).remove(idn);
				getOrInit(after.area.name, byArea).add(idn);
				task.area = cacheInstanceOf(after.area);
				break;
			case rebase: // change of version
				getOrInit(before.base.name, byVersion).remove(idn);
				getOrInit(after.base.name, byVersion).add(idn);
				task.base = cacheInstanceOf(after.base);
				break;
			case attach: // attach/detach
				task.attachments = after.attachments; break;
			case aspire: // become a user 1
				addMissing(after.aspirants, before.aspirants, byUser, idn);
				task.aspirants = after.aspirants;
				task.participants = after.participants;
				break;
			case participate: // become a user 2
				addMissing(after.participants, before.participants, byUser, idn);
				task.participants = after.participants;
				task.aspirants = after.aspirants;
				break;
			case abandon: // no longer a user
				removeMissing(before.users(), after.users(), byUser, idn);
				task.aspirants = after.aspirants;
				task.participants = after.participants;
				break;
			case watch: // become a watcher
				addMissing(after.watchers, before.watchers, byWatcher, idn);
				task.watchers = after.watchers;
				break;
			case unwatch: // no longer a watcher
				removeMissing(before.watchers, after.watchers, byWatcher, idn);
				task.watchers = after.watchers;
				break;
			case propose: // new task
			case indicate:
			case warn:
			case request:
			case advance:
				Task t = after;
				t.area = cacheInstanceOf(t.area);
				t.base = cacheInstanceOf(t.base);
				t.output = cacheInstanceOf(t.output);
				index(t, TaskSet::add);
				break;
			case archive:
				// we do not remove it right away from all caches since this will happen on next day anyway when new cache instance is build
				break;
			}
		}
		// TODO maybe it is easier to just replace the full task in byID with after
	}

	private Version cacheInstanceOf(Version v) {
		TaskSet set = byVersion.get(v.name);
		if (set == null)
			return v;
		int idx = set.first();
		return idx < 0 ? v : byIDN[idx].base;
	}

	private Area cacheInstanceOf(Area a) {
		TaskSet set = byArea.get(a.name);
		if (set == null)
			return a;
		int idx = set.first();
		return idx < 0 ? a : byIDN[idx].area;
	}

	private Output cacheInstanceOf(Output p) {
		return byIDN[1].output;
	}

	private static void removeMissing(Names a, Names b, Map<Name, TaskSet> map, IDN idn) {
		for (Name n : a) {
			if (!b.contains(n)) { getOrInit(n, map).remove(idn); }
		}
	}

	private static void addMissing(Names a, Names b, Map<Name, TaskSet> map, IDN idn) {
		for (Name n : a) {
			if (!b.contains(n)) { getOrInit(n, map).add(idn); }
		}
	}

	private void updateOutput(Entry<Output> e) {
		Output after = e.after;
		Output p = cacheInstanceOf(after);
		if (after.isMoreRecent(p)) {
			// just do that no matter what has changed as it is simpler
			p.integrations = after.integrations;
			p.tasks = after.tasks;
			after.update(p);
		}
	}

	private void updateArea(Entry<Area> e) {
		Area after = e.after;
		Area a = cacheInstanceOf(after);
		if (a == after)
			return;
		for (Change.Operation op : e.transitions) {
			switch (op) {
			case leave:
			case consent: // poll
			case dissent:
				if (after.isMoreRecent(a)) {
					a.abandoned = after.abandoned;
					a.exclusive = after.exclusive;
					a.maintainers = after.maintainers;
				}
				break;
			case categorise:
				a.category = after.category; break;
			}
		}
		if (after.tasks > a.tasks) {
			a.tasks = after.tasks;
		}
		after.update(a);
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
	 *
	 * It supports sets of up to 32,766 items.
	 */
	static class TaskSet {
		private short[] members = new short[8];
		private int usage = -1;
		private int size = 0; // how many members are actually defined
		private int maxIDN = 0;
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
			int idn = task.num;
			members[++usage] = (short) idn;
			size++;
			maxIDN = max(maxIDN, idn);
		}

		int first() {
			int i = 0;
			while (i <= usage && members[i] == 0) i++;
			return members[i] == 0 ? -1 : members[i];
		}

		boolean contains(IDN task) {
			return indexOf(task) >= 0;
		}

		public int size() {
			return size;
		}

		int indexOf(IDN task) {
			final int idn = task.num;
			if (usage > 16) { // try binary search
				int res = indexOf(idn, usage/2, usage/2);
				if (res >= 0)
					return res;
			}
			for (int i = 0; i <= usage; i++) {
				if (idn == members[i])
					return i;
			}
			return -1;
		}

		/**
		 * This is a modified binary search that is being optimistic about the
		 * items in the set being sorted in ascending order. Due to gap filling
		 * with numbers out of order the tried index m might jump out of bounds
		 * so we have to check for that. The idea is to just do the steps until
		 * we have tried all the way down to a delta of 1. If index is not found
		 * by then we got messed up by zeros or filled gaps.
		 */
		private int indexOf(int idn, int m, int d) {
			while (m <= usage && m >= 0 && members[m] != idn) {
				if (d == 1)
					return -1;
				d = d/2;
				m += members[m] > idn ? -d : d;
			}
			return m <= usage && m >= 0 ? m : -1;
		}

		void add(IDN task) {
			int idn = task.num;
			if (idn > maxIDN) {
				init(task);
			} else if (!contains(task)) {
				if (gaps > 0) {
					if (gap0 >= 0) {
						members[gap0] = (short) idn;
						gap0 = -1;
					} else {
						int i = 0;
						while (members[i] != 0) i++;
						members[i] = (short) idn;
					}
					gaps--;
					size++;
					maxIDN = max(maxIDN, idn);
				} else {
					init(task);
				}
			}
		}

		void remove(IDN task) {
			int idx = indexOf(task);
			if (idx >= 0) {
				members[idx] = 0;
				gaps++;
				gap0 = idx;
				size--;
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
