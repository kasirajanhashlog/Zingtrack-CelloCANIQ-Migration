/*
 * BambooDB.java
 *

 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package com.magikMaker.bamboo.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;

import org.apache.log4j.Logger;

import com.claystone.server.heartbeat.HeartBeat;
import com.magikMaker.FileManager.BambooDataFilePoolManager;
import com.magikMaker.FileManager.BambooFileWriter;
import com.magikMaker.bamboo.communication.CanBusParamModel;
import com.magikMaker.bamboo.model.BambooData;
import com.magikMaker.bamboo.model.Module2ParamClass;
import com.magikMaker.server.ServerDisplayListenner;
import com.magikMaker.server.data.BambooUnitDataServer;

/**
 * 
 * @author Administrator
 */
public class BambooDB implements Runnable {

	public static int insert_count = 0;
	private static final String insertBambooGPSData = "INSERT INTO gpsdata ( mobileunitid, gpsdate, gprsdate, gpsstatus, latitude, longitude, speed, direction, acc, in1, in2, in3, in4, out1, out2, eventcode, adc1, adc2, batt, mileage, reserve ) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? );";
	private static final String insertBambooCanbusGPSData = "INSERT INTO canbus_gpsdata (mobileunitid,gpsdate,gprsdate,output_1,output_2,output_3,output_4,output_5,output_6,output_7,output_8,output_9,output_10,output_11,output_12,output_13,output_14,output_15,output_16,output_17,output_18,output_19,output_20,output_21,output_22,output_23,output_24,output_25,output_26,output_27,output_28,output_29,output_30,output_31,output_32,output_33,output_34,output_35,output_36,output_37,output_38,output_39,output_40,output_41,output_42,output_43,output_44,output_45,output_46,output_47,output_48,output_49,output_50,output_51,output_52,output_53,output_54,output_55,output_56,output_57,output_58,output_59,output_60,output_61,output_62,output_63,reserve,processed_flag) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";
	private Connection connBAMBOO; // connGIS, connGPRS, connGoogleData,
	private Timestamp gpsDate, gprsDate;
	private BambooData bamboodata;
	private Hashtable<String, BambooData> mGpsdataHash;
//	private ServerDisplayListenner displayListenner;
//	private byte[] reply = "(Y)".getBytes();
//	private SocketChannel socketChannel;
	private Logger log = Logger.getLogger(BambooDB.class);
	public ArrayList<Module2ParamClass> lModuleParam;
	private BambooDataFilePoolManager dataFilePoolManager = BambooDataFilePoolManager.getFileWriteManager();

	public BambooDB(ServerDisplayListenner _displayListenner,
			// BambooData bamboodata, SocketChannel channel) {
			BambooData bamboodata, Hashtable<String, BambooData> pGpsdataHash) {

		// displayListenner = _displayListenner;

		this.bamboodata = bamboodata;
		mGpsdataHash = pGpsdataHash;
		// socketChannel = channel;
	}

