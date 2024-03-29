package se.jbee.task.engine;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static se.jbee.task.engine.Limit.limit;
import static se.jbee.task.model.Cause.aid;
import static se.jbee.task.model.Cause.direction;
import static se.jbee.task.model.Cause.finding;
import static se.jbee.task.model.Cause.idea;
import static se.jbee.task.model.Date.date;
import static se.jbee.task.model.Goal.adaptation;
import static se.jbee.task.model.Goal.elaboration;
import static se.jbee.task.model.Name.ORIGIN;
import static se.jbee.task.model.Outcome.consent;
import static se.jbee.task.model.Outcome.dissent;
import static se.jbee.task.model.Poll.Matter.participation;
import static se.jbee.task.model.Status.absolved;
import static se.jbee.task.model.Status.dissolved;
import static se.jbee.task.model.Status.resolved;
import static se.jbee.task.model.Status.unsolved;

import java.util.EnumMap;

import se.jbee.task.engine.Limits.ConcurrentUsage;
import se.jbee.task.engine.TransitionDenied.Error;
import se.jbee.task.model.Area;
import se.jbee.task.model.Attachments;
import se.jbee.task.model.Cause;
import se.jbee.task.model.Date;
import se.jbee.task.model.Email;
import se.jbee.task.model.Gist;
import se.jbee.task.model.Goal;
import se.jbee.task.model.IDN;
import se.jbee.task.model.Mail;
import se.jbee.task.model.Name;
import se.jbee.task.model.Names;
import se.jbee.task.model.Outcome;
import se.jbee.task.model.Output;
import se.jbee.task.model.Page;
import se.jbee.task.model.Poll;
import se.jbee.task.model.Status;
import se.jbee.task.model.Task;
import se.jbee.task.model.Template;
import se.jbee.task.model.URL;
import se.jbee.task.model.User;
import se.jbee.task.model.Version;
import se.jbee.task.model.Output.Integration;
import se.jbee.task.model.Poll.Matter;
import se.jbee.task.model.User.AuthState;
import se.jbee.task.util.Array;

/**
 * Implementation of the tracker-business logic.
 */
public final class Tracker {

	private static final Name USER = Name.as("user");

	/**
	 * A minute.
	 */
	private static final long TOKEN_VALIDITY = 600000L;

	private final Server server;

	public Tracker(Server server) {
		this.server = server;
	}

	/* Users + Accounts */

	/**
	 * When a user is registered with just an email the alias is also the email.
	 *
	 * Users never provide a password. Instead an {@link OTP} is set each time
	 * the user wants to log in. The {@link OTP} needs to be
	 * {@link #authenticate(User, byte[])} d.
	 *
	 * After {@link #register(User, Name, Email)} the user uses
	 * {@link #confirm(User)} to request a new {@link OTP} (that is send to him
	 * via email).
	 *
	 * This is actually more secure than using passwords. A users password can
	 * never be stolen or hacked. Each OTP is only usable once. To steal an
	 * account an attacker has to steal the users email account or perform a
	 * man in the middle attack. User passwords can never leak because there
	 * simply are none.
	 */
	public User register(User existing, Name alias, Email email) {
		if (existing != null) {
			if (existing.email.equalTo(email))
				return confirm(existing, existing.authState);
			if (!existing.isDubious(now()))
				denyTransition(Error.E24_NAME_OCCULIED, alias);
		}
		if (!alias.isEmail()) {
			expectRegular(alias); // only regular names are valid user names
		}
		stressNewUser();
		stressDoRegister(email); // prevents creation of many user for a single email
		User user = new User(1);
		user.alias = alias;
		user.email = email;
		user.notificationSettings=new EnumMap<>(Mail.Notification.class);
		user.contributesToOutputs = Names.empty();
		user.watches = 0;
		user.millisLastActive=now(); // cannot use touch as version should stay same
		confirmOTP(user, AuthState.registered);
		return user;
	}

	/**
	 * Used to initialize a "log in" for a registered user.
	 * He confirms that he still (or now) has control over the email connected
	 * to the user account.
	 */
	public User confirm(User user) {
		return confirm(user, AuthState.confirming);
	}

	private User confirm(User user, AuthState state) {
		long now = now();
		//TODO increase time - make cool-down grow with power of 2?
		// 1min cool-down protection against requesting to many tokens
		if (now < user.millisOtpExprires && now < user.millisOtpExprires-TOKEN_VALIDITY+60000L) {
			denyTransition(Error.E21_TOKEN_ON_COOLDOWN);
		}
		user = user.clone();
		confirmOTP(user, state);
		touch(user);
		return user;
	}

	private void confirmOTP(User user, AuthState state) {
		stressDoConfirm();
		user.authState=state;
		user.otp = OTP.next(); // this is blanked out as soon as the OTP is send
		user.encryptedOtp = OTP.encrypt(user.otp);
		user.millisOtpExprires = now() + TOKEN_VALIDITY;
	}

