package com.joelj.distributedinvoke;

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.Callable;

import com.joelj.ezasync.EzAsync;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * <p>Something that can invoke a Callable and return the result, either locally or on a remote machine.</p>
 * <p>
 *     Note: The callable <b>must</b> be serializable,
 *     although it's not specified in the method signature
 *     since it made things too confusing and seemed counter intuitive to keep it in.
 * </p>
 *
 * <p>
 *     See: {@link RemoteMachineImpl}.
 * </p>
 *
 * User: Joel Johnson
 * Date: 3/1/13
 * Time: 10:42 PM
 */
public interface Invokable {
	@Nullable <T extends Serializable> T invoke(@NotNull Callable<T> callable) throws IOException, InterruptedException;
}
