package vizio;

public class Vote {

	public static enum Matter { participation, resignation }

	public Matter matter;
	public Area area;
	public ID initiator;
	public ID affected;
	public Users consenting;
	public Users dissenting;
	public Date expiry;
}
