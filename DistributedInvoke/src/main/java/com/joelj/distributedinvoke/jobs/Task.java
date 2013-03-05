package com.joelj.distributedinvoke.jobs;

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * User: Joel Johnson
 * Date: 3/4/13
 * Time: 9:21 PM
 */
public interface Task<T> extends Callable<T>, Serializable {
	/**
	 * @return The result of this task.
	 * @throws Exception
	 */
	@Override
	T call() throws Exception;

	/**
	 * The weight of the task.
	 * The weight is how many executors of a machine should be used while running this task.
	 * Only machines whose {@link com.joelj.distributedinvoke.machines.Machine#getAvailableExecutorCount()} >= {@link #getWeight()}
	 * will be allowed to run this task.
	 *
	 * @return
	 */
	int getWeight();
}
