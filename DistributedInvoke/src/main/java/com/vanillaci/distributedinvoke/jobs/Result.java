package com.vanillaci.distributedinvoke.jobs;

import org.jetbrains.annotations.NotNull;

/**
 * User: Joel Johnson
 * Date: 3/4/13
 * Time: 11:22 PM
 */
public enum Result {
	SUCCESS(1),
	FAILURE(2),
	ERROR(3),
	CANCELED(4);

	private final int severity;
	Result(int severity) {
		this.severity = severity;
	}

	public int getSeverity() {
		return severity;
	}

	public boolean isWorseThan(@NotNull Result result) {
		return this.severity > result.getSeverity();
	}

	public boolean isWorseThanOrEqualTo(@NotNull Result result) {
		return this.severity >= result.getSeverity();
	}

	public boolean isBetterThan(@NotNull Result result) {
		return this.severity < result.getSeverity();
	}

	public boolean isBetterThanOrEqualTo(@NotNull Result result) {
		return this.severity < result.getSeverity();
	}
}
