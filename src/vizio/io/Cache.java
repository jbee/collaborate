package vizio.io;

import vizio.model.Task;

public interface Cache {

	Task[] tasks(Criteria criteria);
}
