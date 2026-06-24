package com.magikMaker.bamboo.model;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.magikMaker.bamboo.communication.CanBusParamModel;
import com.magikMaker.bamboo.database.DatabasePoolManager;
//import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;
import java.util.HexFormat;
public class BambooData implements Serializable {
	/*
	 * No No Parameter Type Size Pos. Example Extra Head 1 STX Char 1 0 �[� 2
	 * Protocol Type Char 3 1 �MPQ� 3 Unit ID Char 16 4 �??01012345678901�
	 * Body(Data) 4 Date Char 6 20 �YYMMDD� 5 Time Char 6 26 �HHMMSS� 6 GPS
	 * Status Char 1 32 �A� or �V� �A�:Fix / �V�:Invalid 7 Latitude Char 10 33
	 * �+23.123456� �+�:Ease / �-�:West 8 Longitude Char 11 43 �+123.123456�
	 * �+�:North / �-�:South 9 Accumul. Char 7 54 �0000000� m 10 Speed Char 3 61
	 * �000� ~ 255� Km 11 Direction Char 3 64 �000� ~ 359� Degree 12 Start-up
	 * Char 1 67 �0� : Off / �1� : On 13 Event Code Char 2 68 Refer to 3.5.5 14
	 * Event Data Char 3 70 Zone ID 15 Input 1 ~ 4 Char 2 73 16 Input 2 ~ 8 Char
	 * 2 75 17 Output 1 ~ 4 Char 2 77 18 Output 2 ~ 8 Char 2 79 19 ADC 1 Char 3
	 * 81 �000� 20 Battery Status Char 3 84 �000� 21 Reservation Char 5 87
	 * �00000� 22 Ext. Flag Char 1 92 �N� or �A� 23 Ext. Length Char 2 93 �00� ~
	 * �50� 24 Ext. Data Char N 95 Tail 25 ETX Char 1 95+N �]�
	 */

	// HexFormat instance for Java 25 hex conversions
	private static final HexFormat hexFormat = HexFormat.of().withUpperCase();
	
	private static final long serialVersionUID = 2928231690465539380L;
	public String mobileUnitID = "";
	public Calendar gpsDate;
	public Calendar gprsDate;
	public char gpsStatus = 'N';
	public int latitude;
	public int longitude;
	public int speed;
	public int direction;
	public int adc1;
	public int adc2;
	public int batt;
	public int serverPort;
	public int eventCode;
	public boolean acc;
	public boolean in1;
	public boolean in2;
	public boolean in3;
	public boolean in4;
	public boolean out1;
	public boolean out2;
	public int mileage;
	public String reservedStr = "NO";
	public String serverHost = "unknown";
	public int dst;
	public boolean online = true;
	private Logger log = LogManager.getLogger(BambooData.class);
	private Calendar rDate = Calendar.getInstance();
	public ArrayList<Module2ParamClass> lModuleParam;	
	

	public ByteBuffer receivedMessageStr;
	private String imeiCode;
	public String messageType;
	public String lModuleType;
	
	public static ArrayList<CanBusParamModel> mCanbusModel ;
	
	
	
	public BambooData(int _dst) {
		this.dst = _dst;
	}

	private BambooData(ByteBuffer _messageIn, int _dst, String _serverHost,
			int _serverPort) {
		this.dst = _dst;
		this.serverHost = _serverHost;
		this.serverPort = _serverPort;
		this.receivedMessageStr = _messageIn;

		this.gpsDate = Calendar.getInstance();
		this.gpsDate.set(8888, 8, 8, 8, 8, 8);
		this.gprsDate = Calendar.getInstance();

//		String hexString = HexBin.encode(_messageIn.array());
		String hexString = hexFormat.formatHex(_messageIn.array());
		if(hexString.startsWith("4D4347500B")){
			String module_2 = hexString.substring(136, 138);
			if(module_2.equalsIgnoreCase("02"))
			{
				translateMod2(_messageIn);
			}else {
				translateType11(_messageIn);	
			}
			
		}else{
			translate(_messageIn);
		}
	}

	public static BambooData getInstance(ByteBuffer _messageIn, int _dst,
			String _serverHost, int _serverPort, ArrayList<CanBusParamModel> lCanbusModel) {
		mCanbusModel = new ArrayList<CanBusParamModel>();
		mCanbusModel = lCanbusModel;
		return new BambooData(_messageIn, _dst, _serverHost, _serverPort);
	}

	/*
	 * public static BambooData getInstance(String bambooDataStr) { String[]
	 * bambooDataStrs = bambooDataStr.split(",");
	 * 
	 * boolean connect = true; if (bambooDataStrs[4].equals("false")) { connect
	 * = false; } return new BambooData(bambooDataStrs[0], 0, bambooDataStrs[1],
	 * Integer.parseInt(bambooDataStrs[2]), connect,
	 * Long.parseLong(bambooDataStrs[4])); }
	 */

