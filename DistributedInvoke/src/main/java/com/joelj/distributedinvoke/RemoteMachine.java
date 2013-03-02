package com.joelj.distributedinvoke;

import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;

/**
 * User: Joel Johnson
 * Date: 3/2/13
 * Time: 12:21 AM
 */
public interface RemoteMachine extends Invokable {
	@NotNull
	String getName();

	@NotNull
	InetAddress getAddress();

	int getPort();
}
