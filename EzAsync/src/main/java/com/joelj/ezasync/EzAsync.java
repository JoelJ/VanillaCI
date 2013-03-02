package com.joelj.ezasync;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: Joel Johnson
 * Date: 3/1/13
 * Time: 8:37 PM
 */
public class EzAsync {
	private static final int DEFAULT_THREADPOOL_SIZE = 20;

	@NotNull
	public static EzAsync create() {
		return new EzAsync();
	}
	private EzAsync() {

	}

	public <T> void execute(@NotNull Callable<T> task, @NotNull Callback<T> callback) {
		Thread thread = new Thread(new CallbackWrapper<T>(task, callback));
		thread.start();
	}

	public static interface Callback<T> {
		/**
		 * Called when the future is done.
		 * @param result The result of the callable executed by {@link EzAsync#execute(java.util.concurrent.Callable, com.joelj.ezasync.EzAsync.Callback)}}
		 */
		void done(@Nullable T result);
	}

	private static class CallbackWrapper<T> implements Runnable {
		private final Callable<T> task;
		private final Callback<T> callback;

		public CallbackWrapper(@NotNull Callable<T> task, @NotNull Callback<T> callback) {
			this.task = task;
			this.callback = callback;
		}

		@Override
		public void run() {
			try {
				T result = task.call();
				callback.done(result);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
}
