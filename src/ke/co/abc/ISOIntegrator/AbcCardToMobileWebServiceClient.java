package ke.co.abc.ISOIntegrator;


import java.rmi.RemoteException;
import ke.co.ars.entity.TrxRequest;
import ke.co.ars.entity.TrxResponse;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


import poapay.PoaPayServiceProxy;


public class AbcCardToMobileWebServiceClient {
	
    /* Get actual class name to be printed on */
    static Logger log = Logger.getLogger(AbcCardToMobileWebServiceClient.class.getName());
	
	public TrxResponse cardToMobileRequest (TrxRequest request){
	    
	  //PropertiesConfigurator is used to configure logger from properties file
        PropertyConfigurator.configure("/opt/log4j/abclog4j.properties");
        
        log.info("Recieved Card To Mobile request.....");
        
	    String apiResponseMsg  = null;
	    
//        int timeout = request.getTimeout();
        
        PoaPayServiceProxy poaPayServiceProxy = new PoaPayServiceProxy();
        
        poaPayServiceProxy.setEndpoint(request.getApiUrl());
        
        TrxResponse responseMsg = new TrxResponse();
	    
        try {
			
        	log.info("Sendig mpesa request to icontrace api.....");
        	
        	apiResponseMsg = poaPayServiceProxy.poaPayToWallet(request.getMerchantCode(), 
        			request.getApiUname(), request.getApiPasswd(), request.getMsisdn(), 
        			Double.valueOf(request.getAmount()), request.getBeneficiaryName(), 
        			request.getTransactionID());
        	
		} catch (RemoteException e) {
			
			responseMsg.setStatusCode(96);
          
			responseMsg.setStatusDescription("ERROR: connection timeout to " + 
			request.getISOServerIP() + " on port " + request.getISOServerPort());
          
//			e.printStackTrace();
          log.error("Exception: ",e.fillInStackTrace());
		}
	 
	    switch(apiResponseMsg.toUpperCase()){
	    	case "SUCCESS":
	    		
	    		responseMsg.setStatusCode(0);
	    		responseMsg.setStatusDescription(apiResponseMsg);
	    		
	    	break;
	    	
	    	default :
	    		
	    		responseMsg.setStatusCode(101);
	    		responseMsg.setStatusDescription(apiResponseMsg);
	    		
	    	break;
	    }
        return responseMsg;
	}

}
