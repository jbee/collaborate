package vizio;

public class Poll {

	public static enum Matter {
		// on maintainers
		participation, resignation,
		// attitude towards anonymous participation
		inclusion, exclusion
	}

	public Area area;
	public Matter matter;
	public User affected;
	public Name initiator;
	public Date start;
	public Names consenting;
	public Names dissenting;
	public Date expiry;
	public Date end;
	public Outcome outcome;

	public boolean canVote(Name voter) {
		return area.maintainers.contains(voter) && !affected.name.equalTo(voter) && !isSettled();
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
