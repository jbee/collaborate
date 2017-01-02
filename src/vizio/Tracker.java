package vizio;

import static java.lang.Math.min;
import static vizio.Date.date;
import static vizio.Motive.defect;
import static vizio.Motive.intention;
import static vizio.Motive.proposal;
import static vizio.Name.limit;
import static vizio.Outcome.consent;
import static vizio.Outcome.dissent;
import static vizio.Poll.Matter.participation;
import static vizio.Purpose.clarification;
import static vizio.Status.absolved;
import static vizio.Status.dissolved;
import static vizio.Status.resolved;
import static vizio.Status.unsolved;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import vizio.Poll.Matter;

/**
 * Implementation of the tracker-business logic.
 *
 * @author jan
 */
public final class Tracker {

	/**
	 * The most global limit for changes of non-user entities.
	 */
	private static final Name LIMIT_EXTENDED = limit("extended");

	/**
	 * The most global limit for changes of user entities.
	 */
	private static final Name LIMIT_REGISTERED = limit("registered");


	private final Clock clock;
	private final Limits limits;

	public Tracker(Clock clock, Limits limits) {
		super();
		this.clock = clock;
		this.limits = limits;
	}

	/* Users + Accounts */

	public User register(Name name, String email, String unsaltedMd5, String salt) {
		expectExternal(name);
		long now = clock.time();
		approachNewUser(now);
		User user = new User();
		user.name = name;
		user.email = email;
		user.md5 = md5(unsaltedMd5+salt); // also acts as activationKey
		user.sites = new Site[0];
		user.watches = new AtomicInteger(0);
		touch(user);
		return user;
	}

	public void activate(User user, byte[] activationKey) {
		expectNotActivated(user);
		if (!Arrays.equals(user.md5, activationKey)) {
			denyTransition("Wrong activation key");
		}
		user.activated = true;
	}

	public void login(User user, String plainPass, String salt) {
		if (!Arrays.equals(md5(md5(plainPass)+salt), user.md5)) {
			denyTransition("Wrong passphrase!");
		}
	}

	public static byte[] md5(String pass) {
		try {
			return MessageDigest.getInstance("MD5").digest(pass.getBytes("UTF-8"));
		} catch (Exception e) {
			denyTransition(e.getMessage());
			return null;
		}
	}

	private void touch(User user) {
		user.millisLastActive = clock.time();
	}

	public static boolean canView(User user, Task task) {
		return !task.exploitable || user.name.equalTo(task.reporter) || task.area.maintainers.contains(user);
	}

	/* Products */

	public Product initiate(Name product, User originator) {
		expectRegistered(originator);
		expectExternal(product);
		long now = clock.time();
		approachNewProduct(now);
		approachNewEntity(now);
		Product p = new Product();
		p.name = product;
		p.tasks = new AtomicInteger(0);
		p.origin = compart(p.name, Name.ORIGIN, originator);
		p.somewhere = compart(p.name, Name.UNKNOWN, originator);
		p.somewhen = tag(p, Name.UNKNOWN, originator);
		return p;
	}

	/* Areas */

	public Area open(Product product, Name entrance, User originator, Motive motive, Purpose purpose) {
		Area area = compart(product, entrance, originator);
		area.entrance=true;
		area.motive=motive;
		area.purpose=purpose;
		return area;
	}

	public Area compart(Product product, Name area, User originator) {
		expectOriginMaintainer(product, originator);
		return compart(product.name, area, originator);
	}

	public Area compart(Area basis, Name partition, User originator, boolean subarea) {
		expectMaintainer(basis, originator);
		Area area = compart(basis.product, partition, originator);
		area.basis=basis.name;
		if (subarea) {
			area.maintainers = basis.maintainers;
		}
		return area;
	}

	private Area compart(Name product, Name area, User originator) {
		expectRegistered(originator);
		expectExternal(area);
		long now = clock.time();
		approachNewArea(product, now);
		approachNewEntity(now);
		Area a = new Area();
		a.name = area;
		a.product = product;
		a.maintainers=new Names(originator.name);
		a.tasks = new AtomicInteger(0);
		a.polls = new AtomicInteger(0);
		touch(originator);
		return a;
	}

	public void leave(Area area, User maintainer) {
		if (area.maintainers.contains(maintainer)) {
			area.maintainers.remove(maintainer);
			touch(maintainer);
			// NB: votes cannot 'get stuck' as voter can change their vote until a vote is settled
		}
	}