	private boolean storeBamboo(Connection connBAMBOO) {

//		displayListenner.setDisplay("Bamboo process start: "
//				+ bamboodata.mobileUnitID);

		if (bamboodata.mobileUnitID.equals("")) {
			if (BambooUnitDataServer.isLog) {
				log.error("Invalid UnitID");
			}

			return true;
		}
//		if (bamboodata.latitude == 0 || bamboodata.longitude == 0) {
//			if(BambooUnitDataServer.isLog)
//			{
//				log.error(bamboodata.mobileUnitID + " : Latitude/Longitude is 0");
//			}
//			
//			return;
//		}

		Calendar cDate = Calendar.getInstance();

		cDate.add(Calendar.MINUTE, 30); // 30 mins ahead data is rejected.

		if (bamboodata.gpsDate.after(cDate)) {
			if (BambooUnitDataServer.isLog) {
				log.error(bamboodata.mobileUnitID + " : 30 mins ahead data recvd & not processed");
			}

			return true;
		}

		cDate.add(Calendar.DATE, -120); // 4 months old data is rejected.

		if (bamboodata.gpsDate.before(cDate)) {
			if (BambooUnitDataServer.isLog) {
				log.error(bamboodata.mobileUnitID + " : 4 months old data recvd & not processed");
			}

			return true;
		}

		PreparedStatement pstmt = null;

		try {

			pstmt = connBAMBOO.prepareStatement("SELECT * FROM imeidata WHERE imei=? ORDER BY fromdate DESC LIMIT 1;");

			pstmt.setString(1, this.bamboodata.mobileUnitID);

			ResultSet cts = pstmt.executeQuery();

			if (cts.next()) {
				this.bamboodata.mobileUnitID = cts.getString("mobileunitid");
			}
			cts.close();
			pstmt.close();
			pstmt = null;

			// Check for duplicate data (based on mobileunitid,gpsdate)
			// Check added by Sivaraja on 02/02/2015
			// START Duplicate row check
			PreparedStatement dupStmt = null;
			dupStmt = connBAMBOO.prepareStatement("SELECT * FROM gpsdata WHERE mobileunitid=? and gpsdate=? ;");
			dupStmt.setString(1, bamboodata.mobileUnitID);
			dupStmt.setTimestamp(2, gpsDate);

			ResultSet dupSet = dupStmt.executeQuery();
			if (dupSet.next()) {
				if ((bamboodata.messageType.equals("11")
						&& (dupSet.getInt("adc1") != bamboodata.adc1 || dupSet.getInt("adc2") != bamboodata.adc2))
						|| (bamboodata.messageType.equals("0") && dupSet.getBoolean("in3") != bamboodata.in3)) {
					// for avoid discarding the different event data is receiving in same gpsdate
					bamboodata.gpsDate.add(Calendar.MILLISECOND, 1);
					gpsDate = getSQLTimestamp(bamboodata.gpsDate);
				} else {
					// Duplicate record found, discard incoming record and return
					if (BambooUnitDataServer.isLog) {
						log.error("Duplicate data received and discarded for : " + bamboodata.mobileUnitID + "/"
								+ gpsDate);
					}
					dupSet.close();
					dupStmt.close();
					return true;
				}
			} else {
				dupSet.close();
				dupStmt.close();
			}

			// END Duplicate row check

			// changed on 28-04-2017
			pstmt = setGPSDataIntoPSTParameters(connBAMBOO, pstmt);
			pstmt.execute();
			log.info("New Data Inserted : " + pstmt.toString());
			connBAMBOO.commit();
			try {

				pstmt.close();
			} catch (SQLException ex) {
				if (BambooUnitDataServer.isLog) {
					log.error("", ex);
				}
			}
			if (BambooUnitDataServer.isLog) {
				log.info("data inserted for mobile unit - " + bamboodata.mobileUnitID + "; thread - "
						+ Thread.currentThread().getName());
			}

			// Added for 2021-Dec-10 Canbus Module2 Params
			if (bamboodata.lModuleParam != null) {
				lModuleParam = bamboodata.lModuleParam;
				pstmt = setCanbusGPSDataIntoPSTParameters(connBAMBOO, pstmt);
				pstmt.execute();
				log.info("New Data Inserted : " + pstmt.toString());
				connBAMBOO.commit();
				try {

					pstmt.close();
				} catch (SQLException ex) {
					if (BambooUnitDataServer.isLog) {
						log.error("", ex);
					}
				}
			}
			// Added for 2021-Dec-10 Canbus Module2 Params

//			System.out.println( bamboodata.mobileUnitID + " : BambooDb process completed "
//					+ Thread.currentThread().getName());
		} catch (Exception E) {
			try {
				// added on 28-04-2017. Set the actual data into PreparedStatement Parameters
				// for
				// log that data if any exception throws while checking for duplicate data.
				if (pstmt == null) {
					pstmt = setGPSDataIntoPSTParameters(connBAMBOO, pstmt);
				} // added - End

				if (pstmt != null) {
					// added on 05-01-2017 for log the actual data if any exception throws while
					// inserting.
					// Parse the insert query and print in log for manually insert actual data into
					// table
					// String insertQry = parseInsertQuery(pstmt.toString());
					// log.fatal("mobile unit - " + bamboodata.mobileUnitID + "; " +
					// E.getMessage());
					// if(insertQry != null){
					// log.fatal("With Parsed Query: " + insertQry);
					// }else{
					// log.fatal("Without Parsed Query: " + pstmt.toString());
					// }
					log.error("New Data Inserted error: " + pstmt.toString());
					pstmt.close();
				}
				connBAMBOO.rollback();
			} catch (SQLException ex) {
				if (BambooUnitDataServer.isLog) {
					log.error("", ex);
				}
			}

			if (BambooUnitDataServer.isLog) {
				log.error("mobile unit - " + bamboodata.mobileUnitID + " : " + E.getMessage());
			}
			return false;
		}
		return true;

		// displayListenner.setDisplay("Bamboo process finished: " +
		// bamboodata.mobileUnitID);
	}