	/**
	 * Used to confirm the users identity and complete a "log in".
	 */
	public User authenticate(User user, byte[] token) {
		if (server.isOnLockdown() && !server.isAdmin(user)) {
			denyTransition(Error.E26_LOCKDOWN);
		}
		if (now() > user.millisOtpExprires) {
			denyTransition(Error.E22_TOKEN_EXPIRED);
		}
		if (!OTP.isToken(token, user.encryptedOtp)) {
			denyTransition(Error.E23_TOKEN_INVALID);
		}
		user = user.clone();
		user.authState = AuthState.authenticated;
		user.authenticated++;
		// invalidate token
		user.otp=null;
		user.encryptedOtp=null;
		user.millisOtpExprires=now()-1L;
		touch(user);
		return user;
	}

	/**
	 * Name a user later on when first just an email was used.
	 */
	public User name(User user, Name name) {
		expectAuthenticated(user);
		expectEmail(user.alias);
		expectRegular(name);
		stressDoConfiguration(user);
		user = user.clone();
		user.alias = name;
		touch(user);
		return user;
	}

	public User configure(User user, EnumMap<Mail.Notification, Mail.Delivery> notifications) {
		expectAuthenticated(user);
		stressDoConfiguration(user);
		user = user.clone();
		user.notificationSettings = notifications == null ? new EnumMap<>(Mail.Notification.class) : notifications;
		touch(user);
		return user;
	}

	private void touch(User user) {
		user.touch(now());
	}

	/* Outputs */

	public Output envision(Name output, User actor) {
		if (server.isOpen()) {
			expectRegistered(actor);
			expectAuthenticated(actor);
		} else {
			expectAdmin(server, actor);
		}
		expectRegular(output);
		stressNewOutput(actor);
		Output p = new Output(1);
		p.name = output;
		p.tasks = 0;
		p.categories = Names.empty();
		p.integrations = new Output.Integration[0];
		p.origin = compart(p.name, Name.ORIGIN, actor);
		p.somewhere = compart(p.name, Name.UNKNOWN, actor);
		p.somewhen = tag(p, Name.UNKNOWN, actor);
		actor.contributesToOutputs = actor.contributesToOutputs.add(output);
		touch(actor);
		return p;
	}

	public Output connect(Output output, Integration endpoint, User actor) {
		expectRegistered(actor);
		expectAuthenticated(actor);
		expectOriginMaintainer(output, actor);
		expectCanConnect(output);
		stressDoConnect(output, actor);
		int c = Array.indexOf(output.integrations, endpoint, Integration::equalTo);
		Integration[] source = output.integrations;
		if (c >= 0) {
			if (source[c].same(endpoint))
				return output;
			source = Array.remove(source, endpoint, Integration::equalTo);
		}
		output = output.clone();
		output.integrations = Array.add(source, endpoint, Integration::equalTo);
		touch(actor);
		return output;
	}

	public Output disconnect(Output output, Name integration, User actor) {
		expectRegistered(actor);
		expectAuthenticated(actor);
		expectOriginMaintainer(output, actor);
		stressDoConnect(output, actor);
		Integration endpoint = new Integration(integration, null);
		int c = Array.indexOf(output.integrations, endpoint, Integration::equalTo);
		if (c < 0)
			return output;
		output = output.clone();
		output.integrations = Array.remove(output.integrations, endpoint, Integration::equalTo);
		touch(actor);
		return output;
	}

	public Output suggest(Output output, Name category, User actor) {
		expectRegistered(actor);
		expectAuthenticated(actor);
		expectOriginMaintainer(output, actor);
		if (!output.categories.contains(category)) {
			if (output.categories.count() >= 10)
				denyTransition(Error.E28_CATEGORY_LIMIT, 10);
			stressUser(actor);
			output = output.clone();
			output.categories = output.categories.add(category);
			touch(actor);
		}
		return output;
	}

	/* Areas */

	//TODO it might be an idea to have flexible Cause and Goal for boards too
	// the idea to fix those was to reduce the amount of fields that need to be supplied.
	public Area open(Output output, Name boardArea, User actor, Cause cause, Goal goal) {
		Area area = compart(output, boardArea, actor);
		area.board=true;
		area.cause=cause;
		area.goal=goal;
		return area;
	}

	public Area compart(Output output, Name area, User actor) {
		expectOriginMaintainer(output, actor);
		Area a = compart(output.name, area, actor);
		a.safeguarded = output.origin.safeguarded;
		return a;
	}

	public Area compart(Area basis, Name area, User actor, boolean subarea) {
		expectMaintainer(basis, actor);
		Area a = compart(basis.output, area, actor);
		a.basis=basis.name;
		a.safeguarded=basis.safeguarded;
		if (subarea) {
			a.maintainers = basis.maintainers;
		}
		return a;
	}

