/*
 * BambooCommunicator.java
 *
 * Created on 13 สิงหาคม 2550, 12:52 น.
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package com.magikMaker.bamboo.communication;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.magikMaker.bamboo.database.BambooDB;
import com.magikMaker.bamboo.database.DatabasePoolManager;
import com.magikMaker.bamboo.model.BambooData;
import com.magikMaker.server.ServerDisplayListenner;
import com.magikMaker.server.data.BambooUnitDataServer;

/**
 * 
 * @author Administrator
 */
public class BambooCommunicator extends TimerTask {

	private Vector<BambooSocket> bambooSockets;
	private ServerDisplayListenner displayListenner;
	public String host;
	public int port;
	private DatabasePoolManager databasePoolManager;
	private Logger log = Logger.getLogger(BambooCommunicator.class);
	private Hashtable<String, BambooData> mGpsdataHash;
	ArrayList<CanBusParamModel> lCanbusModel;
	private Connection connBAMBOO;
	/**
	 * Creates a new instance of BambooCommunicator
	 */
	public BambooCommunicator(ServerDisplayListenner _displayListenner,
			Properties _properties) {

		displayListenner = _displayListenner;

		// host = _host;
		// port = _port;

		bambooSockets = new Vector<BambooSocket>();
		databasePoolManager = DatabasePoolManager.getDatabasePoolManager();
		mGpsdataHash = new Hashtable<String, BambooData>();
		lCanbusModel = GetCanBusParams();
		Timer tm = new Timer(); // Cleaning

		tm.schedule(this, 0, 5 * 60 * 1000);
	}

	// public int dataReceive(BambooDataReceive bambooDataReceive, BambooDB
	// bambooDB, BambooDB bambooDBLogData) {
	public int dataReceive(BambooDataReceive bambooDataReceive) {
		// displayListenner.setDisplay("Processing " +
		// bambooDataReceive.messageIn + ", " + bambooSockets.size());

		int result = 0;

		try {
			// System.out.println("dataReceived "+
			// Thread.currentThread().getName());
			BambooData bambooData = BambooData.getInstance(
					bambooDataReceive.messageIn, bambooDataReceive.dst, host,
					port,lCanbusModel);

			String mobileUnitID = bambooData.mobileUnitID;

/*			
			BambooSocket bambooSocket = null;
			BambooSocket bs = null;

			for (int i = 0; i < bambooSockets.size(); i++) {

				bs = (BambooSocket) bambooSockets.get(i);

				if (mobileUnitID.equals(bs.getMobileUnitID())) {

					// Check for repeat message

					if (bs.isMessageRepeat(bambooDataReceive.messageIn)) {
						// displayListenner.setDisplay("Repeat message from " +
						// mobileUnitID);
						if (BambooUnitDataServer.isLog) {
							log.error("Repeat message from " + mobileUnitID);
						}

						result = 1;

					} else {

						bs.bambooData = bambooData;

					}

					bambooSocket = bs;

					break;

				}

			}

			if (bambooSocket == null) {

				bambooSocket = new BambooSocket(
						bambooDataReceive.socketChannel, bambooData);

				this.unitConnect(bambooSocket);
			}
*/
			// Process receive data

			if (result == 0) {

				Calendar cDate = Calendar.getInstance();
				// Harish 20.12.2011
				cDate.add(Calendar.MINUTE, 30);
				Date date = cDate.getTime();
				Date gpsDate = bambooData.gpsDate.getTime();
				
				// added by Sivaraj on 28-08-2014
				//When there is no GPS signal, sometimes year will be 8888 (in case of Pointer).
				//If we ignore such data there wont be data in DB for a long time, Hence setting gprsdate as gpsdate to help us find whether we get data from devices or not.
				if(bambooData != null && bambooData.gpsDate != null
						&& bambooData.gpsDate.after(cDate)){
					if(bambooData.gpsStatus == 'N'){
						bambooData.gpsDate = bambooData.gprsDate;
						log.error(bambooData.mobileUnitID + " - 30 mins ahead data recvd. but gps date is set with gprsdate"); 
					}
				}
				// added by Sivaraj - End
				
				if (bambooData != null && bambooData.gpsDate != null
						&& !bambooData.gpsDate.after(cDate)) {
					// BambooDB lBambooDB = new
					// BambooDB(displayListenner,bambooData,bambooDataReceive.socketChannel);
					BambooDB lBambooDB = new BambooDB(displayListenner,
							bambooData,mGpsdataHash);
					databasePoolManager.ProcessUnitData(lBambooDB);
					lBambooDB = null;
					bambooDataReceive = null;
				} else {
					if (BambooUnitDataServer.isLog) {
						log.error(bambooData.mobileUnitID
								+ " : 30 mins ahead data recvd & not processed - Recvd Date : "
								+ bambooData.gpsDate.get(Calendar.YEAR) + "-"
								+ bambooData.gpsDate.get(Calendar.MONTH) + "-"
								+ bambooData.gpsDate.get(Calendar.DATE) + ":"
								+ bambooData.gpsDate.get(Calendar.HOUR_OF_DAY)
								+ ":" + bambooData.gpsDate.get(Calendar.MINUTE)
								+ ":" + bambooData.gpsDate.get(Calendar.SECOND));
					}

				}

			}

		} catch (Exception ex) {
			log.error("", ex);
		}

		return result;

	}
	
