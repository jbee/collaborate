package vizio;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import vizio.db.TestLMDB;
import vizio.engine.TestContraints;
import vizio.engine.TestOTP;
import vizio.io.TestConvert;
import vizio.model.TestUseCode;

@RunWith(Suite.class)
@SuiteClasses({ TestTracker.class, TestConvert.class, TestLMDB.class,
		TestContraints.class, TestOTP.class, TestUseCode.class })
public class TrackerSuit {
	// run all tests...
}
