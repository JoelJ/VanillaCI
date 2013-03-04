package com.joelj.distributedinvoke.channels;

import com.joelj.distributedinvoke.Lock;
import com.joelj.distributedinvoke.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

/**
 * An abstract class that wraps {@link Socket#getInputStream()} and {@link java.net.Socket#getOutputStream()}
 * 	and reconnects the socket if anything goes wrong.
 *
 * To implement, provide a {@link #reconnect()} method that returns the new socket.
 *
 * User: Joel Johnson
 * Date: 3/3/13
 * Time: 6:58 PM
 */
public abstract class AutoReconnectingChannel implements Closeable {
	private static final Logger LOGGER = Logger.forClass(AutoReconnectingChannel.class);

	@NotNull private final String machineName;
	@Nullable private Socket socket;

	private static final Lock writeLock = new Lock();
	private static final Lock readLock = new Lock();

	protected AutoReconnectingChannel(@NotNull String machineName, @Nullable Socket socket) throws IOException {
		this.machineName = machineName;
		this.socket = socket;
	}

	public void writeObject(@Nullable Object object) throws IOException, InterruptedException {
		synchronized (writeLock) {
			ObjectOutputStream outputStream = getOutputStream();
			outputStream.writeObject(object);
			outputStream.flush();
		}
	}

	public Object readObject() throws ClassNotFoundException, IOException, InterruptedException {
		synchronized (readLock) {
			ObjectInputStream inputStream = getInputStream();
			return inputStream.readObject();
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
			if(socket == null) {
				socket = reconnect();
			}
			try {
				outputStream = socket.getOutputStream();
			} catch (SocketException e) {
				LOGGER.error("Lost connection with " + machineName + ". Retrying.", e);
				outputStream = null;
				socket = reconnect();
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
			if(socket == null) {
				socket = reconnect();
			}
			try {
				inputStream = socket.getInputStream();
			} catch (SocketException e) {
				LOGGER.error("Lost connection with " + machineName + ". Retrying.", e);
				inputStream = null;
				socket = reconnect();
			}
			Thread.sleep(1000);
		}
		return new ObjectInputStream(inputStream);
	}

	/**
	 * Reconnects the socket.
	 * This is called when calling {@link #getOutputStream()} or {@link #getInputStream()} and there is a problem obtaining the stream.
	 * @throws IOException
	 */
	@NotNull
	protected abstract Socket reconnect() throws IOException;

	/**
	 * Closes the underlying connection to the remote machine.
	 * @throws IOException
	 */
	public void close() throws IOException {
		if(socket != null) {
			LOGGER.infop("Closing connection with %s", machineName);
			socket.close();
		} else {
			LOGGER.infop("Attempted to close null socket on %s", machineName);
		}
	}

	/**
	 * The name of the machine. Used for debugging purposes.
	 */
	@NotNull
	public String getMachineName() {
		return machineName;
	}

	@Override
	public String toString() {
		return "AutoReconnectingChannel{" +
				"machineName='" + machineName + '\'' +
				'}';
	}
}
