/*
 * CommandProcessor.java
 *
 * Created on 24 ����Ҥ� 2547, 13:35 �.
 */

package com.magikMaker.server.command;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.magikMaker.bamboo.communication.BambooCommunicator;
import com.magikMaker.server.data.BambooUnitDataServer;


public class CommandProcessor implements Runnable {
    
	private BambooCommunicator communicator;
    private Logger log = Logger.getLogger(CommandProcessor.class);    
    private Properties serverProperties;
    private String unitId;
    private String  commandText;
    private ServerSocket listen;
    private Socket socket;
    private int bLenght = 64;
    
    public CommandProcessor(Properties serverProp, BambooCommunicator pCommunicator) {
        try{
           	communicator = pCommunicator;
        	serverProperties = serverProp;
            listen = new ServerSocket(Integer.parseInt(serverProperties.getProperty("serverPortCommand")));
//			if(BambooUnitDataServer.isLog)
//			{
				log.error("CommandProcessor : " + " Started on port : " + listen.getLocalPort());
//			}
            
        } catch(Exception e){
			if(BambooUnitDataServer.isLog)
			{
				log.error(e);
			}
        }
       	
        Thread t = new Thread(this);
        t.start();
    }
    
    public void run() {
        
        while(true){
            
            try{
        			socket = listen.accept();
        			socket.setKeepAlive(true);
        			ReadCommand(socket);
            } catch(Exception e){
                
//    			if(BambooUnitDataServer.isLog)
//    			{
    				log.error("Exception in command processing",e);
//    			}
    			break;
            }
        }
    }
    
    public void ReadCommand(Socket socket)
    {
        try {
            
        	DataInputStream bi = new DataInputStream(new BufferedInputStream(socket.getInputStream(), bLenght));
        	StringBuffer sb = new StringBuffer();
            
            byte[] bufferRead = new byte [bLenght];
            int bytes_read = 0;
            
            sb = new StringBuffer();
            bytes_read = bi.read(bufferRead);
            
            for ( int i = 0; i < bytes_read; i++ ) {
                sb.append((char)(bufferRead[i]));
            }
            bufferRead = null;
            bi.close();
            socket.close();            
            String messageString = sb.toString();
            
    		String command[] = messageString.split("\\/");
    		if(command.length > 1)
    		{
    			unitId = command[0];
    			commandText = command[1];
//    			if(BambooUnitDataServer.isLog)
//    			{
    				log.error("UnitId : " + unitId + " Command : " + commandText);
//    			}
    			
        		communicator.sendCommand(unitId, commandText);
    		}
        } catch (Exception ex) {
//			if(BambooUnitDataServer.isLog)
//			{
				log.error("Exception in reading command",ex);
//			}
        }
    }
        
}
