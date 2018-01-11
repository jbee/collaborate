package se.jbee.track.engine;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static se.jbee.track.engine.Limit.limit;
import static se.jbee.track.model.Date.date;
import static se.jbee.track.model.Motive.defect;
import static se.jbee.track.model.Motive.necessity;
import static se.jbee.track.model.Motive.proposal;
import static se.jbee.track.model.Motive.reminder;
import static se.jbee.track.model.Name.ORIGIN;
import static se.jbee.track.model.Outcome.consent;
import static se.jbee.track.model.Outcome.dissent;
import static se.jbee.track.model.Poll.Matter.participation;
import static se.jbee.track.model.Purpose.clarification;
import static se.jbee.track.model.Purpose.modification;
import static se.jbee.track.model.Status.absolved;
import static se.jbee.track.model.Status.dissolved;
import static se.jbee.track.model.Status.resolved;
import static se.jbee.track.model.Status.unsolved;

import java.util.EnumMap;

import se.jbee.track.engine.Limits.ConcurrentUsage;
import se.jbee.track.engine.TransitionDenied.Error;
import se.jbee.track.model.Area;
import se.jbee.track.model.Attachments;
import se.jbee.track.model.Date;
import se.jbee.track.model.Email;
import se.jbee.track.model.Gist;
import se.jbee.track.model.IDN;
import se.jbee.track.model.Mail;
import se.jbee.track.model.Motive;
import se.jbee.track.model.Name;
import se.jbee.track.model.Names;
import se.jbee.track.model.Outcome;
import se.jbee.track.model.Poll;
import se.jbee.track.model.Poll.Matter;
import se.jbee.track.model.Product;
import se.jbee.track.model.Product.Integration;
import se.jbee.track.model.Purpose;
import se.jbee.track.model.Site;
import se.jbee.track.model.Status;
import se.jbee.track.model.Task;
import se.jbee.track.model.Template;
import se.jbee.track.model.URL;
import se.jbee.track.model.User;
import se.jbee.track.model.User.AuthState;
import se.jbee.track.model.User.Notification;
import se.jbee.track.model.Version;
import se.jbee.track.util.Array;

/**
 * Implementation of the tracker-business logic.
 */
public final class Tracker {

	/**
	 * A minute.
	 */
	private static final long TOKEN_VALIDITY = 600000L;

	private final Server server;

	public Tracker(Server server) {
		super();
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
				denyTransition(Error.E24_USER_EXISTS, alias);
		}
		if (!alias.isEmail()) { // so email or regular names are OK
			expectRegular(alias);
			//TODO should we check that an email is only used for a single user?
			// in general it is OK for a user to have multiple account for a single email but there should be a limit to prevent abuse.
		}
		stressNewUser();
		User user = new User(1);
		user.alias = alias;
		user.email = email;
		user.notificationSettings=new EnumMap<>(Notification.class);
		user.contributesToProducts = Names.empty();
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
	
	public User configure(User user, EnumMap<Notification, Mail.Delivery> notifications) {
		expectAuthenticated(user);
		stressDoConfiguration(user);
		user = user.clone();
		user.notificationSettings = notifications == null ? new EnumMap<>(Notification.class) : notifications;
		touch(user);
		return user;
	}

	private void touch(User user) {
		user.touch(now());
	}

	/* Products */

	public Product constitute(Name product, User actor) {
		if (server.isOpen()) {
			expectRegistered(actor);
			expectAuthenticated(actor);
		} else {
			expectAdmin(server, actor);
		}
		expectRegular(product);
		stressNewProduct(actor);
		Product p = new Product(1);
		p.name = product;
		p.tasks = 0;
		p.categories = Names.empty();
		p.integrations = new Product.Integration[0];
		p.origin = compart(p.name, Name.ORIGIN, actor);
		p.somewhere = compart(p.name, Name.UNKNOWN, actor);
		p.somewhen = tag(p, Name.UNKNOWN, actor);
		actor.contributesToProducts = actor.contributesToProducts.add(product);
		touch(actor);
		return p;
	}
	
	public Product connect(Product product, Integration endpoint, User actor) {
		expectRegistered(actor);
		expectAuthenticated(actor);
		expectOriginMaintainer(product, actor);
		expectCanConnect(product);
		stressDoConnect(product, actor);
		int c = Array.indexOf(product.integrations, endpoint, Integration::equalTo);
		Integration[] source = product.integrations;
		if (c >= 0) {
			if (source[c].same(endpoint))
				return product;
			source = Array.remove(source, endpoint, Integration::equalTo);
		}
		product = product.clone();
		product.integrations = Array.add(source, endpoint, Integration::equalTo);
		touch(actor);
		return product;
	}
	
