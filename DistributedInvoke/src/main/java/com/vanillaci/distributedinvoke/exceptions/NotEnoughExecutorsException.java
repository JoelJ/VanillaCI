package com.vanillaci.distributedinvoke.exceptions;

import com.vanillaci.distributedinvoke.machines.Machine;
import org.jetbrains.annotations.NotNull;

/**
 * User: Joel Johnson
 * Date: 3/4/13
 * Time: 6:45 PM
 */
public class NotEnoughExecutorsException extends Exception {
	public NotEnoughExecutorsException(@NotNull Machine machine) {
		super(machine.getName() + " " + machine.getAvailableExecutorCount() + "/" + machine.getTotalExecutorCount());
	}
}