	private Area compart(Name output, Name area, User actor) {
		expectRegistered(actor);
		expectAuthenticated(actor);
		if (area.isExternal()) {
			expectRegular(area);
		}
		stressNewArea(output, actor);
		Area a = new Area(1);
		a.name = area;
		a.output = output;
		a.maintainers=new Names(actor.alias);
		a.tasks = 0;
		a.polls = 0;
		a.safeguarded = true;
		a.category = Name.UNKNOWN;
		actor.contributesToOutputs = actor.contributesToOutputs.add(output);
		touch(actor);
		return a;
	}

	public Area leave(Area area, User maintainer) {
		if (area.maintainers.contains(maintainer)) {
			stressUser(maintainer);
			area = area.clone();
			area.maintainers = area.maintainers.remove(maintainer);
			touch(maintainer);
			// NB: votes cannot 'get stuck' as voter can change their vote until a vote is settled
		}
		return area;
	}

	public Area categorise(Area area, Name category, User actor) {
		expectRegistered(actor);
		expectAuthenticated(actor);
		expectMaintainer(area, actor);
		if (!area.category.equalTo(category)) {
			stressUser(actor);
			area = area.clone();
			area.category = category;
			touch(actor);
		}
		return area;
	}

	public Task rephrase(Task task, Gist toGist, User actor) {
		expectUnsolved(task);
		expectAuthenticated(actor);
		expectMaintainer(task.area, actor);
		if (!task.gist.equalTo(toGist)) {
			stressUser(actor);
			stressDoUpdateText(task, actor);
			task = task.clone();
			if (task.originalGist.isEmpty()) {
				task.originalGist = task.gist;
			}
			task.gist = toGist;
			touch(actor);
		}
		return task;
	}

	public Task relocate(Task task, Area to, User actor) {
		if (task.area.name.equalTo(to.name))
			return task; //NOOP
		expectNoBoard(task.area);
		expectAuthenticated(actor);
		expectUnsolved(task);
		if (task.area.name.isUnknown()) {
			expectMaintainer(to, actor); // pull from ~
		} else {
			expectMaintainer(task.area, actor); // push from
			if (!to.name.isUnknown()) {
				expectMaintainer(to, actor); // to some else than ~
			}
		}
		stressDoRelocate(task, to, actor);
		task = task.clone();
		task.area = to;
		touch(actor);
		return task;
	}

	public Task rebase(Task task, Version to, User actor) {
		if (task.base.name.equalTo(to.name))
			return task; //NOOP
		expectAuthenticated(actor);
		expectMaintainer(task.area, actor);
		expectUnsolved(task);
		stressDoRebase(task, to, actor);
		task = task.clone();
		task.base = to;
		touch(actor);
		return task;
	}

	/* Versions */

	public Version tag(Output output, Name version, User actor) {
		expectRegistered(actor);
		expectAuthenticated(actor);
		if (version.isExternal()) {
			expectVersion(version);
		}
		expectOriginMaintainer(output, actor);
		stressNewVersion(output, actor);
		Version v = new Version(1);
		v.output = output.name;
		v.name = version;
		v.changeset = Names.empty();
		touch(actor);
		return v;
	}

	/* Tasks */

	/* 3 classic starting points */

	public Task reportProposal(Output output, Gist gist, User reporter, Area area) {
		expectNoBoard(area);
		return report(output, idea, elaboration, gist, reporter, area, output.somewhen, false);
	}

	public Task reportDefect(Output output, Gist gist, User reporter, Area area, Version version, boolean exploitable) {
		expectNoBoard(area);
		return report(output, finding, elaboration, gist, reporter, area, version, exploitable);
	}

	public Task reportRequest(Output output, Gist gist, User reporter, Area board) {
		expectBoard(board);
		return report(output, board.cause, board.goal, gist, reporter, board, output.somewhen, false);
	}

	/* 2 starting points looking backwards from a goal or quality (might also be advancements?) */

	public Task reportDirection(Output output, Gist gist, User reporter, Area area) {
		expectNoBoard(area);
		return report(output, direction, elaboration, gist, reporter, area, output.somewhen, false);
	}

	public Task reportReminder(Output output, Gist gist, User reporter, Area area) {
		expectNoBoard(area);
		return report(output, aid, elaboration, gist, reporter, area, output.somewhen, false);
	}

	/* 2 ways to continue a task - build a chain or tree */

	public Task reportAdvancement(Task basis, Cause cause, Goal why, Gist gist, User reporter) {
		Area area = basis.area.board ? basis.output.somewhere : basis.area;
		Task task = report(basis.output, cause, why, gist, reporter, area, basis.base, basis.exploitable);
		task.basis = basis.id;
		task.origin = !basis.origin.isZero() ? basis.origin : basis.id;
		return task;
	}

