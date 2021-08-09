package se.jbee.task.engine;


final class NoLimits implements Limits {


	@Override
	public void unblock(Limit l) {
		/*fine*/
	}

	@Override
	public boolean stress(Limit l, Clock clock) throws ConcurrentUsage {
		return true;
	}

	@Override
	public boolean block(Limit l, Clock clock) throws ConcurrentUsage {
		return true;
	}

}