	public BambooData(String _mobileUnitID, Connection connBamboo) {
		this.mobileUnitID = _mobileUnitID;
		try {
			PreparedStatement pstmt = connBamboo
					.prepareStatement("SELECT * FROM gpsdata WHERE mobileunitid=? ORDER BY gpsdate DESC LIMIT 1;");

			pstmt.setString(1, this.mobileUnitID);

			ResultSet cts = pstmt.executeQuery();

			cts.next();

			this.gpsDate = Calendar.getInstance();
			this.gpsDate.setTime(cts.getTimestamp("gpsdate"));

			this.gprsDate = Calendar.getInstance();
			this.gprsDate.setTime(cts.getTimestamp("gprsdate"));

			if (cts.getBoolean("gpsstatus")) {
				this.gpsStatus = 'A';
			}

			this.latitude = cts.getInt("latitude");

			this.longitude = cts.getInt("longitude");

			this.speed = cts.getInt("speed");

			this.direction = cts.getInt("direction");

			this.acc = cts.getBoolean("acc");

			this.in1 = cts.getBoolean("in1");

			this.in2 = cts.getBoolean("in2");

			this.in3 = cts.getBoolean("in3");

			this.in4 = cts.getBoolean("in4");

			this.out1 = cts.getBoolean("out1");

			this.out2 = cts.getBoolean("out2");

			this.eventCode = cts.getInt("eventcode");

			this.adc1 = cts.getInt("adc1");

			this.adc2 = cts.getInt("adc2");

			this.batt = cts.getInt("batt");

			this.mileage = cts.getInt("mileage");

			this.reservedStr = cts.getString("reserve");

			this.online = false;

			cts.close();
			pstmt.close();
		} catch (SQLException ex) {
			ex.printStackTrace();
			log.error(ex);
		}
	}

	public BambooData() {
		// TODO Auto-generated constructor stub
	}

	public static String reverse(String hexString) {
		char[] array = hexString.toCharArray();
		if (array == null) {
			return "";
		}
		int i = 0;
		int j = array.length - 1;
		char tmp;
		while (j > i) {
			tmp = array[j];
			array[j] = array[i];
			array[i] = tmp;
			j--;
			i++;
		}

		return new String(array);

	}

	public static String intelHexReverse(String hexString) {

		char[] array = hexString.toCharArray();
		if (array == null) {
			return "";
		}
		int i = 0;
		int j = array.length - 1;
		char tmp;

		while (j >= i) {
			tmp = array[i];
			array[i] = array[i + 1];
			array[i + 1] = tmp;
			i++;
			i++;
		}
		return new String(array);
	}

	public static int parsePositionHex(String message, int start, int end) {
		return (Integer.parseInt(
				intelHexReverse(reverse(message.substring(start, end))), 16));
	}

	public static byte[] getReply(ByteBuffer messageIn) {
		
//		String hexString = HexBin.encode(messageIn.array());
		String hexString = hexFormat.formatHex(messageIn.array());
		// MCGP
		String reply = "4D434750";
		if(hexString.startsWith("4D4347500B")){
			// Message type
			reply += "0B";
			// UNIT ID: unique ID for each unit
			String mobileId = hexString.substring(10, 18);
			reply += mobileId;
			// Command Numerator
			// reply += "5C";
			
			// Message Numerator of message received
			String messageNumberator = hexString.substring(22, 24);
			reply += messageNumberator;
			// Authentication Code Field
			reply += "00000000";
			
			// Packet Control Field
			String PacketControl = hexString.substring(24, 26);
			// Action Code
			reply += PacketControl;
			//Length (of the modules section - not including the checksum)
			//String moduleSecLength = hexString.substring(26, 30);
			reply += "0A00";
			// Unused Byte (Spare Bytes)
			String spareBytes = hexString.substring(30, 38);
			reply += spareBytes;
			
			//Module 9 
			reply += "090300000000";
		}else{			
			// Message type
			reply += "04";
			
			// UNIT ID: unique ID for each unit
			String mobileId = hexString.substring(10, 18);
			reply += mobileId;
			// Command Numerator
			reply += "14";
			// Authentication Code Field
			reply += "00000000";
			// Action Code
			reply += "00";
			// Message Numerator of message received
			String messageNumberator = hexString.substring(22, 24);
			reply += messageNumberator;
			// Unused Byte
			reply += "0000000000000000000000";
		}
		
		reply += getCheckSum(reply);
//		return HexBin.decode(reply);
		return HexFormat.of().parseHex(reply);
		
	}

	public static String getCheckSum(String hexString) {
		hexString = hexString.substring(8);
		Integer value = 0;
		for (int i = 0; i < hexString.length(); i += 2) {
			value += Integer.parseInt(hexString.substring(i, i + 2), 16);
		}

		String sumHex = Integer.toHexString(value).toUpperCase();

		if (sumHex.length() > 2) {			
			return sumHex.substring(sumHex.length() - 2);
		}else if(sumHex.length() == 1){
			return "0"+sumHex ;
		}else{
			return sumHex;
		}
	}


	public static String getCheckSumForInMsg(String hexString) {
		//hexString = hexString.substring(8);
		Integer value = 0;
		for (int i = 0; i < hexString.length(); i += 2) {
			value += Integer.parseInt(hexString.substring(i, i + 2), 16);
		}
		String sumHex = Integer.toHexString(value).toUpperCase();
		if (sumHex.length() > 2) {		// If more than 2 bytes send only last 2 bytes	
			return sumHex.substring(sumHex.length() - 2);
		}else if(sumHex.length() == 1){
			return "0"+sumHex ;
		}else{
			return sumHex;
		}
	}

	public static int safeLongToInt(long l) {

		String OrgValue = String.valueOf(l);
		if (OrgValue.length() < 9) {	// no. of decimal places in latitude and longitude are 8; so length should be above 8
			return 0;
		}		

		// no. of decimal places in latitude and longitude are 8
		// in which, only 6 digits are extracted for precision and skipping the remaining digits
		// that means, skipping the last 2 digits in latitude and longitude	
		int lVal = (int) (((double) l/100000000)*1000000);
		return lVal;
	}
	
