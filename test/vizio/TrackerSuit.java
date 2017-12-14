package vizio;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import vizio.cache.TestCriteria;
import vizio.cache.TestCriterium;
import vizio.engine.TestLMDB;
import vizio.engine.TestOTP;
import vizio.io.TestConvert;

@RunWith(Suite.class)
@SuiteClasses({ TestTracker.class, TestConvert.class, TestLMDB.class,
		TestCriteria.class, TestOTP.class, TestUseCode.class, TestCriterium.class })
public class TrackerSuit {
	// run all tests...
}