	public void relocate(Task task, Area to, User originator) {
		expectNoEntrance(task.area);
		if (task.area.name.isUnknown()) {
			expectMaintainer(to, originator); // pull from ~
		} else {
			expectMaintainer(task.area, originator); // push from
			if (!to.name.isUnknown()) {
				expectMaintainer(to, originator); // to some else than ~
			}
		}
		task.area = to;
		touch(originator);
	}

	/* Versions */

	public Version tag(Product product, Name version, User originator) {
		expectRegistered(originator);
		expectExternal(version);
		expectOriginMaintainer(product, originator);
		long now = clock.time();
		approachNewVersion(product, now);
		approachNewEntity(now);
		Version v = new Version();
		v.product = product.name;
		v.name = version;
		v.changeset = Names.empty();
		return v;
	}

	/* Tasks */

	public Task reportProposal(Product product, String gist, User reporter, Area area) {
		expectNoEntrance(area);
		return report(product, proposal, clarification, gist, reporter, area, product.somewhen, false);
	}

	public Task reportIntention(Product product, String gist, User reporter, Area area) {
		expectNoEntrance(area);
		return report(product, intention, clarification, gist, reporter, area, product.somewhen, false);
	}

	public Task reportDefect(Product product, String gist, User reporter, Area area, Version version, boolean exploitable) {
		expectNoEntrance(area);
		return report(product, defect, clarification, gist, reporter, area, version, exploitable);
	}

	public Task reportRequest(Product product, String gist, User reporter, Area entrance) {
		expectEntrance(entrance);
		return report(product, entrance.motive, entrance.purpose, gist, reporter, entrance, product.somewhen, false);
	}

	public Task reportSequel(Task cause, Purpose purpose, String gist, User reporter, Names changeset) {
		Area area = cause.area.entrance ? cause.product.somewhere : cause.area;
		Task task = report(cause.product, cause.motive, purpose, gist, reporter, area, cause.base, cause.exploitable);
		task.cause = cause.id;
		task.origin = cause.origin != null ? cause.origin : cause.id;
		if (changeset != null && !changeset.isEmpty()) {
			expectNoChangeset(task.base);
			task.changeset = changeset;
		}
		return task;
	}

	private Task report(Product product, Motive motive, Purpose purpose, String gist, User reporter, Area area, Version version, boolean exploitable) {
		if (!area.name.isUnknown() && !area.entrance) { // NB. unknown is not an entrance since it does not dictate motive and goal
			expectMaintainer(area, reporter);
		}
		expectCanReport(reporter);
		long now = clock.time();
		approachNewTask(product, reporter, now);
		Task task = new Task();
		task.id = new IDN(product.tasks.incrementAndGet());
		task.product = product;
		task.area = area;
		task.base = version;
		task.reporter = reporter.name;
		task.start = date(now);
		task.gist = gist;
		task.motive = motive;
		task.purpose = purpose;
		task.status = Status.unsolved;
		task.exploitable = exploitable;
		task.enlistedBy = Names.empty();
		task.approachedBy = Names.empty();
		task.watchedBy = new Names(reporter.name);
		task.changeset = Names.empty();
		if (area.entrance) {
			task.serial=new IDN(area.tasks.incrementAndGet());
		}
		touch(reporter);
		return task;
	}

	/* task resolution */

	public void absolve(Task task, User user, String conclusion) {
		if (!task.area.name.isUnknown()) { // no change is a resolution even if no area has been specified before
			expectMaintainer(task.area, user);
		}
		solve(task, user, conclusion);
		task.status = absolved;
		user.absolved++;
		touch(user);
	}

	public void resolve(Task task, User user, String conclusion) {
		expectMaintainer(task.area, user);
		solve(task, user, conclusion);
		task.status = resolved;
		user.xp += 2;
		user.resolved++;
		touch(user);
		if (!task.changeset.isEmpty()) { // publishing is something that is resolved
			task.base.changeset = task.changeset;
		}
	}

	public void dissolve(Task task, User user, String conclusion) {
		expectMaintainer(task.area, user);
		expectUnsolved(task);
		solve(task, user, conclusion);
		task.status = dissolved;
		user.xp += 5;
		user.dissolved++;
		touch(user);
	}

	private void solve(Task task, User user, String conclusion) {
		expectUnsolved(task);
		task.solver = user.name;
		task.end = date(clock.time());
		task.conclusion = conclusion;
	}

	/* User voting */

