package com.joelj.distributedinvoke.exceptions;

/**
 * User: Joel Johnson
 * Date: 3/1/13
 * Time: 11:11 PM
 */
public class ClassPathOutOfSyncException extends RuntimeException {
	public ClassPathOutOfSyncException(ClassNotFoundException cause) {
		super(cause);
	}
}
