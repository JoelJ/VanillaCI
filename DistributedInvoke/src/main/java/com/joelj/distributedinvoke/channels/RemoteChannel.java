package com.joelj.distributedinvoke.channels;

import com.joelj.distributedinvoke.exceptions.ClassPathOutOfSyncException;
import com.joelj.distributedinvoke.exceptions.UnexpectedResultException;
import com.joelj.distributedinvoke.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the connection between the local machine and a remote machine.
 * When it detects that the connection is lost, it will attempt to re-establish the connection.
 *
 * User: Joel Johnson
 * Date: 3/2/13
 * Time: 12:09 AM
 */
public class RemoteChannel extends AutoReconnectingChannel {
	private static final Logger LOGGER = Logger.forClass(RemoteChannel.class);

	@NotNull private final InetAddress address;
	private final int port;

	@NotNull private final Map<String, ResultFuture> pendingRequests;

	/**
	 * Creates a new instance of the RemoteChannel.
	 *
	 * @param machineName The name of the machine this channel points to. Only used for useful logging. Cannot be null.
	 * @param address The address used to connect to the remote machine. Cannot be null.
	 * @param port The port used to connect to the remote machine. Must be between 1 and 65535.
	 * @return A new instance of RemoteChannel. Never null.
	 * @throws IOException
	 */
	@NotNull
	public static RemoteChannel create(@NotNull String machineName, @NotNull InetAddress address, int port) throws IOException {
		return new RemoteChannel(machineName, address, port);
	}

	private RemoteChannel(@NotNull String machineName, @NotNull InetAddress address, int port) throws IOException {
		super(machineName, new Socket(address, port));
		LOGGER.infop("Opened connection with %s (%s:%d)", machineName, address.toString(), port);

		this.address = address;
		this.port = port;
		this.pendingRequests = new ConcurrentHashMap<String, ResultFuture>();
	}

	/**
	 * Sends the given object to the remote machine.
	 * @param object Object to send to the remote machine. Can be null.
	 * @return Future object that allows you to easily wait for a response from the remote machine. Never null.
	 * @throws IOException Typical IOException. However, if there are any problems with the connection to the remote server,
	 * 						rather than bubbling up the exception the socket is attempted to be reconnected.
	 * @throws InterruptedException When the thread is canceled.
	 */
	@NotNull
	public <T> ResultFuture<T> writeRequest(@Nullable Callable<T> object) throws IOException, InterruptedException {
		Transport<Callable<T>> transport = Transport.wrap(object);
		writeObject(transport);

		ResultFuture<T> future = new ResultFuture<T>();
		pendingRequests.put(transport.getId(), future);
		return future;
	}

	/**
	 * Reads a response from the remote machine, and if the resulting request ID matches a local request,
	 * then all threads waiting for the response will be notified with the value.
	 *
	 * @throws IOException Typical IOException. However, if there are any problems with the connection to the remote server,
	 * 						rather than bubbling up the exception the socket is attempted to be reconnected.
	 * @throws InterruptedException When the thread is canceled.
	 * @throws ClassPathOutOfSyncException When the response is an object that is not on the local JVM classpath.
	 */
	public void readResponse() throws IOException, InterruptedException, ClassPathOutOfSyncException {
		Object readObject;
		try {
			readObject = readObject();
		} catch (ClassNotFoundException e) {
			throw new ClassPathOutOfSyncException(e);
		}

		if(readObject != null && readObject instanceof Transport) {
			Transport transport = (Transport) readObject;
			String id = transport.getId();
			ResultFuture resultFuture = pendingRequests.get(id);
			if(resultFuture == null) {
				LOGGER.warn("Received response for unknown ID");
			} else {
				//noinspection unchecked
				resultFuture.setResult(transport.getObject());
			}
		} else {
			throw new UnexpectedResultException("Expected result of " + Transport.class.getCanonicalName() + " but was " + (readObject == null ? "null" : readObject.getClass().getCanonicalName()));
		}
	}

	@NotNull
	@Override
	protected Socket reconnect() throws IOException {
		try {
			close();
		} catch (IOException closeException) {
			LOGGER.warn("Exception thrown while closing socket", closeException);
		}

		return new Socket(address, port);
	}

	@Override
	public String toString() {
		return "RemoteChannel{" +
				"machineName='" + getMachineName() + '\'' +
				"address=" + address +
				", port=" + port +
				'}';
	}
}
