package se.jbee.track;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import se.jbee.track.cache.TestCriteria;
import se.jbee.track.cache.TestCriterium;
import se.jbee.track.engine.TestLMDB;
import se.jbee.track.engine.TestOTP;
import se.jbee.track.io.TestConvert;

@RunWith(Suite.class)
@SuiteClasses({ TestTracker.class, TestConvert.class, TestLMDB.class,
		TestCriteria.class, TestOTP.class, TestUseCode.class, TestCriterium.class })
public class TrackerSuit {
	// run all tests...
}