	private boolean setGPSDateType11(int dst) throws Exception {
		boolean result = true;
//		String hexString = HexBin.encode(receivedMessageStr.array());
		String hexString = hexFormat.formatHex(receivedMessageStr.array());
		String dateTimeStr = hexString.substring(100, 120);
		if(dateTimeStr.substring(0, 2).equals("07") == true){
			String dateValid = dateTimeStr.substring(6, 8);
			if(dateValid.equals("00") == true){
				//result = false;
				log.info("Invalid GPS date detected: " + this.mobileUnitID);
			}//else{
			int year = parsePositionHex(dateTimeStr, 18, 20);
			int month = parsePositionHex(dateTimeStr, 16, 18);
			int day = parsePositionHex(dateTimeStr, 14, 16);
			int hour = parsePositionHex(dateTimeStr, 12, 14);
			int min = parsePositionHex(dateTimeStr, 10, 12);
			int sec = parsePositionHex(dateTimeStr, 8, 10);
			year += 2000;

			this.rDate.set(year, month - 1, day, hour, min, sec);
			this.rDate.add(Calendar.HOUR, 5);
			this.rDate.add(Calendar.MINUTE, 30);
			if (year < 2015) {
				result = false;

				log.info("Invalid GPS date detected: " + this.mobileUnitID);
			} else {
				this.gpsDate.set(year, month - 1, day, hour, min, sec);
				//Added this line to set millosecond to 0. otherwise system sets a random value and it becomes 
				//impossible to identify duplicate data (same gpsdate). Changed by Sivaraja on 02/02/2015.
				this.gpsDate.set(Calendar.MILLISECOND, 0); 
				this.gpsDate.add(Calendar.HOUR, 5);
				this.gpsDate.add(Calendar.MINUTE, 30);
				this.gpsDate.add(10, dst);
			}
			//}
		}else{
			log.info("Invalid GPS date detected: " + this.mobileUnitID);
			result = false;
		}
		return result;
	}
	
	public boolean translateType11(ByteBuffer messageIn) {
		boolean result = true;
		try {
//			String hexString = HexBin.encode(messageIn.array());
			String hexString = hexFormat.formatHex(messageIn.array());

			int messageType = Integer.parseInt(hexString.substring(8, 10), 16);
			if(messageType == 11){ 
				this.mobileUnitID = String.valueOf(parsePositionHex(hexString, 10, 18));
				this.imeiCode = mobileUnitID;
				result = setGPSDateType11(this.dst);

				this.gprsDate = Calendar.getInstance();

				this.messageType = "11";
				//module 6 Start
				String module6Str = hexString.substring(56, 100);

				int mode1 = Integer.parseInt(module6Str.substring(8, 10), 16);
				int mode2 = Integer.parseInt(module6Str.substring(10, 12), 16);
				int satellites = Integer.parseInt(module6Str.substring(12, 14), 16);
				if (((mode1 == 4) || (mode1 == 3)) && mode2 == 2/* && satellites > 05*/)
					this.gpsStatus = 'A';
				else {
					this.gpsStatus = 'N';
				}

				Long longitude = (long) (parsePositionHex(module6Str, 14, 22) * (180 / Math.PI));
				this.longitude = safeLongToInt(longitude);
				Long latitude = (long) (parsePositionHex(module6Str, 22, 30) * (180 / Math.PI));
				this.latitude = safeLongToInt(latitude);
				// added by vijay on 26-02-2015
				// lat, long are 0 and gpsstatus is true then set it to false
				if(this.latitude == 0 && this.longitude == 0){
					if(this.gpsStatus == 'A'){
						this.gpsStatus = 'N';
						log.error("lat & long are 0; so gpsstatus is set to false");
					}
				}
				// added by vijay - End

				String altitudeStr = module6Str.substring(30, 38);
				
				this.speed = (int) (parsePositionHex(module6Str, 38, 40) * 0.036);
				// speed in gpsdata is incorrect and sometimes it is above 130
				// so, set it as 0
				if(this.speed > 130){
					this.speed = 0;
					log.error("speed is above 130, so it is set 0. Unit: " + this.mobileUnitID);
				}
				
				this.direction = (int) (parsePositionHex(module6Str, 40, 44)
						* (180 / Math.PI) * 0.001);
				
				//module 6 End
				
				this.adc2 = 0;
				this.adc1 = 0;
				//module 45 start
				this.lModuleType = "45";
				String sen45 = hexString.substring(120, (hexString.length()-2));
				String sen44 = null;
				if(hexString.length() > 156) {
					sen44 = hexString.substring(156, (hexString.length()-2));
				}
				
				if(sen45.substring(0, 2).equals("2D") == true){
					int battStr = parsePositionHex(sen45, 28, 30);

					//Temperature Reading(43 44 45 46 )
					int adcInt = parsePositionHex(sen45, 42, 46);
					double adcD = 0.0;
					adcD = adcInt/10;
					this.adc2 = (int) (adcD * 100);

					//Humidity Reading(47 48 49 50)
					int Humidity = parsePositionHex(sen45, 46, 50);
					Humidity = Humidity/10;
					this.adc1 = Humidity;
					log.info("Batt : " + battStr+ " ; Temperature : " + this.adc2 +" ; Humidity: " + Humidity);
				}else if(sen44 != null && sen44.substring(0, 2).equals("2C") == true){
					int battStr = parsePositionHex(sen44, 18, 20);

					int adcInt = parsePositionHex(sen44, 42, 46);
					double adcD = 0.0;
					adcD = adcInt/10;
					this.adc2 = (int) (adcD * 100);

					int Humidity = parsePositionHex(sen44, 46, 50);
					Humidity = Humidity/10;
					this.adc1 = Humidity;
					log.info("Batt : " + battStr+ " ; Temperature : " + this.adc2 +" ; Humidity: " + Humidity);
				}else if(sen45.substring(0, 2).equals("1C") == true){
					
				}
				
				//module 45 end

				this.acc = false;//((Integer.parseInt(hexString.substring(40, 42), 16))>>5 & 0x1) == 1;
				
				this.in1 = false;
				
				this.in2 = false;//((Integer.parseInt(hexString.substring(40, 42), 16))>>1 & 0x1) == 1;
				// New door sensor works reversly, 
				//so that we have changed flag reversly for that unit
				boolean doorFlag = GetDoorFlag(imeiCode);
				if(doorFlag == true){
					if(in2 == false){
						this.in2 = true;
					}else if(in2 == true){
						this.in2 = false;
					}
					log.info("Door Sensor value: " + this.in2);
				}
				
				this.in3 = false;
				
				// Input 6 is set to pin 4
				this.in4 = false;
				
				this.out1 = false;
				
				this.out2 = false;
				
				this.eventCode = 0;//parsePositionHex(hexString, 36, 38);
				
				String bit = hexToBin(hexString.substring(24, 26));
				String hex = bitsToHexConversion(bit.substring(3, 8));
				//System.out.println("Hex String > "+ bit);
				//System.out.println("Hex String > "+ hex);

				double batvolt = 0.0;
				this.batt = 0;			

				log.info("imei code: " + this.imeiCode + "; adc1 value: " + this.adc1);
				log.info("imei code: " + this.imeiCode + "; adc2 value: " + this.adc2);

				this.mileage = 0;//parsePositionHex(hexString, 58, 64);	

				log.info(toString());
			}else{
				log.error("Received Message Type : "  +messageType);
			}
		} catch (Exception ex) {
			log.error("Exception Parsing  GPSData Error: ", ex);
			log.error("Parsing Error For GPSData: " + messageIn);
			ex.printStackTrace();
			result = false;
		}

		return result;
	}

