/*
    Author: Alex Mbolonzi

 */
package ke.co.abc.ISOIntegrator;

import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.StringTokenizer;


import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jpos.iso.ISOException;


//import ke.co.ars.entity.TransactionStatus;
//import ke.co.ars.dao.MobileSubscriberDAO;
//import ke.co.ars.dao.NodeDAO;
import ke.co.ars.dao.TransferB2CDAO;
//import ke.co.ars.entity.Hits;
import ke.co.ars.entity.TrxRequest;
import ke.co.ars.entity.TrxResponse;
import ke.co.ars.entity.StatusCode;
//import ke.co.ars.entity.MobileSubscriber;
//import ke.co.ars.entity.Node;
import ke.co.ars.entity.Transfer;
///import ke.co.abc.ISOIntegrator.AbcCardToMobile;
import ke.co.abc.ISOIntegrator.AbcEcho;
//import ke.co.abc.ISOIntegrator.AbcKyc;
import ke.co.abc.ISOIntegrator.AbcCardToBankAcc;
import ke.co.ars.oracle.dao.OracleTransferB2CDAO;

public class AbcIsoJob {
    
    /* Get actual class name to be printed on */
    static Logger log = Logger.getLogger(AbcIsoJob.class.getName());

    public AbcIsoJob() {
        
      //PropertiesConfigurator is used to configure logger from properties file
        PropertyConfigurator.configure("/opt/log4j/abclog4j.properties");
    }
    
    public void echoTest(String hostName) {
        
        log.info("Initiating Echo request to " + hostName);

        TrxResponse responseMsg = new TrxResponse();

        AbcEcho echo = new AbcEcho();

        String serverName = hostName;

        try {

            responseMsg = echo.echoRequest(serverName);

        } catch (ISOException | IOException e) {
            // TODO Auto-generated catch block
            responseMsg.setStatusCode(30);
            
            responseMsg.setStatusDescription("ERROR: Unable to parse ISO message");
            
//            e.printStackTrace();
            log.error("Exception: ",e.fillInStackTrace());
        }

    }    

    public TrxResponse card2Mobile(String hostName, String destmobile, 
            String sourcemobile, String creditAmount, String trxID, int sourceID, 
            String source, String beneficiary_name) {

        log.info("Initiating Card2Mobile request.....");
        
        Transfer transferC2MTransaction = new Transfer();
        
        transferC2MTransaction.setServerName(hostName);
        transferC2MTransaction.setTransactionType("ABC_C2M");
        transferC2MTransaction.setDestAccount(destmobile);
        transferC2MTransaction.setAmount(creditAmount);
        transferC2MTransaction.setTrxCode(trxID);
        transferC2MTransaction.setSourceMSISDN(sourcemobile);
        transferC2MTransaction.setOrigID(sourceID);
        transferC2MTransaction.setOrig(source);
        
        TransferB2CDAO transferC2MDAO = new TransferB2CDAO();
        
        TrxRequest apiReq = new TrxRequest();
        
        apiReq = transferC2MDAO.logTransaction(transferC2MTransaction);

        log.info("Getting server details for " + hostName);

        TrxResponse c2mresponseMsg = new TrxResponse();
        
//        String response = null;
        
        switch(apiReq.getStatusCode()){
        case 94:
        	
        	c2mresponseMsg.setStatusCode(apiReq.getStatusCode());

            c2mresponseMsg.setStatusDescription("Duplicate transaction.");
            
        	break;
        case 95:
        	
        	c2mresponseMsg.setStatusCode(apiReq.getStatusCode());

            c2mresponseMsg.setStatusDescription("Server currently unavailable.");
            
        	break;
        default:
        	
        	apiReq.setMsisdn(destmobile);
        	apiReq.setAmount(creditAmount);
        	apiReq.setBeneficiaryName(beneficiary_name);

        	AbcCardToMobileWebServiceClient apiTrx = new AbcCardToMobileWebServiceClient();
        	
        	log.info("Insert Card2Mobile transaction details.... ");
        	
        	StatusCode trxStatus = apiTrx.cardToMobileRequest(apiReq);
        	
        	TrxResponse c2mResponseTransactions = new TrxResponse();
        	
        	c2mResponseTransactions.setTransactionID(apiReq.getTransactionID());
        	c2mResponseTransactions.setStatusCode(trxStatus.getStatusCode());
        	c2mResponseTransactions.setStatusDescription(trxStatus.getStatusDescription());
//        	
        	transferC2MDAO.updateTransactionResponse(c2mResponseTransactions);
//        	
        	c2mresponseMsg.setStatusCode(trxStatus.getStatusCode());
            c2mresponseMsg.setStatusDescription(trxStatus.getStatusDescription());
                 	
        	break;
        }
        
//        response = c2mresponseMsg.getStatus() + "$" + c2mresponseMsg.getStatusDescription() 
//                + "$" + c2mresponseMsg.getTransactionData();
        
//        log.info("Response: " + response);
        
        return c2mresponseMsg;
    }
    
