/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.magikMaker.bamboo.communication;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.magikMaker.bamboo.events.BambooEventProcessor;
import com.magikMaker.bamboo.model.BambooData;
import com.magikMaker.bamboo.model.BuzzerEventHashModel;
import com.magikMaker.server.ServerDisplayListenner;
import com.magikMaker.server.data.BambooUnitDataServer;
import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;

/**
 * 
 * @author pockey
 */
public class BambooReader implements Runnable {

	//private SocketChannel socketChannel;
	private BambooCommunicator bambooCommunicator;
	private int dst;
	private byte[] reply = "(Y)".getBytes();
	//private ServerDisplayListenner displayListenner;
	//private SelectionKey key;
	private Logger log = Logger.getLogger(BambooReader.class);
	private Socket clientSocket;
	// private BambooMessageLog bambooMessageLog;
	private Hashtable<String,BuzzerEventHashModel> lBuzzerEventHash;
	private int tempLimit = 10000;
	private int humidityLimit = 10000;
	private int waterLimit = 10000;
	
	public BambooReader(ServerDisplayListenner _displayListenner,
			Socket clientSocket, BambooEventProcessor _bambooEventProcessor, 
			BambooCommunicator _bambooCommunicator, int _dst, Hashtable<String,
			BuzzerEventHashModel> mBuzzerEventHash,	Properties _properties) {

		this.clientSocket = clientSocket;
		bambooCommunicator = _bambooCommunicator;
		//displayListenner = _displayListenner;
		dst = _dst;
		lBuzzerEventHash = mBuzzerEventHash;
		if(_properties!=null){
			String tempLimitStr = _properties.getProperty("TempLimit");
			if(tempLimitStr!=null){
				tempLimit = Integer.parseInt(tempLimitStr);
			}
			String humidityLimitStr = _properties.getProperty("HumidityLimit");
			if(humidityLimitStr!=null){
				humidityLimit = Integer.parseInt(humidityLimitStr);
			}
			String waterLimitStr = _properties.getProperty("WaterLimit");
			if(waterLimitStr!=null){
				waterLimit = Integer.parseInt(waterLimitStr);
			}
		}
	}

