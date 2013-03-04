package com.joelj.distributedinvoke.channels;

import com.joelj.distributedinvoke.Lock;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an return value that hasn't been populated yet and will be by another thread.
 *
 * User: Joel Johnson
 * Date: 3/2/13
 * Time: 11:06 AM
 */
public final class ResultFuture {
	private volatile Object result;
	private volatile boolean set;
	private final Lock waitLock = new Lock();

	/* package */ ResultFuture() {

	}

	/**
	 * Waits for the result to be populated.
	 * If the resulting value has already been populated, then it will immediately return that value.
	 * @return The resulting value. Can be null.
	 * @throws InterruptedException
	 */
	@Nullable
	public Object waitForResult() throws InterruptedException {
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
		return result;
	}

	/**
	 * Sets the result of the future and notifies all callers of {@link #waitForResult()}. Can only be called once.
	 * @param result The value to be set. Can be null.
	 */
	/*package*/ void setResult(@Nullable Object result) {
		if(set) {
			throw new IllegalStateException("Value already set: " + this + " to " + String.valueOf(result));
		}

		this.result = result;
		set = true;

		synchronized (waitLock) {
			waitLock.notifyAll();
		}
	}
}
