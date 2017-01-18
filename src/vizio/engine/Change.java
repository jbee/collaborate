package vizio.engine;

import static vizio.engine.Change.Type.abandon;
import static vizio.engine.Change.Type.absolve;
import static vizio.engine.Change.Type.activate;
import static vizio.engine.Change.Type.approach;
import static vizio.engine.Change.Type.attach;
import static vizio.engine.Change.Type.compart;
import static vizio.engine.Change.Type.consent;
import static vizio.engine.Change.Type.constitute;
import static vizio.engine.Change.Type.dissent;
import static vizio.engine.Change.Type.dissolve;
import static vizio.engine.Change.Type.emphasise;
import static vizio.engine.Change.Type.enlist;
import static vizio.engine.Change.Type.fork;
import static vizio.engine.Change.Type.indicate;
import static vizio.engine.Change.Type.launch;
import static vizio.engine.Change.Type.leave;
import static vizio.engine.Change.Type.open;
import static vizio.engine.Change.Type.poll;
import static vizio.engine.Change.Type.propose;
import static vizio.engine.Change.Type.register;
import static vizio.engine.Change.Type.relocate;
import static vizio.engine.Change.Type.request;
import static vizio.engine.Change.Type.resolve;
import static vizio.engine.Change.Type.restructure;
import static vizio.engine.Change.Type.tag;
import static vizio.engine.Change.Type.unwatch;
import static vizio.engine.Change.Type.warn;
import static vizio.engine.Change.Type.watch;
import vizio.model.Area;
import vizio.model.Email;
import vizio.model.Entity;
import vizio.model.Gist;
import vizio.model.IDN;
import vizio.model.Motive;
import vizio.model.Name;
import vizio.model.Names;
import vizio.model.Poll;
import vizio.model.Poll.Matter;
import vizio.model.Product;
import vizio.model.Purpose;
import vizio.model.Site;
import vizio.model.Task;
import vizio.model.Template;
import vizio.model.URL;
import vizio.model.User;
import vizio.model.Version;

/**
 * All the possible changes wrapped as lazy 'action'.
 */
@FunctionalInterface
public interface Change {

	void apply(Tracker t, Tx tx);
	
	default Change and(Change next) {
		return (t, tx) -> { this.apply(t, tx); next.apply(t, tx); };
	}
	
	enum Type {
		register,
		activate,
		constitute,
		open, 
		compart,
		leave,
		relocate,
		tag,
		propose,
		indicate,
		warn,
		request,
		fork,
		absolve,
		resolve,
		dissolve,
		emphasise,
		attach, 
		poll,
		consent,
		dissent,
		enlist,
		abandon,
		approach,
		watch,
		unwatch,
		launch,
		restructure
	}
	
	/**
	 * An application level transaction made available to a change. 
	 */
	interface Tx {

		User user(Name user);
		Site site(Name user, Name site);
		Poll poll(Name product, Name area, IDN serial);
		Product product(Name product);
		Area area(Name product, Name area);
		Version version(Name product, Name version);
		Task task(Name product, IDN id);

		void put(Type type, Entity<?> e);
	}
	
	static Change register(Name user, Email email, String unsaltedMd5, String salt) {
		return (t, tx) -> { tx.put(register, t.register(user, email, unsaltedMd5, salt)); };
	}
	
	static Change activate(Name user, byte[] activationKey) {
		return (t, tx) -> { tx.put(activate, t.activate(tx.user(user), activationKey)); };
	}
	
	static Change constitute(Name product, Name originator) {
		return (t, tx) -> { tx.put(constitute, t.constitute(product, tx.user(originator))); };
	}
	
	static Change open(Name product, Name board, Name originator, Motive motive, Purpose purpose) {
		return (t, tx) -> { tx.put(open, t.open(tx.product(product), board, tx.user(originator), motive, purpose)); };
	}
	
	static Change compart(Name product, Name area, Name originator) {
		return (t, tx) -> { tx.put(compart, t.compart(tx.product(product), area, tx.user(originator))); };
	}
	
	static Change compart(Name product, Name basis, Name partition, Name originator, boolean subarea) {
		return (t, tx) -> { tx.put(compart, t.compart(tx.area(product, basis), partition, tx.user(originator), subarea)); };
	}
	
	static Change leave(Name product, Name area, Name maintainer) {
		return (t, tx) -> { tx.put(leave, t.leave(tx.area(product, area), tx.user(maintainer))); };
	}
	
	static Change relocate(Name product, IDN task, Name toArea, Name originator) {
		return (t, tx) -> { tx.put(relocate, t.relocate(tx.task(product, task), tx.area(product, toArea), tx.user(originator))); };
	}
	
