/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.magikMaker.bamboo.communication;

/**
 *
 * @author pockey
 */

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class BambooDataReceive {
    
    public SocketChannel socketChannel;
    public ByteBuffer messageIn;
    public int dst;


    public BambooDataReceive(SocketChannel _socketChannel, ByteBuffer _message, int _dst) {
        
         socketChannel = _socketChannel;
         messageIn = _message;
         dst = _dst;
        
    }
    
    

}
