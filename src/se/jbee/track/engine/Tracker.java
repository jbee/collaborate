package se.jbee.track.engine;

import static java.lang.Math.min;
import static se.jbee.track.engine.Limit.limit;
import static se.jbee.track.model.Date.date;
import static se.jbee.track.model.Motive.defect;
import static se.jbee.track.model.Motive.intention;
import static se.jbee.track.model.Motive.proposal;
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
import se.jbee.track.model.Product;
import se.jbee.track.model.Purpose;
import se.jbee.track.model.Site;
import se.jbee.track.model.Status;
import se.jbee.track.model.Task;
import se.jbee.track.model.Template;
import se.jbee.track.model.User;
import se.jbee.track.model.Version;
import se.jbee.track.model.Poll.Matter;
import se.jbee.track.model.Product.Integration;
import se.jbee.track.model.User.Notifications;
import se.jbee.track.util.Array;

/**
 * Implementation of the tracker-business logic.
 */
public final class Tracker {

	private static final long TOKEN_VALIDITY = 600000L;
	
	private final Clock clock;
	private final Limits limits;

	public Tracker(Clock clock, Limits limits) {
		super();
		this.clock = clock;
		this.limits = limits;
	}

	/* Users + Accounts */

	/**
	 * When a user is registered with just an email the name is also the email.  
	 */
	public User register(User user, Name name, Email email) {
		if (user != null)
			denyTransition("User with name "+name+" already exists.");
		if (!name.isEmail()) { // so emails or regular names are OK
			expectRegular(name);
		}
		stressNewUser();
		user = new User(1);
		user.name = name;
		user.email = email;
		user.notifications=new EnumMap<>(Notifications.class);
		user.authenticated = 0;
		user.sites = Names.empty();
		user.contributesToProducts = Names.empty();
		user.watches = 0;
		user.millisLastActive=now(); // cannot use touch as version should stay same
		confirmOTP(user);
		return user;
	}

	public User confirm(User user) {
		long now = clock.time();
		if (now < user.millisTokenExprired && now < user.millisTokenExprired-TOKEN_VALIDITY+60000L) {
			// protected against requesting to many tokens
			denyTransition("Wait a minute before requesting another token.");
		}
		user = user.clone();
		confirmOTP(user);
		touch(user);
		return user;
	}

	private void confirmOTP(User user) {
		stressDoConfirm();
		user.token = OTP.next();
		user.encryptedToken = OTP.encrypt(user.token);
		user.millisTokenExprired = clock.time() + TOKEN_VALIDITY;
	}

	public User authenticate(User user, byte[] token) {
		if (clock.time() > user.millisTokenExprired) {
			denyTransition("Token expired!");
		}
		if (!OTP.isToken(token, user.encryptedToken)) {
			denyTransition("Incorrect token!");
		}
		user = user.clone();
		user.authenticated++;
		user.token=null; // invalidate token
		user.millisTokenExprired=clock.time()-1L;
		touch(user);
		return user;
	}
	
	/**
	 * Name a user later on when first just an email was used. 
	 */
	public User name(User user, Name name) {
		expectEmail(user.name);
		expectRegular(name);
		stressDoConfiguration(user);
		user = user.clone();
		user.name = name;
		touch(user);
		return user;
	}
	
	public User configure(User user, EnumMap<Notifications, Mail.Delivery> notifications) {
		stressDoConfiguration(user);
		user = user.clone();
		user.notifications = notifications == null ? new EnumMap<>(Notifications.class) : notifications;
		touch(user);
		return user;
	}

	private void touch(User user) {
		user.touch(now());
	}

	/* Products */

	public Product constitute(Name product, User originator) {
		expectRegistered(originator);
		expectAuthenticated(originator);
		expectRegular(product);
		stressNewProduct(originator);
		Product p = new Product(1);
		p.name = product;
		p.tasks = 0;
		p.integrations = new Product.Integration[0];
		p.origin = compart(p.name, Name.ORIGIN, originator);
		p.somewhere = compart(p.name, Name.UNKNOWN, originator);
		p.somewhen = tag(p, Name.UNKNOWN, originator);
		originator.contributesToProducts = originator.contributesToProducts.add(product);
		touch(originator);
		return p;
	}
	
