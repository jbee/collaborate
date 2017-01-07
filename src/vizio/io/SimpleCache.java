package vizio.io;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

import vizio.io.Criteria.Property;
import vizio.model.Date;
import vizio.model.Motive;
import vizio.model.Name;
import vizio.model.Names;
import vizio.model.Purpose;
import vizio.model.Status;
import vizio.model.Task;

/**
 * Paths:
 * <pre>
 * Product /<product>/product.dat
 * Area    /<product>/area/<area>.dat
 * Poll    /<product>/poll/<area>/<matter>/<affected>.dat
 * Version /<product>/version/<version>.dat
 * Task    /<product>/task/<IDN>.dat
 * </pre>
 *
 * @author jan
 */
public class SimpleCache implements Cache {

	// Problems:
	// - update index consistently so that queries either see "old" or "new" data but nothing in-between
	// - update Task entities when Areas change
	// - queries that are not constrained by either product or user

	private static final String FILE_EXT = ".dat";

	static interface Key<K> {
		K key(Task task);
	}

	static final Key<Name> product       = (Task task) -> task.product.name;
	static final Key<Name> area          = (Task task) -> task.area.name;
	static final Key<Name> version       = (Task task) -> task.base.name;
	static final Key<Names> enlistedBy   = (Task task) -> task.enlistedBy;
	static final Key<Names> approachedBy = (Task task) -> task.approachedBy;
	static final Key<Names> watchedBy    = (Task task) -> task.watchedBy;
	static final Key<Status> status      = (Task task) -> task.status;
	static final Key<Motive> motive      = (Task task) -> task.motive;
	static final Key<Purpose> purpose    = (Task task) -> task.purpose;

	private final File basePath;

	private Index<Name> idxProductVersion = Index.init();
	private Index<Name> idxProductArea = Index.init();
	private Index<Name> idxProductUser = Index.init();
	private Index<Motive> idxProductMotive = Index.init();
	private Index<Purpose> idxProductPurpose = Index.init();
	private Index<Status> idxProductStatus = Index.init();

	private Index<Status> idxEnlistingUsersStatus = Index.init();
	private Index<Status> idxApprochingUsersStatus = Index.init();
	private Index<Status> idxWatchingUsersStatus = Index.init();

	public SimpleCache(File basePath) {
		super();
		this.basePath = basePath;
	}

	@Override
	public Task[] tasks(Criteria criteria) {
		Date today = Date.today();
		// 1. select index(es)
		// 2. filter (collect results)
		// 1+2 are intertwined as multiple indexes might be used by we use one at a time until we have "enough" results.
		Task[] res = new Task[criteria.range.length()];
		// 3. sort
		sort(res, criteria.orders, today);
		return null;
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
