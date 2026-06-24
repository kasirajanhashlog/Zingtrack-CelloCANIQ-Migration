package com.magikMaker.FileManager;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.magikMaker.server.data.BambooUnitDataServer;

public class BambooDataFilePoolManager {

	private Logger log = LogManager.getLogger(BambooDataFilePoolManager.class);

	private ThreadPoolExecutor threadPool;

	private int port;

	private int poolSize;

	private int maxPoolSize;

	private long keepAliveTime;

	private LinkedBlockingQueue<Runnable> queue;

	private static BambooDataFilePoolManager fileWriteManager;
	private Properties properties = new Properties();

	private RandomAccessFile fileStream = null;

	public static BambooDataFilePoolManager getFileWriteManager() {
		if (fileWriteManager == null) {
			fileWriteManager = new BambooDataFilePoolManager();
		}
		return fileWriteManager;
	}

	public int GetQueueLength() {
		return fileWriteManager.queue.size();
	}

	private BambooDataFilePoolManager() {
		try {
			properties.load(new FileInputStream("server.properties"));
		} catch (FileNotFoundException e1) {

			if (BambooUnitDataServer.isLog) {
				log.error("", e1);
			}

		} catch (IOException e1) {

			if (BambooUnitDataServer.isLog) {
				log.error("", e1);
			}

		}
		port = Integer.parseInt(properties.getProperty("serverPortData"));

		poolSize = Integer.parseInt(properties.getProperty("FilePoolSize"));

		maxPoolSize = Integer.parseInt(properties
				.getProperty("FileMaxPoolSize"));

		keepAliveTime = Integer.parseInt(properties
				.getProperty("FileKeepAliveTime"));
		properties.clear();
		properties = null;
		queue = new LinkedBlockingQueue<Runnable>();
		threadPool = new ThreadPoolExecutor(poolSize, maxPoolSize,
				keepAliveTime, TimeUnit.SECONDS, queue);

		try {
			fileStream = new RandomAccessFile(port + "-BambooMessage", "rwd");
		} catch (Exception e) {
			if (BambooUnitDataServer.isLog) {
				log.error("", e);
			}
		}
	}

	public void StopPool() {
		threadPool.shutdown();
	}

	public void ProcessUnitData(Runnable task) {
		threadPool.execute(task);
	}

	public synchronized void WriteData(ByteBuffer message) {
		if (message != null) {
			try {
				//String temp = message.concat("#");
				if (fileStream != null) {
					fileStream.seek(fileStream.length());
					fileStream.write(message.array());
				} else {
					try {
						fileStream = new RandomAccessFile(port
								+ "-BambooMessage", "rwd");
						fileStream.seek(fileStream.length());
						fileStream.write(message.array());
					} catch (Exception e) {
						if (BambooUnitDataServer.isLog) {
							log.error("", e);
						}
					}
				}
			} catch (IOException e) {

				if (BambooUnitDataServer.isLog) {
					log.error("", e);
				}
				if (fileStream != null) {
					try {
						fileStream.close();
						fileStream = null;
					} catch (IOException e1) {

						if (BambooUnitDataServer.isLog) {
							log.error("", e1);
						}
					}
				}
			}
		}
	}

}