	static Change tag(Name product, Name version, Name originator) {
		return (t, tx) -> { tx.put(tag, t.tag(tx.product(product), version, tx.user(originator))); };
	}
	
	static Change propose(Name product, Gist gist, Name reporter, Name area) {
		return (t, tx) -> { tx.put(propose, t.reportProposal(tx.product(product), gist, tx.user(reporter), tx.area(product, area))); };
	}
	
	static Change indicate(Name product, Gist gist, Name reporter, Name area) {
		return (t, tx) -> { tx.put(indicate, t.reportIntention(tx.product(product), gist, tx.user(reporter), tx.area(product, area))); };
	}
	
	static Change warn(Name product, Gist gist, Name reporter, Name area, Name version, boolean exploitable) {
		return (t, tx) -> { tx.put(warn, t.reportDefect(tx.product(product), gist, tx.user(reporter), tx.area(product, area), tx.version(product, version), exploitable)); };
	}
	
	static Change request(Name product, Gist gist, Name reporter, Name board) {
		return (t, tx) -> { tx.put(request, t.reportRequest(tx.product(product), gist, tx.user(reporter), tx.area(product, board))); };
	}
	
	static Change fork(Name product, IDN basis, Purpose purpose, Gist gist, Name reporter, Names changeset) {
		return (t, tx) -> { tx.put(fork, t.reportFork(tx.task(product, basis), purpose, gist, tx.user(reporter), changeset)); };
	}
	
	static Change absolve(Name product, IDN task, Name byUser, Gist conclusion) {
		return (t, tx) -> { tx.put(absolve, t.absolve(tx.task(product, task), tx.user(byUser), conclusion)); };
	}
	
	static Change resolve(Name product, IDN task, Name byUser, Gist conclusion) {
		return (t, tx) -> { tx.put(resolve, t.resolve(tx.task(product, task), tx.user(byUser), conclusion)); };
	}
	
	static Change dissolve(Name product, IDN task, Name byUser, Gist conclusion) {
		return (t, tx) -> { tx.put(dissolve, t.dissolve(tx.task(product, task), tx.user(byUser), conclusion)); };
	}
	
	static Change emphasise(Name product, IDN task, Name voter) {
		return (t, tx) -> { tx.put(emphasise, t.emphasise(tx.task(product, task), tx.user(voter))); };
	}
	
	static Change attach(Name product, IDN task, Name byUser, URL... attachments) {
		return (t, tx) -> { tx.put(attach, t.attach(tx.task(product, task), tx.user(byUser), attachments)); };
	}
	
	static Change poll(Matter matter, Name product, Name area, Name initiator, Name affected) {
		return (t, tx) -> { tx.put(poll, t.poll(matter, tx.area(product, area), tx.user(initiator), tx.user(affected))); };
	}
	
	static Change consent(Name product, Name area, IDN serial, Name voter) {
		return (t, tx) -> { tx.put(consent, t.consent(tx.poll(product, area, serial), tx.user(voter))); };
	}
	
	static Change dissent(Name product, Name area, IDN serial, Name voter) {
		return (t, tx) -> { tx.put(dissent, t.dissent(tx.poll(product, area, serial), tx.user(voter))); };
	}
	
	static Change enlist(Name product, IDN task, Name user) {
		return (t, tx) -> { tx.put(enlist, t.enlist(tx.task(product, task), tx.user(user))); };
	}

	static Change abandon(Name product, IDN task, Name user) {
		return (t, tx) -> { tx.put(abandon, t.abandon(tx.task(product, task), tx.user(user))); };
	}

	static Change approach(Name product, IDN task, Name user) {
		return (t, tx) -> { tx.put(approach, t.approach(tx.task(product, task), tx.user(user))); };
	}

	static Change watch(Name product, IDN task, Name user) {
		return (t, tx) -> { tx.put(watch, t.watch(tx.task(product, task), tx.user(user))); };
	}
	
	static Change unwatch(Name product, IDN task, Name user) {
		return (t, tx) -> { tx.put(unwatch, t.unwatch(tx.task(product, task), tx.user(user))); };
	}
	
	static Change launch(Name owner, Name site, Template template) {
		return (t, tx) -> { tx.put(launch, t.launch(site, template, tx.user(owner))); };
	}
	
	static Change restructure(Name owner, Name site, Template template) {
		return (t, tx) -> { tx.put(restructure, t.restructure(tx.site(owner, site), template, tx.user(owner))); };
	}
	
}