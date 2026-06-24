package com.claystone.server.heartbeat;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import com.magikMaker.bamboo.database.DatabasePoolManager;

public class HeartBeat {
	private static Properties properties;
	private static String processID = null;
	private static String programName = null;
	private static String startupScript = null;
	private static String shutdownScript = null;
	private static String delayMinsStr = null;
	private static int delayMins = 0;
	private static final String sqlCountStr = "SELECT count(*) as rec_count from heart_beat WHERE process_id=?";
	private static final String sqlInsertHB = "INSERT INTO heart_beat ( process_id, program_name, startup_script, shutdown_script, allowed_delay_mins, start_time, last_heart_beat_time, sms_flag) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?);";
	private static final String sqlUpdateStartupHB = "Update heart_beat set start_time = ? ,last_heart_beat_time = ?, sms_flag = false where process_id = ?;";
	private static final String sqlUpdateHB = "Update heart_beat set last_heart_beat_time = ?, sms_flag = false where process_id = ?;";
	private static long heartBeatInterval = 5 * 60; //seconds. 
	private static Date lastHBTime = new Date();
	public static boolean LogStartup(){
		
		try{
	        properties = new Properties();
	        properties.load(new FileInputStream("server.properties"));
	        
	        processID = properties.getProperty("ProcessID");
	        programName = properties.getProperty("ProgramName");
	        startupScript = properties.getProperty("StartupScript");
	        shutdownScript = properties.getProperty("ShutdownScript");
	        delayMinsStr = properties.getProperty("DelayMins");
	        
	        if(processID == null || startupScript == null || shutdownScript == null){
	        	throw(new Exception("server.properties not set"));
	        }
	        if(delayMinsStr != null){
	        	delayMins = Integer.parseInt(delayMinsStr);
	        }

	        String hbInterval = properties.getProperty("HeartBeatInterval");
	        if(hbInterval != null){
	        	heartBeatInterval = Long.parseLong(hbInterval);
	        }
	        
	        InsertStartupMessage();
	        return true;
		}catch(IOException e){
			e.printStackTrace();
			return false;
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
	}
	
	private static void InsertStartupMessage(){
		

		Connection lConnection = null;
		PreparedStatement pstmt = null;
		PreparedStatement pstmtInsert = null;
		PreparedStatement pstmtUpdate = null;
		
		ResultSet cts = null;
		try{
			lConnection = DatabasePoolManager.getConnection();
			lConnection.setAutoCommit(false);
			pstmt = lConnection.prepareStatement(sqlCountStr);
            Timestamp tStamp = new Timestamp(Calendar.getInstance().getTimeInMillis());
            pstmt.setString(1, processID);
		
            cts = pstmt.executeQuery();
		
            cts.next();
            int recordCount = cts.getInt("rec_count");

            if(recordCount <= 0){
            	pstmtInsert = lConnection.prepareStatement(sqlInsertHB);
            	pstmtInsert.setString(1, processID);
            	pstmtInsert.setString(2, programName);
            	pstmtInsert.setString(3, startupScript);
            	pstmtInsert.setString(4, shutdownScript);
            	pstmtInsert.setInt(5, delayMins);
            	pstmtInsert.setTimestamp(6, tStamp);
            	pstmtInsert.setTimestamp(7, tStamp);
            	pstmtInsert.setBoolean(8,false);
            	pstmtInsert.executeUpdate();
            }else{
            	pstmtUpdate = lConnection.prepareStatement(sqlUpdateStartupHB);
            	pstmtUpdate.setTimestamp(1,tStamp);
            	pstmtUpdate.setTimestamp(2,tStamp);
            	pstmtUpdate.setString(3, processID);
            	pstmtUpdate.executeUpdate();
            }
            
            lConnection.commit();
		}catch(Exception e){
			e.printStackTrace();
			try {
				lConnection.rollback();
			}catch(Exception e1){
				e1.printStackTrace();
			}
		}
				
		try{
			if (pstmt != null)
			{
				pstmt.close();
			}
			if(cts != null)
			{
	            cts.close();	
			}
			if(pstmtInsert != null){
				pstmtInsert.close();
			}
			if(pstmtUpdate != null){
				pstmtUpdate.close();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
				
		try{
			if(lConnection != null){
				lConnection.close();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void LogHearBeat(){

		Date currTime = new Date();
		long diff = (currTime.getTime() - lastHBTime.getTime())/1000;
		if(diff < heartBeatInterval)
			return;
		else
			lastHBTime = currTime;
		
		Connection lConnection = null;
		PreparedStatement pstmtUpdate = null;
		try{
			lConnection = DatabasePoolManager.getConnection();
			lConnection.setAutoCommit(false);
            Timestamp tStamp = new Timestamp(Calendar.getInstance().getTimeInMillis());
        	pstmtUpdate = lConnection.prepareStatement(sqlUpdateHB);
        	pstmtUpdate.setTimestamp(1,tStamp);
        	pstmtUpdate.setString(2, processID);
        	pstmtUpdate.executeUpdate();
            lConnection.commit();
		}catch(Exception e){
			e.printStackTrace();
			try {
				lConnection.rollback();
			}catch(Exception e1){
				e1.printStackTrace();
			}
		}
				
		try{
			if(pstmtUpdate != null){
				pstmtUpdate.close();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
				
		try{
			if(lConnection != null){
				lConnection.close();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

}