	public Task reportRelease(Task basis, Version released, Gist gist, Names baseVersions, User reporter) {
		expectNotYetPublished(basis.base);
		expectNonEmptyChangeset(baseVersions);
		Task task = reportAdvancement(basis, basis.cause, Goal.propagation, gist, reporter);
		task.base = released;
		task.baseVersions = baseVersions;
		return task;
	}

	private Task report(Output output, Cause cause, Goal goal, Gist gist, User reporter, Area area, Version version, boolean exploitable) {
		if (!area.isOpen()) {
			expectAuthenticated(reporter);
			expectMaintainer(area, reporter);
		}
		expectCanReport(reporter);
		stressNewTask(output, reporter);
		Task task = new Task(1);
		task.output = output.clone();
		task.output.tasks++;
		task.id = IDN.idn(task.output.tasks);
		if (area.board) {
			task.area = area.clone();
			task.area.tasks++;
			task.serial=IDN.idn(task.area.tasks);
		} else {
			task.area = area;
		}
		task.base = version;
		task.reporter = reporter.alias;
		task.reported = date(now());
		task.gist = gist;
		task.originalGist = Gist.EMPTY;
		task.conclusion = Gist.EMPTY;
		task.cause = cause;
		task.goal = goal;
		task.status = Status.unsolved;
		task.exploitable = exploitable;
		task.aspirants = Names.empty();
		task.participants = Names.empty();
		task.watchers = new Names(reporter.alias);
		task.baseVersions = Names.empty();
		task.attachments = Attachments.NONE;
		reporter.contributesToOutputs = reporter.contributesToOutputs.add(task.output.name);
		touch(reporter);
		return task;
	}

	public Task disclose(Task task, User actor) {
		expectAuthenticated(actor);
		expectOriginMaintainer(task.output, actor);
		if (task.exploitable && !task.disclosed) {
			task = task.clone();
			task.disclosed = true;
			touch(actor);
		}
		return task;
	}

	public Task attach(Task task, User actor, URL attachment) {
		expectAuthenticated(actor);
		if (!task.area.isOpen()) {
			expectMaintainer(task.area, actor);
		}
		if (task.attachments.contains(attachment))
			return task;
		expectUnsolved(task);
		expectConform(task, attachment);
		stressDoAttach(task, actor);
		task = task.clone();
		if (!attachment.isIntegrated()) {
			attachment = task.output.integrate(attachment);
		}
		task.attachments = task.attachments.add(attachment);
		actor.contributesToOutputs = actor.contributesToOutputs.add(task.output.name);
		touch(actor);
		return task;
	}

	public Task detach(Task task, User actor, URL attachment) {
		expectAuthenticated(actor);
		expectMaintainer(task.area, actor);
		if (!task.attachments.contains(attachment))
			return task;
		stressDoAttach(task, actor);
		task = task.clone();
		task.attachments = task.attachments.remove(attachment);
		actor.contributesToOutputs = actor.contributesToOutputs.add(task.output.name);
		touch(actor);
		return task;
	}

	/* task resolution */

	private int xp(Task task, int base) {
		if (task.reporter.equalTo(task.solver))
			return 0; // prevent XP mining by adding and resolving tasks using same user
		Date today = date(now());
		int age = today.daysSince(task.reported);
		if (age <= 0)
			return 0; // prevent XP mining by adding and resolving tasks using different users
		int xp = (int) max(base, base * (1f+((age-4f)/age))); // 1-2x base value, more with higher age
		xp = (int)max(xp, xp * (1f+ task.emphasis / age / 10f)); // 1-nx, more with higher average emphasis
		if (task.exploitable && task.goal == adaptation) xp *= 2;
		if (task.goal != adaptation) xp /= 2;
		if (task.temperature(today) < 50) xp /= 2;
		return max(1, xp);
	}

	public Task absolve(Task task, User by, Gist conclusion) {
		if (!task.area.name.isUnknown()) { // no change is a resolution even if no area has been specified before
			expectMaintainer(task.area, by);
		}
		task = solve(task, by, conclusion);
		task.status = absolved;
		by.absolved++;
		return task;
	}

	public Task resolve(Task task, User by, Gist conclusion) {
		expectMaintainer(task.area, by);
		task = solve(task, by, conclusion);
		task.status = resolved;
		by.xp += xp(task, 2);
		by.resolved++;
		if (!task.baseVersions.isEmpty()) { // publishing is something that is resolved when its done
			task.base = task.base.clone();
			task.base.changeset = task.baseVersions; // transfer the released versions
		}
		return task;
	}

	public Task dissolve(Task task, User by, Gist conclusion) {
		expectMaintainer(task.area, by);
		expectUnsolved(task);
		task = solve(task, by, conclusion);
		task.status = dissolved;
		by.xp += xp(task, 5);
		by.dissolved++;
		return task;
	}

