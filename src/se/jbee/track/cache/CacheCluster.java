package se.jbee.track.cache;

import static se.jbee.track.model.Criteria.Operator.eq;
import static se.jbee.track.model.Criteria.Operator.in;
import static se.jbee.track.model.Criteria.Operator.neq;
import static se.jbee.track.model.Criteria.Operator.nin;
import static se.jbee.track.model.Criteria.Property.output;

import java.util.ArrayList;
import java.util.Iterator;
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
	public void shutdown() {
		es.shutdown();
		for (Cache c : outputCaches.values()) c.shutdown();
		outputCaches.clear();
	}
	
	private Thread factory(Runnable target) {
		Thread t = new Thread(target);
		t.setDaemon(true);
		t.setName("cache-cluster");
		return t;
	}
	
	@Override
	public Future<Matches> matchesFor(User inquirer, Criteria criteria) {
		// throw away old caches
		Date before = cacheValidity.get();
		Date today = Date.date(clock.time());
		if (today.after(before)) {
			if (cacheValidity.compareAndSet(before, today)) { // make sure only one thread does the shutdown
				for (Cache cache : outputCaches.values()) {
					cache.shutdown();
				}
				outputCaches.clear();
			}
		}
		// might be a indexing request
		if (criteria.isIndexRequest()) {
			Name output = (Name) criteria.get(0).rvalues[0];
			if (outputCaches.containsKey(output)) {
				return readyFuture(Matches.none());
			}
			Cache cache = outputCaches.computeIfAbsent(output, (k) -> {
				return new CacheWorker(k, db, before);
			});
			return cache.matchesFor(inquirer, criteria.without(Property.output));
		}
		// lookup request
		Names outputs = criteria.collect(Names.empty(), Name.class, Names::add, output, eq, in);
		if (outputs.isEmpty()) {
			//TODO what about user bound queries that do not refer to actual user?
			outputs = inquirer.contributesToOutputs;
			Names ignore = criteria.collect(Names.empty(), Name.class, Names::add, output, neq, nin);
			if (!ignore.isEmpty()) {
				for (Name n : ignore) { outputs = outputs.remove(n);	}
			}
		}
		if (outputs.isEmpty())
			return readyFuture(Matches.none()); // if no outputs are involved there cannot be any matches
		criteria = criteria.without(output);
		if (outputs.count() == 1) {
			Cache cache = outputCaches.get(outputs.first());
			if (cache == null)
				return readyFuture(Matches.none().exlcuded(outputs)); // there was just 1 output but it was not cached yet
			return cache.matchesFor(inquirer, criteria);
		}
		final Criteria lookupCriteria = criteria;
		final Names lookupOutputs = outputs;
		return es.submit(() -> lookup(inquirer, lookupOutputs, lookupCriteria));
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
	private Matches lookup(User inquirer, Names outputs, Criteria criteria) {
		Criteria filterCriteria = criteria.without(Property.order, Property.length, Property.offset);
		Names uncached = Names.empty();
		List<Future<Matches>> futures = new ArrayList<>();
		for (Name p : outputs) {
			Cache cache = outputCaches.get(p);
			if (cache == null) {
				uncached = uncached.add(p);
			} else {
				futures.add(cache.matchesFor(inquirer, filterCriteria));
			}
		}
		List<Matches> outputMatches = new ArrayList<>();
		for (Future<Matches> f : futures) {
			try {
				outputMatches.add(f.get());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		int len = 0;
		for (Matches m : outputMatches) 
			len += m.tasks.length;
		Task[] tmp = new Task[len];
		int s = 0;
		for (Matches m : outputMatches) {
			System.arraycopy(m.tasks, 0, tmp, s, m.tasks.length);
			s+=m.tasks.length;
		}
		return CacheWorker.orderAndSlice(tmp, criteria, cacheValidity.get()).exlcuded(uncached);
	}

	@Override
	public Future<Void> invalidate(Changes changes) {
		Iterator<Entry<?>> iter = changes.iterator();
		Name output = Name.ORIGIN;
		while (iter.hasNext() && output.isOrigin())
			output = iter.next().after.output();
		if (!output.isOrigin()) {
			Cache cache = outputCaches.get(output);
			if (cache != null)
				return cache.invalidate(changes);
		}
		return readyFuture(null);
	}

	private static <T> Future<T> readyFuture(T res) {
		return CompletableFuture.completedFuture(res);
	}

}
