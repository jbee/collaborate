package se.jbee.track.cache;

import static se.jbee.track.cache.Criteria.Operator.eq;
import static se.jbee.track.cache.Criteria.Operator.in;
import static se.jbee.track.cache.Criteria.Operator.neq;
import static se.jbee.track.cache.Criteria.Operator.nin;
import static se.jbee.track.cache.Criteria.Property.product;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import se.jbee.track.engine.Changes;
import se.jbee.track.engine.Changes.Entry;
import se.jbee.track.model.Name;
import se.jbee.track.model.Names;


public class CacheCluster implements Cache {

	/*
	 * The cluster has a Cache for each product. The request is delegated.
	 * 
	 * If a query is not limited to a product but by a user the user is used to
	 * see what products the user works with. All product caches for the user
	 * are asked. If a product has not yet been indexed it will not be included
	 * in the result. Instead the result will flag that those products are
	 * excluded so far. The user can chose to trigger indexing explicitly.
	 * Sorting will be removed when delegating since all results are merged and
	 * sorted here.
	 * 
	 * Product caches time out. 
	 * Meta data is kept to see when a product was last used.
	 * If a product is not used for X minutes it is freed. 
	 */
	
	private final Map<Name, Cache> productCaches = new ConcurrentHashMap<>();
	
	@Override
	public Future<Tasks> tasks(Criteria constraints) {
		Names included = constraints.collect(Names.empty(), Name.class, Names::add, product, eq, in);
		Names excluded = constraints.collect(Names.empty(), Name.class, Names::add, product, neq, nin);
		if (included.isEmpty())
			return readyFuture(Tasks.NONE);
		if (included.count() == 1) {
			Cache cache = productCaches.get(included.iterator().next());
			if (cache == null)
				return readyFuture(new Tasks(Tasks.NONE.matches, 0, included));
			return cache.tasks(constraints);
		}
		//TODO	
		
		return null;
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
		FutureTask<T> t = new FutureTask<>(() -> { return res; });
		t.run();
		return t;
	}

}