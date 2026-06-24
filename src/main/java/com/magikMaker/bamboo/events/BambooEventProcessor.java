/*
 * BambooEventProcessor.java
 *
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package com.magikMaker.bamboo.events;

import java.util.ArrayList;
import java.util.Properties;

import com.magikMaker.bamboo.communication.BambooCommunicator;
import com.magikMaker.bamboo.communication.BambooDataReceive;
import com.magikMaker.server.ServerDisplayListenner;

/**
 *
 * @author Administrator
 */
public class BambooEventProcessor implements Runnable {

//    private ServerDisplayListenner displayListenner;
//    private Properties properties;
//    private BambooDB bambooDB;
//    private BambooDB bambooDBLogData;
  //  private EagleDB eagleDB;
//    private Connection connBAMBOO,  connGPRS,  connGoogleData;
    private ArrayList<BambooDataReceive> bambooDataReceives;
    private BambooCommunicator bambooCommunicator;
    
    public BambooEventProcessor(BambooCommunicator _bambooCommunicator, ServerDisplayListenner _displayListenner, Properties _properties) {

//        displayListenner = _displayListenner;

//        this.properties = _properties;

        bambooCommunicator = _bambooCommunicator;

//        bambooDataReceives = new ArrayList<BambooDataReceive>();
       
//        this.connBAMBOO = _connBAMBOO;
//        this.connGPRS = _connGPRS;
//        this.connGoogleData = _connGoogleData;

        Thread t = new Thread(this);
        t.start();

    }

    public void receiveData(BambooDataReceive bambooDataReceive) {

//        bambooDataReceives.add(0, bambooDataReceive);

        bambooCommunicator.dataReceive(bambooDataReceive);
//        bambooDataReceives.remove(0);
//        this.notify();
        /*
        try {
        	int insert_count = BambooDB.insert_count;
        	Date date = BambooUnitDataServer.date;
			BufferedWriter out = new BufferedWriter(new FileWriter("testfile.txt"));
			String write;
		Calendar	cal= Calendar.getInstance();
			
			write=Integer.toString(insert_count);
		String	write1=write.concat("  Starting Time :"+date.toString());
		String	write2=write1.concat(" Ending Time : "+cal.getTime().toString());
			out.write(write2);
			out.close();
		} catch (IOException e) {
		
			e.printStackTrace();
		}
		*/

    }

    @Override
    public void run() {

        while (true) {

            synchronized (this) {

                // Implement datareceive process


                while (!bambooDataReceives.isEmpty()) {

//                    bambooDB = BambooDB.getInstance(displayListenner, properties);
//                    bambooDBLogData = BambooDB.getInstanceLogData(displayListenner, properties);
//                    eagleDB = EagleDB.getInstance(displayListenner, properties);

                    BambooDataReceive bambooDataReceive = (BambooDataReceive) bambooDataReceives.get(0);

                //    bambooCommunicator.dataReceive(bambooDataReceive, BambooDB.getInstance(displayListenner, properties), EagleDB.getInstance(displayListenner, properties), BambooDB.getInstanceLogData(displayListenner, properties));
                    bambooCommunicator.dataReceive(bambooDataReceive);
                    bambooDataReceives.remove(0);

                }

                try {
                    wait();
                } catch (InterruptedException e) {
                }

            }

        }
    }
}
