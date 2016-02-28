package vizio;

public class Vote {

	public static enum Matter { 
		// on maintainers
		participation, resignation, 
		// attitude towards anonymous participation
		inclusion, exclusion }

	public Matter matter;
	public Area area;
	public Name initiator;
	public Name affected;
	public Date start;
	public Names consenting;
	public Names dissenting;
	public Date expiry;
	public Date end;
	
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
	
	public boolean isConsented() {
		return consenting.count() > dissenting.count() && isSettled();
	}
}