	private Task solve(Task task, User by, Gist conclusion) {
		expectRegistered(by);
		expectAuthenticated(by);
		expectUnsolved(task);
		stressDoSolve(task, by);
		task = task.clone();
		task.solver = by.alias;
		task.resolved = date(now());
		task.conclusion = conclusion;
		task.participants = Names.empty();
		task.aspirants = Names.empty();
		by.contributesToOutputs = by.contributesToOutputs.add(task.output.name);
		touch(by);
		return task;
	}

	public Task archive(Task task, User by) {
		expectAuthenticated(by);
		expectMaintainer(task.area, by);
		expectSolved(task);
		task = task.clone();
		task.archived = true;
		touch(by);
		return task;
	}

	/* User voting */

	public Task emphasise(Task task, User voter) {
		long now = now();
		if (voter.canEmphasise(now) && task.canBeEmphasisedBy(voter.alias)) {
			voter.emphasised(now);
			task = task.clone();
			task.emphasise(date(now));
			touch(voter);
		}
		return task;
	}

	public Poll poll(Matter matter, Gist motivation, Area area, User actor, User affected) {
		expectRegistered(actor);
		expectAuthenticated(actor);
		if (matter != participation) {
			expectMaintainer(area, actor);
		}
		stressNewPoll(area, actor);
		Poll poll = new Poll(1);
		poll.area = area.clone();
		poll.area.polls++;
		poll.serial = IDN.idn(poll.area.polls);
		poll.matter = matter;
		poll.motivation = motivation;
		poll.initiator = actor.alias;
		poll.affected = !matter.isUserRelated() ? Name.ORIGIN : affected.alias;
		poll.start = date(now());
		poll.outcome = Outcome.inconclusive;
		poll.consenting = Names.empty();
		poll.dissenting = Names.empty();
		poll.expiry = poll.start.plusDays(min(14, area.maintainers.count()));
		actor.contributesToOutputs = actor.contributesToOutputs.add(area.output);
		touch(actor);
		return poll;
	}

	public Poll consent(Poll poll, User voter) {
		return vote(poll, voter, true);
	}

	public Poll dissent(Poll poll, User voter) {
		return vote(poll, voter, false);
	}

	/**
	 * When users leave an area as a maintainer the polls of that area might be
	 * affected by the change. This method is used to process ALL {@link Poll}s
	 * of the area left by a maintainer. As this is a passive effect of another
	 * event there are no checks on the authentification of the actor or likewise.
	 */
	public Poll recount(Poll poll, Name resignedUser, User actor) {
		if (poll.hasVoted(resignedUser)) {
			poll = poll.clone();
			poll.consenting = poll.consenting.remove(resignedUser);
			poll.dissenting = poll.dissenting.remove(resignedUser);
			if (!poll.isConcluded() && poll.isEffectivelySettled()) {
				settle(poll);
			}
			touch(actor);
		}
		return poll;
	}

	private Poll vote(Poll poll, User voter, boolean consent) {
		expectRegistered(voter);
		expectAuthenticated(voter);
		if (poll.canVote(voter.alias) && (
				consent && !poll.consenting.contains(voter.alias)
			|| !consent && !poll.dissenting.contains(voter.alias))) {
			stressDoVote(poll, voter);
			poll = poll.clone();
			if (consent) {
				poll.consenting = poll.consenting.add(voter);
				poll.dissenting = poll.dissenting.remove(voter);
			} else {
				poll.dissenting = poll.dissenting.add(voter);
				poll.consenting = poll.consenting.remove(voter);
			}
			touch(voter);
			if (poll.isEffectivelySettled()) {
				settle(poll);
			}
		}
		return poll;
	}

	private void settle(Poll poll) {
		poll.end = date(now());
		boolean accepted = poll.isAccepted();
		poll.outcome = accepted ? consent : dissent;
		if (!accepted)
			return;
		Area area = poll.area.clone();
		switch (poll.matter) {
		case abandonment:
			area.abandoned=true; break;
		case inclusion:
			area.exclusive=false; break;
		case exclusion:
			area.exclusive=true; break;
		case resignation:
			area.maintainers = area.maintainers.remove(poll.affected); break;
		case participation:
			area.maintainers = area.maintainers.add(poll.affected); break;
		case safeguarding:
			area.safeguarded=true; break;
		case unblocking:
			area.safeguarded=false; break;
		}
		poll.area = area;
	}

	/* A user's task queues */

	public Task aspire(Task task, User user) {
		expectRegistered(user);
		expectAuthenticated(user);
		expectCanBeInvolved(user, task);
		if (task.participants.contains(user) || !task.aspirants.contains(user)) {
			stressDoList(task, user);
			task = task.clone();
			task.participants = task.participants.remove(user);
			task.aspirants = task.aspirants.add(user);
			user.contributesToOutputs = user.contributesToOutputs.add(task.output.name);
			touch(user);
		}
		return task;
	}