	public boolean translate(ByteBuffer messageIn) {
		boolean result = true;
		try {
//			String hexString = HexBin.encode(messageIn.array());
			String hexString = hexFormat.formatHex(messageIn.array());

			int messageType = Integer.parseInt(hexString.substring(8, 10), 16);
			if(messageType == 0){ 
				this.mobileUnitID = String.valueOf(parsePositionHex(hexString, 10, 18));
				this.imeiCode = mobileUnitID;
				result = setGPSDate(this.dst);
				this.messageType = "0";
				this.lModuleType = "0";
				this.gprsDate = Calendar.getInstance();
				int mode1 = Integer.parseInt(hexString.substring(82, 84), 16);
				int mode2 = Integer.parseInt(hexString.substring(84, 86), 16);
				if (((mode1 == 4) || (mode1 == 3)) && mode2 == 2)
					this.gpsStatus = 'A';
				else {
					this.gpsStatus = 'N';
				}

				Long latitude = (long) (parsePositionHex(hexString, 88, 96) * (180 / Math.PI));
				this.longitude = safeLongToInt(latitude);
				Long longitude = (long) (parsePositionHex(hexString, 96, 104) * (180 / Math.PI));
				this.latitude = safeLongToInt(longitude);
				// added by vijay on 26-02-2015
				// lat, long are 0 and gpsstatus is true then set it to false
				if(this.latitude == 0 && this.longitude == 0){
					if(this.gpsStatus == 'A'){
						this.gpsStatus = 'N';
						log.error("lat & long are 0; so gpsstatus is set to false");
					}
				}
				// added by vijay - End

				this.speed = (int) (parsePositionHex(hexString, 112, 120) * 0.036);
				// speed in gpsdata is incorrect and sometimes it is above 130
				// so, set it as 0
				if(this.speed > 130){
					this.speed = 0;
					log.error("speed is above 130, so it is set 0. Unit: " + this.mobileUnitID);
				}

				this.direction = (int) (parsePositionHex(hexString, 120, 124)
						* (180 / Math.PI) * 0.001);

				this.acc = ((Integer.parseInt(hexString.substring(40, 42), 16))>>5 & 0x1) == 1;

				this.in1 = false;

				this.in2 = ((Integer.parseInt(hexString.substring(40, 42), 16))>>1 & 0x1) == 1;
				// New door sensor works reversly, 
				//so that we have changed flag reversly for that unit
				boolean doorFlag = GetDoorFlag(imeiCode);
				if(doorFlag == true){
					if(in2 == false){
						this.in2 = true;
					}else if(in2 == true){
						this.in2 = false;
					}
					log.info("Door Sensor value: " + this.in2);
				}

				this.in3 = false;

				// Input 6 is set to pin 4
				this.in4 = false;

				this.out1 = false;

				this.out2 = false;

				this.eventCode = parsePositionHex(hexString, 36, 38);
				this.eventCode = parseEventCode(eventCode);


				String bit = hexToBin(hexString.substring(24, 26));
				String hex = bitsToHexConversion(bit.substring(3, 8));
				//System.out.println("Hex String > "+ bit);
				//System.out.println("Hex String > "+ hex);

				double batvolt = 0.0;

				if(hex.equalsIgnoreCase("0C")|| hex.equalsIgnoreCase("12")){ //Analog 1
					//log.error("Portable device data received");	
					batvolt = (parsePositionHex(hexString, 50, 52) *0.01647058823);
					this.batt = getBatteryLevel(batvolt);
					//this.adc1 = parsePositionHex(hexString, 52, 54);

				}else{ //Analog 2
					//this.adc1 = parsePositionHex(hexString, 50, 52);
					this.batt = getBatteryLevel((parsePositionHex(hexString, 52, 54) * 0.01647058823));
				}

				//System.out.println(this.mobileUnitID + " batt : " + this.batt + " batvolt : " + batvolt); 

				/*int adcInt = parsePositionHex(hexString, 54, 56);
				double adcD = 0.0;
				if(adcInt > 50){
					adcD = adcInt * 0.0098;
				}else{
					adcD = adcInt;
				} */
				this.adc2 = 0;//(int) (adcD * 100);
				this.adc1 = 0;
				log.info("imei code: " + this.imeiCode + "; adc1 value: " + this.adc1);
				log.info("imei code: " + this.imeiCode + "; adc2 value: " + this.adc2);
				
				
				// Multi sensor water deduction
				
				int volt = parsePositionHex(hexString, 54, 56);
				double voltDou = 0.0;
				if(volt > 0){
					voltDou = (volt * (0.1176470588235294)); // (volt *(30/255))
					if(voltDou > 0){
						this.in3 = true;
						log.info("Water sensor : " + voltDou);
					}
				}
				

				this.mileage = parsePositionHex(hexString, 58, 64);	
				log.info(toString());				
	
			}else{
				log.error("Received Message Type : "  +messageType);
			}
		} catch (Exception ex) {
			log.error("Exception Parsing  GPSData Error: ", ex);
			log.error("Parsing Error For GPSData: " + messageIn);
			ex.printStackTrace();
			result = false;
		}

		return result;
	}
	
