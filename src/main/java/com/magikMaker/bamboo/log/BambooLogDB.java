/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.magikMaker.bamboo.log;

import com.magikMaker.bamboo.log.BambooLogData;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;

/**
 *
 * @author pockey
 */
public class BambooLogDB implements Runnable {

    private static final String insertLogData = "INSERT INTO unit_log ( mobileunitid, date_time, host, port, status_code ) VALUES ( ?, ?, ?, ?, ?);";
    private static Connection connBAMBOO;
    private static BambooLogDB bambooLogDB;
    private Properties properties;
    private ArrayList<BambooLogData> bambooLogDatas;

    private BambooLogDB(Properties _properties) {

        this.properties = _properties;

        bambooLogDatas = new ArrayList<BambooLogData>();

        Thread th1 = new Thread(this);
        th1.start();

    }

    public static BambooLogDB getInstance(Properties _properties) {

        if (bambooLogDB == null) {
            bambooLogDB = new BambooLogDB(_properties);
        }
        return bambooLogDB;

    }

   
    @Override
    public void run() {

        try {

            connBAMBOO = DriverManager.getConnection(properties.getProperty("bambooConnectionURL"));

            while (true) {

                synchronized (this) {

                    while (!bambooLogDatas.isEmpty()) {

                        try {

                            BambooLogData bld = (BambooLogData) bambooLogDatas.get(0);

                            insertLogData(bld);

                        } catch (SQLException ex) {

                            ex.printStackTrace();

                        }

                        bambooLogDatas.remove(0);

                    }



                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();

            bambooLogDB = null;

            try {

                Thread.sleep(30000);
            } catch (InterruptedException ex1) {
//                LogManager.getLogger(BambooMessageLog.class.getName()).log(Level.SEVERE, null, ex1);
            }

            while (!bambooLogDatas.isEmpty()) {

                try {

                    BambooLogData bld = (BambooLogData) bambooLogDatas.get(0);

                    insertLogData(bld);

                } catch (SQLException ex1) {

                    ex1.printStackTrace();

                }

                bambooLogDatas.remove(0);

            }

        }

        try {
            connBAMBOO.close();
        } catch (SQLException ex) {
        }



    }

    private void insertLogData(BambooLogData bambooLogdata) throws SQLException {

        PreparedStatement pstmt = null;

        pstmt = connBAMBOO.prepareStatement(insertLogData);

        pstmt.setString(1, bambooLogdata.bambooData.mobileUnitID);

        pstmt.setTimestamp(2, new java.sql.Timestamp(bambooLogdata.dateTime.getTimeInMillis()));

        pstmt.setString(3, bambooLogdata.bambooData.serverHost);

        pstmt.setInt(4, bambooLogdata.bambooData.serverPort);

        pstmt.setInt(5, bambooLogdata.statusCode);

        pstmt.execute();

        pstmt.close();

    }
}