	private ArrayList<CanBusParamModel> GetCanBusParams()
	{
		PreparedStatement pstmt = null;
		ArrayList<CanBusParamModel> llParam = new ArrayList<CanBusParamModel>();
		try {			
			connBAMBOO = DatabasePoolManager.getConnection();
			pstmt = connBAMBOO
					.prepareStatement("SELECT * FROM canbus_params;");		
			ResultSet cts = pstmt.executeQuery();			
			   while (cts.next()) {
				CanBusParamModel llMod = new CanBusParamModel();
				llMod.setZt_var_id(cts.getString("zt_var_id"));
				llMod.setCan_var_id(cts.getString("can_var_id"));
				llMod.setCan_var_title(cts.getString("can_var_title"));
				llMod.setCan_var_unit_type(cts.getString("can_var_unit_type"));
				llMod.setCan_var_data_type(cts.getString("can_var_data_type"));
				llMod.setCan_var_offset(cts.getInt("can_var_offset"));
				llMod.setCan_var_mulitpler(cts.getInt("can_var_mulitpler"));
				llMod.setCan_var_divider(cts.getInt("can_var_divider"));
				llParam.add(llMod);
			}
			cts.close();
			pstmt.close();
			pstmt = null;
			connBAMBOO.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return llParam;
		
	}

	private synchronized void unitConnect(BambooSocket bambooSocket) {
		if (bambooSocket != null) {
			bambooSockets.add(bambooSocket);
		}
	}

	public synchronized void unitDisconnected(BambooSocket bambooSocket) {
		if (bambooSocket != null) {
			bambooSocket.disconnect();
			bambooSockets.remove(bambooSocket);
		}
	}

	public void sendCommand(String mobileUnitID, String command) {

		for (int i = 0; i < bambooSockets.size(); i++) {

			BambooSocket bambooSocket = (BambooSocket) bambooSockets.get(i);

			if (mobileUnitID.equals(bambooSocket.getMobileUnitID())) {

				if (!bambooSocket.sendCommand(mobileUnitID, command)) {
					if (BambooUnitDataServer.isLog) {
						log.error("Error in sending command to Unit : "
								+ mobileUnitID);
					}
					this.unitDisconnected(bambooSocket);
				}
				break;
			}

		}
		return;
	}

	public synchronized void cleanning() {

		for (int i = 0; i < bambooSockets.size(); i++) {

			BambooSocket bambooSocket = (BambooSocket) bambooSockets.get(i);
			if (bambooSocket.isClosed()) {

				unitDisconnected(bambooSocket);

				if (bambooSockets.size() > i + 1) {
					bambooSockets.remove(i);
					bambooSocket = null;
					i = i - 1;
				}

			}

		}

	}

	@Override
	public void run() {

		cleanning();

	}
}
