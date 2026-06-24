/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.magikMaker.bamboo.log;

import java.util.Calendar;

import com.magikMaker.bamboo.model.BambooData;

/**
 *
 * @author pockey
 */
public class BambooLogData {
    
    public BambooData bambooData;
    public Calendar dateTime = Calendar.getInstance();
    public int statusCode;
    
    public BambooLogData(BambooData _bambooData, int _statusCode) {
        
        bambooData = _bambooData;
        statusCode = _statusCode;        
        
    }

}
