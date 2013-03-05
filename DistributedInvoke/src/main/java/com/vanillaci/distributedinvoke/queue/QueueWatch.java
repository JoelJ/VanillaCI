package com.vanillaci.distributedinvoke.queue;

import com.vanillaci.distributedinvoke.jobs.Task;

/**
 * Plugin endpoint.
 *
 * Allows a plugin to decide whether or not a task should be allowed to be put in the build queue.
 *
 * User: Joel Johnson
 * Date: 3/4/13
 * Time: 9:29 PM
 */
public interface QueueWatch {
	boolean check(Task<?> task);
}
