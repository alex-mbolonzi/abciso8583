package ke.co.abc.ISOIntegrator;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import ke.co.ars.dao.NodeDAO;
import ke.co.ars.entity.TrxResponse;
import ke.co.ars.entity.Node;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jpos.iso.ISOException;
import org.jpos.iso.channel.ASCIIChannel;
import org.jpos.iso.packager.GenericPackager;
import org.jpos.iso.ISOMsg;


public class AbcEcho {
	
    /* Get actual class name to be printed on */
    static Logger log = Logger.getLogger(AbcEcho.class.getName());
    
	public TrxResponse echoRequest (String serverName) throws ISOException, IOException {
		
		//PropertiesConfigurator is used to configure logger from properties file
        PropertyConfigurator.configure("/opt/log4j/abclog4j.properties");
        
	    log.info("Recieved Echo request.....");
	    
	    NodeDAO echoDAO = new NodeDAO();
	    
	    Node nodeDetails = new Node();
	    
	    nodeDetails = echoDAO.getServerDetails(serverName);
	    
	    TrxResponse responseMsg = new TrxResponse();
	    
	    int timeout = nodeDetails.getTimeout();
        
        String serverIP = nodeDetails.getNodeIP();
        
        int serverPort = nodeDetails.getNodePort();
        
        log.info("Echo request to ..... " + serverIP + serverPort);
 
        ASCIIChannel channel = new ASCIIChannel(
                serverIP, serverPort, new GenericPackager("/opt/ISO/postpack.xml")
        );

        SimpleDateFormat transactionTime = new SimpleDateFormat("YYYYMMddhhmmss");

		Date date = new Date();
		
		String functionCode = "831";

        try {          
            channel.setTimeout(timeout);
			channel.connect ();
		} catch (IOException e) {
			
		    responseMsg.setStatusCode(96);
            
            responseMsg.setStatusDescription("ERROR: connection timeout to " + serverIP + " on port " + serverPort);
            
//			e.printStackTrace();
            log.error("Exception: ",e.fillInStackTrace());
		}
        
        ISOMsg m = new ISOMsg ();
        m.setMTI("1804");
        m.set(11,"049360");
        m.set(12,transactionTime.format(date));
        m.set(24,functionCode);
        m.set(59,"Test Data"); 
        m.set(93,"11111111111");
        m.set(94,"22222222222");
        m.set(123,"SWT");

        try {
            
			channel.send (m);

			log.info("Echo ISO request : " + channel.toString());
//			System.out.println(channel.toString());
			
			ISOMsg isoResponse = channel.receive();
			
			channel.disconnect ();
	        
			log.info("Echo ISO response : " + isoResponse.toString());
			
			AbcIsoParser isoMessageParser = new AbcIsoParser();
            
            responseMsg = isoMessageParser.ParseISOMessage(isoResponse);
            	        
 
		} catch (IOException e) {
		    
			channel.disconnect ();
			
		    responseMsg.setStatusCode(30);
            
            responseMsg.setStatusDescription("ERROR: Unable to parse ISO response message");
            
//			e.printStackTrace();
            log.error("Exception: ",e.fillInStackTrace());
		}
          
        responseMsg.setTransactionID(serverName);       
        
        nodeDetails.setStatusCode(responseMsg.getStatusCode());
        nodeDetails.setStatusDescription(responseMsg.getStatusDescription());
        
        echoDAO.updateEchoStatus(nodeDetails);
        
		return responseMsg;
	}
	
}
