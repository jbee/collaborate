package vizio.engine;

import vizio.engine.DB.Tx;
import vizio.model.IDN;
import vizio.model.URL;
import vizio.model.Motive;
import vizio.model.Name;
import vizio.model.Names;
import vizio.model.Poll.Matter;
import vizio.model.Purpose;

/**
 * All the possible changes wrapped as lazy 'action'.
 */
@FunctionalInterface
public interface Change {

	void apply(Tracker t, Tx tx);
	
	
	default Change and(Change next) {
		return (t, tx) -> { this.apply(t, tx); next.apply(t, tx); };
	}
	
	static Change register(Name user, String email, String unsaltedMd5, String salt) {
		return (t, tx) -> { tx.put(t.register(user, email, unsaltedMd5, salt)); };
	}
	
	static Change activate(Name user, byte[] activationKey) {
		return (t, tx) -> { tx.put(t.activate(tx.user(user), activationKey)); };
	}
	
	static Change constitute(Name product, Name originator) {
		return (t, tx) -> { tx.put(t.constitute(product, tx.user(originator))); };
	}
	
	static Change open(Name product, Name entrance, Name originator, Motive motive, Purpose purpose) {
		return (t, tx) -> { tx.put(t.open(tx.product(product), entrance, tx.user(originator), motive, purpose)); };
	}
	
	static Change compart(Name product, Name area, Name originator) {
		return (t, tx) -> { tx.put(t.compart(tx.product(product), area, tx.user(originator))); };
	}
	
	static Change compart(Name product, Name basis, Name partition, Name originator, boolean subarea) {
		return (t, tx) -> { tx.put(t.compart(tx.area(product, basis), partition, tx.user(originator), subarea)); };
	}
	
	static Change leave(Name product, Name area, Name maintainer) {
		return (t, tx) -> { tx.put(t.leave(tx.area(product, area), tx.user(maintainer))); };
	}
	
	static Change relocate(Name product, IDN task, Name toArea, Name originator) {
		return (t, tx) -> { tx.put(t.relocate(tx.task(product, task), tx.area(product, toArea), tx.user(originator))); };
	}
	
	static Change tag(Name product, Name version, Name originator) {
		return (t, tx) -> { tx.put(t.tag(tx.product(product), version, tx.user(originator))); };
	}
	
	static Change propose(Name product, String gist, Name reporter, Name area) {
		return (t, tx) -> { tx.put(t.reportProposal(tx.product(product), gist, tx.user(reporter), tx.area(product, area))); };
	}
	
	static Change indicate(Name product, String gist, Name reporter, Name area) {
		return (t, tx) -> { tx.put(t.reportIntention(tx.product(product), gist, tx.user(reporter), tx.area(product, area))); };
	}
	
	static Change warn(Name product, String gist, Name reporter, Name area, Name version, boolean exploitable) {
		return (t, tx) -> { tx.put(t.reportDefect(tx.product(product), gist, tx.user(reporter), tx.area(product, area), tx.version(product, version), exploitable)); };
	}
	
	static Change request(Name product, String gist, Name reporter, Name entrance) {
		return (t, tx) -> { tx.put(t.reportRequest(tx.product(product), gist, tx.user(reporter), tx.area(product, entrance))); };
	}
	
	static Change fork(Name product, IDN basis, Purpose purpose, String gist, Name reporter, Names changeset) {
		return (t, tx) -> { tx.put(t.reportFork(tx.task(product, basis), purpose, gist, tx.user(reporter), changeset)); };
	}
	
	static Change absolve(Name product, IDN task, Name byUser, String conclusion) {
		return (t, tx) -> { tx.put(t.absolve(tx.task(product, task), tx.user(byUser), conclusion)); };
	}
	
	static Change resolve(Name product, IDN task, Name byUser, String conclusion) {
		return (t, tx) -> { tx.put(t.resolve(tx.task(product, task), tx.user(byUser), conclusion)); };
	}
	
	static Change dissolve(Name product, IDN task, Name byUser, String conclusion) {
		return (t, tx) -> { tx.put(t.dissolve(tx.task(product, task), tx.user(byUser), conclusion)); };
	}
	
	static Change emphasise(Name product, IDN task, Name voter) {
		return (t, tx) -> { tx.put(t.emphasise(tx.task(product, task), tx.user(voter))); };
	}
	
	static Change attach(Name product, IDN task, Name byUser, URL... attachments) {
		return (t, tx) -> { tx.put(t.attach(tx.task(product, task), tx.user(byUser), attachments)); };
	}
	
	static Change poll(Matter matter, Name product, Name area, Name initiator, Name affected) {
		return (t, tx) -> { tx.put(t.poll(matter, tx.area(product, area), tx.user(initiator), tx.user(affected))); };
	}
	
	static Change consent(Name product, Name area, IDN serial, Name voter) {
		return (t, tx) -> { tx.put(t.consent(tx.poll(product, area, serial), tx.user(voter))); };
	}
	
	static Change dissent(Name product, Name area, IDN serial, Name voter) {
		return (t, tx) -> { tx.put(t.dissent(tx.poll(product, area, serial), tx.user(voter))); };
	}
	
	static Change enlist(Name product, IDN task, Name user) {
		return (t, tx) -> { tx.put(t.enlist(tx.task(product, task), tx.user(user))); };
	}

	static Change abandon(Name product, IDN task, Name user) {
		return (t, tx) -> { tx.put(t.abandon(tx.task(product, task), tx.user(user))); };
	}

	static Change approach(Name product, IDN task, Name user) {
		return (t, tx) -> { tx.put(t.approach(tx.task(product, task), tx.user(user))); };
	}

	static Change watch(Name product, IDN task, Name user) {
		return (t, tx) -> { tx.put(t.watch(tx.task(product, task), tx.user(user))); };
	}
	
	static Change unwatch(Name product, IDN task, Name user) {
		return (t, tx) -> { tx.put(t.unwatch(tx.task(product, task), tx.user(user))); };
	}
	
	static Change launch(Name site, String template, Name owner) {
		return (t, tx) -> { tx.put(t.launch(site, template, tx.user(owner))); };
	}
	
	static Change restructure(Name site, String template, Name owner) {
		return (t, tx) -> { tx.put(t.restructure(tx.site(owner, site), template, tx.user(owner))); };
	}
	
}