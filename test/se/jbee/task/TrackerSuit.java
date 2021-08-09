package se.jbee.task;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import se.jbee.task.api.TestURLToParams;
import se.jbee.task.cache.TestTaskSet;
import se.jbee.task.db.TestHeapMapDB;
import se.jbee.task.engine.TestConvert;
import se.jbee.task.engine.TestLMDB;
import se.jbee.task.engine.TestOTP;
import se.jbee.task.model.TestByteSequenceSecurity;
import se.jbee.task.model.TestCriteria;
import se.jbee.task.model.TestCriterium;
import se.jbee.task.model.TestGist;
import se.jbee.task.model.TestName;
import se.jbee.task.model.TestTemplate;
import se.jbee.task.model.TestURL;

@RunWith(Suite.class)
@SuiteClasses({ TestTracker.class, TestConvert.class, TestLMDB.class,
		TestCriteria.class, TestOTP.class, TestUseCode.class,
		TestCriterium.class, TestGist.class, TestTaskSet.class,
		TestByteSequenceSecurity.class, TestURL.class, TestName.class, TestURLToParams.class,
		TestTemplate.class, TestHeapMapDB.class })
public class TrackerSuit {
	// run all tests...
}
