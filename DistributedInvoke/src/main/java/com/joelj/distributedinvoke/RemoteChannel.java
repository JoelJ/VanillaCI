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
import java.util.concurrent.Callable;

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

	public RemoteChannel(@NotNull String machineName, @NotNull InetAddress address, int port) throws IOException {
		LOGGER.infop("Opening connection with %s (%s:%d)", machineName, address.toString(), port);
		this.machineName = machineName;
		this.address = address;
		this.port = port;
		this.clientSocket = new Socket(address, port);
	}

	/**
	 * Sends the given callable to the remote machine, then immediately waits for the result.
	 *
	 * @param callable The callable object representing the code to be invoked remotely.
	 *                 Must be serializable and must be on the remote machine's classpath.
	 * @return Whatever the Callable returns on the remote slave.
	 * 				Must be serializable and the type of the result must be on the classpath of the local JVM.
	 * @throws IOException Typical networking IO problems.
	 * 				However, in the event of exceptions thrown while trying to
	 * 				get the stream from the underlying socket, it will try to
	 * 				reconnect to the machine then retry until the thread is interrupted.
	 * @throws InterruptedException If the thread is interrupted while trying to reconnect to the remote slave.
	 */
	public <T extends Serializable> T invokeCallable(Callable<T> callable) throws IOException, InterruptedException {
		synchronized (this) {
			writeObject(callable);
			try {
				Object result = readObject();
				try {
					//noinspection unchecked
					return (T)result;
				} catch (ClassCastException e) {
					throw new UnexpectedResultException(e);
				}
			} catch (ClassNotFoundException e) {
				throw new ClassPathOutOfSyncException(e);
			}
		}
	}

	private void writeObject(Object object) throws IOException, InterruptedException {
		ObjectOutputStream outputStream = getOutputStream();
		outputStream.writeObject(object);
		outputStream.flush();
	}

	@Nullable
	private Object readObject() throws IOException, InterruptedException, ClassNotFoundException {
		ObjectInputStream inputStream = getInputStream();
		return inputStream.readObject();
	}

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

	@Override
	public void close() throws IOException {
		LOGGER.infop("Closing connection with %s (%s:%d)", machineName, address.toString(), port);
		clientSocket.close();
	}
}