	public void stress(Task task, User voter) {
		long now = clock.time();
		if (voter.canStress(now) && task.canBeStressedBy(voter.name)) {
			voter.stressed(now);
			task.heatUp(date(now));
			touch(voter);
		}
	}

	public Poll poll(Matter matter, Area area, User initiator, User affected) {
		if (matter != participation) {
			expectMaintainer(area, initiator);
		}
		long now = clock.time();
		approachNewPoll(area, initiator, now);
		approachNewEntity(now);
		Poll poll = new Poll();
		poll.serial = new IDN(area.polls.incrementAndGet());
		poll.matter = matter;
		poll.area = area;
		poll.initiator = initiator.name;
		poll.affected = affected;
		poll.start = date(clock.time());
		poll.outcome = Outcome.unsettled;
		poll.consenting = Names.empty();
		poll.dissenting = Names.empty();
		poll.expiry = poll.start.plusDays(min(14, area.maintainers.count()));
		touch(initiator);
		return poll;
	}

	public void consent(Poll poll, User voter) {
		vote(poll, voter, poll.dissenting, poll.consenting);
	}

	public void dissent(Poll poll, User voter) {
		vote(poll, voter, poll.consenting, poll.dissenting);
	}

	private void vote(Poll poll, User voter, Names removed, Names added) {
		if (poll.canVote(voter.name)) {
			approachNewVote(poll, voter, clock.time());
			removed.remove(voter);
			added.add(voter);
			touch(voter);
			if (poll.isSettled()) {
				settle(poll);
			}
		}
	}

	private void settle(Poll poll) {
		poll.end = date(clock.time());
		boolean accepted = poll.isAccepted();
		poll.outcome = accepted ? consent : dissent;
		if (!accepted)
			return;
		switch (poll.matter) {
		case inclusion:
			poll.area.exclusive=false; break;
		case exclusion:
			poll.area.exclusive=true; break;
		case resignation:
			poll.area.maintainers.remove(poll.affected); break;
		case participation:
			poll.area.maintainers.add(poll.affected);
		}
	}

	/* A user's task queue */

	public void enlist(Task task, User user) {
		expectCanBeInvolved(user, task);
		approachQueue(task, user, clock.time());
		task.approachedBy.remove(user);
		task.enlistedBy.add(user);
		touch(user);
	}

	public void abandon(Task task, User user) {
		expectCanBeInvolved(user, task);
		task.enlistedBy.remove(user);
		task.approachedBy.remove(user);
		touch(user);
	}

	public void approach(Task task, User user) {
		expectCanBeInvolved(user, task);
		expectMaintainer(task.area, user);
		task.approachedBy.add(user);
		task.enlistedBy.remove(user);
		touch(user);
	}

	/* A user's watch list */

	public void watch(Task task, User user) {
		if (!task.watchedBy.contains(user)) {
			expectCanWatch(user);
			task.watchedBy.add(user);
			user.watches.incrementAndGet();
			touch(user);
		}
	}

	public void unwatch(Task task, User user) {
		if (task.watchedBy.contains(user)) {
			task.watchedBy.remove(user);
			user.watches.decrementAndGet();
			touch(user);
		}
	}

	/* A user's sites */

	public Site launch(Name site, String template, User owner) {
		expectNoUserSiteYet(site, owner);
		expectCanHaveMoreSites(owner);
		Site s = new Site(owner.name, site, template);
		owner.sites = Arrays.copyOf(owner.sites, owner.sites.length+1);
		owner.sites[owner.sites.length-1] = s;
		touch(owner);
		return s;
	}

	public void update(Site site, String template, User initiator) {
		expectOwner(site, initiator);
		site.template = template;
		touch(initiator);
	}

	/* limit checks */

	private void approachNewUser(long now) {
		approachLimit(LIMIT_REGISTERED, now,"Too many users registered lately.");
	}

	private void approachNewEntity(long now) {
		approachLimit(LIMIT_EXTENDED, now, "Too many new entities.");
	}

	private void approachNewProduct(long now) {
		approachLimit(limit("x-product"), now, "Too many new products.");
	}

	private void approachNewArea(Name product, long now) {
		approachLimit(limit("product-area", product), now, "Too many new areas for product: "+product);
		approachLimit(limit("x-area"), now, "Too many new areas.");
	}

	private void approachNewVersion(Product product, long now) {
		approachLimit(limit("product-version", product.name), now, "Too many new versions for product: "+product.name);
		approachLimit(limit("x-version"), now, "Too many new versions.");
	}