	public Task abandon(Task task, User user) {
		expectRegistered(user);
		expectAuthenticated(user);
		expectCanBeInvolved(user, task);
		if (task.aspirants.contains(user) || task.participants.contains(user)) {
			stressDoList(task, user);
			task = task.clone();
			task.aspirants = task.aspirants.remove(user);
			task.participants = task.participants.remove(user);
			user.abandoned++;
			if (user.xp > 0) user.xp--;
			touch(user);
		}
		return task;
	}

	public Task participate(Task task, User user) {
		expectRegistered(user);
		expectAuthenticated(user);
		expectCanBeInvolved(user, task);
		expectMaintainer(task.area, user);
		if (!task.participants.contains(user) || task.aspirants.contains(user)) {
			stressDoList(task, user);
			task = task.clone();
			task.participants = task.participants.add(user);
			task.aspirants = task.aspirants.remove(user);
			user.contributesToOutputs = user.contributesToOutputs.add(task.output.name);
			touch(user);
		}
		return task;
	}

	/* A user's watch list */

	public Task watch(Task task, User user) {
		expectRegistered(user);
		expectAuthenticated(user);
		expectCanWatch(user);
		if (!task.watchers.contains(user)) {
			stressDoList(task, user);
			task = task.clone();
			task.watchers = task.watchers.add(user);
			user.watches++;
			user.contributesToOutputs = user.contributesToOutputs.add(task.output.name);
			touch(user);
		}
		return task;
	}

	public Task unwatch(Task task, User user) {
		if (task.watchers.contains(user)) {
			stressDoList(task, user);
			task = task.clone();
			task.watchers = task.watchers.remove(user);
			user.watches--;
			touch(user);
		}
		return task;
	}

	/* A user's or area's pages */

	public Page compose(User owner, Name page, Template template, Page...inSameMenu) {
		return compose(null, page, template, owner, inSameMenu);
	}

	public Page compose(Area area, Name page, Template template, User actor, Page...inSameMenu) {
		expectAuthenticated(actor);
		expectPageDoesNotExist(page, inSameMenu);
		expectCanHaveMorePages(inSameMenu);
		if (area != null) {
			expectMaintainer(area, actor);
		}
		stessNewPage(actor);
		Page res = area == null
				? new Page(1, Name.ORIGIN, actor.alias, page, template)
				: new Page(1, area.output, area.name, page, template);
		touch(actor);
		return res;
	}

	public Page recompose(Page page, Template template, User owner) {
		return recompose(page, null, template, owner);
	}
	public Page recompose(Page page, Area area, Template template, User actor) {
		expectRegistered(actor);
		expectAuthenticated(actor);
		if (page.isUserPage()) {
			expectOwner(page, actor);
		} else {
			expectMaintainer(area, actor);
		}
		stressDoUpdate(page, actor);
		page = page.clone();
		page.template = template;
		touch(actor);
		return page;
	}

	public Page erase(Page page, User actor) {
		return erase(page, null, actor);
	}

	public Page erase(Page page, Area area, User actor) {
		return recompose(page, area, Template.BLANK_PAGE, actor);
	}

	/* limit checks */

	private void stressAction() {
		stressLimit(limit("action", ORIGIN), "Too many actions lately.");
	}

	private void stressNewContent() {
		stressLimit(limit("content", ORIGIN), "Too many new entities.");
	}

	private void stressNewUser() {
		stressLimit(limit("user", ORIGIN), "Too many users registered lately.");
	}

	private void stressUser(User user) {
		stressLimit(limit("user", user.alias), "Too many changes by user: "+user.alias);
	}

	private void stressDoRegister(Email email) {
		stressLimit(limit("user", email.asName()), "Too many changes by user: "+email);
	}

	private void stressNewOutput(User actor) {
		stressLimit(limit("output@user", actor.alias), "Too many recent outputs by user: "+actor.alias);
		stressUser(actor);
		stressLimit(limit("output", ORIGIN), "Too many new outputs.");
		stressNewContent();
	}

	private void stressNewArea(Name output, User actor) {
		stressLimit(limit("area@user", actor.alias), "Too many recent areas by user: "+actor.alias);
		stressUser(actor);
		stressLimit(limit("area@output", output), "Too many new areas for output: "+output);
		stressLimit(limit("area", ORIGIN), "Too many new areas.");
		stressNewContent();
	}

	private void stressNewVersion(Output output, User actor) {
		stressLimit(limit("version@user", actor.alias), "Too many recent versions by user: "+actor.alias);
		stressUser(actor);
		stressLimit(limit("version@output", output.name), "Too many new versions for output: "+output.name);
		stressLimit(limit("version", ORIGIN), "Too many new versions.");
		stressNewContent();
	}

	private void stressNewTask(Output output, User reporter) {
		stressLimit(limit("task@user", reporter.alias), "Too many recent tasks by user: "+reporter.alias);
		stressUser(reporter);
		stressLimit(limit("task@output", output.name), "Too many new task for output: "+output.name);
		stressLimit(limit("task", ORIGIN), "Too many new tasks.");
		stressNewContent();
	}