    public TrxResponse card2BankAcc(String hostName, String creditAcc, 
            String sourcemobile, String creditAmount, String trxID, int sourceID, 
            String source,String beneficiary_name) {

        log.info("Initiating Card2BankAcc request.....");
        
        Transfer transferC2BTransaction = new Transfer();
        
        transferC2BTransaction.setServerName(hostName);
        transferC2BTransaction.setTransactionType("ABC_C2B");
        transferC2BTransaction.setDestAccount(creditAcc);
        transferC2BTransaction.setAmount(creditAmount);
        transferC2BTransaction.setTrxCode(trxID);
        transferC2BTransaction.setSourceMSISDN(sourcemobile);
        transferC2BTransaction.setOrigID(sourceID);
        transferC2BTransaction.setOrig(source);

        TransferB2CDAO transferC2BDAO = new TransferB2CDAO();
        
        TrxRequest abcC2BReq = new TrxRequest();
        
        log.info("Getting server details for " + hostName);
        
        abcC2BReq = transferC2BDAO.logTransaction(transferC2BTransaction);
             
        TrxResponse c2bresponseMsg = new TrxResponse();
        
        switch(abcC2BReq.getStatusCode()){
        case 94:
        	
        	c2bresponseMsg.setStatusCode(94);

            c2bresponseMsg.setStatusDescription("Duplicate transaction.");
            
        	break;
        case 95:
        	
        	c2bresponseMsg.setStatusCode(95);

            c2bresponseMsg.setStatusDescription("Server currently unavailable.");
            
        	break;
        default:
        	
        	TrxRequest abcIsoRequest = new TrxRequest();

            abcIsoRequest.setTransactionID(abcC2BReq.getTransactionID());
            abcIsoRequest.setMsisdn(sourcemobile);
            abcIsoRequest.setAmount(creditAmount);
            abcIsoRequest.setInstitutionCode(abcC2BReq.getInstitutionCode());
            abcIsoRequest.setMerchantCode(abcC2BReq.getMerchantCode());
            abcIsoRequest.setCurrencyCode(abcC2BReq.getCurrencyCode());
            abcIsoRequest.setTimeout(abcC2BReq.getTimeout());
            abcIsoRequest.setISOServerIP(abcC2BReq.getISOServerIP());
            abcIsoRequest.setISOServerPort(abcC2BReq.getISOServerPort());
            abcIsoRequest.setDebitAcc(abcC2BReq.getCreditAcc());
            abcIsoRequest.setCreditAcc(creditAcc);
        	
        	log.info("Sending Card2Mobile transaction details.... ");	
        	
        	TrxResponse c2bResponseTransactions = new TrxResponse();
        	
        	AbcCardToBankAcc bankDeposit = new AbcCardToBankAcc();
        	
        	try {
        		
				c2bResponseTransactions = bankDeposit.cardToBankAccRequest(abcIsoRequest);
				
			} catch (ISOException e) {
				
				c2bresponseMsg.setStatusCode(30);
	            
				c2bresponseMsg.setStatusDescription("ERROR: ISO Exception");
	            
//				e.printStackTrace();
				log.error("Exception: ",e.fillInStackTrace());
			} catch (IOException e) {
				
				c2bresponseMsg.setStatusCode(96);
	            
				c2bresponseMsg.setStatusDescription("ERROR: IO Exception");
	            
//				e.printStackTrace();
				log.error("Exception: ",e.fillInStackTrace());
			}
            
        	
        	c2bResponseTransactions.setTransactionID(abcC2BReq.getTransactionID());
//        	c2bResponseTransactions.setStatus(c2bResponseTransactions.getStatus());
//        	c2bResponseTransactions.setStatusDescription(c2bResponseTransactions.getStatusDescription());
//        	
        	log.info("Update Card2Bank transaction details.... ");
        	
        	transferC2BDAO.updateTransactionResponse(c2bResponseTransactions);
        	
        	c2bresponseMsg.setStatusCode(0);
            c2bresponseMsg.setStatusDescription(c2bResponseTransactions.getStatusDescription());
                 	
        	break;
        }
 
//        
//        response = responseMsg.getStatus() + "$" + responseMsg.getStatusDescription() 
//                + "$" + responseMsg.getTransactionData();
//        
//        log.info("Response: " + response);
        
        return c2bresponseMsg;
    }
}
