package com.magikMaker.Test;



import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TestClientManager {

	private ThreadPoolExecutor threadPool;

	private int poolSize;

	private int maxPoolSize;

	private long keepAliveTime;
	public static int NO_OF_CLIENTS;
	public static long CLIENT_ID;
	public static int PORT;
	public static String HOST;
	private LinkedBlockingQueue<Runnable> queue;

	private Properties properties = new Properties();

	private static TestClientManager testClientManager;

	public static TestClientManager getTestClientManager() {

		if (testClientManager == null) {
			testClientManager = new TestClientManager();
		}
		return testClientManager;
	}

	public int GetQueueLength() {
		return testClientManager.queue.size();
	}

	private TestClientManager()

	{
		try {
			properties.load(new FileInputStream("client.properties"));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		poolSize = Integer.parseInt(properties.getProperty("PoolSize"));

		maxPoolSize = Integer.parseInt(properties
				.getProperty("MaxPoolSize"));
		NO_OF_CLIENTS=Integer.parseInt(properties.getProperty("no_of_clients"));
		CLIENT_ID=Long.parseLong(properties.getProperty("client_id"));
		HOST=(properties.getProperty("host"));
		PORT=Integer.parseInt(properties.getProperty("port"));
		keepAliveTime = Integer.parseInt(properties
				.getProperty("KeepAliveTime"));
		queue = new LinkedBlockingQueue<Runnable>();
		threadPool = new ThreadPoolExecutor(poolSize, maxPoolSize,
				keepAliveTime, TimeUnit.SECONDS, queue);
	}

	public void StopPool() {
		threadPool.shutdown();
	}

	public void ProcessUnitData(Runnable task) {
		threadPool.execute(task);
	}

}