	public boolean translateMod2(ByteBuffer messageIn) {
		boolean result = true;
		try {
//			String hexString = HexBin.encode(messageIn.array());
			String hexString = hexFormat.formatHex(messageIn.array());

			int messageType = Integer.parseInt(hexString.substring(8, 10), 16);
			if(messageType == 11){ 
			/*	this.mobileUnitID = String.valueOf(parsePositionHex(hexString, 10, 18));
				this.imeiCode = mobileUnitID;
				result = setGPSDate(this.dst);
				this.messageType = "0";
				this.gprsDate = Calendar.getInstance();
				int mode1 = Integer.parseInt(hexString.substring(65, 66), 16);
				int mode2 = Integer.parseInt(hexString.substring(67, 68), 16);
				if (((mode1 == 4) || (mode1 == 3)) && mode2 == 2)
					this.gpsStatus = 'A';
				else {
					this.gpsStatus = 'N';
				}

				Long longitude = (long) (parsePositionHex(hexString, 70, 78) * (180 / Math.PI));
				this.longitude = safeLongToInt(longitude);
				Long latitude = (long) (parsePositionHex(hexString, 79, 87) * (180 / Math.PI));
				this.latitude = safeLongToInt(latitude);
				// added by vijay on 26-02-2015
				// lat, long are 0 and gpsstatus is true then set it to false
				if(this.latitude == 0 && this.longitude == 0){
					if(this.gpsStatus == 'A'){
						this.gpsStatus = 'N';
						log.error("lat & long are 0; so gpsstatus is set to false");
					}
				}
				// added by vijay - End

				this.speed = (int) (parsePositionHex(hexString, 95, 97) * 0.036);
				// speed in gpsdata is incorrect and sometimes it is above 130
				// so, set it as 0
				if(this.speed > 130){
					this.speed = 0;
					log.error("speed is above 130, so it is set 0. Unit: " + this.mobileUnitID);
				}

				this.direction = (int) (parsePositionHex(hexString, 97, 101)
						* (180 / Math.PI) * 0.001); */
				this.mobileUnitID = String.valueOf(parsePositionHex(hexString, 10, 18));
				this.imeiCode = mobileUnitID;
				result = setGPSDateType11(this.dst);

				this.gprsDate = Calendar.getInstance();

				this.messageType = "11";
				this.lModuleType = "02"; 
				//module 6 Start
				String module6Str = hexString.substring(56, 100);

				int mode1 = Integer.parseInt(module6Str.substring(8, 10), 16);
				int mode2 = Integer.parseInt(module6Str.substring(10, 12), 16);
				int satellites = Integer.parseInt(module6Str.substring(12, 14), 16);
				if (((mode1 == 4) || (mode1 == 3)) && mode2 == 2/* && satellites > 05*/)
					this.gpsStatus = 'A';
				else {
					this.gpsStatus = 'N';
				}

				Long longitude = (long) (parsePositionHex(module6Str, 14, 22) * (180 / Math.PI));
				this.longitude = safeLongToInt(longitude);
				Long latitude = (long) (parsePositionHex(module6Str, 22, 30) * (180 / Math.PI));
				this.latitude = safeLongToInt(latitude);
				// added by vijay on 26-02-2015
				// lat, long are 0 and gpsstatus is true then set it to false
				if(this.latitude == 0 && this.longitude == 0){
					if(this.gpsStatus == 'A'){
						this.gpsStatus = 'N';
						log.error("lat & long are 0; so gpsstatus is set to false");
					}
				}
				// added by vijay - End

				String altitudeStr = module6Str.substring(30, 38);
				
				this.speed = (int) (parsePositionHex(module6Str, 38, 40));
				// speed in gpsdata is incorrect and sometimes it is above 130
				// so, set it as 0
				if(this.speed > 130){
					this.speed = 0;
					log.error("speed is above 130, so it is set 0. Unit: " + this.mobileUnitID);
				}
				
				this.direction = (int) (parsePositionHex(module6Str, 40, 44)
						* (180 / Math.PI) * 0.001);
				
				//module 6 End				
				this.adc2 = 0;
				this.adc1 = 0;
				this.acc = true;	//2021-Dec-17 Changed to default true status..
				this.in1 = false;				
				this.in2 = false;		
				this.in3 = false;				
				this.in4 = false;				
				this.out1 = false;				
				this.out2 = false;				
				this.eventCode = 0;
				this.batt = 0;			
				this.mileage = 0;				
				
				this.lModuleParam = new ArrayList<Module2ParamClass>();
				String sen = hexString.substring(136, (hexString.length()-2));
				int llValLen = 20;
				if(sen.substring(0, 2).equals("02") == true){
					int strLen = Integer.parseInt(hexString.substring(154, 156), 16);
					for (int lMod2Val = 0; lMod2Val < strLen;lMod2Val++)
					{
						Module2ParamClass lModParam = new Module2ParamClass();
					//	String llOutModStr1 = intelHexReverse(reverse(hexString.substring(156, 160)));	
					//	int llOutStr1Len = Integer.parseInt(hexString.substring(160, 162));
					//	String llOutModStr1Var = intelHexReverse(reverse(hexString.substring(162, (162 + (llOutStr1Len * 2)))));	
					//	int llOut1Val = CalculateModule_2_Params(llOutModStr1,llOutStr1Len,llOutModStr1Var);
						String llOutModStr1 = intelHexReverse(reverse(sen.substring(llValLen, (llValLen + 4))));
						int lenVal = llValLen + 4;
						int llOutStr1Len = Integer.parseInt(sen.substring(lenVal, lenVal + 2));
						int llVariableLen  = lenVal + 2 ;
						String llOutModStr1Var = intelHexReverse(reverse(sen.substring(llVariableLen, (llVariableLen + (llOutStr1Len * 2)))));	
						CanBusParamModel llBusMod = null;
						double llOut1Val = 0 ;
						String llOutValNew = "";
						String llVariableDataStr = "";
						for (int l = 0; l< mCanbusModel.size(); l++)
						{
							llBusMod = (CanBusParamModel)mCanbusModel.get(l);
							if(llOutModStr1.equals(llBusMod.getCan_var_id()))
							{
								int decimal = Integer.parseInt(llOutModStr1Var, 16);
								llOut1Val = llBusMod.getCan_var_offset() + (decimal *((double)llBusMod.getCan_var_mulitpler()/(double)llBusMod.getCan_var_divider())) ;
								if(llBusMod.getCan_var_data_type().equals("integer"))
								llOutValNew = String.valueOf((int)llOut1Val);
								else if(llBusMod.getCan_var_data_type().equals("double"))
								{
								//	llOutValNew = String.valueOf(Math.round(llOut1Val));
									llOutValNew = String.valueOf(llOut1Val);
								}
									
								llVariableDataStr = llBusMod.getZt_var_id();
								break;
							}
						}
						
						lModParam.setModule_Variable_NameStr(llOutModStr1);
						lModParam.setModule_Variable_length(llOutStr1Len);
						lModParam.setModule_Variable_Value(llOutValNew);
						lModParam.setModule_Variable_OutpuStr(llVariableDataStr);
						this.lModuleParam.add(lModParam);	
						llValLen = (llVariableLen + (llOutStr1Len * 2));
					}
				}
				log.info(toString());
			}else{
				log.error("Received Message Type : "  +messageType);
			}

		} catch (Exception ex) {
			log.error("Exception Parsing  GPSData Error: ", ex);
			log.error("Parsing Error For GPSData: " + messageIn);
			ex.printStackTrace();
			result = false;
		}

		return result;
	}
	
	
	private int getBatteryLevel(double battery){
		if(battery>=4.20){
			return 100;
		}
		if(battery>=4.15){
			return 90; //90-95
		}
		if(battery>=4.10){
			return 85; //85-90
		}
		if(battery>=4.05){
			return 80; //80-85
		}
		if(battery>=4.00){
			return 72; //80-85
		}
		if(battery>=3.90){
			return 62;
		}
		if(battery>=3.80){
			return 42;
		}
		if(battery>=3.79){
			return 35;
		}
		if(battery>=3.70){
			return 25;
		}
		if(battery>=3.60){
			return 10;
		}
		return 0;
	}
	int parseEventCode(int event) {
		Integer[] ignoredEvent = { 252, 158, 159, 11, 32, 44, 36 };
		if (Arrays.asList(ignoredEvent).contains(event))
			return 0;
		if (event == 53) {
			event = 2;
		}
		if (event == 69) {
			event = 1;
		}
		return event;
	}