	public Product disconnect(Product product, Name integration, User actor) {
		expectRegistered(actor);
		expectAuthenticated(actor);
		expectOriginMaintainer(product, actor);
		stressDoConnect(product, actor);
		Integration endpoint = new Integration(integration, null);
		int c = Array.indexOf(product.integrations, endpoint, Integration::equalTo);
		if (c < 0)
			return product;
		product = product.clone();
		product.integrations = Array.remove(product.integrations, endpoint, Integration::equalTo);
		touch(actor);
		return product;
	}
	
	public Product suggest(Product product, Name category, User actor) {
		expectRegistered(actor);
		expectAuthenticated(actor);
		expectOriginMaintainer(product, actor);
		if (!product.categories.contains(category)) {
			if (product.categories.count() >= 10)
				denyTransition(Error.E28_CATEGORY_LIMIT, 10);
			stressUser(actor);
			product = product.clone();
			product.categories = product.categories.add(category);
			touch(actor);
		}
		return product;
	}

	/* Areas */

	public Area open(Product product, Name boardArea, User actor, Motive motive, Purpose purpose) {
		Area area = compart(product, boardArea, actor);
		area.board=true;
		area.motive=motive;
		area.purpose=purpose;
		return area;
	}

	public Area compart(Product product, Name area, User actor) {
		expectOriginMaintainer(product, actor);
		Area a = compart(product.name, area, actor);
		a.safeguarded = product.origin.safeguarded;
		return a;
	}

	public Area compart(Area basis, Name partition, User actor, boolean subarea) {
		expectMaintainer(basis, actor);
		Area area = compart(basis.product, partition, actor);
		area.basis=basis.name;
		area.safeguarded=basis.safeguarded;
		if (subarea) {
			area.maintainers = basis.maintainers;
		}
		return area;
	}

