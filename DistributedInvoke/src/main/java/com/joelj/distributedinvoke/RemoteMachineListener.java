package com.joelj.distributedinvoke;

import com.joelj.distributedinvoke.channels.ServerSocketRemoteChannel;
import com.joelj.distributedinvoke.logging.Logger;
import com.joelj.ezasync.EzAsync;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.InetAddress;
import java.util.concurrent.Callable;

/**
 * User: Joel Johnson
 * Date: 3/2/13
 * Time: 12:36 AM
 */
public class RemoteMachineListener implements Closeable {
	private static final Logger LOGGER = Logger.forClass(RemoteMachineListener.class);

	private final InetAddress bindAddress;
	private final int listeningPort;
	private final transient Thread thread;

	private final transient EzAsync ezAsync;

	/**
	 * Creates the listener and starts listening.
	 *
	 * @param bindAddress   The address to bind to.
	 * @param listeningPort The port to listen on.
	 * @return The new instance of RemoteMachineListener that is actively listening for a new connection.
	 */
	public static RemoteMachineListener start(@NotNull InetAddress bindAddress, int listeningPort) {
		return new RemoteMachineListener(bindAddress, listeningPort);
	}

	/**
	 * @param bindAddress   The address to bind to.
	 * @param listeningPort Must be between 1 and 65535. On some operating systems, if the value is between 1 and 1024 the underlying JVM may need special privileges to open the socket.
	 */
	private RemoteMachineListener(@NotNull InetAddress bindAddress, int listeningPort) {
		this.bindAddress = bindAddress;
		this.listeningPort = listeningPort;
		thread = new Thread(new ListeningThread(), this.getClass().getSimpleName());
		thread.start();

		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				close();
			}
		}));

		ezAsync = EzAsync.create();
	}

	@Override
	public void close() {
		thread.interrupt();
	}

	private class ListeningThread implements Runnable {
		@Override
		public void run() {
			ServerSocketRemoteChannel channel = ServerSocketRemoteChannel.create(bindAddress, listeningPort);

			try {
				while (!Thread.interrupted() && !channel.isClosed()) {
					Object object;

					try {
						LOGGER.info("Waiting for request");
						object = channel.readObject();
						LOGGER.info("Received request");
					} catch (ClassNotFoundException e) {
						LOGGER.error("Class path is out of sync", e);
						continue;
					} catch (InterruptedException e) {
						LOGGER.error(e);
						break;
					} catch (IOException e) {
						LOGGER.error(e);
						continue;
					}

					try {
						processRequest(channel, object);
					} catch (InterruptedException e) {
						LOGGER.error(e);
						break;
					}
				}
			} finally {
				try {
					channel.close();
					LOGGER.info("Listener cleanly shut down.");
				} catch (IOException e) {
					LOGGER.error("Unable to do clean shutdown.", e);
				}
			}
		}

		private void processRequest(ServerSocketRemoteChannel channel, Object bareRequest) throws InterruptedException {
			if (bareRequest != null && bareRequest instanceof Transport) {
				Transport transport = (Transport) bareRequest;
				String requestId = transport.getId();
				Object requestObject = transport.getObject();

				if (requestObject instanceof Callable) {
					LOGGER.info("Scheduling request to be executed");
					//noinspection unchecked
					ezAsync.execute((Callable) requestObject, new RequestCallback(requestId, channel, thread));
					LOGGER.info("Request execution scheduled");
				} else {
					String errorMessage = "Unexpected object type. Expected " + Callable.class.getCanonicalName() + " but was " + (requestObject == null ? "null" : requestObject.getClass().getCanonicalName());
					LOGGER.error(errorMessage);

					try {
						channel.writeObject(Transport.wrapWithId(new Transport.TransportError(errorMessage), requestId));
					} catch (IOException e) {
						LOGGER.error("Failed to respond with error.");
					}
				}

			} else {
				LOGGER.error("Unexpected object type. Expected " + Transport.class.getCanonicalName() + " but was " + (bareRequest == null ? "null" : bareRequest.getClass().getCanonicalName()));
			}
		}
	}

	private static class RequestCallback implements EzAsync.Callback<String> {
		@NotNull
		private final String id;

		@NotNull
		private final ServerSocketRemoteChannel channel;

		@NotNull
		private final Thread threadToInterruptOnError;

		public RequestCallback(@NotNull String id, @NotNull ServerSocketRemoteChannel channel, @NotNull Thread threadToInterruptOnError) {
			this.id = id;
			this.channel = channel;
			this.threadToInterruptOnError = threadToInterruptOnError;
		}

		@Override
		public void done(@Nullable String result) {
			LOGGER.info("Done executing request and received result");
			Transport<?> response = Transport.wrapWithId(result, id);
			try {
				channel.writeObject(response);
			} catch (IOException e) {
				LOGGER.error("Couldn't write response.", e);
			} catch (InterruptedException e) {
				LOGGER.error("Writing response interrupted. Interrupting listening loop.", e);
				threadToInterruptOnError.interrupt();
			}
		}
	}
}
