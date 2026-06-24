package com.magikMaker.bamboo.database;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.postgresql.jdbc3.Jdbc3PoolingDataSource;

public class DatabasePoolManager {

	private ThreadPoolExecutor threadPool;
	
	private int poolSize;
	 
	private int maxPoolSize;
 
	private long keepAliveTime;
    
    private static Jdbc3PoolingDataSource  source;    

	private LinkedBlockingQueue<Runnable> queue;
	
	private static DatabasePoolManager databasePoolManager;
	private Properties properties = new Properties();
	
	public static DatabasePoolManager getDatabasePoolManager()
	{
		
		
		if(databasePoolManager == null)
		{
			databasePoolManager = new DatabasePoolManager(); 
		}
		return databasePoolManager;
	}
	
	public int GetQueueLength()
	{
		return databasePoolManager.queue.size();
	}
	
	private DatabasePoolManager()
	
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
		poolSize = Integer.parseInt(properties.getProperty("PoolSize"));
		 
	    maxPoolSize = Integer.parseInt(properties.getProperty("MaxPoolSize"));
	 
	   keepAliveTime = Integer.parseInt(properties.getProperty("KeepAliveTime"));
		queue = new LinkedBlockingQueue<Runnable>();
		threadPool = new ThreadPoolExecutor(poolSize, maxPoolSize,
                keepAliveTime, TimeUnit.SECONDS, queue);
		
        source = new Jdbc3PoolingDataSource();
        try {
			source.setDatabaseName(properties.getProperty("DatabaseName"));
			source.setServerName(properties.getProperty("ServerName"));
	        source.setUser(properties.getProperty("User"));
	        source.setPassword(properties.getProperty("Password"));
	        source.setDataSourceName("vTrack");
	  //      source.setDataSourceName("vTrack-restore1");
	        source.setPortNumber(Integer.parseInt(properties.getProperty("PortNumber")));
	        int IntitialConnection = Integer.parseInt(properties.getProperty("InitialConnections"));
	        source.setInitialConnections(IntitialConnection);
	        int MaximumConnections = Integer.parseInt(properties.getProperty("MaxConnections"));
	        source.setMaxConnections(MaximumConnections);
	        source.initialize();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void StopPool()
	{
		threadPool.shutdown();
	}
	
	public void ProcessUnitData(Runnable task)
	{
		threadPool.execute(task);
	}
	
	public static Connection getConnection() throws SQLException
	{
		return source.getConnection();
	}
	
}
