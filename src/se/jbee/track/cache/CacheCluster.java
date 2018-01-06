package se.jbee.track.cache;

import static se.jbee.track.model.Criteria.Operator.eq;
import static se.jbee.track.model.Criteria.Operator.in;
import static se.jbee.track.model.Criteria.Operator.neq;
import static se.jbee.track.model.Criteria.Operator.nin;
import static se.jbee.track.model.Criteria.Property.product;

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
import se.jbee.track.model.Date;
import se.jbee.track.model.Name;
import se.jbee.track.model.Names;
import se.jbee.track.model.Product;
import se.jbee.track.model.Task;
import se.jbee.track.model.User;
import se.jbee.track.model.Criteria.Property;

/**
 * A {@link CacheCluster} is a fully functional multi-{@link Product}
 * {@link Cache}.
 * 
 * It has an {@link ExecutorService} to run and merge cross-product queries.
 * Product-specific queries are delegated to a {@link CacheWorker}. Each
 * {@link CacheWorker} caches a specific product.
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
	private final Map<Name, Cache> productCaches = new ConcurrentHashMap<>();

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
		for (Cache c : productCaches.values()) c.shutdown();
		productCaches.clear();
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
				for (Cache cache : productCaches.values()) {
					cache.shutdown();
				}
				productCaches.clear();
			}
		}
		// might be a indexing request
		if (criteria.isIndexRequest()) {
			Name product = (Name) criteria.get(0).rvalues[0];
			if (productCaches.containsKey(product)) {
				return readyFuture(Matches.none());
			}
			Cache cache = productCaches.computeIfAbsent(product, (k) -> {
				return new CacheWorker(k, db, before);
			});
			return cache.matchesFor(inquirer, criteria.without(Property.product));
		}
		// lookup request
		Names products = criteria.collect(Names.empty(), Name.class, Names::add, product, eq, in);
		if (products.isEmpty()) {
			//TODO what about user bound queries that do not refer to actual user?
			products = inquirer.contributesToProducts;
			Names ignore = criteria.collect(Names.empty(), Name.class, Names::add, product, neq, nin);
			if (!ignore.isEmpty()) {
				for (Name n : ignore) { products = products.remove(n);	}
			}
		}
		if (products.isEmpty())
			return readyFuture(Matches.none()); // if no products are involved there cannot be any matches
		criteria = criteria.without(product);
		if (products.count() == 1) {
			Cache cache = productCaches.get(products.first());
			if (cache == null)
				return readyFuture(Matches.none().exlcuded(products)); // there was just 1 product but it was not cached yet
			return cache.matchesFor(inquirer, criteria);
		}
		final Criteria lookupCriteria = criteria;
		final Names lookupProducts = products;
		return es.submit(() -> lookup(inquirer, lookupProducts, lookupCriteria));
	}
	
	/**
	 * This is a multi-product lookup that fetches results from the individual
	 * product caches and then joins them to a single result.
	 * 
	 * Like the lookup within a product this is ran by an
	 * {@link ExecutorService} so that {@link #matchesFor(User, Criteria)}
	 * returns without doing the actual work or waiting for other {@link Future}
	 * s.
	 */
	private Matches lookup(User inquirer, Names products, Criteria criteria) {
		Criteria filterCriteria = criteria.without(Property.order, Property.length, Property.offset);
		Names uncached = Names.empty();
		List<Future<Matches>> futures = new ArrayList<>();
		for (Name p : products) {
			Cache cache = productCaches.get(p);
			if (cache == null) {
				uncached = uncached.add(p);
			} else {
				futures.add(cache.matchesFor(inquirer, filterCriteria));
			}
		}
		List<Matches> productMatches = new ArrayList<>();
		for (Future<Matches> f : futures) {
			try {
				productMatches.add(f.get());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		int len = 0;
		for (Matches m : productMatches) 
			len += m.tasks.length;
		Task[] tmp = new Task[len];
		int s = 0;
		for (Matches m : productMatches) {
			System.arraycopy(m.tasks, 0, tmp, s, m.tasks.length);
			s+=m.tasks.length;
		}
		return CacheWorker.orderAndSlice(tmp, criteria, cacheValidity.get()).exlcuded(uncached);
	}

	@Override
	public Future<Void> invalidate(Changes changes) {
		Iterator<Entry<?>> iter = changes.iterator();
		Name product = Name.ORIGIN;
		while (iter.hasNext() && product.isOrigin())
			product = iter.next().after.product();
		if (!product.isOrigin()) {
			Cache cache = productCaches.get(product);
			if (cache != null)
				return cache.invalidate(changes);
		}
		return readyFuture(null);
	}

	private static <T> Future<T> readyFuture(T res) {
		return CompletableFuture.completedFuture(res);
	}

}
