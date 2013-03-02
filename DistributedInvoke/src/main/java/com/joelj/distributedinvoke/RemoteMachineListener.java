package com.joelj.distributedinvoke;

import com.joelj.distributedinvoke.logging.Logger;
import com.joelj.ezasync.EzAsync;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
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
	 * @param bindAddress The address to bind to.
	 * @param listeningPort The port to listen on.
	 * @return The new instance of RemoteMachineListener that is actively listening for a new connection.
	 */
	public static RemoteMachineListener start(@NotNull InetAddress bindAddress, int listeningPort) {
		return new RemoteMachineListener(bindAddress, listeningPort);
	}

	/**
	 * @param bindAddress The address to bind to.
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
			ServerSocket serverSocket;
			try {
				serverSocket = new ServerSocket(listeningPort, 0, bindAddress);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			Socket clientSocket = null;
			while(!Thread.interrupted()) {
				if(clientSocket == null) {
					//Either this is the first iteration, or we lost connection with the client and we should wait for it to reconnect.
					try {
						clientSocket = serverSocket.accept();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}

				assert clientSocket != null;

				ObjectInputStream inputStream;
				try {
					inputStream = new ObjectInputStream(clientSocket.getInputStream());
				} catch (IOException e) {
					LOGGER.warn("IOException. Retrying connection.", e);
					LOGGER.info("Attempting to close current socket.");
					try {
						clientSocket.close();
					} catch (IOException closeException) {
						LOGGER.info("Couldn't close current socket.", closeException);
					}
					clientSocket = null;
					continue;
				}

				Object object;
				try {
					LOGGER.info("Waiting for request");
					object = inputStream.readObject();
					LOGGER.info("Received request");
				} catch (IOException e) {
					LOGGER.error(e);
					continue;
				} catch (ClassNotFoundException e) {
					LOGGER.error("Class path is out of sync", e);
					continue;
				}

				if(object != null && object instanceof Transport) {
					Transport transport = (Transport)object;
					String requestId = transport.getId();
					Object requestObject = transport.getObject();

					if(requestObject instanceof Callable) {
						LOGGER.info("Scheduling request to be executed");
						//noinspection unchecked
						ezAsync.execute((Callable) requestObject, new RequestCallback(requestId, clientSocket));
						LOGGER.info("Request execution scheduled");
					} else {
						LOGGER.error("Unexpected object type. Expected " + Callable.class.getCanonicalName() + " but was " + (requestObject == null ? "null" : requestObject.getClass().getCanonicalName()));
					}

				} else {
					LOGGER.error("Unexpected object type. Expected " + Transport.class.getCanonicalName() + " but was " + (object == null ? "null" : object.getClass().getCanonicalName()));
				}
			}

			try {
				serverSocket.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			LOGGER.info("Listener shut down.");
		}
	}

	private static class RequestCallback implements EzAsync.Callback<String> {
		@NotNull
		private final String id;

		@NotNull
		private final Socket clientSocket;

		public RequestCallback(@NotNull String id, @NotNull Socket clientSocket) {
			this.id = id;
			this.clientSocket = clientSocket;
		}

		@Override
		public void done(@Nullable String result) {
			LOGGER.info("Done executing request and received result");
			Transport<?> response = Transport.wrapWithId(result, id);
			try {
				ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
				LOGGER.info("Writing response");
				outputStream.writeObject(response);
			} catch (IOException e) {
				LOGGER.error("Couldn't write response.", e);
			}
		}
	}
}