	private boolean setGPSDate(int dst) throws Exception {
		boolean result = true;
//		String hexString = HexBin.encode(receivedMessageStr.array());
		String hexString = hexFormat.formatHex(receivedMessageStr.array());
		int year = parsePositionHex(hexString, 134, 138);
		int month = receivedMessageStr.get(66);
		int day = receivedMessageStr.get(65);
		int hour = receivedMessageStr.get(64);
		int min = receivedMessageStr.get(63);
		int sec = receivedMessageStr.get(62);

		this.rDate.set(year, month - 1, day, hour, min, sec);
		this.rDate.add(Calendar.HOUR, 5);
		this.rDate.add(Calendar.MINUTE, 30);
		if (year < 2008) {
			result = false;

			log.info("Invalid GPS date detected: " + this.mobileUnitID);
		} else {
			this.gpsDate.set(year, month - 1, day, hour, min, sec);
			//Added this line to set millosecond to 0. otherwise system sets a random value and it becomes 
			//impossible to identify duplicate data (same gpsdate). Changed by Sivaraja on 02/02/2015.
			this.gpsDate.set(Calendar.MILLISECOND, 0); 
			this.gpsDate.add(Calendar.HOUR, 5);
			this.gpsDate.add(Calendar.MINUTE, 30);
			this.gpsDate.add(10, dst);
		}

		return result;
	}

