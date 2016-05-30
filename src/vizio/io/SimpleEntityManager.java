package vizio.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

import vizio.Area;
import vizio.Cluster;
import vizio.Date;
import vizio.IDN;
import vizio.Motive;
import vizio.Name;
import vizio.Names;
import vizio.Poll;
import vizio.Product;
import vizio.Purpose;
import vizio.Status;
import vizio.Task;
import vizio.User;
import vizio.Version;
import vizio.io.Criteria.Property;
import vizio.io.stream.AreaStreamer;
import vizio.io.stream.ClusterStreamer;
import vizio.io.stream.PollStreamer;
import vizio.io.stream.ProductStreamer;
import vizio.io.stream.TaskStreamer;
import vizio.io.stream.UserStreamer;
import vizio.io.stream.VersionStreamer;

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
public class SimpleEntityManager implements EntityManager {

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
	
	public SimpleEntityManager(File basePath) {
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

	@Override
	public User user(Name user) {
		return load(new UserStreamer(), userFile(user));
	}
	@Override
	public Poll poll(Name product, Name area, IDN serial) {
		return load(new PollStreamer(), pollFile(product, area, serial));
	}
	@Override
	public Product product(Name product) {
		return load(new ProductStreamer(), productFile(product));
	}
	@Override
	public Area area(Name product, Name area) {
		return load(new AreaStreamer(), areaFile(product, area));
	}
	@Override
	public Version version(Name product, Name version) {
		return load(new VersionStreamer(), versionFile(product, version));
	}
	@Override
	public Task task(Name product, IDN id) {
		return load(new TaskStreamer(), taskFile(product, id));
	}
	
	private <T> T load(Streamer<T> streamer, File file) {
		try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
			return streamer.read(in, this);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private <T> void store(T obj, Streamer<T> streamer, File file) {
		try (DataOutputStream out = new DataOutputStream(new FileOutputStream(file))) {
			streamer.write(obj, out);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void update(User user) {
		store(user, new UserStreamer(), userFile(user.name));
	}
	@Override
	public void update(Cluster cluster) {
		store(cluster, new ClusterStreamer(), cluserFile());
	}
	@Override
	public void update(Product product) {
		store(product, new ProductStreamer(), productFile(product.name));
	}
	@Override
	public void update(Version version) {
		store(version, new VersionStreamer(), versionFile(version.product, version.name));
	}
	@Override
	public void update(Area area) {
		store(area, new AreaStreamer(), areaFile(area.product, area.name));
	}
	@Override
	public void update(Task task) {
		store(task, new TaskStreamer(), taskFile(task.product.name, task.id));
	}
	@Override
	public void update(Poll poll) {
		store(poll, new PollStreamer(), pollFile(poll.area.product, poll.area.name, poll.serial));
	}
	
	private File cluserFile() {
		return new File(basePath, "__cluster__"+FILE_EXT);
	}
	private File userFile(Name name) {
		return new File(basePath, "__users__/"+name.toString()+FILE_EXT);
	}
	private File productFile(Name product) {
		return new File(basePath, product.toString()+FILE_EXT);
	}
	private File areaFile(Name product, Name area) {
		return new File(basePath, product.toString()+"/area/"+area.toString()+FILE_EXT);
	}
	private File versionFile(Name product, Name version) {
		return new File(basePath, product.toString()+"/version/"+version.toString()+FILE_EXT);
	}
	private File taskFile(Name product, IDN id) {
		return new File(basePath, product.toString()+"/task/"+id.toString()+FILE_EXT);
	}
	private File pollFile(Name produdct, Name area, IDN serial) {
		return new File(basePath, produdct.toString()+"/area/"+area.toString()+"/__poll__/"+serial.toString()+FILE_EXT);
	}
}
