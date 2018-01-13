package se.jbee.track.engine;

public final class TransitionDenied extends IllegalStateException {

	public static enum Error {
		// should answer one of: What do I want you (the user) to do/change? What can be done about it?
		E1_LIMIT_EXCEEDED(1, "Activity limit %s exceeded. %s Please try again later."), 
		E2_MUST_NOT_BE_BOARD(2, "Area %s is a board. Use 'request' to submit to a board."), 
		E3_MUST_BE_BOARD(3, "Cannot use 'request' for area %s. Only boards allow it."), 
		E4_VERSION_RELEASED(4, "Version %s is already released."), 
		E5_PAGE_LIMIT_REACHED(5, "Pages per user/area are limited to %d."), 
		E6_PAGE_OWNERSHIP_REQUIRED(6, "Only the owner or maintainer of page %s may change it."), 
		E7_PAGE_EXISTS(7, "Page %s already exists."), 
		E8_OUTPUT_OWNERSHIP_REQUIRED(8, "You have a maintainer of %s %s to perform this operation, like %s."), 
		E9_REQUIRES_REGISTRATION(9, "Please login or register to perform this action."), 
		E10_REQUIRES_AUTHENTICATION(10, "Please (re)authenticate to perform this action."), 
		E11_TASK_USER_LIMIT_REACHED(11, "The maximum number of users that can be involved with task %s is reached."), 
		E12_AREA_MAINTAINER_REQUIRED(12, "You have to be a maintainer of area %s to perform this action."), 
		E13_TASK_ALREAD_SOLVED(13, "Task %s is already concluded. This cannot be undone. Derive futher tasks if a change needed."), 
		E14_TASK_NOT_SOLVED(14, "Task %s need to be solved before this action can be performed. Use dissolve to solve it without any action."), 
		E15_URL_NOT_INTEGRATED(15, "External URL %s not allowed. URLs are limited to integrations %s."), 
		E16_NAME_IS_NO_VERSION(16, "%s is not a valid version name. 17 characters max, no '@'."), 
		E17_NAME_IS_NOT_REGULAR(17, "%s is not a valid name. Chose a name without '@' and at most 17 characters starting with a letter."), 
		E18_NAME_IS_NO_EMAIL(18, "%s is not a valid email. A user name can obly be changed as long as an email was used before."), 
		E19_OUTPUT_INTEGRATION_LIMIT_REACHED(19, "Integrations per output are limited to %d."), 
		E20_USER_WATCH_LIMIT_REACHED(20, "You reached your limit for maximum number of watched tasks. Unwatch tasks or increase your limit by solving tasks."), 
		E21_TOKEN_ON_COOLDOWN(21, "Please wait a minute before requesting another token."), 
		E22_TOKEN_EXPIRED(22, "Sorry. Your token expired already."), 
		E23_TOKEN_INVALID(23, "The token you provided is not correct."), 
		E24_NAME_OCCULIED(24, "The name %s is already taken. Please chose another name."), 
		E25_ADMIN_REQUIRED(25, "Only admin may perform this action for this installation."), 
		E26_LOCKDOWN(26, "Server is undergoing maintenance work. Access is restricted to administrators. Please try again later."), 
		E27_LIMIT_OCCUPIED( 27, "Concurrent change detected by limit %s. Please try again."), 
		E28_CATEGORY_LIMIT(28, "Categories per output are limited to %d.")
		
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
		super();
		this.error=error;
		this.args=args;
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
