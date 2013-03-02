package com.joelj.distributedinvoke;

import com.joelj.distributedinvoke.exceptions.ClassPathOutOfSyncException;
import com.joelj.distributedinvoke.exceptions.UnexpectedResultException;
import com.joelj.distributedinvoke.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the connection between the local machine and a remote machine.
 * When it detects that the connection is lost, it will attempt to re-establish the connection.
 *
 * User: Joel Johnson
 * Date: 3/2/13
 * Time: 12:09 AM
 */
public class RemoteChannel implements Closeable {
	private static final Logger LOGGER = Logger.forClass(RemoteChannel.class);

	private final String machineName;
	private final InetAddress address;
	private final int port;
	private Socket clientSocket;

	private static final Lock writeLock = new Lock();
	private static final Lock readLock = new Lock();

	private final Map<String, ResultFuture> pendingRequests;

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
		LOGGER.infop("Opening connection with %s (%s:%d)", machineName, address.toString(), port);
		this.machineName = machineName;
		this.address = address;
		this.port = port;
		this.clientSocket = new Socket(address, port);
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
	public ResultFuture writeRequest(@Nullable Object object) throws IOException, InterruptedException {
		Transport<Object> transport = Transport.wrap(object);

		synchronized (writeLock) {
			ObjectOutputStream outputStream = getOutputStream();
			outputStream.writeObject(transport);
			outputStream.flush();
		}

		ResultFuture future = new ResultFuture();
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
		synchronized (readLock) {
			ObjectInputStream inputStream = getInputStream();
			try {
				readObject = inputStream.readObject();
			} catch (ClassNotFoundException e) {
				throw new ClassPathOutOfSyncException(e);
			}
		}

		if(readObject != null && readObject instanceof Transport) {
			Transport transport = (Transport) readObject;
			String id = transport.getId();
			ResultFuture resultFuture = pendingRequests.get(id);
			if(resultFuture == null) {
				LOGGER.warn("Received response for unknown ID");
			} else {
				resultFuture.setResult(transport.getObject());
			}
		} else {
			throw new UnexpectedResultException("Expected result of " + Transport.class.getCanonicalName() + " but was " + (readObject == null ? "null" : readObject.getClass().getCanonicalName()));
		}
	}

	/**
	 * Gets the output stream from the socket.
	 * If there are any problems obtaining the stream (such as the socket has been closed),
	 * 	then it will attempt to reconnect until the thread is interrupted or until the stream has been obtained.
	 * @return The output stream for the current connection to the remote socket. Never will be null.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@NotNull
	private ObjectOutputStream getOutputStream() throws IOException, InterruptedException {
		OutputStream outputStream = null;
		while(outputStream == null) {
			try {
				outputStream = clientSocket.getOutputStream();
			} catch (SocketException e) {
				LOGGER.error("Lost connection with " + machineName + ". Retrying.", e);

				outputStream = null;

				try {
					clientSocket.close();
				} catch (IOException closeException) {
					LOGGER.warn("Exception thrown while closing socket", closeException);
				}
				clientSocket = new Socket(address, port);
			}
			Thread.sleep(1000);
		}
		return new ObjectOutputStream(outputStream);
	}

	/**
	 * Gets the input stream from the socket.
	 * If there are any problems obtaining the stream (such as the socket has been closed),
	 * 	then it will attempt to reconnect until the thread is interrupted or until the stream has been obtained.
	 * @return The output stream for the current connection to the remote socket. Never will be null.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@NotNull
	private ObjectInputStream getInputStream() throws IOException, InterruptedException {
		InputStream inputStream = null;
		while(inputStream == null) {
			try {
				inputStream = clientSocket.getInputStream();
			} catch (SocketException e) {
				LOGGER.error("Lost connection with " + machineName + ". Retrying.", e);

				inputStream = null;

				try {
					clientSocket.close();
				} catch (IOException closeException) {
					LOGGER.warn("Exception thrown while closing socket", closeException);
				}
				clientSocket = new Socket(this.address, this.port);
			}
			Thread.sleep(1000);
		}
		return new ObjectInputStream(inputStream);
	}

	/**
	 * Closes the underlying connection to the remote machine.
	 * @throws IOException
	 */
	@Override
	public void close() throws IOException {
		LOGGER.infop("Closing connection with %s (%s:%d)", machineName, address.toString(), port);
		clientSocket.close();
	}
}
