package com.vanillaci.distributedinvoke.queue;

import com.vanillaci.distributedinvoke.jobs.Task;
import com.vanillaci.distributedinvoke.machines.Machine;

/**
 * User: Joel Johnson
 * Date: 3/4/13
 * Time: 9:24 PM
 */
public interface DequeueWatch {
	boolean check(Task<?> task, Machine machineToRunOn);
}
