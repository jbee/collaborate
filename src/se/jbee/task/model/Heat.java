package se.jbee.task.model;

@UseCode("ctwhb")
public enum Heat {

	cold, tepid, warm, hot, burning;

	public static Heat valueOf(int temp) {
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
