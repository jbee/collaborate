package se.jbee.track.cache;

import static se.jbee.track.model.Criteria.Operator.eq;
import static se.jbee.track.model.Criteria.Operator.in;
import static se.jbee.track.model.Criteria.Operator.neq;
import static se.jbee.track.model.Criteria.Operator.nin;
import static se.jbee.track.model.Criteria.Property.output;
import static se.jbee.track.util.Array.fold;
import static se.jbee.track.util.Array.map;

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

import se.jbee.track.db.DB;
import se.jbee.track.engine.Changes;
import se.jbee.track.engine.Changes.Entry;
import se.jbee.track.engine.Clock;
import se.jbee.track.model.Criteria;
import se.jbee.track.model.Criteria.Property;
import se.jbee.track.model.Date;
import se.jbee.track.model.Name;
import se.jbee.track.model.Names;
import se.jbee.track.model.Output;
import se.jbee.track.model.Task;
import se.jbee.track.model.User;

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
 *
 * @author jan
 */
public class CacheCluster implements Cache {

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
				return readyFuture(Matches.none().exlcuded(outputs)); // there was just 1 output but it was not cached yet
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
		List<Matches> outputMatches = new ArrayList<>();
		for (java.util.Map.Entry<Name, Future<Matches>> o : futures.entrySet()) {
			try {
				outputMatches.add(o.getValue().get());
			} catch (Exception e) {
				erroneous.add(o.getKey());
			}
		}
		Task[] mergedMatches = outputMatches.isEmpty() ? new Task[0] : fold(map(outputMatches, m -> m.tasks));
		//TODO add erroneous
		return CacheWorker.orderAndSlice(mergedMatches, criteria, cacheValidity.get()).exlcuded(uncached);
	}

	@Override
	public Future<Void> invalidate(Changes changes) {
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
