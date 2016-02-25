package vizio;

import static vizio.Date.date;
import static vizio.Status.absolved;
import static vizio.Status.dissolved;
import static vizio.Status.resolved;

/**
 * Implementation of the business logic of VIZIO.
 * Mostly to see the data-dependencies.
 *
 * @author jan
 */
public class Tracker {

	private final Clock clock;

	public Tracker(Clock clock) {
		super();
		this.clock = clock;
	}

	public User register(String email) {
		User user = new User();
		user.email = email;
		touch(user);
		return user;
	}

	private void touch(User user) {
		user.lastActive = Date.date(clock.time());
	}

	/* User created entities */

	public Product introduce(Name product, User initiator) {
		Product p = new Product();
		p.name = product;
		touch(initiator);
		return p;
	}

	public Area structure(Name product, Name area, User initiator) {
		Area a = new Area();
		a.name = area;
		a.product = product;
		a.maintainers=new Names(initiator.name);
		touch(initiator);
		return a;
	}

	public Task track(Motive motive, Goal goal, String summay, User initiator) {
		Task task = new Task();
		task.creator = initiator.name;
		task.start = date(clock.time());
		task.summary = summay;
		task.status = Status.unsolved;
		task.motive = motive;
		task.goal = goal;
		task.usersMarked = Names.empty();
		task.usersStarted = Names.empty();
		touch(initiator);
		return task;
	}

	/* User initiated entity changes */

	public void relocate(Task task, Area to, User initiator) {
		if (task.area == null || task.area.maintainers.contains(initiator)) {
			task.area = to;
			task.product = to.product;
			touch(initiator);
		}
	}

	public void leave(Area area, User maintainer) {
		if (area.maintainers.contains(maintainer)) {
			area.maintainers.remove(maintainer);
			touch(maintainer);
		}
	}

	public void pursue(Task cause, Task effect) {
		effect.cause = cause.id;
		effect.origin = cause.origin != null ? cause.origin : cause.id;
	}

	/* User voting */

	public void consent(Vote vote, User voter) {
		if (vote.area.maintainers.contains(voter)
				&& vote.affected.equalTo(voter.name)) {
			vote.consenting.add(voter);
			touch(voter);
		}
	}

	public void dissent(Vote vote, User voter) {
		if (vote.area.maintainers.contains(voter)
				&& vote.affected.equalTo(voter.name)) {
			vote.dissenting.add(voter);
			touch(voter);
		}
	}

	public void lift(Task task, User voter) {
		Date today = date(clock.time());
		if (voter.canLift(today)) {
			voter.lift(today);
			task.heat(today);
			touch(voter);
		}
	}

	/* A user's task queue */

	public void mark(Task task, User user) {
		task.usersStarted.remove(user);
		task.usersMarked.add(user);
		checkUserCount(task);
		touch(user);
	}

	public void drop(Task task, User user) {
		task.usersMarked.remove(user);
		task.usersStarted.remove(user);
		checkUserCount(task);
		touch(user);
	}

	public void start(Task task, User user) {
		task.usersStarted.add(user);
		task.usersMarked.remove(user);
		touch(user);
	}

	private void checkUserCount(Task task) {
		if (task.users() > 5) {
			throw new IllegalArgumentException("Max users!");
		}
	}

	/* task resolution */

	public void absolve(Task task, User user) {
		task.status = absolved;
		user.absolved++;
		touch(user);
	}

	public void resolve(Task task, User user) {
		task.status = resolved;
		user.xp += 2;
		user.resolved++;
		touch(user);
	}

	public void dissolve(Task task, User user) {
		task.status = dissolved;
		user.xp += 5;
		user.dissolved++;
		touch(user);
	}

}