	public static String toString(ByteBuffer messageIn, String _serverHost,
			int _serverPort) {
		return new BambooData(messageIn, 0, _serverHost, _serverPort)
				.toString();
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();

		sb.append(this.mobileUnitID);
		sb.append(", ");
		sb.append(this.gpsDate.getTime());
		sb.append(", GPS ");
		sb.append(this.gpsStatus);
		sb.append(", ");
		sb.append(this.latitude / 1000000.0D);
		sb.append(", ");
		sb.append(this.longitude / 1000000.0D);
		sb.append(", Speed ");
		sb.append(this.speed);
		sb.append(", Direction ");
		sb.append(this.direction);
		sb.append(", Acc ");
		if (this.acc)
			sb.append("On");
		else {
			sb.append("Off");
		}
		sb.append(", Emergency ");
		if (getEmergencyStatus())
			sb.append("Yes");
		else {
			sb.append("No");
		}
		sb.append(", I1 ");
		if (this.in1)
			sb.append("On");
		else {
			sb.append("Off");
		}
		sb.append(", I2 ");
		if (this.in2)
			sb.append("On");
		else {
			sb.append("Off");
		}
		sb.append(", I3 ");
		if (this.in3)
			sb.append("On");
		else {
			sb.append("Off");
		}
		sb.append(", I4 ");
		if (this.in4)
			sb.append("On");
		else {
			sb.append("Off");
		}
		sb.append(", O1 ");
		if (this.out1)
			sb.append("On");
		else {
			sb.append("Off");
		}
		sb.append(", O2 ");
		if (this.out2)
			sb.append("On");
		else {
			sb.append("Off");
		}
		sb.append(", A1 ");
		sb.append(this.adc1 / 10.0D);
		sb.append(", A2 ");
		sb.append(this.adc2 / 10.0D);
		sb.append(", VIn ");
		sb.append(this.batt / 10.0D);
		sb.append(", EventCode=");
		sb.append(this.eventCode);
		sb.append(", Mileage=");
		sb.append(this.mileage);
		sb.append(", Reserved=");
		sb.append(this.reservedStr);
		sb.append(", Host=");
		sb.append(this.serverHost);
		sb.append(", port=");
		sb.append(this.serverPort);

		sb.append(".");

		return sb.toString();
	}

	public String getDataString() {
		return getMessageInStr() + "," + this.serverHost + ","
				+ this.serverPort + "," + this.online + ","
				+ this.gprsDate.getTimeInMillis();
	}

	public String getMessageInStr() {
		StringBuffer sb = new StringBuffer();

		sb.append('[');

		for (int i = 0; i < 8 - this.mobileUnitID.length(); ++i) {
			sb.append(" ");
		}
		sb.append(this.mobileUnitID);
		sb.append(getDataDateTimeString(this.gpsDate));
		sb.append(this.gpsStatus);

		if (this.latitude != 0) {
			if (this.latitude < 10000000) {
				sb.append(0);
			}
			if (this.latitude < 1000000) {
				sb.append(0);
			}
			if (this.latitude < 100000) {
				sb.append(0);
			}
			if (this.latitude < 10000) {
				sb.append(0);
			}
			if (this.latitude < 1000) {
				sb.append(0);
			}
			if (this.latitude < 100) {
				sb.append(0);
			}
			if (this.latitude < 10) {
				sb.append(0);
			}
			sb.append(this.latitude / 10);
		} else {
			sb.append("0000000");
		}

		if (this.longitude != 0) {
			if (this.longitude < 100000000) {
				sb.append(0);
			}
			if (this.longitude < 10000000) {
				sb.append(0);
			}
			if (this.longitude < 1000000) {
				sb.append(0);
			}
			if (this.longitude < 100000) {
				sb.append(0);
			}
			if (this.longitude < 10000) {
				sb.append(0);
			}
			if (this.longitude < 1000) {
				sb.append(0);
			}
			if (this.longitude < 100) {
				sb.append(0);
			}
			if (this.longitude < 10) {
				sb.append(0);
			}
			sb.append(this.longitude / 10);
		} else {
			sb.append("00000000");
		}

		if (this.speed < 100) {
			sb.append(0);
		}
		if (this.speed < 10) {
			sb.append(0);
		}
		sb.append(this.speed);

		if (this.direction < 100) {
			sb.append(0);
		}
		if (this.direction < 10) {
			sb.append(0);
		}
		sb.append(this.direction);

		addBoolean(sb, this.acc);
		addBoolean(sb, this.in1);
		addBoolean(sb, this.in2);
		addBoolean(sb, this.in3);
		addBoolean(sb, this.in4);

		addBoolean(sb, this.out1);
		addBoolean(sb, this.out2);

		if (this.eventCode < 10) {
			sb.append(0);
		}
		sb.append(this.eventCode);

		if (this.adc1 < 100) {
			sb.append(0);
		}
		if (this.adc1 < 10) {
			sb.append(0);
		}
		sb.append(this.adc1);

		if (this.adc2 < 100) {
			sb.append(0);
		}
		if (this.adc2 < 10) {
			sb.append(0);
		}
		sb.append(this.adc2);

		if (this.batt < 100) {
			sb.append(0);
		}
		if (this.batt < 10) {
			sb.append(0);
		}
		sb.append(this.batt);
		sb.append(this.mileage);
		sb.append(this.reservedStr);

		sb.append(']');

		return sb.toString();
	}

	private void addBoolean(StringBuffer sb, boolean b) {
		if (b)
			sb.append(1);
		else
			sb.append(0);
	}

