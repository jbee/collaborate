package se.jbee.task.cache;

import static se.jbee.task.model.Criteria.Operator.eq;
import static se.jbee.task.model.Criteria.Operator.in;
import static se.jbee.task.model.Criteria.Operator.neq;
import static se.jbee.task.model.Criteria.Operator.nin;
import static se.jbee.task.model.Criteria.Property.output;
import static se.jbee.task.util.Array.fold;
import static se.jbee.task.util.Array.map;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import se.jbee.task.db.DB;
import se.jbee.task.engine.ChangeLog;
import se.jbee.task.engine.ChangeLog.Entry;
import se.jbee.task.engine.Clock;
import se.jbee.task.model.Criteria;
import se.jbee.task.model.Criteria.Property;
import se.jbee.task.model.Date;
import se.jbee.task.model.Name;
import se.jbee.task.model.Names;
import se.jbee.task.model.Output;
import se.jbee.task.model.Task;
import se.jbee.task.model.User;
import se.jbee.task.util.Log;

/**
 * A {@link CacheCluster} is a fully functional multi-{@link Output}
 * {@link Cache}.
 *
 * It has an {@link ExecutorService} to run and merge cross-output queries.
 * {@link Output}-specific queries are delegated to a {@link CacheWorker}. Each
 * {@link CacheWorker} caches a specific {@link Output}.
 *
 * The content of a {@link CacheWorker} is valid one day. On a new day now
 * outdated caches are {@link #shutdown()} and a fresh {@link CacheWorker}
 * instance is created for the new day. This way the cache updates itself when
 * needed.
 */
public class CacheCluster implements Cache {

	private static final Log LOG = Log.forClass(CacheCluster.class);

	private final ExecutorService es;
	private final DB db;
	private final Clock clock;
	private final Map<Name, Cache> outputCaches = new ConcurrentHashMap<>();

	/**
	 * The date the existing caches have been build for.
	 */
	private AtomicReference<Date> cacheValidity;

	public CacheCluster(DB db, Clock clock) {
		this.es = Executors.newSingleThreadExecutor(this::factory);
		this.db = db;
		this.clock = clock;
		this.cacheValidity = new AtomicReference<>(Date.date(clock.time()));
	}

	@Override
	public void close() {
		es.shutdown();
		closeAndClearCaches();
	}

	private void closeAndClearCaches() {
		LOG.info("Invalidating caches for outputs: " + outputCaches.keySet());
		for (Cache c : outputCaches.values()) c.close();
			outputCaches.clear();
	}

	private Thread factory(Runnable target) {
		Thread t = new Thread(target);
		t.setDaemon(true);
		t.setName("task-cache: *");
		return t;
	}

	@Override
	public Future<Matches> matchesFor(User actor, Criteria criteria) {
		// throw away old caches
		Date before = cacheValidity.get();
		Date today = Date.date(clock.time());
		if (today.after(before)) {
			if (cacheValidity.compareAndSet(before, today)) // make sure only one thread does the shutdown
				closeAndClearCaches();
		}
		// might be a indexing request
		if (criteria.isIndexRequest()) {
			Name output = (Name) criteria.get(0).rvalues[0];
			Cache cache = outputCaches.computeIfAbsent(output, (k) -> new CacheWorker(k, db, before));
			return cache.matchesFor(actor, criteria.without(Property.output));
		}
		// lookup request
		Names outputs = criteria.collect(Names.empty(), Name.class, Names::add, output, eq, in);
		if (outputs.isEmpty()) {
			//TODO what about user bound queries that do not refer to actual user?
			outputs = actor.contributesToOutputs;
			Names ignore = criteria.collect(Names.empty(), Name.class, Names::add, output, neq, nin);
			if (!ignore.isEmpty()) {
				for (Name n : ignore) { outputs = outputs.remove(n);	}
			}
		}
		if (outputs.isEmpty())
			return readyFuture(Matches.none()); // if no outputs are involved there cannot be any matches
		criteria = criteria.without(output);
		if (outputs.count() == 1) {
			Cache cache = cacheFor(outputs.first());
			if (cache == null)
				return readyFuture(Matches.none().inContext(Names.empty(), outputs, Names.empty())); // there was just 1 output but it was not cached yet
			return cache.matchesFor(actor, criteria);
		}
		final Criteria lookupCriteria = criteria;
		final Names lookupOutputs = outputs;
		return es.submit(() -> lookup(actor, lookupOutputs, lookupCriteria));
	}

	/**
	 * This is a multi-output lookup that fetches results from the individual
	 * output caches and then joins them to a single result.
	 *
	 * Like the lookup within a output this is ran by an
	 * {@link ExecutorService} so that {@link #matchesFor(User, Criteria)}
	 * returns without doing the actual work or waiting for other {@link Future}
	 * s.
	 */
	private Matches lookup(User actor, Names outputs, Criteria criteria) {
		Criteria filterCriteria = criteria.without(Property.order, Property.length, Property.offset);
		Names uncached = Names.empty();
		Names erroneous = Names.empty();
		Map<Name, Future<Matches>> futures = new LinkedHashMap<>();
		for (Name o : outputs) {
			Cache cache = cacheFor(o);
			if (cache == null) {
				uncached = uncached.add(o);
			} else {
				futures.put(o, cache.matchesFor(actor, filterCriteria));
			}
		}
		Names included = Names.empty();
		List<Matches> outputMatches = new ArrayList<>();
		for (java.util.Map.Entry<Name, Future<Matches>> o : futures.entrySet()) {
			try {
				outputMatches.add(o.getValue().get());
				included.add(o.getKey());
			} catch (Exception e) {
				erroneous.add(o.getKey());
			}
		}
		Task[] mergedMatches = outputMatches.isEmpty() ? new Task[0] : fold(map(outputMatches, m -> m.tasks));
		return CacheWorker.orderAndSlice(mergedMatches, criteria, cacheValidity.get()).inContext(included, uncached, erroneous);
	}

	@Override
	public Future<Void> invalidate(ChangeLog changes) {
		Iterator<Entry<?>> iter = changes.iterator();
		Name output = Name.ORIGIN;
		while (iter.hasNext() && output.isOrigin())
			output = iter.next().after.output();
		if (!output.isOrigin()) {
			Cache cache = cacheFor(output);
			if (cache != null)
				return cache.invalidate(changes);
		}
		return readyFuture(null);
	}

	private Cache cacheFor(Name output) {
		return outputCaches.get(output);
	}

	private static <T> Future<T> readyFuture(T res) {
		return CompletableFuture.completedFuture(res);
	}

}
