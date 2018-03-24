package se.jbee.track.model;



public final class Mail {

	@UseCode("ci")
	public static enum Objective { confirmation, information }

	@UseCode("pshdwn")
	public static enum Delivery { promptly, shortly, hourly, daily, weekly, never }

	/**
	 * The idea of a fine grained setting is to reduce mails or mail handling by
	 * allowing users to configure away messages by using
	 * {@link Delivery#never} and at the same time give the user a way to
	 * adopt mails to their usage pattern.
	 *
	 * In general one get mails for all tasks involved or watched. So watching
	 * is mostly a way to trigger notifications for task one isn't involved in.
	 */
	@UseCode("acoltpvrdmse")
	public static enum Notification {
		// user
		authenticated(Delivery.never), // a login occurred
		// output
		constituted(Delivery.daily),
		// area
		opened(Delivery.daily),    // for user that are origin maintainers
		left(Delivery.daily),      // by a maintainer (to other maintainers)
		// version
		tagged(Delivery.daily),    // for user that are origin maintainers
		// poll
		polled(Delivery.hourly),   // in an area the user is maintainer (can vote)
		voted(Delivery.hourly),     // for a poll where user can vote (is maintainer)
		// task
		reported(Delivery.hourly), // new tasks (in maintained area)
		developed(Delivery.daily), // a task the user is involved in has been updated or segmented
		moved(Delivery.daily),     // where user is involved
		solved(Delivery.hourly),   // where user is involved
		extended(Delivery.hourly)  // where user is involved
		;

		public final Delivery preset;

		Notification(Delivery preset) {
			this.preset = preset;
		}
	}

	public final Delivery method;
	public final Email to;
	public final Objective objective;
	public final Notification subject;
	public final String text;

	public Mail(Delivery method, Email to, Objective objective, Notification subject, String text) {
		this.method = method;
		this.to = to;
		this.objective = objective;
		this.subject = subject;
		this.text = text;
	}

}