	public String getDataDateTimeString(Calendar dateTime) {
		StringBuffer sb = new StringBuffer();

		int i = dateTime.get(1) % 100;

		if (i < 100) {
			sb.append('0');
		}
		sb.append(i);

		i = dateTime.get(2) + 1;

		if (i < 10) {
			sb.append('0');
		}
		sb.append(i);

		i = dateTime.get(5);

		if (i < 10) {
			sb.append('0');
		}
		sb.append(i);

		i = dateTime.get(11);

		if (i < 10) {
			sb.append('0');
		}
		sb.append(i);

		i = dateTime.get(12);

		if (i < 10) {
			sb.append('0');
		}
		sb.append(i);

		i = dateTime.get(13);

		if (i < 10) {
			sb.append('0');
		}
		sb.append(i);

		return sb.toString();
	}

	public int getInputInt() {
		int i = 0;
		if (this.in1) {
			i += 8;
		}
		if (this.in2) {
			i += 4;
		}
		if (this.in3) {
			i += 2;
		}
		if (this.in4) {
			++i;
		}
		return i;
	}

	public int getOutputInt() {
		int i = 0;
		if (this.out1) {
			i += 2;
		}
		if (this.out2) {
			++i;
		}
		return i;
	}

	public boolean getAccStatus() {
		return this.acc;
	}

	public boolean getEmergencyStatus() {
		return (this.eventCode == 4);
	}

	public boolean getGPSStatus() {
		return (this.gpsStatus == 'A');
	}

	public static String getMobileUnitId(String messageIn) {
		String mid = messageIn.substring(1, 9);

		mid = mid.replaceAll(" ", "");

		return mid;
	}

	public double getDoubleDegree(int latlon) {
		return (latlon / 1000000.0D);
	}

	public String getDateTimeString(Calendar dateTime) {
		StringBuffer sb = new StringBuffer();

		int i = dateTime.get(5);

		if (i < 10) {
			sb.append('0');
		}
		sb.append(i);

		sb.append('/');

		i = dateTime.get(2);

		++i;

		if (i < 10) {
			sb.append('0');
		}
		sb.append(i);

		sb.append('/');

		i = dateTime.get(1);

		sb.append(i);

		sb.append(' ');

		i = dateTime.get(11);

		if (i < 10) {
			sb.append('0');
		}
		sb.append(i);

		sb.append(':');

		i = dateTime.get(12);

		if (i < 10) {
			sb.append('0');
		}
		sb.append(i);

		sb.append(':');

		i = dateTime.get(13);

		if (i < 10) {
			sb.append('0');
		}
		sb.append(i);

		return sb.toString();
	}

	public boolean restartServer(String server, int port) {
		if ((server.equals(this.serverHost)) && (port == this.serverPort)) {
			this.online = false;
		}

		return this.online;
	}

	private boolean GetDoorFlag(String imeiCode) {
		boolean doorFlag = false;    	
		String sqlStr = "SELECT door_flag from imeidata WHERE imei=? AND fromdate<?  order by fromdate desc LIMIT 1";
		Connection lConnection = null;
		PreparedStatement pstmt = null;
		ResultSet cts = null;
		try{
			lConnection = DatabasePoolManager.getConnection();
			lConnection.setAutoCommit(false);
			pstmt = lConnection.prepareStatement(sqlStr);
			Timestamp tStamp = new Timestamp(Calendar.getInstance().getTimeInMillis());
			pstmt.setString(1, imeiCode);
			pstmt.setTimestamp(2, tStamp);		
			cts = pstmt.executeQuery();		
			cts.next();
			if(cts.getRow() > 0){
				doorFlag = cts.getBoolean("door_flag");
				log.info("door flag : " + doorFlag);
			}

			lConnection.commit();
			//imeiList.put(imeiCode, lMobileUnitID);
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
		return doorFlag;
	}
	private static String hexToBin(String hex) throws NumberFormatException {
		StringBuilder binStrBuilder = new StringBuilder();
		int c = 1;
		for (int i = 0; i < hex.length() - 1; i += 2) {

			String output = hex.substring(i, (i + 2));

			int decimal = Integer.parseInt(output, 16);

			String binStr = Integer.toBinaryString(decimal);
			int len = binStr.length();
			StringBuilder sbf = new StringBuilder();
			if (len < 8) {

				for (int k = 0; k < (8 - len); k++) {
					sbf.append("0");
				}
				sbf.append(binStr);
			} else {
				sbf.append(binStr);
			}

			c++;
			binStrBuilder.append(sbf.toString());
		}

		return binStrBuilder.toString();
	}
	private static String bitsToHexConversion(String bitStream){ 
		int byteLength = 4;
		int bitStartPos = 0, bitPos = 0;
		String hexString = "";
		int sum = 0; 
		if(bitStream.length()%4 !=0){
			int tempCnt = 0;
			int tempBit = bitStream.length() % 4;           
			while(tempCnt < (byteLength - tempBit)){
				bitStream = "0" + bitStream;
				tempCnt++;
			}
		} 
		while(bitStartPos < bitStream.length()){
			while(bitPos < byteLength){
				sum = (int) (sum + Integer.parseInt("" + bitStream.charAt(bitStream.length()- bitStartPos -1)) * Math.pow(2, bitPos)) ;
				bitPos++;
				bitStartPos++;
			}
			if(sum < 10)
				hexString = Integer.toString(sum) + hexString;
			else 
				hexString = (char) (sum + 55) + hexString;

			bitPos = 0;
			sum = 0;
		}
		return hexString;
	}
}