	public Product connect(Product product, Integration endpoint, User originator) {
		expectRegistered(originator);
		expectAuthenticated(originator);
		expectOriginMaintainer(product, originator);
		expectCanConnect(product);
		stressDoConnect(product, originator);
		int c = Array.indexOf(product.integrations, endpoint, Integration::equalTo);
		Integration[] source = product.integrations;
		if (c >= 0) {
			if (source[c].same(endpoint))
				return product;
			source = Array.remove(source, endpoint, Integration::equalTo);
		}
		product = product.clone();
		product.integrations = Array.add(source, endpoint, Integration::equalTo);
		touch(originator);
		return product;
	}
	
	public Product disconnect(Product product, Name integration, User originator) {
		expectRegistered(originator);
		expectAuthenticated(originator);
		expectOriginMaintainer(product, originator);
		stressDoConnect(product, originator);
		Integration endpoint = new Integration(integration, null);
		int c = Array.indexOf(product.integrations, endpoint, Integration::equalTo);
		if (c < 0)
			return product;
		product = product.clone();
		product.integrations = Array.remove(product.integrations, endpoint, Integration::equalTo);
		touch(originator);
		return product;
	}

	/* Areas */

	public Area open(Product product, Name boardArea, User originator, Motive motive, Purpose purpose) {
		Area area = compart(product, boardArea, originator);
		area.board=true;
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
		expectAuthenticated(originator);
		if (area.isEditable()) {
			expectRegular(area);
		}
		stressNewArea(product, originator);
		Area a = new Area(1);
		a.name = area;
		a.product = product;
		a.maintainers=new Names(originator.name);
		a.tasks = 0;
		a.polls = 0;
		originator.contributesToProducts = originator.contributesToProducts.add(product);
		touch(originator);
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

	public Task relocate(Task task, Area to, User originator) {
		expectNoBoard(task.area);
		expectAuthenticated(originator);
		if (task.area.name.isUnknown()) {
			expectMaintainer(to, originator); // pull from ~
		} else {
			expectMaintainer(task.area, originator); // push from
			if (!to.name.isUnknown()) {
				expectMaintainer(to, originator); // to some else than ~
			}
		}
		stressDoRelocate(task, to, originator);
		task = task.clone();
		task.area = to;
		touch(originator);
		return task;
	}

	/* Versions */

	public Version tag(Product product, Name version, User originator) {
		expectRegistered(originator);
		expectAuthenticated(originator);
		if (version.isEditable()) {
			expectRegular(version);
		}
		expectOriginMaintainer(product, originator);
		stressNewVersion(product, originator);
		Version v = new Version(1);
		v.product = product.name;
		v.name = version;
		v.changeset = Names.empty();
		touch(originator);
		return v;
	}

	/* Tasks */

	public Task reportProposal(Product product, Gist gist, User reporter, Area area) {
		expectNoBoard(area);
		return report(product, proposal, clarification, gist, reporter, area, product.somewhen, false);
	}

	public Task reportIntention(Product product, Gist gist, User reporter, Area area) {
		expectNoBoard(area);
		return report(product, intention, clarification, gist, reporter, area, product.somewhen, false);
	}

	public Task reportDefect(Product product, Gist gist, User reporter, Area area, Version version, boolean exploitable) {
		expectNoBoard(area);
		return report(product, defect, clarification, gist, reporter, area, version, exploitable);
	}

	public Task reportRequest(Product product, Gist gist, User reporter, Area board) {
		expectBoard(board);
		return report(product, board.motive, board.purpose, gist, reporter, board, product.somewhen, false);
	}

	public Task reportFork(Task cause, Purpose purpose, Gist gist, User reporter, Names changeset) {
		Area area = cause.area.board ? cause.product.somewhere : cause.area;
		Task task = report(cause.product, cause.motive, purpose, gist, reporter, area, cause.base, cause.exploitable);
		task.basis = cause.id;
		task.origin = cause.origin != null ? cause.origin : cause.id;
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
		task.id = new IDN(task.product.tasks);
		if (area.board) {
			task.area = area.clone();
			task.area.tasks++;
			task.serial=new IDN(task.area.tasks);
		} else {
			task.area = area;
		}
		task.base = version;
		task.reporter = reporter.name;
		task.reported = date(now());
		task.gist = gist;
		task.motive = motive;
		task.purpose = purpose;
		task.status = Status.unsolved;
		task.exploitable = exploitable;
		task.pursuedBy = Names.empty();
		task.engagedBy = Names.empty();
		task.watchedBy = new Names(reporter.name);
		task.changeset = Names.empty();
		task.attachments = Attachments.NONE;
		reporter.contributesToProducts = reporter.contributesToProducts.add(task.product.name);
		touch(reporter);
		return task;
	}
	
	public Task attach(Task task, User initiator, Attachments attachments) {
		if (!task.area.isOpen()) {
			expectAuthenticated(initiator);
			expectMaintainer(task.area, initiator);
		}
		stressDoAttach(task, initiator);
		task = task.clone();
		task.attachments = attachments;
		initiator.contributesToProducts = initiator.contributesToProducts.add(task.product.name);
		touch(initiator);
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
		int xp = (int) Math.max(base, base * (1f+((age-4f)/age))); // 1-2x base value, more with higher age
		if (task.exploitable && task.purpose == modification) {
			xp *= 2;
		}
		if (task.purpose != modification) {
			xp /= 2;
		}
		if (task.temperature(today) < 75) {
			xp /= 2;
		}
		return xp;
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
			task.base.changeset = task.changeset;
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
		task.solver = by.name;
		task.resolved = date(now());
		task.conclusion = conclusion;
		by.contributesToProducts = by.contributesToProducts.add(task.product.name);
		touch(by);
		return task;
	}

	/* User voting */

	public Task emphasise(Task task, User voter) {
		long now = now();
		if (voter.canEmphasise(now) && task.canBeEmphasisedBy(voter.name)) {
			voter.emphasised(now);
			task = task.clone();
			task.emphasise(date(now));
			touch(voter);
		}
		return task;
	}

	public Poll poll(Matter matter, Area area, User initiator, User affected) {
		expectRegistered(initiator);
		expectAuthenticated(initiator);
		if (matter != participation) {
			expectMaintainer(area, initiator);
		}
		stressNewPoll(area, initiator);
		Poll poll = new Poll(1);
		poll.area = area.clone();
		poll.area.polls++;
		poll.serial = new IDN(poll.area.polls);
		poll.matter = matter;
		poll.initiator = initiator.name;
		poll.affected = matter == Matter.abandonment ? Name.ORIGIN : affected.name;
		poll.start = date(now());
		poll.outcome = Outcome.unsettled;
		poll.consenting = Names.empty();
		poll.dissenting = Names.empty();
		poll.expiry = poll.start.plusDays(min(14, area.maintainers.count()));
		initiator.contributesToProducts = initiator.contributesToProducts.add(area.product);
		touch(initiator);
		return poll;
	}

	public Poll consent(Poll poll, User voter) {
		return vote(poll, voter, true);
	}

	public Poll dissent(Poll poll, User voter) {
		return vote(poll, voter, false);
	}

	private Poll vote(Poll poll, User voter, boolean consent) {
		expectRegistered(voter);
		expectAuthenticated(voter);
		if (poll.canVote(voter.name) && (
				consent && !poll.consenting.contains(voter.name)
			|| !consent && !poll.dissenting.contains(voter.name))) {
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
			if (poll.isSettled()) {
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
		poll.area = poll.area.clone();
		switch (poll.matter) {
		case abandonment:
			poll.area.abandoned=accepted; break;
		case inclusion:
			poll.area.exclusive=false; break;
		case exclusion:
			poll.area.exclusive=true; break;
		case resignation:
			poll.area.maintainers = poll.area.maintainers.remove(poll.affected); break;
		case participation:
			poll.area.maintainers = poll.area.maintainers.add(poll.affected);
		}
	}

	/* A user's task queues */

	public Task pursue(Task task, User user) {
		expectRegistered(user);
		expectAuthenticated(user);
		expectCanBeInvolved(user, task);
		if (task.engagedBy.contains(user) || !task.pursuedBy.contains(user)) {
			stressDoList(task, user);
			task = task.clone();
			task.engagedBy = task.engagedBy.remove(user);
			task.pursuedBy = task.pursuedBy.add(user);
			user.contributesToProducts = user.contributesToProducts.add(task.product.name);
			touch(user);
		}
		return task;
	}

	public Task abandon(Task task, User user) {
		expectRegistered(user);
		expectAuthenticated(user);
		expectCanBeInvolved(user, task);
		if (task.pursuedBy.contains(user) || task.engagedBy.contains(user)) {
			stressDoList(task, user);
			task = task.clone();
			task.pursuedBy = task.pursuedBy.remove(user);
			task.engagedBy = task.engagedBy.remove(user);
			touch(user);
		}
		return task;
	}

	public Task engage(Task task, User user) {
		expectRegistered(user);
		expectAuthenticated(user);
		expectCanBeInvolved(user, task);
		expectMaintainer(task.area, user);
		if (!task.engagedBy.contains(user) || task.pursuedBy.contains(user)) {
			stressDoList(task, user);
			task = task.clone();
			task.engagedBy = task.engagedBy.add(user);
			task.pursuedBy = task.pursuedBy.remove(user);
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
		if (!task.watchedBy.contains(user)) {
			stressDoList(task, user);
			task = task.clone();
			task.watchedBy = task.watchedBy.add(user);
			user.watches++;
			user.contributesToProducts = user.contributesToProducts.add(task.product.name);
			touch(user);
		}
		return task;
	}

	public Task unwatch(Task task, User user) {
		if (task.watchedBy.contains(user)) {
			stressDoList(task, user);
			task = task.clone();
			task.watchedBy = task.watchedBy.remove(user);
			user.watches--;
			touch(user);
		}
		return task;
	}

	/* A user's sites */

	public Site launch(Name site, Template template, User owner) {
		expectAuthenticated(owner);
		expectNoUserSiteYet(site, owner);
		expectCanHaveMoreSites(owner);
		stessNewSite(owner);
		Site s = new Site(1, owner.name, site, template);
		owner.sites = owner.sites.add(site);
		touch(owner);
		return s;
	}

	public Site restructure(Site site, Template template, User owner) {
		expectRegistered(owner);
		expectAuthenticated(owner);
		expectOwner(site, owner);
		stressDoUpdate(site, owner);
		site = site.clone();
		site.template = template;
		touch(owner);
		return site;
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
		stressLimit(limit("user", reporter.name), "Too many changes by user: "+reporter.name);
	}

	private void stressNewProduct(User originator) {
		stressLimit(limit("product@user", originator.name), "Too many recent products by user: "+originator.name);
		stressUser(originator);
		stressLimit(limit("product", ORIGIN), "Too many new products.");
		stressNewContent();
	}

	private void stressNewArea(Name product, User originator) {
		stressLimit(limit("area@user", originator.name), "Too many recent areas by user: "+originator.name);
		stressUser(originator);
		stressLimit(limit("area@product", product), "Too many new areas for product: "+product);
		stressLimit(limit("area", ORIGIN), "Too many new areas.");
		stressNewContent();
	}

	private void stressNewVersion(Product product, User originator) {
		stressLimit(limit("version@user", originator.name), "Too many recent versions by user: "+originator.name);
		stressUser(originator);
		stressLimit(limit("version@product", product.name), "Too many new versions for product: "+product.name);
		stressLimit(limit("version", ORIGIN), "Too many new versions.");
		stressNewContent();
	}

	private void stressNewTask(Product product, User reporter) {
		stressLimit(limit("task@user", reporter.name), "Too many recent tasks by user: "+reporter.name);
		stressUser(reporter);
		stressLimit(limit("task@product", product.name), "Too many new task for product: "+product.name);
		stressLimit(limit("task", ORIGIN), "Too many new tasks.");
		stressNewContent();
	}

	private void stressNewPoll(Area area, User initiator) {
		stressLimit(limit("poll@user", initiator.name), "Too many new polls by user: "+initiator.name);
		stressUser(initiator);
		stressLimit(limit("poll@area", area.name), "Too many new polls in area: "+area.name);
		stressLimit(limit("poll", ORIGIN), "Too many new polls.");
		stressNewContent();
	}

	private void stessNewSite(User owner) {
		stressLimit(limit("site@user", owner.name), "Too many new sites by user: "+owner.name);
		stressUser(owner);
		stressLimit(limit("site", ORIGIN), "Too many new sites.");
		stressNewContent();
	}
	
	private void stressDoConfiguration(User user) {
		stressLimit(limit("configure@user", user.name), "Too many recent configuration changes by user: "+user.name);
		stressUser(user);
		stressLimit(limit("configure", ORIGIN), "Too many recent configuration changes.");
		stressAction();
	}
	
	private void stressDoConnect(Product product, User originator) {
		stressLimit(limit("connect@user", originator.name), "Too many recent connections by user: "+originator.name);
		stressUser(originator);
		stressLimit(limit("connect", ORIGIN), "Too many recent connections.");
		stressNewContent();
	}
	
	private void stressDoUpdate(Site site, User owner) {
		stressLimit(limit("update@user", owner.name), "Too many recent site updates by user: "+owner.name);
		stressUser(owner);
		stressLimit(limit("update", site.name), "Too many site updates for site: "+site.name);
		stressLimit(limit("update", ORIGIN), "Too many site updates recently.");
		stressAction();		
	}

	private void stressDoRelocate(Task task, Area to, User originator) {
		stressLimit(limit("move@user", originator.name), "Too many recent relocations by user: "+originator.name);
		stressUser(originator);
		stressLimit(limit("move@area", to.name), "Too many relocations for area: "+to.name);
		stressLimit(limit("move@task", task.id.asName()), "Too many queue activities for task: "+task.id);
		stressLimit(limit("move", ORIGIN), "Too many relocations recently.");
		stressAction();
	}

	private void stressDoVote(Poll poll, User voter) {
		stressLimit(limit("vote@user", voter.name), "Too many recent votes by user: "+voter.name);
		stressUser(voter);
		stressLimit(limit("vote@poll", voter.name), "Too many recent votes in poll: "+poll.matter+" "+poll.affected);
		stressLimit(limit("vote", ORIGIN), "Too many votes recently.");
		stressAction();
	}

	private void stressDoList(Task task, User user) {
		stressLimit(limit("list@user", user.name), "Too many queue activities by user: "+user.name);
		stressUser(user);
		stressLimit(limit("list@task", task.id.asName()), "Too many queue activities for task: "+task.id);
		stressLimit(limit("list", ORIGIN), "Too many queue activities recently.");
		stressAction();
	}
	
	private void stressDoSolve(Task task, User by) {
		stressLimit(limit("solve@user", by.name), "Too many solution activities by user: "+by.name);
		stressUser(by);
		stressLimit(limit("solve@task", task.id.asName()), "Too many solution activities for task: "+task.id);
		stressLimit(limit("solve", ORIGIN), "Too many solution activities recently.");
		stressAction();
	}
	
	private void stressDoAttach(Task task, User by) {
		stressLimit(limit("attach@user", by.name), "Too many recent attachments by user: "+by.name);
		stressUser(by);
		stressLimit(limit("attach@task", task.id.asName()), "Too many recent attachments for task: "+task.id);
		stressLimit(limit("attach", ORIGIN), "Too many recent attachments.");
		stressAction();		
	}
	
	private void stressDoConfirm() {
		stressLimit(limit("confirm", ORIGIN), "Too many recent confirm requests.");
		stressAction();
	}
	
	private void stressLimit(Limit limit, String error) {
		if (!limits.stress(limit, clock)) {
			denyTransition("Limit exceeded! "+error+" Please try again later!");
		}
	}

	/* consistency rules */

	private static void expectNoBoard(Area area) {
		if (area.board) {
			denyTransition("Use request to submit to boards!");
		}
	}

	private static void expectBoard(Area area) {
		if (!area.board) {
			denyTransition("Request must be submitted within an board area!");
		}
	}

	private static void expectNotYetPublished(Version version) {
		if (version.isPublished()) {
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
		if (user.isAnonymous()) {
			denyTransition("Only registered users can create products and areas!");
		}
	}
	
	private static void expectCanReport(User reporter) {
		if (!reporter.isAuthenticated()) {
			denyTransition("Only authenticated users can report tasks!");
		}
	}

	private static void expectCanBeInvolved(User user, Task task) {
		if (task.participants() >= 5 && !task.pursuedBy.contains(user) && !task.engagedBy.contains(user)) {
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

	private static void expectRegular(Name name) {
		if (!name.isRegular()) {
			denyTransition("A registered user's name must not use '@' and be shorter than 17 characters! but was: "+name);
		}
	}
	
	private static void expectEmail(Name name) {
		if (!name.isEmail()) {
			denyTransition("Name can obly be changed if previously an email was used but was: "+name);
		}
	}

	private static void expectAuthenticated(User user) {
		if (!user.isAuthenticated()) {
			denyTransition("User account must be authenticated first!");
		}
	}
	
	private static void expectCanConnect(Product product) {
		if (product.integrations.length >= 8) {
			denyTransition("Integrations are limited to 8 per product!");
		}
	}

	private static void expectCanWatch(User user) {
		if (!user.isAuthenticated()) {
			denyTransition("Only authenticated users can watch");
		}
		if (!user.canWatch()) {
			denyTransition("User has reached maximum number of watched tasks. Unwatch tasks or increase limit by closing tasks.");
		}
	}

	private static void denyTransition(String reason) {
		throw new IllegalStateException(reason);
	}

	private long now() {
		return clock.time();
	}

}
