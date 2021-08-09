package se.jbee.task.engine;

public final class TransitionDenied extends IllegalStateException {

	public static enum Error {
		// Wording:
		// Sorry! => bad state/user could not know this
		// Oops!  => foolery on users site
		E1_LIMIT_EXCEEDED(1, "Sorry! The activity limit %s was exceeded. %s Please try again later."),
		E2_MUST_NOT_BE_BOARD(2, "Oops! The area %s is a configured as a board. Make a 'request' to submit to a board."),
		E3_MUST_BE_BOARD(3, "Oops! Cannot use 'request' for area %s. Only boards allow it."),
		E4_VERSION_RELEASED(4, "Sorry! Version %s is already released and cannot be modified any more."),
		E5_PAGE_LIMIT_REACHED(5, "Sorry! The pages per user/area are limited to %d."),
		E6_PAGE_OWNERSHIP_REQUIRED(6, "Only the owner or maintainer of page %s can change it."),
		E7_PAGE_EXISTS(7, "Oops! A page named %s already exists. Please chose another name."),
		E8_OUTPUT_OWNERSHIP_REQUIRED(8, "Sorry! Only a maintainer of %s %s can perform this operation. Those are %s."),
		E9_REQUIRES_REGISTRATION(9, "Please login or register to perform this action."),
		E10_REQUIRES_AUTHENTICATION(10, "Please (re)authenticate to perform this action."),
		E11_TASK_USER_LIMIT_REACHED(11, "Sorry! The maximum number of users that can be involved with task %s is reached."),
		E12_AREA_MAINTAINER_REQUIRED(12, "Sorry! You have to be a maintainer of area %s to perform this action."),
		E13_TASK_ALREAD_SOLVED(13, "Oops! Task %s is already concluded. This cannot be undone. Please derive futher tasks if a change is needed."),
		E14_TASK_NOT_SOLVED(14, "Oops! Task %s need to be solved before this action can be performed. Use dissolve to solve it without any action."),
		E15_URL_NOT_INTEGRATED(15, "Sorry! External URLs like %s are not allowed. URLs are limited to integrations %s."),
		E16_NAME_IS_NO_VERSION(16, "Sorry! %s is not a valid name for a version. Use letters, digits or the symbols `-`, `_` or `.` and up to 16 characters."),
		E17_NAME_IS_NOT_REGULAR(17, "Sorry! %s is not a valid name. Start with a letter. Use letters, digits or the symbols `-` or `_` and up to 16 characters. "),
		E18_NAME_IS_NO_EMAIL(18, "Sorry! %s is not a valid email adress. (A user's name can only be changed as long as an email was used before.)"),
		E19_OUTPUT_INTEGRATION_LIMIT_REACHED(19, "Sorry! Integrations per output are limited to %d."),
		E20_USER_WATCH_LIMIT_REACHED(20, "Sorry! You reached your limit for maximum number of watched tasks. Unwatch tasks or increase your limit by solving tasks."),
		E21_TOKEN_ON_COOLDOWN(21, "Please wait a minute before requesting another token."),
		E22_TOKEN_EXPIRED(22, "Sorry! Your token expired already. Please request a new one."),
		E23_TOKEN_INVALID(23, "Sorry! The token you provided is not correct. Request a new token if you entered the token correctly."),
		E24_NAME_OCCULIED(24, "Sorry! The name %s is already taken. Please chose another name."),
		E25_ADMIN_REQUIRED(25, "Sorry! Only the administrator %s may perform this action for this installation."),
		E26_LOCKDOWN(26, "Sorry! Server is undergoing maintenance work. Access is restricted to administrators. Please try again later."),
		E27_LIMIT_OCCUPIED( 27, "Sorry! Another user just changed the same data (%s). Please reload the page and redo your actions if they still apply."),
		E28_CATEGORY_LIMIT(28, "Sorry! Categories per output are limited to %d."),
		E29_CHANGESET_REQUIRED(29, "Oops! Please provide the names of the versions released.")

		;
		public final int code;
		public final int args;
		final String msg;

		private Error(int code, String msg) {
			this.code = code;
			this.msg = msg;
			this.args = msg.length() - msg.replaceAll("%[a-z]", "x").length();
		}

	}

	public final Error error;
	private final Object[] args;
	private transient String msg;

	public TransitionDenied(Error error, Object...args) {
		this.error=error;
		this.args=args;
		for (int i = 0; i < args.length; i++)
			if (args[i] == null)
				args[i] = "(unknown)";
	}

	public Object arg(int index) {
		return args[index];
	}

	@Override
	public String toString() {
		if (msg == null)
			msg = TransitionDenied.class.getName()+":"+String.format(error.msg, args);
		return msg;
	}
}
