package com.joelj.distributedinvoke.machines;

import com.joelj.distributedinvoke.channels.ResultFuture;
import com.joelj.distributedinvoke.exceptions.NotEnoughExecutorsException;
import com.joelj.distributedinvoke.machines.labels.Label;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.concurrent.Callable;

/**
 * User: Joel Johnson
 * Date: 3/2/13
 * Time: 12:21 AM
 */
public interface Machine extends Closeable {
	@NotNull
	String getName();

	@NotNull
	InetAddress getAddress();

	int getPort();

	/**
	 * Executes the given callable.
	 *
	 * @param callable The callable to execute. Cannot be null.
	 * @param weight The number of executors to use. Must be positive. If a non-positive value is given, 1 will be assumed.
	 * @return The result of the callable. Can be null.
	 * @throws NotEnoughExecutorsException Thrown if there isn't enough capacity to run the request.
	 * 	{@link #getAvailableExecutorCount()} can be called to check to see if there is enough capacity, but
	 * 	because of the multi-threaded environment this is meant to be run in, it cannot be guaranteed that that number
	 * 	will be accurate by the time this method is called.
	 * @throws java.io.IOException
	 * @throws InterruptedException
	 */
	@Nullable
	<T extends Serializable> T invoke(@NotNull Callable<T> callable, int weight) throws IOException, InterruptedException, NotEnoughExecutorsException;

	/**
	 * Executes the given callable asynchronously.
	 * @param callable The callable to execute. Cannot be null.
	 * @param weight The number of executors to use. Must be positive. If a non-positive value is given, 1 will be assumed.
	 * @return The future that represents the pending results. Cannot be null, but the eventual value may or may not be null.
	 * @throws NotEnoughExecutorsException Thrown if there isn't enough capacity to run the request.
	 * 	{@link #getAvailableExecutorCount()} can be called to check to see if there is enough capacity, but
	 * 	because of the multi-threaded environment this is meant to be run in, it cannot be guaranteed that that number
	 * 	will be accurate by the time this method is called.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@NotNull
	<T extends Serializable> ResultFuture<T> invokeAsync(@NotNull Callable<T> callable, int weight) throws IOException, InterruptedException, NotEnoughExecutorsException;

	int getTotalExecutorCount();
	int getAvailableExecutorCount();
	int getBusyExecutorCount();

	Label.Expression getLabels();
}
