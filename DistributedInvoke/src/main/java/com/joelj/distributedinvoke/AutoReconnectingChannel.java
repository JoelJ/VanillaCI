package com.joelj.distributedinvoke;

import com.joelj.distributedinvoke.logging.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

/**
 * An abstract class that wraps {@link Socket#getInputStream()} and {@link java.net.Socket#getOutputStream()}
 * 	and reconnects the socket if anything goes wrong.
 *
 * 	To implement, provide a {@link #reconnect()} method that returns the new socket.
 *
 * User: Joel Johnson
 * Date: 3/3/13
 * Time: 6:58 PM
 */
public abstract class AutoReconnectingChannel implements Closeable {
	private static final Logger LOGGER = Logger.forClass(AutoReconnectingChannel.class);

	@NotNull private final String machineName;
	@NotNull private Socket socket;

	protected AutoReconnectingChannel(@NotNull String machineName, @NotNull Socket socket) throws IOException {
		this.machineName = machineName;
		this.socket = socket;
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
	protected ObjectOutputStream getOutputStream() throws IOException, InterruptedException {
		OutputStream outputStream = null;
		while(outputStream == null) {
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
	protected ObjectInputStream getInputStream() throws IOException, InterruptedException {
		InputStream inputStream = null;
		while(inputStream == null) {
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
		LOGGER.infop("Closing connection with %s", machineName);
		socket.close();
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
