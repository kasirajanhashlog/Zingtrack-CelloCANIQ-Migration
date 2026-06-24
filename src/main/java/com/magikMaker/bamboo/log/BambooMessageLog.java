/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.magikMaker.bamboo.log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

/**
 *
 * @author pockey
 */
public class BambooMessageLog implements Runnable {

    private static final String insertMessageData = "INSERT INTO unit_message_log ( date_time, message, host, port, status_code ) VALUES ( ?, ?, ?, ?, 0);";
    private static Connection connBAMBOO;
    private static BambooMessageLog bambooMessageLogDB;
    private Properties properties;
    private ArrayList<String> messageIns;
    private String host;
    private int port;

    private BambooMessageLog(Properties _properties, String _host, int _port) {

        this.properties = _properties;
        this.host = _host;
        this.port = _port;

        messageIns = new ArrayList<String>();

        Thread th1 = new Thread(this);
        th1.start();

    }

    public static BambooMessageLog getInstance(Properties _properties, String _host, int _port) {

        if (bambooMessageLogDB == null) {
            bambooMessageLogDB = new BambooMessageLog(_properties, _host, _port);
        }
        return bambooMessageLogDB;

    }

    public synchronized void storeBambooLogData(String messageIn) {

        messageIns.add(messageIn);

        this.notify();

    }

    @Override
    public void run() {

        try {

            connBAMBOO = DriverManager.getConnection(properties.getProperty("bambooConnectionURL"));

            while (true) {

                synchronized (this) {

                    while (!messageIns.isEmpty()) {

                        try {

                            String mi = (String) messageIns.get(0);

                            insertMessageData(mi);

                        } catch (SQLException ex) {

                            ex.printStackTrace();

                        }

                        messageIns.remove(0);

                    }

                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
                }
            }

        } catch (SQLException ex) {
            ex.printStackTrace();

            bambooMessageLogDB = null;
            
            try {

                Thread.sleep(30000);
            } catch (InterruptedException ex1) {
//                LogManager.getLogger(BambooMessageLog.class.getName()).log(Level.SEVERE, null, ex1);
            }

            try {

                connBAMBOO = DriverManager.getConnection(properties.getProperty("bambooConnectionURL"));

                while (!messageIns.isEmpty()) {

                    String mi = (String) messageIns.get(0);

                    insertMessageData(mi);

                    messageIns.remove(0);

                }

            } catch (SQLException ex3) {
                ex3.printStackTrace();
            }

            try {
                connBAMBOO.close();
            } catch (SQLException ex4) {
            }
        }


    }

    private void insertMessageData(String messageIn) throws SQLException {

        PreparedStatement pstmt = null;

        pstmt = connBAMBOO.prepareStatement(insertMessageData);

        pstmt.setTimestamp(1, new Timestamp(new Date().getTime()));

        pstmt.setString(2, messageIn);

        pstmt.setString(3, host);

        pstmt.setInt(4, port);

        pstmt.execute();

        pstmt.close();

    }
}
