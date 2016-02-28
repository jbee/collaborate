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

import java.security.MessageDigest;
import java.util.Arrays;

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
		ensureExtendable(cluster, now);
		ensureRegisteredUser(originator);
		cluster.extended(now);
	}	
	
	/* Users + Accounts */

	public User register(Name name, String email, String unsaltedMd5) {
		ensureExternal(name);
		long now = clock.time();
		ensureCanRegister(cluster, now);
		cluster.registered(now);
		User user = new User();
		user.name = name;
		user.email = email;
		user.md5 = md5(unsaltedMd5+cluster.salt);
		touch(user);
		return user;
	}
	
	public void activate(User user) {
		ensureNotActivated(user);
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
		user.lastActive = date(clock.time());
	}

	/* Products */

	public Product initiate(Name product, User originator) {
		extend(cluster, originator);
		Product p = new Product();
		p.name = product;
		p.star = compart(p, Name.STAR, originator);
		return p;
	}
	
	/* Areas */
	
	public Area compart(Product product, Name area, User originator) {
		ensureCanInitiate(product, originator);
		return compart(product.name, area, originator);
	}
	
	public Area compart(Area area, Name subarea, User originator) {
		ensureIsMaintainer(area, originator);
		return compart(area.product, subarea, originator);
	}
	
	private Area compart(Name product, Name area, User originator) {
		extend(cluster, originator);
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
		if (task.area == null) {
			ensureIsMaintainer(to, originator);
		} else {
			ensureIsMaintainer(task.area, originator);
			if (to != null) {
				ensureIsMaintainer(to, originator);
			}
		}
		task.area = to;
		touch(originator);
	}
	
	/* Versions */
	
	//TODO
	
	/* Tasks */

	public Task reportIdea(Product product, String summay, User reporter, Area area) {
		return report(product, idea, clarification, summay, reporter, area, null, false);
	}

	public Task reportProposal(Product product, String summay, User reporter, Area area) {
		return report(product, proposal, clarification, summay, reporter, area, null, false);
	}
	
	public Task reportDefect(Product product, String summay, User reporter, Area area, Version version, boolean exploitable) {
		return report(product, defect, clarification, summay, reporter, area, version, exploitable);
	}

	public Task reportSequel(Task cause, Goal goal, String summary, User reporter) {
		Task task = report(cause.product, cause.motive, goal, summary, reporter, cause.area, cause.version, cause.exploitable);
		task.cause = cause.id;
		task.origin = cause.origin != null ? cause.origin : cause.id;
		return task;
	}
	
	private Task report(Product product, Motive motive, Goal goal, String summay, User reporter, Area area, Version version, boolean exploitable) {
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
		task.product = product;
		task.area = area;
		task.version = version;
		task.reporter = reporter.name;
		task.start = date(now);
		task.summary = summay;
		task.motive = motive;
		task.goal = goal;
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
	
	/* task resolution */

	public void absolve(Task task, User user) {
		if (task.area != null) { // no change is a resolution even if no area has been specified before
			ensureIsMaintainer(task.area, user);
		}
		solve(task, user);
		task.status = absolved;
		user.absolved++;
		touch(user);
	}

	public void resolve(Task task, User user) {
		ensureIsMaintainer(task.area, user);
		solve(task, user);
		task.status = resolved;
		user.xp += 2;
		user.resolved++;
		touch(user);
	}

	public void dissolve(Task task, User user) {
		ensureIsMaintainer(task.area, user);
		ensureUnsolved(task);
		solve(task, user);
		task.status = dissolved;
		user.xp += 5;
		user.dissolved++;
		touch(user);
	}
	
	private void solve(Task task, User user) {
		ensureUnsolved(task);
		task.solver = user.name;
		task.end = date(clock.time());
	}

	/* User voting */

	public void emphasize(Task task, User voter) {
		long now = clock.time();
		if (voter.canEmphasize(now)) {
			voter.emphasized(now);
			task.heat(date(now));
			touch(voter);
		}
	}
	
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
			vote.area.maintainers.add(vote.affected);
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

	/* consistency rules */

	private static void ensureCanInitiate(Product product, User originator) {
		if (product.star.maintainers.contains(originator.name)) {
			denyTransition("Only maintainers of area '*' can initiate new areas and versions.");
		}
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

	private static void ensureExternal(Name name) {
		if (name.isInternal()) {
			denyTransition("A registered user's name must not use '@' and be shorter than 17 characters!");
		}
	}
	
	private static void ensureCanRegister(Cluster cluster, long now) {
		if (!cluster.canRegister(now)) {
			denyTransition("To many unconfirmed accounts created today. Please try again tomorrow!");
		}
	}
	
	private static void ensureNotActivated(User user) {
		if (user.activated) {
			denyTransition("User account already activated!");
		}
	}
	
	private static void denyTransition(String reason) {
		throw new IllegalStateException(reason);
	}
}
