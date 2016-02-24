package vizio;

import static vizio.Date.today;
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

	public static User register(String email) {
		User user = new User();
		user.email = email;
		touch(user);
		return user;
	}

	private static void touch(User user) {
		user.lastActive = today();
	}

	/* User created entities */

	public static Product introduce(String product, User initiator) {
		Product p = new Product();
		p.name = product;
		touch(initiator);
		return p;
	}

	public static Area structure(String area, User initiator) {
		Area a = new Area();
		a.name = area;
		a.maintainers.add(initiator);
		touch(initiator);
		return a;
	}

	public static Task track(Stimulus stimulus, Goal goal, String summay, User initiator) {
		Task task = new Task();
		task.creator = initiator.id;
		task.summary = summay;
		task.status = Status.unsolved;
		task.stimulus = stimulus;
		task.goal = goal;
		touch(initiator);
		return task;
	}

	/* User initiated entity changes */

	public static void relocate(Task task, Area to, User initiator) {
		if (task.area.maintainers.contains(initiator)) {
			task.area = to;
			touch(initiator);
		}
	}

	public static void chain(Task step1, Task step2) {
		step2.origin = step1.num;
		step2.chronicle = step1.chronicle != null ? step1.chronicle : step1.num;
	}

	/* User voting */

	public static void consent(Vote vote, User voter) {
		if (vote.area.maintainers.contains(voter)
				&& vote.affected.id != voter.id.id) {
			vote.consenting.add(voter);
			touch(voter);
		}
	}

	public static void dissent(Vote vote, User voter) {
		if (vote.area.maintainers.contains(voter)
				&& vote.affected.id != voter.id.id) {
			vote.dissenting.add(voter);
			touch(voter);
		}
	}

	public static void lift(Task task, User voter) {
		if (voter.canLift()) {
			voter.lift();
			task.heat();
			touch(voter);
		}
	}

	/* A user's task queue */

	public static void mark(User user, Task task) {
		task.started.remove(user);
		task.marked.add(user);
		touch(user);
	}

	public static void drop(User user, Task task) {
		task.marked.remove(user);
		task.started.remove(user);
		touch(user);
	}

	public static void start(User user, Task task) {
		task.started.add(user);
		task.marked.remove(user);
		touch(user);
	}

	/* task resolution */

	public static void absolve(User user, Task task) {
		task.status = absolved;
		user.absolved++;
		touch(user);
	}

	public static void resolve(User user, Task task) {
		task.status = resolved;
		user.xp += 2;
		user.resolved++;
		touch(user);
	}

	public static void dissolve(User user, Task task) {
		task.status = dissolved;
		user.xp += 5;
		user.dissolved++;
		touch(user);
	}

}
