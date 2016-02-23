package vizio;

import static java.lang.Math.max;
import static java.lang.System.currentTimeMillis;
import static vizio.VIZIO.days;
import static vizio.VIZIO.today;

public class User {

	public int id;
	public String email;
	public int xp;
	public int absolved;
	public int resolved;
	public int dissolved;
	// voting
	public long millisVoted;
	public int votesToday;

	public int votingDelay() {
		return max(60000, (int)( 3600000f / (1f+(xp/50f))));
	}

	public int votesPerDay() {
		return 10 + (xp/5);
	}

	public boolean canVote() {
		return currentTimeMillis() - millisVoted > votingDelay()
			&& (votesToday < votesPerDay() || today() > days(millisVoted));
	}

	public void vote() {
		if (today() > days(millisVoted)) {
			votesToday = 1;
		} else {
			votesToday++;
		}
		millisVoted = currentTimeMillis();
	}
}
