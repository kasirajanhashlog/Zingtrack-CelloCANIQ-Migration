package com.magikMaker.FileManager;

import java.nio.ByteBuffer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.magikMaker.server.data.BambooUnitDataServer;

public class BambooFileWriter implements Runnable{

    private Logger log = LogManager.getLogger(BambooFileWriter.class);

	private ByteBuffer messageIn;

	public BambooFileWriter(ByteBuffer messageBuffer) {

		messageIn = messageBuffer;
	}

	public void run() {

		BambooDataFilePoolManager lBambooDataFilePoolManager = null;
		try {
			lBambooDataFilePoolManager = BambooDataFilePoolManager.getFileWriteManager();
			lBambooDataFilePoolManager.WriteData(messageIn);
			if (messageIn.get(0)==0x77&&messageIn.get(1)==0x67&&messageIn.get(2)== 0x71) {
				String unitid =  String.valueOf(messageIn.get(9))+String.valueOf(messageIn.get(8))+String.valueOf(messageIn.get(7))+String.valueOf(messageIn.get(6));
				log.error("Data for Unit " + unitid + " Written into File");
			}
		} catch (Exception e) {
			if(BambooUnitDataServer.isLog)
			{
				log.error("",e);
			}
		}
	}
}