	@Override
	public void run() {

		ByteBuffer dataBuffer = null;
		try {
			dataBuffer = ByteBuffer.allocate(8*1024); 
			int bytesRead = -1;
			BufferedInputStream inputStream = new BufferedInputStream(clientSocket.getInputStream());
			BufferedOutputStream outputStream = new BufferedOutputStream(clientSocket.getOutputStream());
			ArrayList<ByteBuffer> messageList = new ArrayList<ByteBuffer>();
			String imei = "";
			while(true)
			{
				if(dataBuffer!=null)
				{
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					dataBuffer.clear();
					bytesRead = inputStream.read(dataBuffer.array());
					if(BambooUnitDataServer.isLog)
					{
						log.info("");
						log.info("************************************************************");
						log.error("Process Cell Locator Data - " + bytesRead);
						//log.info("Recvd data : " +HexBin.encode(dataBuffer.array()));
					}
					if(bytesRead <= 0)
					{
						throw new Exception("Invalid Read from Socket");
					}
					//dataBuffer.flip();
					if(dataBuffer.limit()<60){
						throw new Exception("Invalid Read from Socket");
					}

					dataBuffer.limit(bytesRead);
					dataBuffer.flip();
					ByteBuffer receivedDataBuffer = ByteBuffer.allocate(bytesRead);
					receivedDataBuffer.put(dataBuffer.array(), 0, bytesRead);
					// Start message processing
					ArrayList<ByteBuffer> tempBufferList = new ArrayList<ByteBuffer>(); 
					ByteBuffer tempBuffer = null;
					
					String recvdString = HexBin.encode(receivedDataBuffer.array());
					String[] messageStrings = recvdString.split("4D4347");
					
					for(int i=0; i < messageStrings.length; i++) 
					{
						String currentData =  messageStrings[i];							

						if(currentData == null || currentData.equals("") || currentData.equals(" ")
								|| !currentData.startsWith("500")){
							log.info("skipping index :" + i +" ; "+ currentData);
						}else{
							StringBuffer temp = new StringBuffer(); 
							temp.append("4D4347");
							temp.append(currentData);
							currentData = temp.toString();

							tempBuffer = ByteBuffer.allocate((currentData.length() / 2));
							byte [] tempBuff = HexBin.decode(currentData);
							log.info(" tempBuf size : " + tempBuff.length);
							tempBuffer.put(tempBuff);
							tempBufferList.add(tempBuffer);
						}
					}

					log.info(" Total Messages parsed for processing : " + tempBufferList.size());
					for(int i=0; i < tempBufferList.size(); i++)
					{
						if (tempBufferList.get(i).get(0) == 77 && tempBufferList.get(i).get(1) == 67
								&& tempBufferList.get(i).get(2) == 71) {
							// UnitId is 16 char long and starts at position 4 and ends
							// at 20
							String hexString = HexBin.encode(tempBufferList.get(i).array());

							imei = String.valueOf(BambooData.parsePositionHex(
									hexString, 10, 18));
							if (BambooUnitDataServer.isLog) {
								log.info(" Recvd Message from IMEI-Unit : " + imei + " - " + Thread.currentThread().getName());
								log.error("message: " + hexString);
							}

							//Checksum Validation
							try{
								// First 4 bytes (or 8 bytes in hexString) are message header
								// Last 1 byte (or 2 bytes in hexString) is CheckSum
								// Calculate checksum excluding header and checksum and validate against incoming checksum
								String checkSum = BambooData.getCheckSumForInMsg(hexString.substring(8,hexString.length()-2));
								String inComingCS = hexString.substring(hexString.length()-2,hexString.length());  
								if(checkSum.equalsIgnoreCase(inComingCS) == false){
									log.error("Checksum Failed: Incoming CS :" + inComingCS + " Calculated CS : " + checkSum);
									//continue;
								}else{

									//Accumulate all the messages in the list for DB processing
									String module2845 = hexString.substring(120, (hexString.length()-2)); // skipping module 28 with type 11
									String module2844 = null;
									if(hexString.length() > 156) {
										module2844 = hexString.substring(156, (hexString.length()-2));
									}
									
									if(hexString.startsWith("4D4347500B") && (module2845.substring(0, 2).equals("1C") == true 
											|| (module2844 != null && module2844.substring(0, 2).equals("1C") == true))){
										log.error("Process ACK but Avoid DB Processing for Type 11 with Module 28.");
									}else{
										messageList.add(tempBufferList.get(i));
									}


									//Accumulate the ACKs for all the messages in this packet
									reply = BambooData.getReply(tempBufferList.get(i));
									baos.write(reply);
									log.info("Checksum validated");
									if(outputStream != null)
									{
										// send ACK to unit
										try {
											if(baos.size() > 0)
											{
												outputStream.write(baos.toByteArray());
												//log.error(count + " ; ACK : " +  HexBin.encode(baos.toByteArray()));
												log.error("ACK : " +  HexBin.encode(baos.toByteArray()));
												log.info("Sending ACK to IMEI-Unit : "
														+ imei
														+ " - IP  : "
														+ clientSocket.getInetAddress()
														.getHostAddress());
												baos.reset();												 
												log.info("Flushing socket");
												outputStream.flush();
											}
										} catch (IOException ioe) {
											if (BambooUnitDataServer.isLog) {
												log.error("Sending ACK to IMEI-Unit : "
														+ imei
														+ " - IP  : "
														+ clientSocket.getInetAddress()
														.getHostAddress() + " failed");
											}
										}
									}

									try{

										boolean isValidData = false;
										Boolean tempCurboolean = null;
										Boolean humdCurboolean = null;
										Boolean watCurboolean = null;
										Boolean buzzerOnOff = null;

										if(hexString.startsWith("4D4347500B")){
											if(module2845.substring(0, 2).equals("2D") == true){
												int battStr = parsePositionHex(module2845, 28, 30);
												isValidData = true;
												double tempCur = 0;
												double humdCur = 0;
												//Temperature Reading(43 44 45 46 )
												int adcInt = parsePositionHex(module2845, 42, 46);
												tempCur = adcInt/10;
												if(tempCur > tempLimit)
													tempCurboolean = true;
												else
													tempCurboolean = false;

												//Humidity Reading(47 48 49 50)
												int Humidity = parsePositionHex(module2845, 46, 50);
												humdCur = Humidity/10;
												if(humdCur > humidityLimit)
													humdCurboolean = true;
												else
													humdCurboolean = false;
												log.error("Batt : " + battStr+ " ; Temperature : " + tempCur+" - "+tempCurboolean +" ; Humidity: " + humdCur+" - "+ humdCurboolean);
											}else if(module2844 != null && module2844.substring(0, 2).equals("2C") == true){
												int battStr = parsePositionHex(module2844, 18, 20);
												//isValidData = true;
												double tempCur = 0;
												double humdCur = 0;
												int adcInt = parsePositionHex(module2844, 42, 46);
												tempCur = adcInt/10;
												if(tempCur > tempLimit)
													tempCurboolean = true;
												else
													tempCurboolean = false;

												int Humidity = parsePositionHex(module2844, 46, 50);
												humdCur = Humidity/10;
												if(humdCur > humidityLimit)
													humdCurboolean = true;
												else
													humdCurboolean = false;
												log.error("Batt : " + battStr+ " ; Temperature : " + tempCur+" - "+tempCurboolean +" ; Humidity: " + humdCur+" - "+ humdCurboolean);
											}
											
										}else if(hexString.startsWith("4D43475000")){
											isValidData = true;
											watCurboolean = false;
											int volt = parsePositionHex(hexString, 54, 56);
											double voltDou = 0.0;
											if(volt > 0){
												voltDou = (volt * (0.1176470588235294)); // (volt *(30/255))
												if(voltDou > waterLimit){
													watCurboolean = true;
												}
											}
											log.error("Water sensor : " + voltDou + " - "+ watCurboolean);
										}

										if(isValidData == true){
											BuzzerEventHashModel buzzerHashMap = (BuzzerEventHashModel)lBuzzerEventHash.get(imei);
											if(buzzerHashMap == null){
												buzzerHashMap = new BuzzerEventHashModel();
												if(tempCurboolean != null){
													if(tempCurboolean == true || humdCurboolean == true){
														buzzerOnOff = true;
														buzzerHashMap.tempValue = tempCurboolean;
														buzzerHashMap.humudityValue = humdCurboolean;
														buzzerHashMap.buzzerValue = true;
														lBuzzerEventHash.put(imei,buzzerHashMap);
													}else if(tempCurboolean == false && humdCurboolean == false){
														buzzerOnOff = false;
														buzzerHashMap.tempValue = tempCurboolean;
														buzzerHashMap.humudityValue = humdCurboolean;
														buzzerHashMap.buzzerValue = false;
														lBuzzerEventHash.put(imei,buzzerHashMap);
													}
												}else if(watCurboolean != null){
													if(watCurboolean == true){
														buzzerOnOff = true;
														buzzerHashMap.waterValue = true;
														buzzerHashMap.buzzerValue = true;
														lBuzzerEventHash.put(imei,buzzerHashMap);
													}else{
														buzzerOnOff = false;
														buzzerHashMap.waterValue = false;
														buzzerHashMap.buzzerValue = false;
														lBuzzerEventHash.put(imei,buzzerHashMap);
													}
												}
											}else{
												if(tempCurboolean != null){
													log.error("Temp Buzzer : " + tempCurboolean + " - "+ humdCurboolean + " - " + buzzerHashMap.waterValue +" ; "+ buzzerHashMap.buzzerValue);
													
													if(tempCurboolean == true || humdCurboolean == true){
														buzzerHashMap.tempValue = tempCurboolean;
														buzzerHashMap.humudityValue = humdCurboolean;
														if(buzzerHashMap.buzzerValue == false){
															buzzerOnOff = true;	
															buzzerHashMap.buzzerValue = true;
														}	
														lBuzzerEventHash.put(imei,buzzerHashMap);
													}else if(tempCurboolean == false && humdCurboolean == false){
														buzzerHashMap.tempValue = tempCurboolean;
														buzzerHashMap.humudityValue = humdCurboolean;
														if(buzzerHashMap.buzzerValue == true && (buzzerHashMap.waterValue == null || buzzerHashMap.waterValue == false)){
															buzzerOnOff = false;															
															buzzerHashMap.buzzerValue = false;
														}
														lBuzzerEventHash.put(imei,buzzerHashMap);
													}
												}else if(watCurboolean != null){
													log.error("Water Buzzer : " + watCurboolean + " - " + buzzerHashMap.tempValue + " - "+
															buzzerHashMap.humudityValue +" ; "+buzzerHashMap.buzzerValue);
													if(watCurboolean == true){
														buzzerHashMap.waterValue = true;
														if(buzzerHashMap.buzzerValue == false){
															buzzerOnOff = true;
															buzzerHashMap.buzzerValue = true;
														}
														lBuzzerEventHash.put(imei,buzzerHashMap);
													}else{
														buzzerHashMap.waterValue = false;
														if(buzzerHashMap.buzzerValue == true && ((buzzerHashMap.tempValue == null || buzzerHashMap.tempValue == false)
																&& ( buzzerHashMap.humudityValue == null || buzzerHashMap.humudityValue == false))){
															buzzerOnOff = false;
															buzzerHashMap.buzzerValue = false;
														}
														lBuzzerEventHash.put(imei,buzzerHashMap);
													}
												}
											}
											
											if(buzzerOnOff != null){
												String buzzerCommand = "4D43475300D9BA2000050000000003030505000000000000C8";
												if(buzzerOnOff == true){
													buzzerCommand = "4D43475300D9BA2000040000000003031515000000000000E7";
												}
												
												baos.write(HexBin.decode(buzzerCommand));
												if(outputStream != null)
												{
													// send ACK to unit
													try {
														if(baos.size() > 0)
														{
															outputStream.write(baos.toByteArray());
															log.error("Buzzer ACK : " +  HexBin.encode(baos.toByteArray())+ " ; Temp : " + buzzerHashMap.tempValue
																	+ " ; Humidity : " + buzzerHashMap.humudityValue + " ; Water : " + buzzerHashMap.waterValue + " ; Buzzer "+ buzzerOnOff);
															log.info("Sending ACK to IMEI-Unit : "
																	+ imei
																	+ " - IP  : "
																	+ clientSocket.getInetAddress()
																	.getHostAddress());
															baos.reset();												 
															log.info("Flushing socket");
															outputStream.flush();
														}
													} catch (IOException ioe) {
														if (BambooUnitDataServer.isLog) {
															log.error("Sending ACK to IMEI-Unit : "
																	+ imei
																	+ " - IP  : "
																	+ clientSocket.getInetAddress()
																	.getHostAddress() + " failed");
														}
													}
												}
											}
										}	
									}catch (Exception e) {
										// TODO: handle exception
									}
								}
							}catch(Exception e){
								log.error(e);
							}

						}
					}
					tempBufferList.clear();
					
					//Process all the messages
					for(int j=0; j < messageList.size(); j++)
					{
						bambooCommunicator.dataReceive(new BambooDataReceive(
								null, messageList.get(j), dst));
					}
					//Clear the list of messages
					messageList.clear();
					//End Multi message processing
				}
			}
		}
		catch (Exception e) {

			if(BambooUnitDataServer.isLog)
			{
				log.error("Exception in Socket Read",e);
			}

			dataBuffer.clear();
			dataBuffer = null;
			reply = null;


			try {
				if(clientSocket != null)
				{
					clientSocket.close();
				}
			} catch (IOException ex) {
				if(BambooUnitDataServer.isLog)
				{
					log.error("",ex);
				}
			}
		}
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
}