package se.jbee.track.model;

public final class Poll extends Entity<Poll> {

	@UseCode
	public static enum Matter {
		// on maintainers
		participation, resignation,
		// attitude towards anonymous participation
		inclusion, exclusion,
		// towards the area (non user related)
		abandonment
	}

	public IDN serial;
	public Area area;
	public Matter matter;
	public Name affected;
	public Name initiator;
	public Date start;
	public Names consenting;
	public Names dissenting;
	public Date expiry;
	public Date end;
	public Outcome outcome;

	public Poll(int version) {
		super(version);
	}
	
	@Override
	public ID computeID() {
		return ID.pollId(area.product, area.name, serial);
	}
	
	@Override
	public Name product() {
		return area.product;
	}
	
	public boolean canVote(Name voter) {
		return area.maintainers.contains(voter) && !affected.equalTo(voter) && !isSettled();
	}

	public boolean isSettled() {
		int all = area.maintainers.count();
		int pro = consenting.count();
		int contra = dissenting.count();
		int voted = pro + contra;
		return all - voted < voted && pro != contra;
	}

	public boolean isAccepted() {
		return consenting.count() > dissenting.count() && isSettled();
	}
}
