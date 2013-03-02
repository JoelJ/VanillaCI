package com.joelj.ezasync;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.*;

/**
 * User: Joel Johnson
 * Date: 3/1/13
 * Time: 8:37 PM
 */
public class EzAsync {
	@NotNull
	public static EzAsync create() {
		return new EzAsync();
	}

	private EzAsync() {

	}

	/**
	 * Executes the given task in it's own thread and calls the callback when that task is done.
	 * @param task Invoked in it's own thread.
	 * @param callback Invoked in the same thread that the task is invoked by.
	 */
	public <T> void execute(@NotNull Callable<T> task, @Nullable Callback<T> callback) {
		Thread thread = new Thread(new CallbackWrapper<T>(task, callback), this.toString());
		thread.start();
	}

	public static interface Callback<T> {
		/**
		 * Called when the task is complete.
		 * @param result The result of the callable executed by {@link EzAsync#execute(java.util.concurrent.Callable, com.joelj.ezasync.EzAsync.Callback)}}
		 */
		void done(@Nullable T result);
	}

	private static class CallbackWrapper<T> implements Runnable {
		@NotNull private final Callable<T> task;
		@Nullable private final Callback<T> callback;

		public CallbackWrapper(@NotNull Callable<T> task, @Nullable Callback<T> callback) {
			this.task = task;
			this.callback = callback;
		}

		@Override
		public void run() {
			try {
				T result = task.call();
				if(callback != null) {
					callback.done(result);
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
}
