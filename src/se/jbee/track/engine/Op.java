package se.jbee.track.engine;

import static se.jbee.track.engine.Change.Operation.abandon;
import static se.jbee.track.engine.Change.Operation.absolve;
import static se.jbee.track.engine.Change.Operation.archive;
import static se.jbee.track.engine.Change.Operation.aspire;
import static se.jbee.track.engine.Change.Operation.attach;
import static se.jbee.track.engine.Change.Operation.authenticate;
import static se.jbee.track.engine.Change.Operation.categorise;
import static se.jbee.track.engine.Change.Operation.compart;
import static se.jbee.track.engine.Change.Operation.compose;
import static se.jbee.track.engine.Change.Operation.configure;
import static se.jbee.track.engine.Change.Operation.confirm;
import static se.jbee.track.engine.Change.Operation.connect;
import static se.jbee.track.engine.Change.Operation.consent;
import static se.jbee.track.engine.Change.Operation.envision;
import static se.jbee.track.engine.Change.Operation.detach;
import static se.jbee.track.engine.Change.Operation.disclose;
import static se.jbee.track.engine.Change.Operation.disconnect;
import static se.jbee.track.engine.Change.Operation.dissent;
import static se.jbee.track.engine.Change.Operation.dissolve;
import static se.jbee.track.engine.Change.Operation.emphasise;
import static se.jbee.track.engine.Change.Operation.erase;
import static se.jbee.track.engine.Change.Operation.indicate;
import static se.jbee.track.engine.Change.Operation.leave;
import static se.jbee.track.engine.Change.Operation.open;
import static se.jbee.track.engine.Change.Operation.participate;
import static se.jbee.track.engine.Change.Operation.poll;
import static se.jbee.track.engine.Change.Operation.propose;
import static se.jbee.track.engine.Change.Operation.rebase;
import static se.jbee.track.engine.Change.Operation.recompose;
import static se.jbee.track.engine.Change.Operation.register;
import static se.jbee.track.engine.Change.Operation.relocate;
import static se.jbee.track.engine.Change.Operation.remind;
import static se.jbee.track.engine.Change.Operation.request;
import static se.jbee.track.engine.Change.Operation.resolve;
import static se.jbee.track.engine.Change.Operation.segment;
import static se.jbee.track.engine.Change.Operation.tag;
import static se.jbee.track.engine.Change.Operation.unwatch;
import static se.jbee.track.engine.Change.Operation.warn;
import static se.jbee.track.engine.Change.Operation.watch;

import java.util.EnumMap;

import se.jbee.track.engine.Change.Operation;
import se.jbee.track.engine.Change.Tx;
import se.jbee.track.model.Email;
import se.jbee.track.model.Gist;
import se.jbee.track.model.IDN;
import se.jbee.track.model.Mail;
import se.jbee.track.model.Motive;
import se.jbee.track.model.Name;
import se.jbee.track.model.Names;
import se.jbee.track.model.Output.Integration;
import se.jbee.track.model.Poll;
import se.jbee.track.model.Poll.Matter;
import se.jbee.track.model.Purpose;
import se.jbee.track.model.Template;
import se.jbee.track.model.URL;
import se.jbee.track.model.User;

/**
 * Just a place for the {@link Tracker}s {@link Change} {@link Operation}s to be. 
 *  
 * @author jan
 */
public final class Op {

	/**
	 * A new user registers using an alias and and email. 
	 * If no alias is provided the email is the alias.   
	 */
	public static Change register(Name alias, Email email) {
		return (t, tx) -> { 
			User u = null;
			try { u = tx.user(alias); } catch (Repository.UnknownEntity e) { };
			tx.put(register, t.register(u, alias, email)); 
		};
	}
	
	public static Change confirm(Name user) {
		return (t, tx) -> { tx.put(confirm, t.confirm(tx.user(user))); };
	}
	
	public static Change authenticate(Name user, byte[] otp) {
		return (t, tx) -> { tx.put(authenticate, t.authenticate(tx.user(user), otp)); };
	}
	
	public static Change name(Name email, Name name) {
		return (t, tx) -> { tx.put(Operation.name, t.name(tx.user(email), name)); };
	}
	
	public static Change configure(Name user, EnumMap<Mail.Notification, Mail.Delivery> notifications) {
		return (t, tx) -> { tx.put(configure, t.configure(tx.user(user), notifications)); };
	}
	
	public static Change envision(Name output, Name actor) {
		return (t, tx) -> { tx.put(envision, t.envision(output, tx.user(actor))); };
	}
	
