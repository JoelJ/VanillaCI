package com.joelj.distributedinvoke.channels;

import com.joelj.distributedinvoke.Lock;
import com.joelj.distributedinvoke.exceptions.UnexpectedResultException;
import com.joelj.ezasync.EzAsync;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

/**
 * Represents an return value that hasn't been populated yet and will be by another thread.
 *
 * User: Joel Johnson
 * Date: 3/2/13
 * Time: 11:06 AM
 */
public final class ResultFuture<T> {
	private final String requestId;
	private volatile T result;
	private volatile boolean set;
	private final Lock waitLock = new Lock();
	private final List<EzAsync.Callback<T>> callbacks = new LinkedList<EzAsync.Callback<T>>();

	@NotNull
	/* package */ static <T> ResultFuture<T> create(@NotNull String id) {
		return new ResultFuture<T>(id);
	}

	private ResultFuture(@NotNull String requestId) {
		this.requestId = requestId;
	}

	public String getRequestId() {
		return requestId;
	}

	/**
	 * Waits for the result to be populated.
	 * If the resulting value has already been populated, then it will immediately return that value.
	 * @return The resulting value. Can be null.
	 * @throws InterruptedException
	 */
	@Nullable
	public T waitForResult() throws InterruptedException {
		if(!set) {
			synchronized (waitLock) {
				if(!set) {
					waitLock.wait();
				}
			}
		}
		if(!set) {
			throw new IllegalStateException("Notified but never set: " + this);
		}

		try {
			//noinspection unchecked
			return (T)result;
		} catch (ClassCastException e) {
			throw new UnexpectedResultException("Return type was not what was expected. Received: " + result);
		}
	}

	/**
	 * Sets the result of the future and notifies all callers of {@link #waitForResult()}. Can only be called once.
	 * @param result The value to be set. Can be null.
	 */
	/*package*/ void setResult(@Nullable T result) {
		if(set) {
			throw new IllegalStateException("Value already set: " + this + " to " + String.valueOf(result));
		}

		this.result = result;
		set = true;

		synchronized (callbacks) {
			for (EzAsync.Callback<T> callback : callbacks) {
				callback.done(this.result);
			}
		}

		synchronized (waitLock) {
			waitLock.notifyAll();
		}
	}

	public void registerCallback(EzAsync.Callback<T> callback) {
		if(set) {
			callback.done(this.result);
		} else {
			synchronized (callbacks) {
				callbacks.add(callback);
			}
		}
	}
}
