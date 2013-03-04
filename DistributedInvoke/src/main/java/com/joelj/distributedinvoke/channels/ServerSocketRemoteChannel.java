package com.joelj.distributedinvoke.channels;

import com.joelj.distributedinvoke.Lock;
import com.joelj.distributedinvoke.logging.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * User: Joel Johnson
 * Date: 3/3/13
 * Time: 7:31 PM
 */
public class ServerSocketRemoteChannel extends AutoReconnectingChannel {
	private static final Logger LOGGER = Logger.forClass(AutoReconnectingChannel.class);

	@NotNull private final ServerSocket serverSocket;
	private volatile boolean closed;
	private final Lock closeLock = new Lock();

	public static ServerSocketRemoteChannel create(InetAddress bindAddress, int listeningPort) {
		try {
			ServerSocket serverSocket = new ServerSocket(listeningPort, 0, bindAddress);
			return new ServerSocketRemoteChannel(serverSocket);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private ServerSocketRemoteChannel(@NotNull ServerSocket serverSocket) throws IOException {
		super(ServerSocketRemoteChannel.class.getCanonicalName(), null);
		this.serverSocket = serverSocket;
	}

	@NotNull
	@Override
	protected Socket reconnect() throws IOException {
		LOGGER.info("Waiting for client to connect.");
		Socket accept = serverSocket.accept();
		LOGGER.info("Accepted client.");
		return accept;
	}

	@Override
	public void close() throws IOException {
		if(closed) {
			throw new IllegalStateException("Already closed.");
		}

		synchronized (closeLock) {
			if(closed) {
				throw new IllegalStateException("Already closed.");
			}

			closed = true;
			try {
				super.close();
			} finally {
				serverSocket.close();
			}
		}
	}

	public boolean isClosed() {
		return closed;
	}
}
