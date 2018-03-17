package se.jbee.track;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import se.jbee.track.api.TestURLToParams;
import se.jbee.track.cache.TestTaskSet;
import se.jbee.track.db.TestHeapMapDB;
import se.jbee.track.engine.TestConvert;
import se.jbee.track.engine.TestLMDB;
import se.jbee.track.engine.TestOTP;
import se.jbee.track.model.TestByteSequenceSecurity;
import se.jbee.track.model.TestCriteria;
import se.jbee.track.model.TestCriterium;
import se.jbee.track.model.TestGist;
import se.jbee.track.model.TestName;
import se.jbee.track.model.TestTemplate;
import se.jbee.track.model.TestURL;

@RunWith(Suite.class)
@SuiteClasses({ TestTracker.class, TestConvert.class, TestLMDB.class,
		TestCriteria.class, TestOTP.class, TestUseCode.class,
		TestCriterium.class, TestGist.class, TestTaskSet.class,
		TestByteSequenceSecurity.class, TestURL.class, TestName.class, TestURLToParams.class,
		TestTemplate.class, TestHeapMapDB.class })
public class TrackerSuit {
	// run all tests...
}
