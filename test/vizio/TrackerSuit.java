package vizio;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import vizio.db.TestLMDB;
import vizio.io.TestConvert;

@RunWith(Suite.class)
@SuiteClasses({TestTracker.class, TestConvert.class, TestLMDB.class })
public class TrackerSuit {
	// run all tests...
}
