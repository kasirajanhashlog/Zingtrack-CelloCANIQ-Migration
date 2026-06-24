/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.magikMaker.bamboo.communication;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.magikMaker.bamboo.model.BambooData;
import com.magikMaker.server.data.BambooUnitDataServer;

/**
 *
 * @author pockey
 */
public class BambooSocket {
        
    public SocketChannel socketChannel = null;
    public BambooData bambooData = null;
    private Logger log = LogManager.getLogger(BambooSocket.class);
    
   
    public BambooSocket(SocketChannel _socketChannel, BambooData _bambooData) {
               
        socketChannel = _socketChannel;
        bambooData = _bambooData;
    }    

    public String getMobileUnitID() {
        
        return bambooData.mobileUnitID;
        
    }

    public void receiveData(SocketChannel _socketChannel, BambooData _bambooData) {

        socketChannel = _socketChannel;
        bambooData = _bambooData;
        
    }
    
    public boolean isMessageRepeat(ByteBuffer messageIn) {
        
        boolean result = false;
        
        if ( this.bambooData.receivedMessageStr.equals(messageIn)) result = true;
        
        return result;
        
    }
    

    public boolean isClosed() {

        return socketChannel.socket().isClosed();
    }

    public void disconnect() {

        try {
            socketChannel.socket().close();
            socketChannel.close();
        } catch (IOException ex) {
			if(BambooUnitDataServer.isLog)
			{
				log.error("Disconnect Exception" , ex);
			}
        }

    }
    public boolean sendCommand(String mobileUnitID, String command) {
        
        boolean result = false;

        if (bambooData.mobileUnitID.equals(mobileUnitID)) {
//			if(BambooUnitDataServer.isLog)
//			{
				log.error("Sending " + command + " to " + bambooData.mobileUnitID);
//			}

            try {
                this.socketChannel.write(ByteBuffer.wrap(command.getBytes()));
                result = true;                
            } catch (Exception e) {
                // client is no longer active
//	    			if(BambooUnitDataServer.isLog)
//	    			{
	    				log.error("Unable to send command to unit : " + mobileUnitID , e);
//	    			}
	                result = false;	    			
            	}
        }
        return result;
    }

}
