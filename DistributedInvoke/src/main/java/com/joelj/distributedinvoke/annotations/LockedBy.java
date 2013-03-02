package com.joelj.distributedinvoke.annotations;

/**
 * Simply meta-data on any object to remind maintainers that the annotated object should never be accessed or modified
 * without owning the lock on the object with the same name as the value of {@link #value()}.
 *
 * User: Joel Johnson
 * Date: 3/1/13
 * Time: 10:54 PM
 */
public @interface LockedBy {
	String value();
}
