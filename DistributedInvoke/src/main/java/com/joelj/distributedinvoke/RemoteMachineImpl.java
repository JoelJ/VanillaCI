package com.joelj.distributedinvoke;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.InetAddress;
import java.util.concurrent.Callable;

/**
 * User: Joel Johnson
 * Date: 3/1/13
 * Time: 10:45 PM
 */
public class RemoteMachineImpl implements RemoteMachine, Invokable, Closeable {
	private final String name;
	private final InetAddress address;
	private final int port;

	private transient final RemoteChannel channel;

	@NotNull
	public static RemoteMachine connectToMachine(@NotNull String name, @NotNull InetAddress address, int port) throws IOException {
		if(port <= 0) {
			throw new IllegalArgumentException("'port' must be a positive integer");
		}

		return new RemoteMachineImpl(name, address, port);
	}

	private RemoteMachineImpl(@NotNull String name, @NotNull InetAddress address, int port) throws IOException {
		assert port > 0;
		this.name = name;
		this.address = address;
		this.port = port;
		this.channel = new RemoteChannel(name, address, port);
	}

	@Override
	public <T extends Serializable> T invoke(@NotNull Callable<T> remoteCall) throws IOException, InterruptedException {
		return channel.invokeCallable(remoteCall);
	}

	@Override
	@NotNull
	public String getName() {
		return name;
	}

	@Override
	@NotNull
	public InetAddress getAddress() {
		return address;
	}

	@Override
	public int getPort() {
		return port;
	}

	@Override
	public void close() throws IOException {
		channel.close();
	}
}
