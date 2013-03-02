package com.joelj.distributedinvoke;

import com.joelj.distributedinvoke.exceptions.UnexpectedException;

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.Callable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: Joel Johnson
 * Date: 3/1/13
 * Time: 11:16 PM
 */
public class LocalMachine implements Invokable {
	@Nullable
	@Override
	public <T extends Serializable> T invoke(@NotNull Callable<T> callable) throws IOException {
		try {
			return callable.call();
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new UnexpectedException(e);
		}
	}
}
