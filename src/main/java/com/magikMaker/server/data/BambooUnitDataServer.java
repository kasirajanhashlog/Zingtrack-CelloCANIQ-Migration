/*
 * BambooUnitDataServer.java
 *
 * Created on 24 ?????? 2547, 13:35 ?.
 */
package com.magikMaker.server.data;

/**
 *
 * @author  pockey
 */
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.magikMaker.bamboo.communication.BambooCommunicator;
import com.magikMaker.bamboo.communication.BambooReader;
import com.magikMaker.bamboo.communication.BambooReaderManager;
import com.magikMaker.bamboo.events.BambooEventProcessor;
import com.magikMaker.bamboo.model.BuzzerEventHashModel;
import com.magikMaker.server.ServerDisplayListenner;

public class BambooUnitDataServer implements Runnable {

	private ServerDisplayListenner displayListenner;
	private BambooEventProcessor bambooEventProcessor;
	private String version = "Claystone Technologies version 1.0";
	private Properties properties;
	private int dst;
	private int port;
	public static Date date;
	private Logger log = Logger.getLogger(BambooUnitDataServer.class);
	public static boolean isLog = false;
	Calendar cal = Calendar.getInstance();
	private BambooCommunicator bambooCommunicator;
	private BambooReaderManager bambooReaderManager;
	private Hashtable<String,BuzzerEventHashModel> mBuzzerEventHash;
	
	public BambooUnitDataServer(Properties _properties, ServerDisplayListenner _displayListenner, BambooCommunicator _bambooCommunicator, Hashtable<String, BuzzerEventHashModel> lBuzzerEventHash) {

		displayListenner = _displayListenner;
		properties = _properties;
		bambooCommunicator = _bambooCommunicator;
		date=cal.getTime();
		mBuzzerEventHash = lBuzzerEventHash;
		//        serverURL = "http://"+ servletHost + ":" + servletPort +"/" + servletURL + "/" + servletName;

		try {

			this.dst = Integer.parseInt(properties.getProperty("dst"));

			port = Integer.parseInt(properties.getProperty("serverPortData"));

			int log = 0;
			String temp = properties.getProperty("Logging");
			if(temp != null)
			{
				log = Integer.parseInt(temp);
				if(1 == log)
				{
					isLog = true;
				}
			}

			bambooReaderManager = BambooReaderManager.getDatabasePoolManager();

		} catch (Exception e) {
			//System.out.println("Exception " + e);
			if(isLog)
			{
				log.error(e);
			}

			System.exit(1);
		}

		Thread t = new Thread(this);
		t.start();

	}

	public void terminate() {

		//        try {
		//            listen.close();
		//        } catch (Exception e) {
		//        }
	}

