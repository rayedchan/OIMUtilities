package com.blogspot.oraclestack.testdriver;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.platform.OIMClient;
import oracle.iam.reconciliation.api.EventAttributes;
import oracle.iam.reconciliation.api.ChangeType;
import oracle.iam.reconciliation.api.ReconOperationsService;

/**
 *
 * @author rayedchan
 */
public class MultithreadTestDriver 
{
    // Logger
    private static final ODLLogger LOGGER = ODLLogger.getODLLogger(MultithreadTestDriver.class.getName());
    
    // Adjust constant variables according to you OIM environment
    public static final String OIM_HOSTNAME = "localhost";
    public static final String OIM_PORT = "14000"; // For SSL, use 14001; For non-SSL, use 14000
    public static final String OIM_PROVIDER_URL = "t3://"+ OIM_HOSTNAME + ":" + OIM_PORT; // For SSL, use t3s protocol; For non-SSL, use t3 protocol
    public static final String AUTHWL_PATH = "lib/config/authwl.conf";
    public static final String APPSERVER_TYPE = "wls";
    public static final String FACTORY_INITIAL_TYPE = "weblogic.jndi.WLInitialContextFactory";
   
    // Use if using SSL connection for OIMClient
    public static final String TRUST_KEYSTORE_FOR_SSL = "/home/oracle/Oracle/Middleware/wlserver_10.3/server/lib/DemoTrust.jks";
    
    // OIM Administrator Credentials
    public static final String OIM_ADMIN_USERNAME = "xelsysadm";
    public static final String OIM_ADMIN_PASSWORD = "Password1";
    
    public static void main(String[] args) throws Exception
    {
        OIMClient oimClient = null;
        BufferedReader bReader = null;
        
        try
        {
            // Set system properties required for OIMClient
            System.setProperty("java.security.auth.login.config", AUTHWL_PATH);
            System.setProperty("APPSERVER_TYPE", APPSERVER_TYPE);
            System.setProperty("weblogic.security.SSL.trustedCAKeyStore", TRUST_KEYSTORE_FOR_SSL); // Provide if using SSL

            // Create an instance of OIMClient with OIM environment information 
            Hashtable<String, String> env = new Hashtable<String, String>();
            env.put(OIMClient.JAVA_NAMING_FACTORY_INITIAL, FACTORY_INITIAL_TYPE);
            env.put(OIMClient.JAVA_NAMING_PROVIDER_URL, OIM_PROVIDER_URL);
            
            // Establish an OIM Client
            oimClient = new OIMClient(env);
            
            // Login to OIM with System Administrator Credentials
            oimClient.login(OIM_ADMIN_USERNAME, OIM_ADMIN_PASSWORD.toCharArray());
            
            ReconOperationsService reconOps = oimClient.getService(ReconOperationsService.class);
            
            String csvFilePath = "/home/oracle/Desktop/psft_hrms_users.csv";
            FileReader fReader = new FileReader(csvFilePath); 
            bReader = new BufferedReader(fReader);
            String delimiter = ",";
            
            String resourceObject = "Peoplesoft HRMS";
            Boolean eventFinished = true; // No child data provided; mark event to Data Received
            String dateFormat = "yyyy-MM-dd";
            Date actionDate = null; // Event to be processed immediately
            EventAttributes evtAttr = new EventAttributes(eventFinished, dateFormat, ChangeType.CHANGELOG, actionDate);
            
            // Header Line
            String line = bReader.readLine();
            if(line == null || "".equalsIgnoreCase(line))
            {
                throw new Exception("Header must be provided as the first entry in file.");
            }
            String[] header = line.split(delimiter);
            System.out.println(Arrays.asList(header));
            
            // Process data entries
            line = bReader.readLine();
            while(line != null)
            {
                
                String[] entry = line.split(delimiter);
                HashMap<String,Object> reconEventData = new HashMap<String,Object>();
                
                // Iterate entry
                for(int i = 0; i < entry.length; i++)
                {
                    String attributeName = header[i];
                    String attributeValue = entry[i];
                    reconEventData.put(attributeName, attributeValue);
                }
                
                // Create Reconciliation Event
                long reconKey = reconOps.createReconciliationEvent(resourceObject, reconEventData, evtAttr);
                
                // Process Reconciliation Event
                reconOps.processReconciliationEvent(reconKey);
                
                line = bReader.readLine();
            }  
        }
        
        catch(Exception ex)
        {
            LOGGER.log(ODLLevel.ERROR, "", ex);
        }
                
        finally
        {
            // Logout of OIM client
            if(oimClient != null)
            {
                oimClient.logout();
            }
            
            if(bReader != null)
            {
                bReader.close();
            }
        } 
    }  
}

/**
 * Each thread will create and process a reconciliation event.
 * @author rayedchan
 */
class EventProcessor implements Runnable
{
    private String entry = null;
    
    public EventProcessor(String entry)
    {
        this.entry = entry;
    }
    
    public void run()
    {
        
    }
}