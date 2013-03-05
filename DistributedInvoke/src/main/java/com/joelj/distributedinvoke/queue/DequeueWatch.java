package com.joelj.distributedinvoke.queue;

import com.joelj.distributedinvoke.jobs.Task;
import com.joelj.distributedinvoke.machines.Machine;

/**
 * User: Joel Johnson
 * Date: 3/4/13
 * Time: 9:24 PM
 */
public interface DequeueWatch {
	boolean check(Task<?> task, Machine machineToRunOn);
}