	private Area compart(Name product, Name area, User actor) {
		expectRegistered(actor);
		expectAuthenticated(actor);
		if (area.isEditable()) {
			expectRegular(area);
		}
		stressNewArea(product, actor);
		Area a = new Area(1);
		a.name = area;
		a.product = product;
		a.maintainers=new Names(actor.alias);
		a.tasks = 0;
		a.polls = 0;
		a.safeguarded = true;
		a.category = Name.UNKNOWN;
		actor.contributesToProducts = actor.contributesToProducts.add(product);
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

	public Version tag(Product product, Name version, User actor) {
		expectRegistered(actor);
		expectAuthenticated(actor);
		if (version.isEditable()) {
			expectVersion(version);
		}
		expectOriginMaintainer(product, actor);
		stressNewVersion(product, actor);
		Version v = new Version(1);
		v.product = product.name;
		v.name = version;
		v.changeset = Names.empty();
		touch(actor);
		return v;
	}

	/* Tasks */

	public Task reportProposal(Product product, Gist gist, User reporter, Area area) {
		expectNoBoard(area);
		return report(product, proposal, clarification, gist, reporter, area, product.somewhen, false);
	}

	public Task reportNecessity(Product product, Gist gist, User reporter, Area area) {
		expectNoBoard(area);
		return report(product, necessity, clarification, gist, reporter, area, product.somewhen, false);
	}
	
	public Task reportThought(Product product, Gist gist, User reporter, Area area) {
		expectNoBoard(area);
		return report(product, reminder, clarification, gist, reporter, area, product.somewhen, false);
	}

	public Task reportDefect(Product product, Gist gist, User reporter, Area area, Version version, boolean exploitable) {
		expectNoBoard(area);
		return report(product, defect, clarification, gist, reporter, area, version, exploitable);
	}

	public Task reportRequest(Product product, Gist gist, User reporter, Area board) {
		expectBoard(board);
		return report(product, board.motive, board.purpose, gist, reporter, board, product.somewhen, false);
	}

	public Task reportSegment(Task basis, Purpose why, Gist gist, User reporter, Names changeset) {
		Area area = basis.area.board ? basis.product.somewhere : basis.area;
		Task task = report(basis.product, basis.motive, why, gist, reporter, area, basis.base, basis.exploitable);
		task.basis = basis.id;
		task.origin = !basis.origin.isZero() ? basis.origin : basis.id;
		if (changeset != null && !changeset.isEmpty()) {
			expectNotYetPublished(task.base);
			task.changeset = changeset;
		}
		return task;
	}

	private Task report(Product product, Motive motive, Purpose purpose, Gist gist, User reporter, Area area, Version version, boolean exploitable) {
		if (!area.isOpen()) {
			expectAuthenticated(reporter);
			expectMaintainer(area, reporter);
		}
		expectCanReport(reporter);
		stressNewTask(product, reporter);
		Task task = new Task(1);
		task.product = product.clone();
		task.product.tasks++;
		task.id = IDN.idn(task.product.tasks);
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
		task.motive = motive;
		task.purpose = purpose;
		task.status = Status.unsolved;
		task.exploitable = exploitable;
		task.aspirants = Names.empty();
		task.participants = Names.empty();
		task.watchers = new Names(reporter.alias);
		task.changeset = Names.empty();
		task.attachments = Attachments.NONE;
		reporter.contributesToProducts = reporter.contributesToProducts.add(task.product.name);
		touch(reporter);
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
			attachment = task.product.integrate(attachment);
		}
		task.attachments = task.attachments.add(attachment); 
		actor.contributesToProducts = actor.contributesToProducts.add(task.product.name);
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
		actor.contributesToProducts = actor.contributesToProducts.add(task.product.name);
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
		if (task.exploitable && task.purpose == modification) xp *= 2;
		if (task.purpose != modification) xp /= 2;
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
		if (!task.changeset.isEmpty()) { // publishing is something that is resolved when its done
			task.base = task.base.clone();
			task.base.changeset = task.changeset; // transfer the released versions 
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
		by.contributesToProducts = by.contributesToProducts.add(task.product.name);
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
		actor.contributesToProducts = actor.contributesToProducts.add(area.product);
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
			user.contributesToProducts = user.contributesToProducts.add(task.product.name);
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
			user.contributesToProducts = user.contributesToProducts.add(task.product.name);
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
			user.contributesToProducts = user.contributesToProducts.add(task.product.name);
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

	/* A user's or area's sites */

	public Site compose(User owner, Name site, Template template, Site...inSameMenu) {
		return compose(null, site, template, owner, inSameMenu);
	}
	
	public Site compose(Area area, Name site, Template template, User actor, Site...inSameMenu) {
		expectAuthenticated(actor);
		expectSiteDoesNotExist(site, inSameMenu);
		expectCanHaveMoreSites(inSameMenu);
		if (area != null) {
			expectMaintainer(area, actor);
		}
		stessNewSite(actor);
		Site s = area == null
				? new Site(1, Name.ORIGIN, actor.alias, site, template)
				: new Site(1, area.product, area.name, site, template);
		touch(actor);
		return s;
	}

	public Site recompose(Site site, Template template, User owner) {
		return recompose(site, null, template, owner);
	}
	public Site recompose(Site site, Area area, Template template, User actor) {
		expectRegistered(actor);
		expectAuthenticated(actor);
		if (site.isUserSite()) {
			expectOwner(site, actor);
		} else {
			expectMaintainer(area, actor);
		}
		stressDoUpdate(site, actor);
		site = site.clone();
		site.template = template;
		touch(actor);
		return site;
	}
	
	public Site erase(Site site, User actor) {
		return erase(site, null, actor);
	}
	
	public Site erase(Site site, Area area, User actor) {
		return recompose(site, area, Template.BLANK_PAGE, actor);
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

	private void stressUser(User reporter) {
		stressLimit(limit("user", reporter.alias), "Too many changes by user: "+reporter.alias);
	}

	private void stressNewProduct(User actor) {
		stressLimit(limit("product@user", actor.alias), "Too many recent products by user: "+actor.alias);
		stressUser(actor);
		stressLimit(limit("product", ORIGIN), "Too many new products.");
		stressNewContent();
	}

	private void stressNewArea(Name product, User actor) {
		stressLimit(limit("area@user", actor.alias), "Too many recent areas by user: "+actor.alias);
		stressUser(actor);
		stressLimit(limit("area@product", product), "Too many new areas for product: "+product);
		stressLimit(limit("area", ORIGIN), "Too many new areas.");
		stressNewContent();
	}

	private void stressNewVersion(Product product, User actor) {
		stressLimit(limit("version@user", actor.alias), "Too many recent versions by user: "+actor.alias);
		stressUser(actor);
		stressLimit(limit("version@product", product.name), "Too many new versions for product: "+product.name);
		stressLimit(limit("version", ORIGIN), "Too many new versions.");
		stressNewContent();
	}

	private void stressNewTask(Product product, User reporter) {
		stressLimit(limit("task@user", reporter.alias), "Too many recent tasks by user: "+reporter.alias);
		stressUser(reporter);
		stressLimit(limit("task@product", product.name), "Too many new task for product: "+product.name);
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

	private void stessNewSite(User owner) {
		stressLimit(limit("site@user", owner.alias), "Too many new sites by user: "+owner.alias);
		stressUser(owner);
		stressLimit(limit("site", ORIGIN), "Too many new sites.");
		stressNewContent();
	}
	
	private void stressDoConfiguration(User user) {
		stressLimit(limit("configure@user", user.alias), "Too many recent configuration changes by user: "+user.alias);
		stressUser(user);
		stressLimit(limit("configure", ORIGIN), "Too many recent configuration changes.");
		stressAction();
	}
	
	private void stressDoConnect(Product product, User actor) {
		stressLimit(limit("connect@user", actor.alias), "Too many recent connections by user: "+actor.alias);
		stressUser(actor);
		stressLimit(limit("connect", ORIGIN), "Too many recent connections.");
		stressNewContent();
	}
	
	private void stressDoUpdate(Site site, User owner) {
		stressLimit(limit("update@user", owner.alias), "Too many recent site updates by user: "+owner.alias);
		stressUser(owner);
		stressLimit(limit("update", site.name), "Too many site updates for site: "+site.name);
		stressLimit(limit("update", ORIGIN), "Too many site updates recently.");
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

	private static void expectCanHaveMoreSites(Site[] inSameMenu) {
		if (inSameMenu.length >= 10)
			denyTransition(Error.E5_SITE_LIMIT_REACHED, 10);
	}

	private static void expectOwner(Site site, User actor) {
		if (!site.menu.equalTo(actor.alias))
			denyTransition(Error.E6_SITE_OWNERSHIP_REQUIRED, site.name);
	}

	private static void expectSiteDoesNotExist(Name site, Site[] inSameMenu) {
		for (Site s : inSameMenu)
			if (s.name.equalTo(site))
				denyTransition(Error.E7_SITE_EXISTS, site);
	}

	private static void expectOriginMaintainer(Product product, User user) {
		if (!product.origin.maintainers.contains(user.alias))
			denyTransition(Error.E8_PRODUCT_OWNERSHIP_REQUIRED, product.name, product.origin.name);
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
		if (task.participants() >= 5 
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
		if (!task.product.isIntegrated(url) && task.area.safeguarded)
			denyTransition(Error.E15_URL_NOT_INTEGRATED, url, task.product.integrations());
	}

	private static void expectVersion(Name name) {
		if (!(name.isVersion() || name.isRegular()))
			denyTransition(Error.E16_NAME_IS_NO_VERSION, name);
	}
	
	private static void expectRegular(Name name) {
		if (!name.isRegular())
			denyTransition(Error.E17_NAME_IS_NOT_REGULAR, name);
	}
	
	private static void expectEmail(Name name) {
		if (!name.isEmail())
			denyTransition(Error.E18_NAME_IS_NO_EMAIL, name);
	}

	private static void expectAuthenticated(User user) {
		if (!user.isAuthenticated())
			denyTransition(Error.E10_REQUIRES_AUTHENTICATION);
	}
	
	private static void expectCanConnect(Product product) {
		if (product.integrations.length >= 8)
			denyTransition(Error.E19_PRODUCT_INTEGRATION_LIMIT_REACHED, 8, product.name, product.integrations());
	}

	private static void expectCanWatch(User user) {
		if (!user.isAuthenticated())
			denyTransition(Error.E10_REQUIRES_AUTHENTICATION);
		if (!user.canWatch())
			denyTransition(Error.E20_USER_WATCH_LIMIT_REACHED);
	}
	
	private static void expectAdmin(Server server, User actor) {
		if (!server.isAdmin(actor)) {
			denyTransition(Error.E25_ADMIN_REQUIRED);
		}
	}

	private static void denyTransition(Error error, Object...args) {
		throw new TransitionDenied(error, args);
	}

	private long now() {
		return server.clock.time();
	}

}
