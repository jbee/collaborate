package vizio;

import static vizio.Status.absolved;
import static vizio.Status.dissolved;
import static vizio.Status.resolved;

/**
 * Implementation of the business logic of VIZIO.
 * Mostly to see the data-dependencies.
 *
 * @author jan
 *
 */
public class VIZIO {

	public static void vote(User user, Task task) {
		if (user.canVote()) {
			user.vote();
			task.heat();
		}
	}

	public static void mark(User user, Task task) {
		task.started.remove(user);
		task.marked.add(user);
	}

	public static void drop(User user, Task task) {
		task.marked.remove(user);
		task.started.remove(user);
	}

	public static void start(User user, Task task) {
		task.started.add(user);
		task.marked.remove(user);
	}

	public static void absolve(User user, Task task) {
		task.status = absolved;
		user.absolved++;
	}

	public static void resolve(User user, Task task) {
		task.status = resolved;
		user.xp += 2;
		user.resolved++;
	}

	public static void dissolve(User user, Task task) {
		task.status = dissolved;
		user.xp += 5;
		user.dissolved++;
	}

	public static void connect(Task step1, Task step2) {
		step2.origin = step1.id;
		step2.chronicle = step1.chronicle != null ? step1.chronicle : step1.id;
	}
}
