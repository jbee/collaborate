package vizio.io;

import vizio.Motive;
import vizio.Name;
import vizio.Names;
import vizio.Purpose;
import vizio.Status;
import vizio.Task;

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
public class SimpleEntityManager {

	// Problems:
	// - update index consistently so that queries either see "old" or "new" data but nothing in-between
	// - update Task entities when Areas change
	// - queries that are not constrained by either product or user
	
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

	private Index<Name> idxProductVersion = Index.init();
	private Index<Name> idxProductArea = Index.init();
	private Index<Name> idxProductUser = Index.init();
	private Index<Motive> idxProductMotive = Index.init();
	private Index<Purpose> idxProductPurpose = Index.init();
	private Index<Status> idxProductStatus = Index.init();
	
	private Index<Status> idxEnlistingUsersStatus = Index.init();
	private Index<Status> idxApprochingUsersStatus = Index.init();
	private Index<Status> idxWatchingUsersStatus = Index.init();
}
