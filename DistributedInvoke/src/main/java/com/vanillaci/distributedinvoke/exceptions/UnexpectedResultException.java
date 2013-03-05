package com.vanillaci.distributedinvoke.exceptions;

/**
 * When sending a value to a remote machine,
 * if that machine responds with an object we don't know what to do with,
 * this exception is thrown.
 * User: Joel Johnson
 * Date: 3/1/13
 * Time: 11:12 PM
 */
public class UnexpectedResultException extends RuntimeException {
	public UnexpectedResultException(String message) {
		super(message);
	}
}
