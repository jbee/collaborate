package se.jbee.track.model;

public final class Poll extends Entity<Poll> {

	@UseCode("prieasu")
	public static enum Matter {
		// on maintainers
		participation, resignation,
		// attitude towards anonymous participation
		inclusion, exclusion,
		// towards the area (non user related)
		abandonment,
		// non integration URLs or not?
		safeguarding, unblocking;

		public boolean isUserRelated() {
			return ordinal() < abandonment.ordinal();
		}
	}

	public IDN serial;
	public Area area;
	public Matter matter;
	public Gist motivation;
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
		return ID.pollId(area.output, area.name, serial);
	}

	@Override
	public Name output() {
		return area.output;
	}

	public boolean canVote(Name voter) {
		return !isConcluded() && area.maintainers.contains(voter) && !affected.equalTo(voter) && !isEffectivelySettled();
	}

	public boolean isEffectivelySettled() {
		int all = area.maintainers.count();
		int pro = consenting.count();
		int contra = dissenting.count();
		int voted = pro + contra;
		return all - voted < voted && pro != contra;
	}

	public boolean isAccepted() {
		return outcome == Outcome.consent || (consenting.count() > dissenting.count() && isEffectivelySettled());
	}

	public boolean isConcluded() {
		return outcome != Outcome.inconclusive;
	}

	public boolean hasVoted(Name user) {
		return consenting.contains(user) || dissenting.contains(user);
	}

}
