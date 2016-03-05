package vizio.ctrl;

import static java.lang.System.currentTimeMillis;
import static vizio.Name.as;
import vizio.Area;
import vizio.Name;
import vizio.Product;
import vizio.Site;
import vizio.Task;
import vizio.Tracker;
import vizio.User;
import vizio.Version;
import vizio.store.Selection;
import vizio.view.Coloring;
import vizio.view.Menu;
import vizio.view.View;
import vizio.view.View.Silo;
import vizio.view.Widget;

public class DummyController implements Controller {

	private long now;
	private User user;
	private Task[] tasks;
	
	public DummyController() {
		init();
	}
	
	private void init() {
		now = currentTimeMillis();
		Tracker tracker = new Tracker(() -> { now += 70000; return now; } );
		tasks = new Task[5];
		user = tracker.register(as("tester"), "test@example.com", "xxx");
		tracker.activate(user, user.md5);
		Product product = tracker.initiate(as("vizio"), user);
		Area area = tracker.compart(product, as("core"), user);
		Area ui = tracker.compart(product, as("ui"), user);
		Version v0_1= tracker.tag(product, as("v0.1"), user);
		tasks[0] = tracker.reportDefect(product, "Something is wrong with...", user, area, product.somewhen, false);
		tasks[1] = tracker.reportDefect(product, "Regression for 0.1 showed bug...", user, area, v0_1, true);
		tasks[2] = tracker.reportProposal(product, "We should count ...", user, product.origin);
		tasks[3] = tracker.reportIntention(product, "At some point the tracker should be released", user, product.origin);
		tasks[4] = tracker.reportProposal(product, "Use bold text for everything important", user, ui);
		tracker.target(tasks[1], user);
		tracker.approach(tasks[2], user);
		tasks[0].heat = 97;
		tasks[1].heat = 78;
		tasks[2].heat = 56;
		tasks[3].heat = 28;
		tasks[4].heat = 14;
	}	
	
	@Override
	public Task[] tasks(Selection selection, Context ctx) {
		return tasks;
	}

	@Override
	public User user(Name user) {
		return this.user;
	}

	@Override
	public Menu[] menus(Context ctx) {
		return new Menu[] { 
				new Menu("My", new Site(Name.MY, Name.as("dashboard"), "")),
				new Menu("User jan", 
						new Site(as("jan"), Name.as("home"), ""),
						new Site(as("jan"), Name.as("special"), "")
						)
				};
	}
	
	@Override
	public View view(Name space, Name site) {
        Widget left = new Widget("Assorted tasks", Coloring.temp, new Selection());
        Widget right = new Widget("Some others...", Coloring.goal, new Selection());
		return new View("Test", new Silo(left), new Silo(right));
	}

}