	private PreparedStatement setCanbusGPSDataIntoPSTParameters(Connection connBAMBOO, PreparedStatement pstmt) {
		boolean llProcFlag = false;
		String str = "";
		String strOutput1 = "";
		String strOutput2 = "";
		String strOutput3 = "";
		String strOutput4 = "";
		String strOutput5 = "";
		String strOutput6 = "";
		String strOutput7 = "";
		String strOutput8 = "";
		String strOutput9 = "";
		String strOutput10 = "";
		String strOutput11 = "";
		String strOutput12 = "";
		String strOutput13 = "";
		String strOutput14 = "";
		String strOutput15 = "";
		String strOutput16 = "";
		String strOutput17 = "";
		String strOutput18 = "";
		String strOutput19 = "";
		String strOutput20 = "";
		String strOutput21 = "";
		String strOutput22 = "";
		String strOutput23 = "";
		String strOutput24 = "";
		String strOutput25 = "";
		String strOutput26 = "";
		String strOutput27 = "";
		String strOutput28 = "";
		String strOutput29 = "";
		String strOutput30 = "";
		String strOutput31 = "";
		String strOutput32 = "";
		String strOutput33 = "";
		String strOutput34 = "";
		String strOutput35 = "";
		try {
			connBAMBOO.setAutoCommit(false);
			pstmt = connBAMBOO.prepareStatement(insertBambooCanbusGPSData);
			// mobileunitid smallint, -- Unit ID
			pstmt.setString(1, bamboodata.mobileUnitID);
			// gpsdate timestamp with time zone, -- GPS date
			pstmt.setTimestamp(2, gpsDate);
			// gprsdate timestamp with time zone, -- GPRS receiving time at the
			// server.
			pstmt.setTimestamp(3, gprsDate);
			// gpsstatus boolean DEFAULT true, -- GPS Status...
			ArrayList<CanBusParamModel> mCanbusModel = bamboodata.mCanbusModel;
			for (int i = 0; i < lModuleParam.size(); i++) {
				Module2ParamClass lParam = lModuleParam.get(i);

				if (lParam.getModule_Variable_OutpuStr().equals("output_1")) {
					strOutput1 = lParam.getModule_Variable_Value();
				}
				if (lParam.getModule_Variable_OutpuStr().equals("output_2")) {
					strOutput2 = lParam.getModule_Variable_Value();
				}
				if (lParam.getModule_Variable_OutpuStr().equals("output_3")) {
					strOutput3 = lParam.getModule_Variable_Value();
				}
				if (lParam.getModule_Variable_OutpuStr().equals("output_4")) {
					strOutput4 = lParam.getModule_Variable_Value();
				}
				if (lParam.getModule_Variable_OutpuStr().equals("output_5")) {
					strOutput5 = lParam.getModule_Variable_Value();
				}
				if (lParam.getModule_Variable_OutpuStr().equals("output_6")) {
					strOutput6 = lParam.getModule_Variable_Value();
				}
				if (lParam.getModule_Variable_OutpuStr().equals("output_7")) {
					strOutput7 = lParam.getModule_Variable_Value();
				}
				if (lParam.getModule_Variable_OutpuStr().equals("output_8")) {
					strOutput8 = lParam.getModule_Variable_Value();
				}
				if (lParam.getModule_Variable_OutpuStr().equals("output_9")) {
					strOutput9 = lParam.getModule_Variable_Value();
				}
				if (lParam.getModule_Variable_OutpuStr().equals("output_10")) {
					strOutput10 = lParam.getModule_Variable_Value();
				}
				if (lParam.getModule_Variable_OutpuStr().equals("output_11")) {
					strOutput11 = lParam.getModule_Variable_Value();
				}
				if (lParam.getModule_Variable_OutpuStr().equals("output_12")) {
					strOutput12 = lParam.getModule_Variable_Value();
				}
				if (lParam.getModule_Variable_OutpuStr().equals("output_13")) {
					strOutput13 = lParam.getModule_Variable_Value();
				}
				if (lParam.getModule_Variable_OutpuStr().equals("output_14")) {
					strOutput14 = lParam.getModule_Variable_Value();
				}
				if (lParam.getModule_Variable_OutpuStr().equals("output_15")) {
					strOutput15 = lParam.getModule_Variable_Value();
				}
				if (lParam.getModule_Variable_OutpuStr().equals("output_16")) {
					strOutput16 = lParam.getModule_Variable_Value();
				}
				if (lParam.getModule_Variable_OutpuStr().equals("output_17")) {
					strOutput17 = lParam.getModule_Variable_Value();
				}
				if (lParam.getModule_Variable_OutpuStr().equals("output_18")) {
					strOutput18 = lParam.getModule_Variable_Value();
				}
				if (lParam.getModule_Variable_OutpuStr().equals("output_19")) {
					strOutput19 = lParam.getModule_Variable_Value();
				}
				if (lParam.getModule_Variable_OutpuStr().equals("output_20")) {
					strOutput20 = lParam.getModule_Variable_Value();
				}
				if (lParam.getModule_Variable_OutpuStr().equals("output_21")) {
					strOutput21 = lParam.getModule_Variable_Value();
				}
				if (lParam.getModule_Variable_OutpuStr().equals("output_22")) {
					strOutput22 = lParam.getModule_Variable_Value();
				}
				if (lParam.getModule_Variable_OutpuStr().equals("output_23")) {
					strOutput23 = lParam.getModule_Variable_Value();
				}
				if (lParam.getModule_Variable_OutpuStr().equals("output_24")) {
					strOutput24 = lParam.getModule_Variable_Value();
				}
				if (lParam.getModule_Variable_OutpuStr().equals("output_25")) {
					strOutput25 = lParam.getModule_Variable_Value();
				}
				if (lParam.getModule_Variable_OutpuStr().equals("output_26")) {
					strOutput26 = lParam.getModule_Variable_Value();
				}
				if (lParam.getModule_Variable_OutpuStr().equals("output_27")) {
					strOutput27 = lParam.getModule_Variable_Value();
				}
				if (lParam.getModule_Variable_OutpuStr().equals("output_28")) {
					strOutput28 = lParam.getModule_Variable_Value();
				}
				if (lParam.getModule_Variable_OutpuStr().equals("output_29")) {
					strOutput29 = lParam.getModule_Variable_Value();
				}
				if (lParam.getModule_Variable_OutpuStr().equals("output_30")) {
					strOutput30 = lParam.getModule_Variable_Value();
				}
				if (lParam.getModule_Variable_OutpuStr().equals("output_31")) {
					strOutput31 = lParam.getModule_Variable_Value();
				}
				if (lParam.getModule_Variable_OutpuStr().equals("output_32")) {
					strOutput32 = lParam.getModule_Variable_Value();
				}
				if (lParam.getModule_Variable_OutpuStr().equals("output_33")) {
					strOutput33 = lParam.getModule_Variable_Value();
				}
				if (lParam.getModule_Variable_OutpuStr().equals("output_34")) {
					strOutput34 = lParam.getModule_Variable_Value();
				}
				if (lParam.getModule_Variable_OutpuStr().equals("output_35")) {
					strOutput35 = lParam.getModule_Variable_Value();
				}
//						 if(lParam.getModule_Variable_OutpuStr().equals("output_36"))
//						 {
//							 pstmt.setString(39, lParam.getModule_Variable_Value());
//						 }
//						 if(lParam.getModule_Variable_OutpuStr().equals("output_37"))
//						 {
//							 pstmt.setString(40, lParam.getModule_Variable_Value());
//						 }
//						 if(lParam.getModule_Variable_OutpuStr().equals("output_38"))
//						 {
//							 pstmt.setString(41, lParam.getModule_Variable_Value());
//						 }
//						 if(lParam.getModule_Variable_OutpuStr().equals("output_39"))
//						 {
//							 pstmt.setString(42, lParam.getModule_Variable_Value());
//						 }
//						 if(lParam.getModule_Variable_OutpuStr().equals("output_40"))
//						 {
//							 pstmt.setString(43, lParam.getModule_Variable_Value());
//						 }
//						 if(lParam.getModule_Variable_OutpuStr().equals("output_41"))
//						 {
//							 pstmt.setString(44, lParam.getModule_Variable_Value());
//						 }
//						 if(lParam.getModule_Variable_OutpuStr().equals("output_42"))
//						 {
//							 pstmt.setString(45, lParam.getModule_Variable_Value());
//						 }
//						 if(lParam.getModule_Variable_OutpuStr().equals("output_43"))
//						 {
//							 pstmt.setString(46, lParam.getModule_Variable_Value());
//						 }
//						 if(lParam.getModule_Variable_OutpuStr().equals("output_44"))
//						 {
//							 pstmt.setString(47, lParam.getModule_Variable_Value());
//						 }
//						 if(lParam.getModule_Variable_OutpuStr().equals("output_45"))
//						 {
//							 pstmt.setString(48, lParam.getModule_Variable_Value());
//						 }
//						 if(lParam.getModule_Variable_OutpuStr().equals("output_46"))
//						 {
//							 pstmt.setString(49, lParam.getModule_Variable_Value());
//						 }
//						 if(lParam.getModule_Variable_OutpuStr().equals("output_47"))
//						 {
//							 pstmt.setString(50, lParam.getModule_Variable_Value());
//						 }
//						 if(lParam.getModule_Variable_OutpuStr().equals("output_48"))
//						 {
//							 pstmt.setString(51, lParam.getModule_Variable_Value());
//						 }
//						 if(lParam.getModule_Variable_OutpuStr().equals("output_49"))
//						 {
//							 pstmt.setString(52, lParam.getModule_Variable_Value());
//						 }
//						 if(lParam.getModule_Variable_OutpuStr().equals("output_50"))
//						 {
//							 pstmt.setString(53, lParam.getModule_Variable_Value());
//						 }
//						 if(lParam.getModule_Variable_OutpuStr().equals("output_51"))
//						 {
//							 pstmt.setString(54, lParam.getModule_Variable_Value());
//						 }
//						 if(lParam.getModule_Variable_OutpuStr().equals("output_52"))
//						 {
//							 pstmt.setString(55, lParam.getModule_Variable_Value());
//						 }
//						 if(lParam.getModule_Variable_OutpuStr().equals("output_53"))
//						 {
//							 pstmt.setString(56, lParam.getModule_Variable_Value());
//						 }
//						 if(lParam.getModule_Variable_OutpuStr().equals("output_54"))
//						 {
//							 pstmt.setString(57, lParam.getModule_Variable_Value());
//						 }
//						 if(lParam.getModule_Variable_OutpuStr().equals("output_55"))
//						 {
//							 pstmt.setString(58, lParam.getModule_Variable_Value());
//						 }
//						 if(lParam.getModule_Variable_OutpuStr().equals("output_56"))
//						 {
//							 pstmt.setString(59, lParam.getModule_Variable_Value());
//						 }
//						 if(lParam.getModule_Variable_OutpuStr().equals("output_57"))
//						 {
//							 pstmt.setString(60, lParam.getModule_Variable_Value());
//						 }
//						 if(lParam.getModule_Variable_OutpuStr().equals("output_58"))
//						 {
//							 pstmt.setString(61, lParam.getModule_Variable_Value());
//						 }
//						 if(lParam.getModule_Variable_OutpuStr().equals("output_59"))
//						 {
//							 pstmt.setString(62, lParam.getModule_Variable_Value());
//						 }
//						 if(lParam.getModule_Variable_OutpuStr().equals("output_60"))
//						 {
//							 pstmt.setString(63, lParam.getModule_Variable_Value());
//						 }
//						 if(lParam.getModule_Variable_OutpuStr().equals("output_61"))
//						 {
//							 pstmt.setString(64, lParam.getModule_Variable_Value());//							
//						 }
//						 if(lParam.getModule_Variable_OutpuStr().equals("output_62"))
//						 {
//							 pstmt.setString(65, lParam.getModule_Variable_Value());//							
//						 }
//						 if(lParam.getModule_Variable_OutpuStr().equals("output_63"))
//						 {
//							 pstmt.setString(66, lParam.getModule_Variable_Value());//							
//						 }					

			}

			pstmt.setString(4, strOutput1);
			pstmt.setString(5, strOutput2);
			pstmt.setString(6, strOutput3);
			pstmt.setString(7, strOutput4);
			pstmt.setString(8, strOutput5);
			pstmt.setString(9, strOutput6);
			pstmt.setString(10, strOutput7);
			pstmt.setString(11, strOutput8);
			pstmt.setString(12, strOutput9);
			pstmt.setString(13, strOutput10);
			pstmt.setString(14, strOutput11);
			pstmt.setString(15, strOutput12);
			pstmt.setString(16, strOutput13);
			pstmt.setString(17, strOutput14);
			pstmt.setString(18, strOutput15);
			pstmt.setString(19, strOutput16);
			pstmt.setString(20, strOutput17);
			pstmt.setString(21, strOutput18);
			pstmt.setString(22, strOutput19);
			pstmt.setString(23, strOutput20);
			pstmt.setString(24, strOutput21);
			pstmt.setString(25, strOutput22);
			pstmt.setString(26, strOutput23);
			pstmt.setString(27, strOutput24);
			pstmt.setString(28, strOutput25);
			pstmt.setString(29, strOutput26);
			pstmt.setString(30, strOutput27);
			pstmt.setString(31, strOutput28);
			pstmt.setString(32, strOutput29);
			pstmt.setString(33, strOutput30);
			pstmt.setString(34, strOutput31);
			pstmt.setString(35, strOutput32);
			pstmt.setString(36, strOutput33);
			pstmt.setString(37, strOutput34);
			pstmt.setString(38, strOutput35);

			pstmt.setString(39, str);
			pstmt.setString(40, str);
			pstmt.setString(41, str);
			pstmt.setString(42, str);
			pstmt.setString(43, str);
			pstmt.setString(44, str);
			pstmt.setString(45, str);
			pstmt.setString(46, str);
			pstmt.setString(47, str);
			pstmt.setString(48, str);
			pstmt.setString(49, str);
			pstmt.setString(50, str);
			pstmt.setString(51, str);
			pstmt.setString(52, str);
			pstmt.setString(53, str);
			pstmt.setString(54, str);
			pstmt.setString(55, str);
			pstmt.setString(56, str);
			pstmt.setString(57, str);
			pstmt.setString(58, str);
			pstmt.setString(59, str);
			pstmt.setString(60, str);
			pstmt.setString(61, str);
			pstmt.setString(62, str);
			pstmt.setString(63, str);
			pstmt.setString(64, str);
			pstmt.setString(65, str);
			pstmt.setString(66, str);
			pstmt.setString(67, bamboodata.reservedStr);
			pstmt.setBoolean(68, llProcFlag);

		} catch (SQLException e) {
			log.error("mobile unit - " + bamboodata.mobileUnitID
					+ "; Error when setting gpsdatas into prepareStatement: " + e.getMessage());
			e.printStackTrace();
		}
		return pstmt;

	}

