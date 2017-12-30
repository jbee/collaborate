package se.jbee.track.ui.ctrl;

import static java.lang.System.currentTimeMillis;
import static se.jbee.track.model.Email.email;
import static se.jbee.track.model.Gist.gist;
import static se.jbee.track.model.Name.as;

import java.util.Arrays;
import java.util.Random;

import se.jbee.track.cache.Criteria;
import se.jbee.track.cache.Criteria.Coloring;
import se.jbee.track.engine.NoLimits;
import se.jbee.track.engine.Tracker;
import se.jbee.track.model.Area;
import se.jbee.track.model.Name;
import se.jbee.track.model.Product;
import se.jbee.track.model.Site;
import se.jbee.track.model.Task;
import se.jbee.track.model.Template;
import se.jbee.track.model.User;
import se.jbee.track.model.Version;
import se.jbee.track.ui.view.Menu;
import se.jbee.track.ui.view.View;
import se.jbee.track.ui.view.View.Silo;
import se.jbee.track.ui.view.Widget;

public class DummyController implements Controller {

	private long now;
	private User user;
	private Task[] tasks;

	public DummyController() {
		init();
	}

	private void init() {
		now = currentTimeMillis();
		Tracker tracker = new Tracker(() -> { now += 70000; return now; }, new NoLimits() );
		tasks = new Task[5];
		user = tracker.register(null, as("tester"), email("test@example.com"));
		user = tracker.authenticate(user, user.otp);
		Product product = tracker.constitute(as("vizio"), user);
		Area area = tracker.compart(product, as("core"), user);
		Area ui = tracker.compart(product, as("ui"), user);
		Version v0_1 = tracker.tag(product, as("0.1"), user);
		tasks[0] = tracker.reportDefect(product, gist("Something is wrong with..."), user, area, product.somewhen, false);
		tasks[1] = tracker.reportDefect(product, gist("Regression for 0.1 showed bug..."), user, area, v0_1, true);
		tasks[2] = tracker.reportProposal(product, gist("We should count ..."), user, product.origin);
		tasks[3] = tracker.reportIntention(product, gist("At some point the tracker should be released"), user, product.origin);
		tasks[4] = tracker.reportProposal(product, gist("Use bold text for everything important"), user, ui);
		tasks[1] = tracker.aspire(tasks[1], user);
		tasks[2] = tracker.participate(tasks[2], user);
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
				new Menu(as("jan"), Action.user,
						new Site(1, as("jan"), Name.as("@home"), Template.BLANK_PAGE),
						new Site(1, as("jan"), Name.as("special"), Template.BLANK_PAGE)
						),
				new Menu(Name.MY, Action.my, new Site(1, Name.MY, Name.as("dashboard"), Template.BLANK_PAGE)),
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
