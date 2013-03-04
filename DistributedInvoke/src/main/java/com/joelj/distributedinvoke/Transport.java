package com.joelj.distributedinvoke;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.UUID;

/**
 * Represents an object that has been sent to a remote machine.
 * The Transport has a unique ID that can be used to match up with a response.
 *
 * User: Joel Johnson
 * Date: 3/2/13
 * Time: 10:45 AM
 */
public class Transport<T> implements Serializable {
	private final String id;
	private final T object;

	@NotNull
	public static <T> Transport<T> wrap(@Nullable T object) {
		String id = UUID.randomUUID().toString();
		return new Transport<T>(id, object);
	}

	@NotNull
	public static <T> Transport<T> wrapWithId(@Nullable T object, @NotNull String id) {
		return new Transport<T>(id, object);
	}

	private Transport(@NotNull String id, @Nullable T object) {
		this.id = id;
		this.object = object;
	}

	@NotNull
	public String getId() {
		return id;
	}

	@Nullable
	public T getObject() {
		return object;
	}

	@Override
	public String toString() {
		return "Transport<" + (object == null ? "null" : object.getClass().getCanonicalName()) +"> " + id;
	}

	public static class TransportError implements Serializable {
		private final String message;

		public TransportError(String message) {
			this.message = message;
		}

		public String getMessage() {
			return message;
		}
	}
}
