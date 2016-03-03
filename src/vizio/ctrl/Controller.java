package vizio.ctrl;

import vizio.Name;
import vizio.Task;
import vizio.User;
import vizio.store.Selection;
import vizio.view.View;

public interface Controller {

	Task[] select(Selection selection, Context ctx);

	View show(Name space, Name site);

	User user(Name user);
}
