package com.joelj.distributedinvoke.exceptions;

import org.jetbrains.annotations.NotNull;

/**
 * User: Joel Johnson
 * Date: 3/1/13
 * Time: 11:12 PM
 */
public class UnexpectedResultException extends RuntimeException {
	public UnexpectedResultException(@NotNull Throwable cause) {
		super(cause.getMessage(), cause);
	}
}
