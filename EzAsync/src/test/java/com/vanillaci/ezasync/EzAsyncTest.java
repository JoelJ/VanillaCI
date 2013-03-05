package com.vanillaci.ezasync;

import org.jetbrains.annotations.Nullable;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.*;

/**
 * User: Joel Johnson
 * Date: 3/2/13
 * Time: 12:52 PM
 */
public class EzAsyncTest {
	@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
	@Test(timeOut = 10000)
	public void testCallback() throws InterruptedException {
		System.out.println("1");
		final Callable<String> task = new Callable<String>() {
			@Override
			public String call() throws Exception {
				System.out.println("2");
				return "This is the task";
			}
		};

		final boolean[] called = new boolean[1];
		called[0] = false;

		EzAsync.Callback<String> callback = new EzAsync.Callback<String>() {
			@Override
			public void done(@Nullable String result) {
				called[0] = true;
				Assert.assertEquals(result, "This is the task");
				System.out.println("3");
			}
		};

		EzAsync async = EzAsync.create();
		async.execute(task, callback);
		Thread.sleep(5000);
		System.out.println("4");

		Assert.assertTrue(called[0]);
	}
}
