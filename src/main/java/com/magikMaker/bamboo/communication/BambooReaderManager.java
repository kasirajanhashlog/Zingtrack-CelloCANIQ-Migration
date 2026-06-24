package com.magikMaker.bamboo.communication;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class BambooReaderManager {

	private ThreadPoolExecutor threadPool;

	private int poolSize;

	private int maxPoolSize;

	private long keepAliveTime;

	private LinkedBlockingQueue<Runnable> queue;

	private Properties properties = new Properties();

	private static BambooReaderManager bambooReaderManager;

	public static BambooReaderManager getDatabasePoolManager() {

		if (bambooReaderManager == null) {
			bambooReaderManager = new BambooReaderManager();
		}
		return bambooReaderManager;
	}

	public int GetQueueLength() {
		return bambooReaderManager.queue.size();
	}

	private BambooReaderManager()

	{
		try {
			properties.load(new FileInputStream("server.properties"));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		poolSize = Integer.parseInt(properties.getProperty("BambooPoolSize"));

		maxPoolSize = Integer.parseInt(properties
				.getProperty("BambooMaxPoolSize"));

		keepAliveTime = Integer.parseInt(properties
				.getProperty("BambooKeepAliveTime"));
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