	private void stressNewPoll(Area area, User actor) {
		stressLimit(limit("poll@user", actor.alias), "Too many new polls by user: "+actor.alias);
		stressUser(actor);
		stressLimit(limit("poll@area", area.name), "Too many new polls in area: "+area.name);
		stressLimit(limit("poll", ORIGIN), "Too many new polls.");
		stressNewContent();
	}

	private void stessNewPage(User owner) {
		stressLimit(limit("page@user", owner.alias), "Too many new pages by user: "+owner.alias);
		stressUser(owner);
		stressLimit(limit("page", ORIGIN), "Too many new pages.");
		stressNewContent();
	}

	private void stressDoConfiguration(User user) {
		stressLimit(limit("configure@user", user.alias), "Too many recent configuration changes by user: "+user.alias);
		stressUser(user);
		stressLimit(limit("configure", ORIGIN), "Too many recent configuration changes.");
		stressAction();
	}

	private void stressDoConnect(Output output, User actor) {
		stressLimit(limit("connect@user", actor.alias), "Too many recent connections by user: "+actor.alias);
		stressUser(actor);
		stressLimit(limit("connect", ORIGIN), "Too many recent connections.");
		stressNewContent();
	}

	private void stressDoUpdate(Page page, User owner) {
		stressLimit(limit("update@user", owner.alias), "Too many recent page updates by user: "+owner.alias);
		stressUser(owner);
		stressLimit(limit("update", page.name), "Too many page updates for page: "+page.name);
		stressLimit(limit("update", ORIGIN), "Too many page updates recently.");
		stressAction();
	}

	private void stressDoUpdateText(Task task, User actor) {
		stressLimit(limit("text@user", actor.alias), "Too many recent text changes by user: "+actor.alias);
		stressUser(actor);
		stressLimit(limit("text@task", task.id.asName()), "Too many text changes for task: "+task.id);
		stressLimit(limit("text", ORIGIN), "Too many text changes recently.");
		stressAction();
	}

	private void stressDoRelocate(Task task, Area to, User actor) {
		stressLimit(limit("move@user", actor.alias), "Too many recent relocations by user: "+actor.alias);
		stressUser(actor);
		stressLimit(limit("move@area", to.name), "Too many relocations for area: "+to.name);
		stressLimit(limit("move@task", task.id.asName()), "Too many queue activities for task: "+task.id);
		stressLimit(limit("move", ORIGIN), "Too many relocations recently.");
		stressAction();
	}

	private void stressDoRebase(Task task, Version to, User actor) {
		stressLimit(limit("base@user", actor.alias), "Too many recent rebase actions by user: "+actor.alias);
		stressUser(actor);
		stressLimit(limit("base@version", to.name), "Too many rebase actions for version: "+to.name);
		stressLimit(limit("base@task", task.id.asName()), "Too many queue activities for task: "+task.id);
		stressLimit(limit("base", ORIGIN), "Too many rebase actions recently.");
		stressAction();
	}

	private void stressDoVote(Poll poll, User voter) {
		stressLimit(limit("vote@user", voter.alias), "Too many recent votes by user: "+voter.alias);
		stressUser(voter);
		stressLimit(limit("vote@poll", voter.alias), "Too many recent votes in poll: "+poll.matter+" "+poll.affected);
		stressLimit(limit("vote", ORIGIN), "Too many votes recently.");
		stressAction();
	}

	private void stressDoList(Task task, User user) {
		stressLimit(limit("list@user", user.alias), "Too many queue activities by user: "+user.alias);
		stressUser(user);
		stressLimit(limit("list@task", task.id.asName()), "Too many queue activities for task: "+task.id);
		stressLimit(limit("list", ORIGIN), "Too many queue activities recently.");
		stressAction();
	}

	private void stressDoSolve(Task task, User by) {
		stressLimit(limit("solve@user", by.alias), "Too many solution activities by user: "+by.alias);
		stressUser(by);
		stressLimit(limit("solve@task", task.id.asName()), "Too many solution activities for task: "+task.id);
		stressLimit(limit("solve", ORIGIN), "Too many solution activities recently.");
		stressAction();
	}

	private void stressDoAttach(Task task, User by) {
		stressLimit(limit("attach@user", by.alias), "Too many recent attachments by user: "+by.alias);
		stressUser(by);
		stressLimit(limit("attach@task", task.id.asName()), "Too many recent attachments for task: "+task.id);
		stressLimit(limit("attach", ORIGIN), "Too many recent attachments.");
		stressAction();
	}

	private void stressDoConfirm() {
		stressLimit(limit("confirm", ORIGIN), "Too many recent confirm requests.");
		stressAction();
	}