	public static Change connect(Name output, Integration endpoint, Name actor) {
		return (t, tx) -> { tx.put(connect, t.connect(tx.output(output), endpoint, tx.user(actor))); };
	}

	public static Change disconnect(Name output, Name integration, Name actor) {
		return (t, tx) -> { tx.put(disconnect, t.disconnect(tx.output(output), integration, tx.user(actor))); };
	}

	public static Change open(Name output, Name board, Name actor, Motive motive, Purpose purpose) {
		return (t, tx) -> { tx.put(open, t.open(tx.output(output), board, tx.user(actor), motive, purpose)); };
	}
	
	public static Change compart(Name output, Name area, Name actor) {
		return (t, tx) -> { tx.put(compart, t.compart(tx.output(output), area, tx.user(actor))); };
	}
	
	public static Change compart(Name output, Name basis, Name partition, Name actor, boolean subarea) {
		return (t, tx) -> { tx.put(compart, t.compart(tx.area(output, basis), partition, tx.user(actor), subarea)); };
	}
	
	public static Change leave(Name output, Name area, Name leavingMaintainer) {
		return (t, tx) -> { 
			tx.put(leave, t.leave(tx.area(output, area), tx.user(leavingMaintainer)));
			recount(leave, t,tx, output, area, leavingMaintainer, leavingMaintainer);
		};
	}
	
	public static Change categorise(Name output, Name area, Name category, Name actor) {
		return (t, tx) -> {	tx.put(categorise, t.categorise(tx.area(output, area), category, tx.user(actor))); };
	}

	public static Change relocate(Name output, IDN task, Name toArea, Name actor) {
		return (t, tx) -> { tx.put(relocate, t.relocate(tx.task(output, task), tx.area(output, toArea), tx.user(actor))); };
	}
	
	public static Change rebase(Name output, IDN task, Name toVersion, Name actor) {
		return (t, tx) -> { tx.put(rebase, t.rebase(tx.task(output, task), tx.version(output, toVersion), tx.user(actor)));};
	}
	
	public static Change tag(Name output, Name version, Name actor) {
		return (t, tx) -> { tx.put(tag, t.tag(tx.output(output), version, tx.user(actor))); };
	}
	
	public static Change propose(Name output, Gist gist, Name reporter, Name area) {
		return (t, tx) -> { tx.put(propose, t.reportProposal(tx.output(output), gist, tx.user(reporter), tx.area(output, area))); };
	}
	
	static Change indicate(Name output, Gist gist, Name reporter, Name area) {
		return (t, tx) -> { tx.put(indicate, t.reportNecessity(tx.output(output), gist, tx.user(reporter), tx.area(output, area))); };
	}
	
	public static Change remind(Name output, Gist gist, Name reporter, Name area) {
		return (t, tx) -> { tx.put(remind, t.reportThought(tx.output(output), gist, tx.user(reporter), tx.area(output, area))); };
	}
	
	public static Change warn(Name output, Gist gist, Name reporter, Name area, Name version, boolean exploitable) {
		return (t, tx) -> { tx.put(warn, t.reportDefect(tx.output(output), gist, tx.user(reporter), tx.area(output, area), tx.version(output, version), exploitable)); };
	}
	
	public static Change request(Name output, Gist gist, Name reporter, Name board) {
		return (t, tx) -> { tx.put(request, t.reportRequest(tx.output(output), gist, tx.user(reporter), tx.area(output, board))); };
	}
	
	public static Change segment(Name output, IDN basis, Purpose purpose, Gist gist, Name reporter, Names changeset) {
		return (t, tx) -> { tx.put(segment, t.reportSegment(tx.task(output, basis), purpose, gist, tx.user(reporter), changeset)); };
	}
	
	public static Change absolve(Name output, IDN task, Name byUser, Gist conclusion) {
		return (t, tx) -> { tx.put(absolve, t.absolve(tx.task(output, task), tx.user(byUser), conclusion)); };
	}
	
	public static Change resolve(Name output, IDN task, Name byUser, Gist conclusion) {
		return (t, tx) -> { tx.put(resolve, t.resolve(tx.task(output, task), tx.user(byUser), conclusion)); };
	}
	
	public static Change dissolve(Name output, IDN task, Name byUser, Gist conclusion) {
		return (t, tx) -> { tx.put(dissolve, t.dissolve(tx.task(output, task), tx.user(byUser), conclusion)); };
	}
	
