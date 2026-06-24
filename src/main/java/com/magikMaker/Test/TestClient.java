/*
 * TestClient.java
 *
 * Created on 25 ����Ҥ� 2547, 12:32 �.
 */

package com.magikMaker.Test;

/**
 *
 * @author  pockey
 */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.text.NumberFormat;
import java.util.Calendar;

import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;

public class TestClient implements Runnable {

	/** Creates a new instance of TestClient */

	private String hostName;
	private int portNum;
	private static int Count = -1;
	private long unitid;
	private static TestClientManager testClientManager;
	public TestClient(String _hostName, int _portNum, long id) {

		this.hostName = _hostName;
		this.portNum = _portNum;
		unitid = id;
		System.out.println(new java.util.Date() + " : " + Count);

	}


	public void connect() {

		Socket smtpSocket = null;
		DataInputStream in = null;
		DataOutputStream out = null;
		try {

			smtpSocket = new Socket(hostName, portNum);
			smtpSocket.setSendBufferSize(8*1024);
			in = new DataInputStream(smtpSocket
					.getInputStream());
			out = new DataOutputStream(smtpSocket
					.getOutputStream());

			String str = "";

			// System.out.println("Thread Name : " +
			// Thread.currentThread().getName());

			Calendar cal = Calendar.getInstance();
			int hour = cal.get(Calendar.HOUR_OF_DAY);
			int min = cal.get(Calendar.MINUTE);
			int sec = cal.get(Calendar.SECOND);
			NumberFormat minFormat = NumberFormat.getNumberInstance();
			minFormat.setMinimumIntegerDigits(2);
			/*				str = "[MPQ" + unitid + "110525" + minFormat.format(hour)
						+ minFormat.format(min) + minFormat.format(sec)
						+ "A558973203751765987460000000202451000000000050501301200000N]";*/
			//				 str = "[MPQ?????79161561230"+"110525" + minFormat.format(hour)
			//						+ minFormat.format(min) + minFormat.format(sec)+"A+55.781260+037.58338800000000150000022550400000000011100000N]";


			//str = "4D4347500040990F00080E45DE1F04009D002C0020800080F96EEFFF78CF25010000000000009B1A00040208200B580808B75A01880B00001E00000049000D1B0A0302DF076E";
		//	str = "4D434750001127180008153D6B2B04009D000B0103330000F881D4800000000000000000000000000000000000000000000000000000000000000000000027330B050CE1074E";
			str = "4D4347500BC1D7220008112A00A100000000000806000001261118000613000004020E9E6B5B0836A16201A401000000691207070001180903010C151905001E000100000269001E0079BFCCD60E34800484000000248004FA0000002A8004F4FA01000580040000000007800475000000188004E62A0000288004580000000B800400000000268004000000002780044D00000006800400000000038004C4120000048004140000002B8004D73E02002B";
		//	str = "4D4347500BD9BA2000081A46005300000000000806000001241A090006130000000000000000000000000000000000000000070700001C15010A0C152D2300010101481A84002E58057060B0016304FD7ED6006F02010000F6000C0000800000000044";
				
			StringBuffer strBuf = new StringBuffer();

			strBuf.append(str);

			out.write(HexBin.decode(strBuf.toString()));
			System.out.println(" Mobile Unit : " + unitid
					+ " -  Sending : " + str.getBytes().toString());
			String recvStr = null;

			byte[] bufferRead = new byte[8*1024];
			int bytes_read = -1;

			while ((bytes_read = in.read(bufferRead)) != -1) {

				int acklen = 0;
				for (int i = 0; i + acklen <  bytes_read; i++)
				{
					ByteBuffer tempBuffer = ByteBuffer.allocate(28);
					tempBuffer.put(bufferRead,acklen,28);
					acklen = acklen + 28;
					System.out.println("ACK : " + HexBin.encode(tempBuffer.array()));

				}

			} 



		} catch (Exception e) {
			try {
				in.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {
				out.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {
				smtpSocket.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} 

	}

	public static void main(String[] args) {
		testClientManager = TestClientManager.getTestClientManager();
		int t = TestClientManager.NO_OF_CLIENTS;
		long id ;
		TestClient[] testClient = new TestClient[t];
		String host;
		int port;
		host=TestClientManager.HOST;
		port=TestClientManager.PORT;
		try {
			id = Integer.parseInt(args[0]);
		} catch (Exception e) {
			id = TestClientManager.CLIENT_ID;
		}
		for (int i = 0; i < t; i++) {

			// testClient[i] = new TestClient("203.155.19.226", 6600);

			// testClient[i] = new TestClient("192.168.98.48", 6600);

			// testClient[i] = new TestClient("192.168.1.123", 6600);

			// testClient[i] = new TestClient("203.146.245.68", 6600);

			// testClient[i] = new TestClient("localhost", 6600);

			// testClient[i] = new TestClient("61.19.242.70", 6600);

			// testClient[i] = new TestClient("61.19.242.70", 4424);

			// testClient[i] = new TestClient("88.212.197.122", 8765);
			testClient[i] = new TestClient(host, port, id++);
			testClientManager = TestClientManager.getTestClientManager();
			testClientManager.ProcessUnitData(testClient[i]);
			// testClient[i] = new TestClient("88.212.204.250", 8888);

			// testClient[i] = new TestClient("61.19.242.71", 6600);

			// testClient[i] = new TestClient("202.57.140.146", 6600);

			// TestClient testClient = new TestClient("192.168.8.254", 6600);
			// TestClient testClient = new TestClient("wlinno.opengps.com",
			// 6600);

			// testClient[i] = new TestClient("localhost", 8382);

			//	testClient[i].connect();

			// System.out.println(new java.util.Date() + " : " + i);
			//
			// Thread th = new Thread();

			// try {
			// th.sleep(5000);
			// } catch (InterruptedException ie) { }

		}

	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

		Count++;

		/*
		Random ran = new Random();

		try {
			Thread.sleep(ran.nextInt(300000));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 */
		connect();
	}

}
