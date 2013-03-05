package com.joelj.distributedinvoke;

import com.joelj.distributedinvoke.channels.ResultFuture;
import com.joelj.distributedinvoke.exceptions.NotEnoughExecutorsException;
import com.joelj.distributedinvoke.machines.Machine;
import com.joelj.distributedinvoke.machines.RemoteMachine;
import com.joelj.distributedinvoke.machines.RemoteMachineListener;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.Serializable;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.concurrent.Callable;

import static org.testng.Assert.*;

/**
 * User: Joel Johnson
 * Date: 3/2/13
 * Time: 12:35 PM
 */
public class CommunicationTest {
	private RemoteMachineListener listener;
	private Machine machine;

	@AfterMethod
	public void tearDownListener() throws IOException {
		if(listener != null) {
			listener.close();
		}
	}

	@AfterMethod
	public void tearDownMachine() throws IOException, InterruptedException {
		if(machine != null) {
			machine.close();
		}
	}

	@Test
	public void testBasic() throws Exception {
		InetAddress localHost = Inet4Address.getLocalHost();
		int listeningPort = 9191;

		listener = RemoteMachineListener.start(localHost, listeningPort);
		machine = RemoteMachine.connectToMachine("Test Machine", localHost, listeningPort, 1, "");

		assertEquals(machine.getAvailableExecutorCount(), 1);
		String invoke = machine.invoke(new MyCallable("Hello There"), 1);
		assertEquals(machine.getAvailableExecutorCount(), 1, "Available executor count should've been reset.");

		assertEquals(invoke, "Hello There");

		try {
			machine.invoke(new MyCallable("This is too heavy!"), 10);
			fail("Should throw " + NotEnoughExecutorsException.class.getCanonicalName());
		} catch (NotEnoughExecutorsException ignore) {}
	}

	@Test
	public void testAsync() throws Exception {
		InetAddress localHost = Inet4Address.getLocalHost();
		int listeningPort = 9191;

		listener = RemoteMachineListener.start(localHost, listeningPort);
		machine = RemoteMachine.connectToMachine("Test Machine", localHost, listeningPort, 1, "");

		assertEquals(machine.getAvailableExecutorCount(), 1);
		ResultFuture<String> future = machine.invokeAsync(new SlowCallable("Hello There", 100), 1);
		assertEquals(machine.getAvailableExecutorCount(), 0, "Available executor count should be zero");

		String result = future.waitForResult();

		assertEquals(result, "Hello There");
	}
}

class MyCallable implements Callable<String>, Serializable {
	private final String returnValue;
	public MyCallable(String returnValue) {

		this.returnValue = returnValue;
	}

	@Override
	public String call() throws Exception {
		return returnValue;
	}
}

class SlowCallable implements Callable<String>, Serializable {
	private final String returnValue;
	private final int speed;

	public SlowCallable(String returnValue, int speed) {
		this.returnValue = returnValue;
		this.speed = speed;
	}

	@Override
	public String call() throws Exception {
		Thread.sleep(speed);
		return returnValue;
	}
}