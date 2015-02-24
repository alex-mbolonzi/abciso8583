package ke.co.abc.ISOIntegrator;

import java.io.IOException;
//import java.text.DecimalFormat;
//import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import ke.co.ars.entity.TrxRequest;
import ke.co.ars.entity.TrxResponse;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
//import org.jpos.util.*;
import org.jpos.iso.ISOException;
import org.jpos.iso.channel.ASCIIChannel;
import org.jpos.iso.packager.GenericPackager;
import org.jpos.iso.ISOMsg;


public class AbcCardToBankAcc {
	
    /* Get actual class name to be printed on */
    static Logger log = Logger.getLogger(AbcCardToBankAcc.class.getName());
	
	public TrxResponse cardToBankAccRequest (TrxRequest request) throws ISOException, IOException {
	    
	  //PropertiesConfigurator is used to configure logger from properties file
        PropertyConfigurator.configure("/opt/log4j/abclog4j.properties");
        
        log.info("Recieved Card To Account request.....");
        
//	    String msisdn = request.getMsisdn();
	    
	    String AMOUNT = request.getAmount();
	    
	    String debitAcc = request.getDebitAcc();
	    
	    String creditAcc = request.getCreditAcc();
	    
	    String transactionID = request.getTransactionID();
	    
        String InstitutionCode = request.getInstitutionCode();
        
//	    String merchantCode = request.getMerchantCode();
	    
	    String currencyCode = request.getCurrencyCode();
	    
        int timeout = request.getTimeout();
        
        String serverIP = request.getISOServerIP();
        
        int serverPort = request.getISOServerPort();
	    
	    String PROCESSING_CODE = "40";
	    
//	    String cardAcceptorTerminalID = "NW";
	    
//	    String echoData = "Card2BANK";
	    
	    String functionCode = "200";
	    
	    TrxResponse responseMsg = new TrxResponse();
	    
//        Logger logger = new Logger();
//        logger.addListener (new SimpleLogListener(System.out));
 
        ASCIIChannel channel = new ASCIIChannel(
                serverIP, serverPort, new GenericPackager("/opt/ISO/postpack.xml")
        );
 
//        ((LogSource)channel).setLogger (logger, "test-channel");

        
        SimpleDateFormat transactionTime = new SimpleDateFormat("YYYYMMddhhmmss");
        SimpleDateFormat transmissionDate = new SimpleDateFormat("YYYYMMdd");
//		SimpleDateFormat transactionMonthDay = new SimpleDateFormat("MMdd");
		Date date = new Date();

		String pc = PROCESSING_CODE + "000000000000";
		pc = pc.substring(0,6);
		
//		int stan = (int)Math.floor( Math.random() * 999998 + 1 );
//        NumberFormat formatter = new DecimalFormat("000000");
//        String stanNumber = formatter.format(stan);
        
        String stanNumber = "000000000000" + transactionID;       
        stanNumber = stanNumber.substring(stanNumber.length()-12);
        
        String IdentificationCode = "00000000000" + transactionID;
        IdentificationCode = IdentificationCode.substring(IdentificationCode.length()-11);
        
        String cardAcceptorIDCode = "000000000000000" + InstitutionCode;
        cardAcceptorIDCode = cardAcceptorIDCode.substring(cardAcceptorIDCode.length()-15);
        
		if (AMOUNT.indexOf(".") != -1) {
            String substra = AMOUNT.substring(AMOUNT.indexOf("."), AMOUNT
                    .length());
//            System.out.println("The substr is " + substra);
//            System.out.println("The length of substr is " + substra.length());
            if (substra.length() < 3) {
                AMOUNT = AMOUNT + "0";
//                System.out.println("amt_str = " + AMOUNT);
            }
                AMOUNT = AMOUNT.replaceAll("\\.", "");
        } else
        {
            AMOUNT = AMOUNT + "00";
        }

            AMOUNT = AMOUNT.replaceAll("\\.", "");
            AMOUNT = "0000000000000000" + AMOUNT;
            AMOUNT = AMOUNT.substring(AMOUNT.length() - 16);
            
            
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
        m.setMTI("1200");
//        m.set(2,msisdn);
        m.set(3,pc);
        m.set(4,AMOUNT);
        m.set(11,stanNumber);
        m.set(12,transactionTime.format(date));
        m.set(17,transmissionDate.format(date));
        m.set(24,functionCode);
        m.set(32,IdentificationCode); //Acquiring Institution Identification Code       
//        m.set(46,"Amount fees");
        m.set(49,currencyCode);
        m.set(102,debitAcc);//Account Identification-1 debit acc
        m.set(103,creditAcc);//Account Identification-2 credit acc
        m.set(123,"SWT");
        
        try {
            
            log.info("card2Bank ISO request : " + m.toString());
            
			channel.send (m);
//			System.out.println(channel.toString());
			ISOMsg isoResponse = channel.receive();
			
			AbcIsoParser isoMessageParser = new AbcIsoParser();
			
			responseMsg = isoMessageParser.ParseISOMessage(isoResponse);
            
            channel.disconnect ();
			
		} catch (IOException e) {
		    
			channel.disconnect ();
			
		    responseMsg.setStatusCode(30);
            
            responseMsg.setStatusDescription("ERROR: Unable to parse ISO response message");
            
//			e.printStackTrace();
            log.error("Exception: ",e.fillInStackTrace());
		}
        
        return responseMsg;
	}

}
