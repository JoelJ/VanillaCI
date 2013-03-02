package com.joelj.distributedinvoke;

import com.joelj.distributedinvoke.exceptions.ClassPathOutOfSyncException;
import com.joelj.distributedinvoke.exceptions.UnexpectedResultException;
import com.joelj.distributedinvoke.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.InetAddress;
import java.util.concurrent.Callable;

/**
 * User: Joel Johnson
 * Date: 3/1/13
 * Time: 10:45 PM
 */
public class RemoteMachineImpl implements RemoteMachine, Invokable {
	private static final Logger LOGGER = Logger.forClass(RemoteChannel.class);

	private final String name;
	private final InetAddress address;
	private final int port;

	private transient final RemoteChannel channel;
	private transient Thread listenerThread;

	/**
	 * Connects to the machine at the given address.
	 *
	 * @param name Unique name describing the machine. Used for logging. Cannot be null.
	 * @param address Address of the remote machine. Cannot be null.
	 * @param port The port the remote machine is listening on. Must be between 1 through 65535.
	 * @return The machine we have connected to. Never null.
	 * @throws IOException  A connection is attempted before returning,
	 * 		so if the remote machine isn't listening on the given address or port,
	 * 		an exception IOException is thrown
	 */
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
		this.channel = RemoteChannel.create(name, address, port);

		this.listenerThread = new Thread(new RemoteMachineListener());
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				listenerThread.interrupt();
			}
		}));
		this.listenerThread.start();
	}

	@Nullable
	@Override
	public <T extends Serializable> T invoke(@NotNull Callable<T> remoteCall) throws IOException, InterruptedException {
		ResultFuture resultFuture = channel.writeRequest(remoteCall);
		Object result = resultFuture.waitForResult();
		//noinspection unchecked
		return (T)result;
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

	private class RemoteMachineListener implements Runnable {
		@Override
		public void run() {
			LOGGER.info("started " + this);

			try {
				Thread thread = Thread.currentThread();
				while(!thread.isInterrupted()) {
					try {
						LOGGER.info("Waiting for response");
						channel.readResponse();
						LOGGER.info("Received response");
					} catch (IOException e) {
						throw new RuntimeException(e);
					} catch (InterruptedException e) {
						LOGGER.warn("Waiting for response interrupted. Breaking.");
						break;
					} catch (ClassPathOutOfSyncException e) {
						LOGGER.error("Class path out of sync. Continuing.", e);
					} catch (UnexpectedResultException e) {
						LOGGER.error("Unexpected Result. Continuing.", e);
					}
				}
			} finally {
				LOGGER.info("shut down " + this);
			}
		}
	}
}
