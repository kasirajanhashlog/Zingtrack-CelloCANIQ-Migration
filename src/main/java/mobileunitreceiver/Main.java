/*
 * Main.java
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package mobileunitreceiver;

import com.claystone.server.heartbeat.HeartBeat;
import com.magikMaker.bamboo.communication.BambooCommunicator;
import com.magikMaker.bamboo.model.BuzzerEventHashModel;
import com.magikMaker.server.command.CommandProcessor;
import com.magikMaker.server.data.BambooUnitDataServer;
import com.magikMaker.server.ServerDisplay;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Properties;

/**
 *
 * @author Administrator
 */
public class Main {
    
    private static Properties properties;
    private static Hashtable<String,BuzzerEventHashModel> lBuzzerEventHash;
    /** Creates a new instance of Main */
    public Main() {
    }
    
    
    //Implement Reload property
    //
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        
        try {
            
            properties = new Properties();
            
            properties.load(new FileInputStream("server.properties"));
            lBuzzerEventHash = new Hashtable<String, BuzzerEventHashModel>();
//            int portNum = Integer.parseInt(args[0]);
            
            //localhost
            //String servletHost = "www.onelink.co.th";
            //String servletPort = "8080";
            //String servletApp = "GPRSDataServlet";
            //String servletName = "gprsMessageServlet";
            
//            String servletHost = (args[1]);
//            String servletPort = (args[2]);
            
//            int portNumCommand = 9033;
//            int portNumData = 9034;
//
//            String servletHost = ("localhost");
//            String servletPort = ("8080");
//
//            String servletApp = "BambooUnitServlet";
//            String servletName = "BambooUnitServlet";
            
//            MagikServerFrame msf = new MagikServerFrame();
            
            ServerDisplay msf = new ServerDisplay();
                        
            BambooCommunicator bc = new BambooCommunicator(msf, properties);
            
            BambooUnitDataServer mobileUnitServer = new BambooUnitDataServer(properties, msf, bc, lBuzzerEventHash);
            
            CommandProcessor mcs = new CommandProcessor(properties,bc);
           // HeartBeat.LogStartup();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
    
}
