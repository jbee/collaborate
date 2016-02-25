package vizio;

public class Vote {

	public static enum Matter { participation, resignation }

	public Matter matter;
	public Area area;
	public Name initiator;
	public Name affected;
	public Names consenting;
	public Names dissenting;
	public Date expiry;
}