	private PreparedStatement setGPSDataIntoPSTParameters(Connection connBAMBOO, PreparedStatement pstmt) {
		try {
			connBAMBOO.setAutoCommit(false);
			pstmt = connBAMBOO.prepareStatement(insertBambooGPSData);
			// mobileunitid smallint, -- Unit ID
			pstmt.setString(1, bamboodata.mobileUnitID);
			// gpsdate timestamp with time zone, -- GPS date
			pstmt.setTimestamp(2, gpsDate);
			// gprsdate timestamp with time zone, -- GPRS receiving time at the
			// server.
			pstmt.setTimestamp(3, gprsDate);
			// gpsstatus boolean DEFAULT true, -- GPS Status...
			pstmt.setBoolean(4, bamboodata.getGPSStatus());
			// latitude integer, -- Latitude..
			pstmt.setInt(5, bamboodata.latitude);
			// longitude integer, -- Longitude...
			pstmt.setInt(6, bamboodata.longitude);
			// speed smallint, -- Speed...
			pstmt.setInt(7, bamboodata.speed);
			// direction smallint, -- Direction...
			pstmt.setInt(8, bamboodata.direction);
			// acc boolean DEFAULT true, -- ACC status
			pstmt.setBoolean(9, bamboodata.getAccStatus());
			// in1 boolean, -- Input port1 status
			pstmt.setBoolean(10, bamboodata.in1);
			// in2 boolean, -- Input port2 status
			pstmt.setBoolean(11, bamboodata.in2);
			pstmt.setBoolean(12, bamboodata.in3);
			pstmt.setInt(17, bamboodata.adc1);
			// adc2 smallint, -- ADC2...
			pstmt.setInt(18, bamboodata.adc2);

			// in4 boolean, -- Input port4 status
			pstmt.setBoolean(13, bamboodata.in4);
			// out1 boolean, -- Out port1 status
			pstmt.setBoolean(14, bamboodata.out1);
			// out2 boolean, -- Out port2 status
			pstmt.setBoolean(15, bamboodata.out2);
			// eventcode smallint, -- Event Code...
			pstmt.setInt(16, bamboodata.eventCode);

			// batt smallint, -- BATTERY VOLT....
			pstmt.setInt(19, bamboodata.batt);
			// mileage int, -- Mileage....
			pstmt.setInt(20, bamboodata.mileage);
			// reserve character(2) -- Reserved
			pstmt.setString(21, bamboodata.reservedStr);

			BambooData gpsdata = (BambooData) mGpsdataHash.get(bamboodata.mobileUnitID);
			if (gpsdata == null) {
				PreparedStatement gpsStmt = null;
				gpsStmt = connBAMBOO
						.prepareStatement("SELECT * FROM gps_unit_last_location WHERE mobileunitid=? and gpsdate=? ;");
				gpsStmt.setString(1, bamboodata.mobileUnitID);
				gpsStmt.setTimestamp(2, gpsDate);
				ResultSet gpsSet = gpsStmt.executeQuery();
				if (gpsSet.next()) {
					gpsdata = new BambooData();
					gpsdata.adc1 = gpsSet.getInt("adc1");
					gpsdata.adc2 = gpsSet.getInt("adc2");
					gpsdata.in3 = gpsSet.getBoolean("in3");
					gpsdata.mileage = gpsSet.getInt("mileage");
				} else {
					gpsdata = bamboodata;
				}
				gpsSet.close();
				gpsStmt.close();
				mGpsdataHash.put(bamboodata.mobileUnitID, gpsdata);
			}
			if (bamboodata.messageType.equals("11")) {
				// in3 boolean, -- Input port3 status
				pstmt.setBoolean(12, gpsdata.in3);
				pstmt.setInt(19, gpsdata.batt);
				gpsdata.adc1 = bamboodata.adc1;
				gpsdata.adc2 = bamboodata.adc2;
				
				if(bamboodata.lModuleType.equals("02"))
				{
					pstmt.setInt(20, gpsdata.mileage);
				}
				mGpsdataHash.put(bamboodata.mobileUnitID, gpsdata);
			} else if (bamboodata.messageType.equals("0")) {
				// adc1 smallint, -- ADC1...
				pstmt.setInt(17, gpsdata.adc1);
				// adc2 smallint, -- ADC2...
				pstmt.setInt(18, gpsdata.adc2);
				gpsdata.in3 = bamboodata.in3;
				gpsdata.batt = bamboodata.batt;
				gpsdata.mileage = bamboodata.mileage;
				mGpsdataHash.put(bamboodata.mobileUnitID, gpsdata);
			}
		} catch (SQLException e) {
			log.error("mobile unit - " + bamboodata.mobileUnitID
					+ "; Error when setting gpsdatas into prepareStatement: " + e.getMessage());
			e.printStackTrace();
		}
		return pstmt;
	}

