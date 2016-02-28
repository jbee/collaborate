package vizio;

import static vizio.Date.date;
import static vizio.Goal.clarification;
import static vizio.Motive.defect;
import static vizio.Motive.idea;
import static vizio.Motive.proposal;
import static vizio.Status.absolved;
import static vizio.Status.dissolved;
import static vizio.Status.resolved;
import static vizio.Status.unsolved;

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

	public User register(String email) {
		User user = new User();
		user.email = email;
		touch(user);
		return user;
	}

	private void touch(User user) {
		user.lastActive = date(clock.time());
	}

	/* User created entities */

	public Product introduce(Name product, User initiator) {
		extendCluster(initiator);
		Product p = new Product();
		p.name = product;
		touch(initiator);
		return p;
	}
	
	public Area structure(Name product, Name area, User initiator) {
		extendCluster(initiator);
		Area a = new Area();
		a.name = area;
		a.product = product;
		a.maintainers=new Names(initiator.name);
		touch(initiator);
		return a;
	}

	private void extendCluster(User initiator) {
		long now = clock.time();
		ensureExtendable(cluster, now);
		ensureRegisteredUser(initiator);
		cluster.extended(now);
	}

	public Task reportIdea(Product product, String summay, User reporter, Area area) {
		return report(product, idea, summay, reporter, area, null, false);
	}

	public Task reportProposal(Product product, String summay, User reporter, Area area) {
		return report(product, proposal, summay, reporter, area, null, false);
	}
	
	public Task reportDefect(Product product, String summay, User reporter, Area area, Version version, boolean exploitable) {
		return report(product, defect, summay, reporter, area, version, exploitable);
	}

	private Task report(Product product, Motive motive, String summay, User reporter, Area area, Version version, boolean exploitable) {
		if (area != null) {
			ensureIsMaintainer(area, reporter);
		}
		long now = clock.time();
		if (reporter.name.isInternal()) {
			ensureCanReportAnonymously(product);
			product.unconfirmedTasks++;
		}
		ensureCanReport(reporter, now);
		reporter.reports(now);
		Task task = new Task();
		product.tasks++;
		task.id = new IDN(product.tasks);
		task.product = product.name;
		task.area = area;
		task.version = version != null ? version.name : null;
		task.reporter = reporter.name;
		task.start = date(now);
		task.summary = summay;
		task.motive = motive;
		task.goal = clarification;
		task.status = Status.unsolved;
		task.exploitable = exploitable;
		task.confirmed = task.reporter.isExternal();
		task.usersMarked = Names.empty();
		task.usersStarted = Names.empty();
		touch(reporter);
		return task;
	}

	public void confirm(Product product, Task task) {
		task.confirmed = true;
		product.unconfirmedTasks--;
	}

	/* User initiated entity changes */

	public void relocate(Task task, Area to, User initiator) {
		if (task.area == null) {
			ensureIsMaintainer(to, initiator);
		} else {
			ensureIsMaintainer(task.area, initiator);
			if (to != null) {
				ensureIsMaintainer(to, initiator);
			}
		}
		task.area = to;
		touch(initiator);
	}

	public void leave(Area area, User maintainer) {
		if (area.maintainers.contains(maintainer)) {
			area.maintainers.remove(maintainer);
			touch(maintainer);
			// NB: votes cannot 'get stuck' as voter can change their vote until a vote is settled 
		}
	}

	public void pursue(Task cause, Task effect) {
		effect.cause = cause.id;
		effect.origin = cause.origin != null ? cause.origin : cause.id;
	}

	/* User voting */

	public void consent(Vote vote, User voter) {
		vote(vote, voter, vote.dissenting, vote.consenting);
	}

	public void dissent(Vote vote, User voter) {
		vote(vote, voter, vote.consenting, vote.dissenting);
	}
	
	private void vote(Vote vote, User voter, Names removed, Names added) {
		if (vote.canVote(voter.name)) {
			removed.remove(voter);
			added.add(voter);
			touch(voter);
			if (vote.isSettled()) {
				settle(vote);
			}
		}
	}
	
	private void settle(Vote vote) {
		vote.end = date(clock.time());
		boolean consented = vote.isConsented();
		if (!consented)
			return;
		switch (vote.matter) {
		case inclusion: 
			vote.area.exclusive=false; break;
		case exclusion:
			vote.area.exclusive=true; break;
		case resignation:
			vote.area.maintainers.remove(vote.affected); break;
		case participation: 
			vote.area.maintainers.add(vote.affected); break;
		}
	}

	public void support(Task task, User voter) {
		long now = clock.time();
		if (voter.canSupport(now)) {
			voter.supports(now);
			task.heat(date(now));
			touch(voter);
		}
	}

	/* A user's task queue */

	public void mark(Task task, User user) {
		ensureCanBeInvolved(task, user);
		task.usersStarted.remove(user);
		task.usersMarked.add(user);
		touch(user);
	}

	public void drop(Task task, User user) {
		ensureCanBeInvolved(task, user);
		task.usersMarked.remove(user);
		task.usersStarted.remove(user);
		touch(user);
	}

	public void start(Task task, User user) {
		task.usersStarted.add(user);
		task.usersMarked.remove(user);
		touch(user);
	}

	/* task resolution */

	public void absolve(Task task, User user) {
		ensureUnsolved(task);
		if (task.area != null) { // no change is a resolution even if no area has been specified before
			ensureIsMaintainer(task.area, user);
		}
		task.status = absolved;
		user.absolved++;
		touch(user);
	}

	public void resolve(Task task, User user) {
		ensureUnsolved(task);
		ensureIsMaintainer(task.area, user);
		task.status = resolved;
		user.xp += 2;
		user.resolved++;
		touch(user);
	}

	public void dissolve(Task task, User user) {
		ensureUnsolved(task);
		ensureIsMaintainer(task.area, user);
		task.status = dissolved;
		user.xp += 5;
		user.dissolved++;
		touch(user);
	}

	private static void ensureRegisteredUser(User user) {
		if (user.name.isInternal()) { // a anonymous user
			denyTransition("Only registered users can create products and areas!");
		}
	}

	private static void ensureExtendable(Cluster cluster, long now) {
		if (!cluster.canExtend(now)) {
			denyTransition("To many new products and areas in last 24h! Wait until tomorrow.");
		}
	}
	
	private static void ensureCanReport(User reporter, long now) {
		if (!reporter.canReport(now)) {
			denyTransition("User cannot report due to abuse protection limits!");
		}
	}

	private static void ensureCanBeInvolved(Task task, User user) {
		if (task.users() >= 5 && !task.usersMarked.contains(user) && !task.usersStarted.contains(user)) {
			denyTransition("There are already to much users involved with the task: "+task);
		}
	}
	
	private static void ensureIsMaintainer(Area area, User user) {
		if (area == null || !area.maintainers.contains(user)) {
			denyTransition("Only maintainers of an area may assign that area to a task (pull).");
		}
	}

	private static void ensureUnsolved(Task task) {
		if (task.status != unsolved) {
			denyTransition("Cannot change the outcome of a task once it is concluded!");
		}
	}
	
	private static void ensureCanReportAnonymously(Product product) {
		if (!product.allowsAnonymousReports()) {
			denyTransition("To many unconfirmed anonymous reports. Try again later.");
		}
	}
	
	private static void denyTransition(String reason) {
		throw new IllegalStateException(reason);
	}
}
