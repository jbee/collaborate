package vizio.engine;

import vizio.model.IDN;
import vizio.model.Motive;
import vizio.model.Name;
import vizio.model.Names;
import vizio.model.Purpose;
import vizio.model.Poll.Matter;

/**
 * All the possible changes wrapped as lazy 'action'.
 * 
 * @author jan
 */
@FunctionalInterface
public interface Change {

	void apply(Tracker t, EntityManager em);
	
	static Change register(Name user, String email, String unsaltedMd5, String salt) {
		return (t, em) -> { t.register(user, email, unsaltedMd5, salt); };
	}
	
	static Change activate(Name user, byte[] activationKey) {
		return (t, em) -> { t.activate(em.user(user), activationKey); };
	}
	
	static Change found(Name product, Name originator) {
		return (t, em) -> { t.found(product, em.user(originator)); };
	}
	
	static Change open(Name product, Name entrance, Name originator, Motive motive, Purpose purpose) {
		return (t, em) -> { t.open(em.product(product), entrance, em.user(originator), motive, purpose); };
	}
	
	static Change compart(Name product, Name area, Name originator) {
		return (t, em) -> { t.compart(em.product(product), area, em.user(originator)); };
	}
	
	static Change compart(Name product, Name basis, Name partition, Name originator, boolean subarea) {
		return (t, em) -> { t.compart(em.area(product, basis), partition, em.user(originator), subarea); };
	}
	
	static Change leave(Name product, Name area, Name maintainer) {
		return (t, em) -> { t.leave(em.area(product, area), em.user(maintainer)); };
	}
	
	static Change relocate(Name product, IDN task, Name toArea, Name originator) {
		return (t, em) -> { t.relocate(em.task(product, task), em.area(product, toArea), em.user(originator)); };
	}
	
	static Change tag(Name product, Name version, Name originator) {
		return (t, em) -> { t.tag(em.product(product), version, em.user(originator)); };
	}
	
	static Change propose(Name product, String gist, Name reporter, Name area) {
		return (t, em) -> { t.reportProposal(em.product(product), gist, em.user(reporter), em.area(product, area)); };
	}
	
	static Change indicate(Name product, String gist, Name reporter, Name area) {
		return (t, em) -> { t.reportIntention(em.product(product), gist, em.user(reporter), em.area(product, area)); };
	}
	
	static Change warn(Name product, String gist, Name reporter, Name area, Name version, boolean exploitable) {
		return (t, em) -> { t.reportDefect(em.product(product), gist, em.user(reporter), em.area(product, area), em.version(product, version), exploitable); };
	}
	
	static Change request(Name product, String gist, Name reporter, Name entrance) {
		return (t, em) -> { t.reportRequest(em.product(product), gist, em.user(reporter), em.area(product, entrance)); };
	}
	
	static Change fork(Name product, IDN basis, Purpose purpose, String gist, Name reporter, Names changeset) {
		return (t, em) -> { t.reportFork(em.task(product, basis), purpose, gist, em.user(reporter), changeset); };
	}
	
	static Change absolve(Name product, IDN task, Name byUser, String conclusion) {
		return (t, em) -> { t.absolve(em.task(product, task), em.user(byUser), conclusion); };
	}
	
	static Change resolve(Name product, IDN task, Name byUser, String conclusion) {
		return (t, em) -> { t.resolve(em.task(product, task), em.user(byUser), conclusion); };
	}
	
	static Change dissolve(Name product, IDN task, Name byUser, String conclusion) {
		return (t, em) -> { t.dissolve(em.task(product, task), em.user(byUser), conclusion); };
	}
	
	static Change emphasise(Name product, IDN task, Name voter) {
		return (t, em) -> { t.emphasise(em.task(product, task), em.user(voter)); };
	}
	
	static Change poll(Matter matter, Name product, Name area, Name initiator, Name affected) {
		return (t, em) -> { t.poll(matter, em.area(product, area), em.user(initiator), em.user(affected)); };
	}
	
	static Change consent(Name product, Name area, IDN serial, Name voter) {
		return (t, em) -> { t.consent(em.poll(product, area, serial), em.user(voter)); };
	}
	
	static Change dissent(Name product, Name area, IDN serial, Name voter) {
		return (t, em) -> { t.dissent(em.poll(product, area, serial), em.user(voter)); };
	}
	
	static Change enlist(Name product, IDN task, Name user) {
		return (t, em) -> { t.enlist(em.task(product, task), em.user(user)); };
	}

	static Change abandon(Name product, IDN task, Name user) {
		return (t, em) -> { t.abandon(em.task(product, task), em.user(user)); };
	}

	static Change approach(Name product, IDN task, Name user) {
		return (t, em) -> { t.approach(em.task(product, task), em.user(user)); };
	}

	static Change watch(Name product, IDN task, Name user) {
		return (t, em) -> { t.watch(em.task(product, task), em.user(user)); };
	}
	
	static Change unwatch(Name product, IDN task, Name user) {
		return (t, em) -> { t.unwatch(em.task(product, task), em.user(user)); };
	}
	
	static Change launch(Name site, String template, Name owner) {
		return (t, em) -> { t.launch(site, template, em.user(owner)); };
	}
	
	static Change restructure(Name site, String template, Name owner) {
		return (t, em) -> { t.restructure(em.site(owner, site), template, em.user(owner)); };
	}
	
}