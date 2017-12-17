package se.jbee.track.engine;


public class NoLimits implements Limits {


	@Override
	public void free(Limit l) {
		/*fine*/
	}

	@Override
	public boolean stress(Limit l, Clock clock) throws ConcurrentUsage {
		return true;
	}

	@Override
	public boolean alloc(Limit l, Clock clock) throws ConcurrentUsage {
		return true;
	}

}
