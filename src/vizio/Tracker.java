package vizio;

import static vizio.Date.date;
import static vizio.Goal.clarification;
import static vizio.Motive.defect;
import static vizio.Motive.intention;
import static vizio.Motive.proposal;
import static vizio.Outcome.consent;
import static vizio.Outcome.dissent;
import static vizio.Poll.Matter.participation;
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

	private final Cluster cluster;
	private final Clock clock;

	public Tracker(Clock clock) {
		super();
		this.clock = clock;
		this.cluster = new Cluster();
	}

	/* Cluster */

	private void extend(Cluster cluster, User originator) {
		long now = clock.time();
		expectExtendable(cluster, now);
		expectRegistered(originator);
		cluster.extended(now);
	}

	/* Users + Accounts */

	public User register(Name name, String email, String unsaltedMd5) {
		expectExternal(name);
		long now = clock.time();
		expectCanRegister(cluster, now);
		cluster.registered(now);
		User user = new User();
		user.name = name;
		user.email = email;
		user.md5 = md5(unsaltedMd5+cluster.salt); // also acts as activationKey
		user.sites = Names.empty();
		touch(user);
		return user;
	}

	public void activate(User user, byte[] activationKey) {
		expectNotActivated(user);
		if (!Arrays.equals(user.md5, activationKey)) {
			denyTransition("Wrong activation key");
		}
		user.activated = true;
		cluster.unconfirmedRegistrationsToday--;
	}

	public void login(User user, String plainPass) {
		if (!Arrays.equals(md5(md5(plainPass)+cluster.salt), user.md5)) {
			denyTransition("Wrong passphrase!");
		}
	}

	private byte[] md5(String pass) {
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
		extend(cluster, originator);
		expectExternal(product);
		Product p = new Product();
		p.name = product;
		p.tasks = new AtomicInteger(0);
		p.origin = compart(p.name, Name.ORIGIN, originator);
		p.somewhere = compart(p.name, Name.UNKNOWN, originator);
		p.somewhen = tag(p, Name.UNKNOWN, originator);
		return p;
	}

	/* Areas */

	public Area open(Product product, Name entrance, User originator, Motive motive, Goal goal) {
		extend(cluster, originator);
		expectOriginMaintainer(product, originator);
		Area area = compart(product, entrance, originator);
		area.entrance=true;
		area.tasks = new AtomicInteger(0);
		area.motive=motive;
		area.goal=goal;
		return area;
	}

	public Area compart(Product product, Name area, User originator) {
		extend(cluster, originator);
		expectOriginMaintainer(product, originator);
		return compart(product.name, area, originator);
	}

	public Area compart(Area basis, Name partition, User originator, boolean subarea) {
		extend(cluster, originator);
		expectMaintainer(basis, originator);
		Area area = compart(basis.product, partition, originator);
		area.basis=basis.name;
		if (subarea) {
			area.maintainers = basis.maintainers;
		}
		return area;
	}

	private Area compart(Name product, Name area, User originator) {
		expectExternal(area);
		Area a = new Area();
		a.name = area;
		a.product = product;
		a.maintainers=new Names(originator.name);
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
		if (task.area.name.isUnknown()) {
			expectMaintainer(to, originator);
		} else {
			expectMaintainer(task.area, originator);
			if (!to.name.isUnknown()) {
				expectMaintainer(to, originator);
			}
		}
		task.area = to;
		touch(originator);
	}

	/* Versions */

	public Version tag(Product product, Name version, User originator) {
		extend(cluster, originator);
		expectExternal(version);
		expectOriginMaintainer(product, originator);
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
		return report(product, entrance.motive, entrance.goal, gist, reporter, entrance, product.somewhen, false);
	}

	public Task reportSequel(Task cause, Goal goal, String gist, User reporter, Names changeset) {
		Area area = cause.area.entrance ? cause.product.somewhere : cause.area;
		Task task = report(cause.product, cause.motive, goal, gist, reporter, area, cause.version, cause.exploitable);
		task.cause = cause.id;
		task.origin = cause.origin != null ? cause.origin : cause.id;
		if (changeset != null && changeset.count() > 0) {
			expectNoChangeset(task.version);
			task.changeset = changeset;
		}
		return task;
	}

	private Task report(Product product, Motive motive, Goal goal, String gist, User reporter, Area area, Version version, boolean exploitable) {
		if (!area.name.isUnknown() && !area.entrance) { // NB. unknown is not an entrance since it does not dictate motive and goal
			expectMaintainer(area, reporter);
		}
		long now = clock.time();
		if (reporter.name.isInternal()) {
			expectCanReportAnonymously(product);
			product.unconfirmedTasks.incrementAndGet();
		}
		expectCanReport(reporter, now);
		reporter.reported(now);
		Task task = new Task();
		task.id = new IDN(product.tasks.incrementAndGet());
		task.product = product;
		task.area = area;
		task.version = version;
		task.reporter = reporter.name;
		task.start = date(now);
		task.gist = gist;
		task.motive = motive;
		task.goal = goal;
		task.status = Status.unsolved;
		task.exploitable = exploitable;
		task.confirmed = task.reporter.isExternal();
		task.targetedBy = Names.empty();
		task.approachedBy = Names.empty();
		if (area.entrance) {
			task.serial=new IDN(area.tasks.incrementAndGet());
		}
		touch(reporter);
		return task;
	}

	public void confirm(Product product, Task task) {
		task.confirmed = true;
		product.unconfirmedTasks.decrementAndGet();
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
		if (task.changeset != null) { // publishing is something that is resolved
			task.version.changeset = task.changeset;
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
			task.heat(date(now));
			touch(voter);
		}
	}

	public Poll poll(Matter matter, Area area, User initiator, User affected) {
		if (matter != participation) {
			expectMaintainer(area, initiator);
		}
		Poll poll = new Poll();
		poll.matter = matter;
		poll.area = area;
		poll.initiator = initiator.name;
		poll.affected = affected;
		poll.start = date(clock.time());
		poll.outcome = Outcome.unsettled;
		poll.consenting = Names.empty();
		poll.dissenting = Names.empty();
		poll.expiry = poll.start.plusDays(area.maintainers.count());
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

	public void target(Task task, User user) {
		expectCanBeConnected(user, task);
		task.approachedBy.remove(user);
		task.targetedBy.add(user);
		touch(user);
	}

	public void abandon(Task task, User user) {
		expectCanBeConnected(user, task);
		task.targetedBy.remove(user);
		task.approachedBy.remove(user);
		touch(user);
	}

	public void approach(Task task, User user) {
		expectCanBeConnected(user, task);
		expectMaintainer(task.area, user);
		task.approachedBy.add(user);
		task.targetedBy.remove(user);
		touch(user);
	}

	/* A user's sites */

	public Site launch(Name site, String template, User owner) {
		expectNoUserSiteYet(site, owner);
		expectCanHaveMoreSites(owner);
		Site s = new Site(owner.name, site, template);
		owner.sites.add(site);
		touch(owner);
		return s;
	}

	public void update(Site site, String template, User initiator) {
		expectOwner(site, initiator);
		site.template = template;
		touch(initiator);
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
		if (owner.sites.count() >= 10) {
			denyTransition("Currently each user can only have 10 sites!");
		}
	}

	private static void expectOwner(Site site, User initiator) {
		if (!site.owner.equalTo(initiator.name)) {
			denyTransition("Only a site's owner can update it!");
		}
	}

	private static void expectNoUserSiteYet(Name site, User owner) {
		if (owner.sites.contains(site)) {
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

	private static void expectExtendable(Cluster cluster, long now) {
		if (!cluster.canExtend(now)) {
			denyTransition("To many new products and areas in last 24h! Wait until tomorrow.");
		}
	}

	private static void expectCanReport(User reporter, long now) {
		if (!reporter.canReport(now)) {
			denyTransition("User cannot report due to abuse protection limits!");
		}
	}

	private static void expectCanBeConnected(User user, Task task) {
		if (task.users() >= 5 && !task.targetedBy.contains(user) && !task.approachedBy.contains(user)) {
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

	private static void expectCanReportAnonymously(Product product) {
		if (!product.allowsAnonymousReports()) {
			denyTransition("To many unconfirmed anonymous reports. Try again later.");
		}
	}

	private static void expectExternal(Name name) {
		if (name.isInternal()) {
			denyTransition("A registered user's name must not use '@' and be shorter than 17 characters!");
		}
	}

	private static void expectCanRegister(Cluster cluster, long now) {
		if (!cluster.canRegister(now)) {
			denyTransition("To many unconfirmed accounts created today. Please try again tomorrow!");
		}
	}

	private static void expectNotActivated(User user) {
		if (user.activated) {
			denyTransition("User account already activated!");
		}
	}

	private static void denyTransition(String reason) {
		throw new IllegalStateException(reason);
	}
}