	public static Change archive(Name output, IDN task, Name byUser) {
		return (t, tx) -> { tx.put(archive, t.archive(tx.task(output, task), tx.user(byUser))); };
	}
	
	public static Change emphasise(Name output, IDN task, Name voter) {
		return (t, tx) -> { tx.put(emphasise, t.emphasise(tx.task(output, task), tx.user(voter))); };
	}
	
	public static Change disclose(Name output, IDN task, Name actor) {
		return (t, tx) -> { tx.put(disclose, t.disclose(tx.task(output, task), tx.user(actor))); };
	}
	
	public static Change attach(Name output, IDN task, Name byUser, URL attachment) {
		return (t, tx) -> { tx.put(attach, t.attach(tx.task(output, task), tx.user(byUser), attachment)); };
	}
	
	public static Change detach(Name output, IDN task, Name byUser, URL attachment) {
		return (t, tx) -> { tx.put(detach, t.detach(tx.task(output, task), tx.user(byUser), attachment)); };
	}
	
	public static Change poll(Matter matter, Gist motivation, Name output, Name area, Name actor, Name affected) {
		return (t, tx) -> { tx.put(poll, t.poll(matter, motivation, tx.area(output, area), tx.user(actor), tx.user(affected))); };
	}
	
	public static Change consent(Name output, Name area, IDN serial, Name voter) {
		return (t, tx) -> { 
			Poll poll = t.consent(tx.poll(output, area, serial), tx.user(voter));
			tx.put(consent, poll);
			if (poll.isConcluded() && poll.matter == Matter.resignation) {
				recount(consent, t, tx, output, area, poll.affected, voter);
			}
		};
	}
	
	public static Change dissent(Name output, Name area, IDN serial, Name voter) {
		return (t, tx) -> { tx.put(dissent, t.dissent(tx.poll(output, area, serial), tx.user(voter))); };
	}
	
	public static Change aspire(Name output, IDN task, Name user) {
		return (t, tx) -> { tx.put(aspire, t.aspire(tx.task(output, task), tx.user(user))); };
	}

	public static Change participate(Name output, IDN task, Name user) {
		return (t, tx) -> { tx.put(participate, t.participate(tx.task(output, task), tx.user(user))); };
	}
	
	public static Change abandon(Name output, IDN task, Name user) {
		return (t, tx) -> { tx.put(abandon, t.abandon(tx.task(output, task), tx.user(user))); };
	}

	public static Change watch(Name output, IDN task, Name user) {
		return (t, tx) -> { tx.put(watch, t.watch(tx.task(output, task), tx.user(user))); };
	}
	
	public static Change unwatch(Name output, IDN task, Name user) {
		return (t, tx) -> { tx.put(unwatch, t.unwatch(tx.task(output, task), tx.user(user))); };
	}
	
	public static Change compose(Name user, Name page, Template template) {
		return (t, tx) -> { tx.put(compose, t.compose(tx.user(user), page, template, tx.pages(Name.ORIGIN, user))); };
	}
	
	public static Change compose(Name output, Name area, Name page, Template template, Name user) {
		return (t, tx) -> { tx.put(compose, t.compose(tx.area(output, area), page, template, tx.user(user), tx.pages(output, area))); };
	}
	
	public static Change recompose(Name user, Name page, Template template) {
		return (t, tx) -> { tx.put(recompose, t.recompose(tx.page(Name.ORIGIN, user, page), template, tx.user(user))); };
	}
	
	public static Change recompose(Name output, Name area, Name page, Template template, Name user) {
		return (t, tx) -> { tx.put(recompose, t.recompose(tx.page(output, area, page), tx.area(output, area), template, tx.user(user))); };
	}
	
	public static Change erase(Name user, Name page) {
		return (t, tx) -> { tx.put(erase, t.erase(tx.page(Name.ORIGIN, user, page), tx.user(user))); };
	}
	
	public static Change erase(Name output, Name area, Name page, Name user) {
		return (t, tx) -> { tx.put(erase, t.erase(tx.page(output, area, page), tx.area(output, area), tx.user(user))); };
	}
	
	private static void recount(Operation op, Tracker t, Tx tx, Name output, Name area, Name resignedUser, Name actor) {
		//FIXME sadly not that simple. Recounting can exclude more maintainers and so forth. 
		// So first we have to recount all that potentially could cause that always removing all maintainer removed so far
		// than we remove all removed from all the other polls and see if those are settled
		User user = tx.user(actor);
		for (Poll p : tx.polls(output, area))
			tx.put(op, t.recount(p, resignedUser, user));
	}
}