	private void stressLimit(Limit limit, String msg) {
		if (server.isOnLockdown())
			return; // no limit checks for admins during the lockdown
		try {
			if (!server.limits.stress(limit, server.clock)) {
				denyTransition(Error.E1_LIMIT_EXCEEDED, limit, msg);
			}
		} catch (ConcurrentUsage e) {
			denyTransition(Error.E27_LIMIT_OCCUPIED, limit, msg);
		}
	}

	/* consistency rules */

	private static void expectNoBoard(Area area) {
		if (area.board)
			denyTransition(Error.E2_MUST_NOT_BE_BOARD, area.name);
	}

	private static void expectBoard(Area area) {
		if (!area.board)
			denyTransition(Error.E3_MUST_BE_BOARD, area.name);
	}

	private static void expectNotYetPublished(Version version) {
		if (version.isPublished())
			denyTransition(Error.E4_VERSION_RELEASED, version.name);
	}

	private static void expectCanHaveMorePages(Page[] inSameMenu) {
		if (inSameMenu.length >= 10)
			denyTransition(Error.E5_PAGE_LIMIT_REACHED, 10);
	}

	private static void expectOwner(Page page, User actor) {
		if (!page.menu.equalTo(actor.alias))
			denyTransition(Error.E6_PAGE_OWNERSHIP_REQUIRED, page.name);
	}

	private static void expectPageDoesNotExist(Name page, Page[] inSameMenu) {
		for (Page s : inSameMenu)
			if (s.name.equalTo(page))
				denyTransition(Error.E7_PAGE_EXISTS, page);
	}

	private static void expectOriginMaintainer(Output output, User user) {
		if (!output.origin.maintainers.contains(user.alias))
			denyTransition(Error.E8_OUTPUT_OWNERSHIP_REQUIRED, output.name, output.origin.name, output.origin.maintainers);
	}

	private static void expectRegistered(User user) {
		if (user.isAnonymous())
			denyTransition(Error.E9_REQUIRES_REGISTRATION);
	}

	private static void expectCanReport(User reporter) {
		if (!reporter.isAuthenticated())
			denyTransition(Error.E10_REQUIRES_AUTHENTICATION);
	}

	private static void expectCanBeInvolved(User user, Task task) {
		if (task.supporterCount() >= 5
				&& !task.aspirants.contains(user)
				&& !task.participants.contains(user))
			denyTransition(Error.E11_TASK_USER_LIMIT_REACHED, task.id);
	}

	private static void expectMaintainer(Area area, User user) {
		if (!area.maintainers.contains(user))
			denyTransition(Error.E12_AREA_MAINTAINER_REQUIRED, area.name, area.maintainers);
	}

	private static void expectUnsolved(Task task) {
		if (task.status != unsolved)
			denyTransition(Error.E13_TASK_ALREAD_SOLVED, task.id);
	}

	private static void expectSolved(Task task) {
		if (task.status == unsolved)
			denyTransition(Error.E14_TASK_NOT_SOLVED, task.id);
	}

	private static void expectConform(Task task, URL url) {
		if (!task.output.isIntegrated(url) && task.area.safeguarded)
			denyTransition(Error.E15_URL_NOT_INTEGRATED, url, task.output.integrations());
	}

	private static void expectVersion(Name name) {
		if (!(name.isVersion() || name.isRegular()))
			denyTransition(Error.E16_NAME_IS_NO_VERSION, name);
	}

	private static void expectRegular(Name name) {
		if (!name.isRegular())
			denyTransition(Error.E17_NAME_IS_NOT_REGULAR, name);
		if (name.equalTo(USER)) // no object created by a user should be named "user"
			denyTransition(Error.E24_NAME_OCCULIED, name);
	}

	private static void expectEmail(Name name) {
		if (!name.isEmail())
			denyTransition(Error.E18_NAME_IS_NO_EMAIL, name);
	}

	private static void expectAuthenticated(User user) {
		if (!user.isAuthenticated())
			denyTransition(Error.E10_REQUIRES_AUTHENTICATION);
	}

	private static void expectCanConnect(Output output) {
		if (output.integrations.length >= 8)
			denyTransition(Error.E19_OUTPUT_INTEGRATION_LIMIT_REACHED, 8, output.name, output.integrations());
	}

	private static void expectCanWatch(User user) {
		if (!user.isAuthenticated())
			denyTransition(Error.E10_REQUIRES_AUTHENTICATION);
		if (!user.canWatch())
			denyTransition(Error.E20_USER_WATCH_LIMIT_REACHED);
	}

	private static void expectAdmin(Server server, User actor) {
		if (!server.isAdmin(actor))
			denyTransition(Error.E25_ADMIN_REQUIRED, server.admin());
	}

	private static void expectNonEmptyChangeset(Names changeset) {
		if (changeset == null || changeset.isEmpty())
			denyTransition(Error.E29_CHANGESET_REQUIRED);
	}

	private static void denyTransition(Error error, Object...args) {
		throw new TransitionDenied(error, args);
	}

	private long now() {
		return server.clock.time();
	}

}