	private java.sql.Timestamp getSQLTimestamp(Calendar dateTime) {

		java.sql.Timestamp tStamp = new java.sql.Timestamp(dateTime.getTimeInMillis());

		return tStamp;

	}

	public void run() {
		boolean dataInserted = false;
		if (bamboodata.mobileUnitID.equals("") || bamboodata.mobileUnitID.equals("0")) {
		} else {
			// Valid data
			gpsDate = getSQLTimestamp(bamboodata.gpsDate);
			gprsDate = getSQLTimestamp(bamboodata.gprsDate);

			try {

				connBAMBOO = DatabasePoolManager.getConnection();
				dataInserted = storeBamboo(connBAMBOO);
				bamboodata = null;

			} catch (Exception ex1) {
				if (bamboodata != null && bamboodata.receivedMessageStr != null) {
					dataFilePoolManager.ProcessUnitData(new BambooFileWriter(bamboodata.receivedMessageStr));
				}
				if (BambooUnitDataServer.isLog) {
					log.error("", ex1);
				}
			}

		}

		try {
			if (connBAMBOO != null) {
				connBAMBOO.close();
			}
		} catch (SQLException ex) {
			if (BambooUnitDataServer.isLog) {
				log.error("", ex);
			}
		}
		try {
			if (dataInserted) {
				// HeartBeat.LogHearBeat();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String parseInsertQuery(String pstmtStr) {
		String insertQry = null;
		try {
			if (pstmtStr.contains("INSERT INTO") && pstmtStr.contains("VALUES (") && pstmtStr.endsWith(")")) {
				String pstmtStrArray[] = pstmtStr.split("VALUES \\(");
				String start = pstmtStrArray[0];
				String end = pstmtStrArray[1];
				String[] parameterValues = (end.substring(0, end.length() - 1)).split(",");
				if (parameterValues.length == 21) {
					int in = start.indexOf("INSERT INTO");
					insertQry = start.substring(in, start.length()) + "VALUES (";
					for (int i = 0; i < parameterValues.length; i++) {
						String parameterValue = parameterValues[i].trim();
						if (!parameterValue.equals("?")) {
							if ((i >= 0 && i <= 2)) {
								insertQry = insertQry + "'" + parameterValue + "',";
							} else if ((i >= 4 && i <= 7) || (i >= 15 && i <= 19)) {
								insertQry = insertQry + parameterValue + ",";
							} else if ((i == 3) || (i >= 8 && i <= 14)) {
								int booleanNum = Integer.parseInt(parameterValue);
								insertQry = insertQry + String.valueOf((booleanNum == 1) ? true : false) + ",";
							} else if (i == 20) {
								insertQry = insertQry + "'" + parameterValue + "')";
							}
						} else {
							return null;
						}
					}
				}
			}
			return insertQry;
		} catch (Exception e) {
			log.fatal("Error ParsingInsertQuery: " + e);
			return null;
		}
	}
}
