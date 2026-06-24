/*
 * SendCommand.java
 *
 * Created on 13 สิงหาคม 2550, 13:36 น.
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.magikMaker.bamboo.communication;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 *
 * @author Administrator
 */
public class SendCommand {
    
    private String hostName;
    
    private int portNum;
    
    private String command;
    
    /**
     * Creates a new instance of SendCommand
     */
    public SendCommand(String _hostName, int _portNum, String _command) {
        
        hostName = _hostName;
        portNum = _portNum;
        command = "_/\\_" + _command;
                
    }
    
    public boolean sendCommand() {
        
        boolean result = false;
        
        Socket smtpSocket = null;
        
        try {
            
            // Convert String to byte
            byte b[] = command.getBytes();
            
//            for ( int i = 0; i < b.length - 1; i ++ ) {
//                
//                System.out.println(b[i]);
//                
//            }
            
            // Send data to server
            
            smtpSocket = new Socket(hostName, portNum);
            DataInputStream in = new DataInputStream(smtpSocket.getInputStream());
            DataOutputStream out = new DataOutputStream(smtpSocket.getOutputStream());
            
            out.write(b);
            
            in.close();
            out.close();
            
            smtpSocket.close();
            
            result = true;
            
            
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host: hostname");
        } catch (IOException e) {
            System.err.println(e);
        }
        
        return result;
        
    }
    
//    private String byteToHex(byte b){
//        int i = b & 0xFF;
//        return Integer.toHexString(i);
//    }
//    
//    private String byteToBin(byte b){
//        int i = b & 0xFF;
//        return Integer.toBinaryString(i);
//    }
//    
//    public static void main(String[] args) {
//        
//        SendCommand sc = new SendCommand("127.0.0.1", 9034, ">>>1|(SSC,0000,0,0818316559)");
//        
////        SendCommand sc = new SendCommand("61.19.242.71", 9034, ">>>1|(SSC,0000,0,0818316559)");
//        
//    }
    
}
