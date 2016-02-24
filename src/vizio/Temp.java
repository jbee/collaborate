package vizio;

public enum Temp {

	cold, tepid, warm, hot, burning;

	public static Temp fromNumeric(int temp) {
		if (temp < 25)
			return cold;
		if (temp < 50)
			return tepid;
		if (temp < 75)
			return warm;
		if (temp < 95)
			return hot;
		return burning;
	}
}
