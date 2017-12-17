package se.jbee.track.engine;

import static se.jbee.track.engine.Change.Operation.abandon;
import static se.jbee.track.engine.Change.Operation.absolve;
import static se.jbee.track.engine.Change.Operation.archive;
import static se.jbee.track.engine.Change.Operation.attach;
import static se.jbee.track.engine.Change.Operation.authenticate;
import static se.jbee.track.engine.Change.Operation.compart;
import static se.jbee.track.engine.Change.Operation.configure;
import static se.jbee.track.engine.Change.Operation.confirm;
import static se.jbee.track.engine.Change.Operation.connect;
import static se.jbee.track.engine.Change.Operation.consent;
import static se.jbee.track.engine.Change.Operation.constitute;
import static se.jbee.track.engine.Change.Operation.disconnect;
import static se.jbee.track.engine.Change.Operation.dissent;
import static se.jbee.track.engine.Change.Operation.dissolve;
import static se.jbee.track.engine.Change.Operation.emphasise;
import static se.jbee.track.engine.Change.Operation.participate;
import static se.jbee.track.engine.Change.Operation.fork;
import static se.jbee.track.engine.Change.Operation.indicate;
import static se.jbee.track.engine.Change.Operation.launch;
import static se.jbee.track.engine.Change.Operation.leave;
import static se.jbee.track.engine.Change.Operation.open;
import static se.jbee.track.engine.Change.Operation.poll;
import static se.jbee.track.engine.Change.Operation.propose;
import static se.jbee.track.engine.Change.Operation.aspire;
import static se.jbee.track.engine.Change.Operation.rebase;
import static se.jbee.track.engine.Change.Operation.register;
import static se.jbee.track.engine.Change.Operation.relocate;
import static se.jbee.track.engine.Change.Operation.request;
import static se.jbee.track.engine.Change.Operation.resolve;
import static se.jbee.track.engine.Change.Operation.restructure;
import static se.jbee.track.engine.Change.Operation.tag;
import static se.jbee.track.engine.Change.Operation.unwatch;
import static se.jbee.track.engine.Change.Operation.warn;
import static se.jbee.track.engine.Change.Operation.watch;

import java.util.EnumMap;

import se.jbee.track.model.Attachments;
import se.jbee.track.model.Email;
import se.jbee.track.model.Entity;
import se.jbee.track.model.Gist;
import se.jbee.track.model.IDN;
import se.jbee.track.model.Mail;
import se.jbee.track.model.Motive;
import se.jbee.track.model.Name;
import se.jbee.track.model.Names;
import se.jbee.track.model.Poll.Matter;
import se.jbee.track.model.Product.Integration;
import se.jbee.track.model.Purpose;
import se.jbee.track.model.Template;
import se.jbee.track.model.User;
import se.jbee.track.model.User.Notifications;

/**
 * All the possible changes wrapped as lazy 'action'.
 * 
 * All {@link Change}s are constructed from keys like {@link Name}s and {@link IDN}s.
 * The actual loading and storing occurs when the {@link Change} is {@link #apply(Tracker, Tx)}ed.
 */
@FunctionalInterface
public interface Change {

	void apply(Tracker t, Tx tx);
	
	default Change and(Change next) {
		return (t, tx) -> { this.apply(t, tx); next.apply(t, tx); };
	}
	
	/**
	 * What can be done to tracker data 
	 */
	enum Operation {
		
		/*
		 * !!!OBS!!!
		 * ALWAYS ADD AT THE END (since ordinal is stored) 
		 */
		
		// users
		register,
		confirm,
		authenticate,
		name,
		configure,
		
		// sites
		launch,
		restructure,
		
		// products
		constitute,
		connect,
		disconnect,
		
		// areas
		open, 
		compart,
		leave,
		
		// versions
		tag,
		
		// polls
		poll,
		consent,
		dissent,		
		
		// tasks
		relocate,
		rebase,
		attach, 
		
		propose,
		indicate,
		warn,
		request,
		fork,
		
		absolve,
		resolve,
		dissolve,
		
		archive,
		
		emphasise,

		aspire,
		abandon,
		participate,
		
		watch,
		unwatch;
	}
	
	/**
	 * An application level transaction made available to a {@link Change}. 
	 */
	interface Tx extends Repository {

		void put(Operation op, Entity<?> e);
	}
	
	/**
	 * A new user registers using an alias and and email. 
	 * If no alias is provided the email is the alias.   
	 */
	static Change register(Name alias, Email email) {
		return (t, tx) -> { 
			User u = null;
			try { u = tx.user(alias); } catch (Repository.UnknownEntity e) { };
			tx.put(register, t.register(u, alias, email)); 
		};
	}
	
	static Change confirm(Name user) {
		return (t, tx) -> { tx.put(confirm, t.confirm(tx.user(user))); };
	}
	
	static Change authenticate(Name user, byte[] token) {
		return (t, tx) -> { tx.put(authenticate, t.authenticate(tx.user(user), token)); };
	}
	
	static Change name(Name email, Name name) {
		return (t, tx) -> { tx.put(Operation.name, t.name(tx.user(email), name)); };
	}
	
	static Change configure(Name user, EnumMap<Notifications, Mail.Delivery> notifications) {
		return (t, tx) -> { tx.put(configure, t.configure(tx.user(user), notifications)); };
	}
	
	static Change constitute(Name product, Name originator) {
		return (t, tx) -> { tx.put(constitute, t.constitute(product, tx.user(originator))); };
	}
	
	static Change connect(Name product, Integration endpoint, Name originator) {
		return (t, tx) -> { tx.put(connect, t.connect(tx.product(product), endpoint, tx.user(originator))); };
	}

	static Change disconnect(Name product, Name integration, Name originator) {
		return (t, tx) -> { tx.put(disconnect, t.disconnect(tx.product(product), integration, tx.user(originator))); };
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
	
	static Change rebase(Name product, IDN task, Name toVersion, Name originator) {
		return (t, tx) -> { tx.put(rebase, t.rebase(tx.task(product, task), tx.version(product, toVersion), tx.user(originator)));};
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
	
	static Change archive(Name product, IDN task, Name byUser) {
		return (t, tx) -> { tx.put(archive, t.archive(tx.task(product, task), tx.user(byUser))); };
	}
	
	static Change emphasise(Name product, IDN task, Name voter) {
		return (t, tx) -> { tx.put(emphasise, t.emphasise(tx.task(product, task), tx.user(voter))); };
	}
	
	static Change attach(Name product, IDN task, Name byUser, Attachments attachments) {
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
	
	static Change aspire(Name product, IDN task, Name user) {
		return (t, tx) -> { tx.put(aspire, t.aspire(tx.task(product, task), tx.user(user))); };
	}

	static Change participate(Name product, IDN task, Name user) {
		return (t, tx) -> { tx.put(participate, t.participate(tx.task(product, task), tx.user(user))); };
	}
	
	static Change abandon(Name product, IDN task, Name user) {
		return (t, tx) -> { tx.put(abandon, t.abandon(tx.task(product, task), tx.user(user))); };
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