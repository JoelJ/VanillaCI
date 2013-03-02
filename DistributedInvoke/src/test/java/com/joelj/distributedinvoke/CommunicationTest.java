package com.joelj.distributedinvoke;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

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

	@AfterMethod
	public void tearDown() {
		if(listener != null) {
			listener.close();
		}
	}

	@Test
	public void testBasic() throws Exception {
		InetAddress localHost = Inet4Address.getLocalHost();
		int listeningPort = 9191;

		listener = RemoteMachineListener.start(localHost, listeningPort);

		RemoteMachine machine = RemoteMachineImpl.connectToMachine("Test Machine", localHost, listeningPort);

		String invoke = machine.invoke(new MyCallable("Hello There"));

		assertEquals(invoke, "Hello There");
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