	private void approachNewTask(Product product, User reporter, long now) {
		if (reporter.name.isInternal()) {
			approachLimit(limit("x-task", product.name), now, "Too many anonymous task reports.");
		} else {
			approachLimit(limit("user-task", reporter.name), now, "Too many new task by user: "+reporter.name);
		}
		approachLimit(limit("product-task", product.name), now, "Too many new task for product: "+product.name);
	}

	private void approachNewPoll(Area area, User initiator, long now) {
		approachLimit(limit("user-poll", initiator.name), now, "Too many new polls by user: "+initiator.name);
		approachLimit(limit("area-poll", area.name), now, "Too many new polls in area: "+area.name);
		approachLimit(limit("x-poll"), now, "Too many new polls.");
	}

	private void approachNewVote(Poll poll, User voter, long now) {
		approachLimit(limit("user-vote", voter.name), now, "Too many recent votes by user: "+voter.name);
		approachLimit(limit("poll-vote", voter.name), now, "Too many recent votes in poll: "+poll.matter+" "+poll.affected);
		approachLimit(limit("x-vote"), now, "Too many votes recently.");
	}

	private void approachQueue(Task task, User user, long now) {
		approachLimit(limit("user-queue"), now, "Too many queue activities by user: "+user.name);
		approachLimit(limit("task-queue"), now, "Too many queue activities for task: "+task.id);
		approachLimit(limit("x-queue"), now, "Too many queue activities recently.");
	}

	private void approachLimit(Name limit, long now, String error) {
		if (!limits.approach(limit, now)) {
			denyTransition("Limit exceeded! "+error+" Please try again later!");
		}
	}

	/* consistency rules */

	private static void expectNoEntrance(Area area) {
		if (area.entrance) {
			denyTransition("Use request to submit to entrance areas!");
		}
	}

	private static void expectEntrance(Area area) {
		if (!area.entrance) {
			denyTransition("Request must be submitted within an entrance area!");
		}
	}

	private static void expectNoChangeset(Version version) {
		if (version.changeset.count() > 0) {
			denyTransition("This version is already released");
		}
	}

	private static void expectCanHaveMoreSites(User owner) {
		if (owner.sites.length >= 10) {
			denyTransition("Currently each user can only have 10 sites!");
		}
	}

	private static void expectOwner(Site site, User initiator) {
		if (!site.owner.equalTo(initiator.name)) {
			denyTransition("Only a site's owner can update it!");
		}
	}

	private static void expectNoUserSiteYet(Name site, User owner) {
		if (owner.hasSite(site)) {
			denyTransition("Site already exists!");
		}
	}

	private static void expectOriginMaintainer(Product product, User user) {
		if (!product.origin.maintainers.contains(user.name)) {
			denyTransition("Only maintainers of area '*' can initiate new areas and versions.");
		}
	}

	private static void expectRegistered(User user) {
		if (user.name.isInternal()) { // a anonymous user
			denyTransition("Only registered users can create products and areas!");
		}
	}

	private static void expectCanReport(User reporter) {
		if (!reporter.activated) {
			denyTransition("Only activated users can report tasks!");
		}
	}

	private static void expectCanBeInvolved(User user, Task task) {
		if (task.involvedUsers() >= 5 && !task.enlistedBy.contains(user) && !task.approachedBy.contains(user)) {
			denyTransition("There are already to much users involved with the task: "+task);
		}
	}

	private static void expectMaintainer(Area area, User user) {
		if (!area.maintainers.contains(user)) {
			denyTransition("Only maintainers of an area may assign that area to a task (pull).");
		}
	}

	private static void expectUnsolved(Task task) {
		if (task.status != unsolved) {
			denyTransition("Cannot change the outcome of a task once it is concluded!");
		}
	}

	private static void expectExternal(Name name) {
		if (name.isInternal()) {
			denyTransition("A registered user's name must not use '@' and be shorter than 17 characters!");
		}
	}

	private static void expectNotActivated(User user) {
		if (user.activated) {
			denyTransition("User account already activated!");
		}
	}

	private static void expectCanWatch(User user) {
		if (!user.activated) {
			denyTransition("Only active users can watch");
		}
		if (!user.canWatch()) {
			denyTransition("User has reached maximum number of watched tasks. Unwatch tasks or increase limit by closing tasks.");
		}
	}

	private static void denyTransition(String reason) {
		throw new IllegalStateException(reason);
	}
}
