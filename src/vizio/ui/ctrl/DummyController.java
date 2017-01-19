package vizio.ui.ctrl;

import static java.lang.System.currentTimeMillis;
import static vizio.model.Email.email;
import static vizio.model.Gist.gist;
import static vizio.model.Name.as;

import java.util.Arrays;
import java.util.Random;

import vizio.engine.Tracker;
import vizio.io.Criteria;
import vizio.model.Area;
import vizio.model.Name;
import vizio.model.Product;
import vizio.model.Site;
import vizio.model.Task;
import vizio.model.Template;
import vizio.model.User;
import vizio.model.Version;
import vizio.ui.view.Coloring;
import vizio.ui.view.Menu;
import vizio.ui.view.View;
import vizio.ui.view.View.Silo;
import vizio.ui.view.Widget;

public class DummyController implements Controller {

	private long now;
	private User user;
	private Task[] tasks;

	public DummyController() {
		init();
	}

	private void init() {
		now = currentTimeMillis();
		Tracker tracker = new Tracker(() -> { now += 70000; return now; }, (l) -> true );
		tasks = new Task[5];
		user = tracker.register(as("tester"), email("test@example.com"), "xxx", "salt");
		user = tracker.activate(user, user.md5);
		Product product = tracker.constitute(as("vizio"), user);
		Area area = tracker.compart(product, as("core"), user);
		Area ui = tracker.compart(product, as("ui"), user);
		Version v0_1 = tracker.tag(product, as("0.1"), user);
		tasks[0] = tracker.reportDefect(product, gist("Something is wrong with..."), user, area, product.somewhen, false);
		tasks[1] = tracker.reportDefect(product, gist("Regression for 0.1 showed bug..."), user, area, v0_1, true);
		tasks[2] = tracker.reportProposal(product, gist("We should count ..."), user, product.origin);
		tasks[3] = tracker.reportIntention(product, gist("At some point the tracker should be released"), user, product.origin);
		tasks[4] = tracker.reportProposal(product, gist("Use bold text for everything important"), user, ui);
		tasks[1] = tracker.pursue(tasks[1], user);
		tasks[2] = tracker.engage(tasks[2], user);
		tasks[0].emphasis = 97;
		tasks[1].emphasis = 78;
		tasks[2].emphasis = 56;
		tasks[3].emphasis = 28;
		tasks[4].emphasis = 14;
	}

	@Override
	public Task[] tasks(Criteria selection, Context ctx) {
		return Arrays.copyOf(tasks, new Random().nextInt(tasks.length)+1) ;
	}

	@Override
	public User user(Name user) {
		return this.user;
	}

	@Override
	public Menu[] menus(Context ctx) {
		return new Menu[] {
				new Menu("My", Action.my, new Site(1, Name.MY, Name.as("dashboard"), Template.BLANK_PAGE)),
				new Menu("jan's", Action.user,
						new Site(1, as("jan"), Name.as("@home"), Template.BLANK_PAGE),
						new Site(1, as("jan"), Name.as("special"), Template.BLANK_PAGE)
						)
				};
	}

	@Override
	public View view(Context ctx) {
        Widget left = new Widget("Assorted tasks", Coloring.heat, new Criteria());
        Widget right = new Widget("Some others...", Coloring.goal, new Criteria());
        Widget right2 = new Widget("And more", Coloring.motive, new Criteria());
		return new View(new Silo("My Tasks", left), new Silo("Inbox", right), new Silo("Urgent", right2));
	}

}