	@Override
	/*public void run() {

        // Create the server socket channel
        ServerSocketChannel server = null;
        Selector selector = null;

        try {
            server = ServerSocketChannel.open();
            // nonblocking I/O
            server.configureBlocking(false);
            server.socket().setSoTimeout(1000 * 30);
// host-port port
            server.socket().bind(new InetSocketAddress(port));
// Create the selector
            selector = Selector.open();
// Recording server to selector (type OP_ACCEPT)
            server.register(selector, SelectionKey.OP_ACCEPT);

            if (!(displayListenner == null)) {
                displayListenner.setDisplay("Bamboo Server  : " + version + " Started on port: " + port);
            }

            if(isLog)
            {
            	log.info("Bamboo Server  : " + version + " Started on port: " + port);
            }


        } catch (IOException ex) {
            if(isLog)
            {
            	log.error(ex);
            }

        }


    	int nSel = 0;
        boolean run = true;
        while (run) {

            try {
            	try{
            	   selector.selectedKeys().clear();
                   nSel = selector.select();
            	}catch(Exception e){
                    if(isLog)
                    {
                    	log.error("Select Exception" ,e);
                    	continue;
                    }
            	}
                // Get keys
                Set keys = selector.selectedKeys();
                Iterator i = keys.iterator();

                // For each keys...
                while (i.hasNext()) {
                    SelectionKey key = (SelectionKey) i.next();

                    // Remove the current key
                    i.remove();

                    if(!key.isValid())
                    {
                    	continue;
                    }

                    // if isAccetable = true
                    // then a client required a connection
                    if (key.isAcceptable()) {
                    	SocketChannel client = null;
                        // get client socket channel
                    	try
                    	{
                    		client = server.accept();
                    	}
                    	catch(SocketTimeoutException se)
                    	{
                            if(isLog)
                            {
                            	log.error("SocketTimeoutException" ,se);
                            	continue;
                            }
                    	}

                        if(isLog)
                        {
                        	log.info("Received Connection from " + client.socket().getInetAddress().getHostAddress() + " : " + client.socket().getPort());
                        }
                        if (!(displayListenner == null)) {
                            displayListenner.setDisplay("Received Connection from " + client.socket().getInetAddress().getHostAddress() + " : " + client.socket().getPort());
                        }
                        // Set timeout
                        try {
                        	client.socket().setKeepAlive(true);
                            client.socket().setSoTimeout(1000 * 60); // 1 mins
                            client.socket().setTcpNoDelay(true);
                        } catch (SocketException ex) {
                            if(isLog)
                            {
                            	log.error("SoTimeoutException",ex);
                            }
                        	client.socket().close();
                        	client.close();
                        }

                        // Non Blocking I/O
                        client.configureBlocking(false);
                        // recording to the selector (reading)
                        client.register(selector, SelectionKey.OP_READ);
                    }

                    // if isReadable = true
                    // then the server is ready to read
                    if (key.isReadable())
                    {
                    	bambooReaderManager.ProcessUnitData(new BambooReader(displayListenner, key, bambooEventProcessor, bambooCommunicator, dst));
                    }
                }
            } catch (Exception ex) {
//                Logger.getLogger(BambooUnitDataServer.class.getName()).log(Level.SEVERE, null, ex);
//                run = false;

//                ex.printStackTrace();
                if(isLog)
                {
                	log.error(ex);
                }

                bambooCommunicator.cleanning();

            }

        }
    }
	 */
	public void run() {

		// Create the server socket channel
		ServerSocket server = null;
		Socket client = null;

		try {
			server = new ServerSocket();
			server.setReceiveBufferSize(8*1024);
			// host-port port
			server.bind(new InetSocketAddress(port));

			if (!(displayListenner == null)) {
				displayListenner.setDisplay("Bamboo Server  : " + version + " Started on port: " + port);
			}

			if(isLog)
			{
				log.info("Bamboo Server  : " + version + " Started on port: " + port);
			}


		} catch (IOException ex) {
			if(isLog)
			{
				log.error(ex);
			}

		}


		boolean run = true;
		while (run) {

			try
			{
				try
				{
					client = server.accept();
					client.setReceiveBufferSize(8*1024);
				}
				catch(SocketTimeoutException se)
				{
					if(isLog)
					{
						log.error("SocketTimeoutException" ,se);
						continue;
					}
				}

				if(isLog)
				{
					log.info("Received Connection from " + client.getInetAddress().getHostAddress() + " : " + client.getPort() + " Rcv Size : " + client.getReceiveBufferSize());                

				}
				if (!(displayListenner == null)) {
					displayListenner.setDisplay("Received Connection from " + client.getInetAddress().getHostAddress() + " : " + client.getPort());
				}
				// Set timeout
				try {
					client.setKeepAlive(true);
					//client.setSoTimeout(1000 * 60); // 1 mins
					client.setSoTimeout(1000 * 60 * 3); // 3 mins
					client.setTcpNoDelay(true);
				} catch (SocketException ex) {
					if(isLog)
					{
						log.error("SoTimeoutException",ex);
					}
					client.close();
				}


				new Thread(new BambooReader(displayListenner, client, bambooEventProcessor, bambooCommunicator, dst, mBuzzerEventHash, properties)).start();
			} catch (Exception ex) {
				if(isLog)
				{
					log.error("",ex);
				}
			}

		}
	}

}
