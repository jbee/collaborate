package vizio.io;

import vizio.engine.Constraints;
import vizio.model.Task;

public interface Cache {

	Task[] tasks(Constraints criteria);
}
