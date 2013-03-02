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

	private final ThreadPoolExecutor executor;
	private int threadPoolSize;

	@NotNull
	public static EzAsync create() {
		return createWithThreadPoolSize(DEFAULT_THREADPOOL_SIZE);
	}

	@NotNull
	public static EzAsync createWithThreadPoolSize(int size) {
		return new EzAsync(size);
	}

	private EzAsync(int threadPoolSize) {
		this.threadPoolSize = threadPoolSize;

		ThreadFactory threadFactory = new EzThreadFactory(this.toString());
		executor = new ThreadPoolExecutor(1, DEFAULT_THREADPOOL_SIZE, 1, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>(), threadFactory);
	}

	public int getThreadPoolSize() {
		return threadPoolSize;
	}

	public void setThreadPoolSize(int value) {
		if(value <= 0) {
			throw new IllegalArgumentException("value must be positive, but was " + value);
		}
		threadPoolSize = value;
		executor.setCorePoolSize(value);
	}

	@NotNull
	public <T> Future<T> execute(@NotNull Callable<T> task) {
		return executor.submit(task);
	}

	@NotNull
	public <T> Future<T> execute(@NotNull Callable<T> task, @NotNull Callback<T> callback) {
		return executor.submit(new CallbackWrapper<T>(task, callback));
	}

	private static class EzThreadFactory implements ThreadFactory {
		private final String name;
		private final AtomicInteger threadNumber = new AtomicInteger(0);

		public EzThreadFactory(@NotNull String name) {
			this.name = name;
		}

		@NotNull
		@Override
		public Thread newThread(Runnable r) {
			return new Thread(name + "-" + threadNumber.incrementAndGet());
		}
	}

	public static interface Callback<T> {
		/**
		 * Called when the future is done.
		 * @param result The result of the callable executed by {@link EzAsync#execute(java.util.concurrent.Callable, com.joelj.ezasync.EzAsync.Callback)}}
		 */
		void done(@NotNull T result);
	}

	private static class CallbackWrapper<T> implements Callable<T> {
		private final Callable<T> task;
		private final Callback<T> callback;

		public CallbackWrapper(@NotNull Callable<T> task, @NotNull Callback<T> callback) {
			this.task = task;
			this.callback = callback;
		}

		@Nullable
		@Override
		public T call() throws Exception {
			T result = task.call();
			callback.done(result);
			return result;
		}
	}
}
