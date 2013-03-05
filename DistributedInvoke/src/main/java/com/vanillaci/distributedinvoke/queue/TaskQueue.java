package com.vanillaci.distributedinvoke.queue;

import com.vanillaci.distributedinvoke.Lock;
import com.vanillaci.distributedinvoke.channels.ResultFuture;
import com.vanillaci.distributedinvoke.exceptions.NotEnoughExecutorsException;
import com.vanillaci.distributedinvoke.jobs.Result;
import com.vanillaci.distributedinvoke.jobs.Task;
import com.vanillaci.distributedinvoke.logging.Logger;
import com.vanillaci.distributedinvoke.machines.Machine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * User: Joel Johnson
 * Date: 3/4/13
 * Time: 9:16 PM
 */
public class TaskQueue implements Runnable {
	private static final Logger LOGGER = Logger.forClass(TaskQueue.class);

	@Nullable private static volatile TaskQueue $_instance;
	@NotNull private static final Lock $_instance_lock = new Lock();

	private static final int SECONDS = 1000;
	@NotNull private final List<Task<Result>> taskQueue;
	@NotNull private final List<Machine> machines;
	@NotNull private final List<QueueWatch> queueWatchPlugins;
	@NotNull private final List<DequeueWatch> dequeueWatchPlugins;

	private TaskQueue(@NotNull List<Machine> machines, @NotNull List<QueueWatch> queueWatchPlugins, @NotNull List<DequeueWatch> dequeueWatchPlugins) {
		this.taskQueue = Collections.synchronizedList(new LinkedList<Task<Result>>());
		this.machines = machines;
		this.queueWatchPlugins = queueWatchPlugins;
		this.dequeueWatchPlugins = dequeueWatchPlugins;
	}

	@Override
	public void run() {
		while(!Thread.interrupted()) {
			if(taskQueue.size() > 0) {
				Iterator<Task<Result>> iterator = taskQueue.iterator();
				while(iterator.hasNext()) {
					Task<Result> next = iterator.next();

					LOGGER.infop("Checking to run: %s", next);

					Machine machineToRunOn = checkDequeueItem(next);
					if(machineToRunOn != null) {
						iterator.remove();
						boolean interrupted = invokeTask(next, machineToRunOn);
						if(interrupted) {
							Thread.currentThread().interrupt();
							break;
						}
					}
				}
			} else {
				try {
					LOGGER.infop("Nothing in queue. Sleeping for a 5 seconds.");
					Thread.sleep(5 * SECONDS);
				} catch (InterruptedException e) {
					LOGGER.info("Interrupted queue. Attempting clean exit.");
					break;
				}
			}
		}

		LOGGER.warn("Queue thread has died.");
	}

	public boolean enqueueTask(@NotNull Task<Result> task) {
		LOGGER.infop("Asked to be queued: %s", task);
		if(checkEnqueueItem(task)) {
			LOGGER.infop("Queued: %s", task);
			taskQueue.add(task);
			return true;
		}

		return false;
	}

	private boolean invokeTask(@NotNull Task<Result> task, @NotNull Machine machineToRunOn) {
		LOGGER.infop("Invoking: %s", task);
		try {
			ResultFuture<Result> resultFuture = machineToRunOn.invokeAsync(task, task.getWeight());
			LOGGER.infop("Running task %s and got %s.", task, resultFuture); //TODO: do something useful with the future.
		} catch (IOException e) {
			LOGGER.error("Error occurred when trying to invoke task.", e);
		} catch (InterruptedException e) {
			LOGGER.info("Interrupted queue. Attempting clean exit.");
			return false;
		} catch (NotEnoughExecutorsException e) {
			LOGGER.warn(e);
		}

		return true;
	}

	private boolean checkEnqueueItem(@NotNull Task<Result> task) {
		if(queueWatchPlugins.isEmpty()) {
			return true;
		} else {
			for (QueueWatch queueWatchPlugin : queueWatchPlugins) {
				if(queueWatchPlugin.check(task)) {
					return true;
				}
			}
			return false;
		}
	}

	@Nullable
	private Machine checkDequeueItem(@NotNull Task<Result> task) {
		if(dequeueWatchPlugins.isEmpty()) {
			if(machines.isEmpty()) {
				return null;
			} else {
				return machines.get(0);
			}
		} else {
			for (Machine machine : machines) {
				for (DequeueWatch dequeueWatchPlugin : dequeueWatchPlugins) {
					if(dequeueWatchPlugin.check(task, machine)) {
						return machine;
					}
				}
			}
			return null;
		}
	}
}
