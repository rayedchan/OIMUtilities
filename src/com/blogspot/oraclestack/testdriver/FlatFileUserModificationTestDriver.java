package com.blogspot.oraclestack.testdriver;

import com.blogspot.oraclestack.objects.UserProcessor;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.platform.OIMClient;
import oracle.iam.reconciliation.api.EventMgmtService;
import oracle.iam.reconciliation.api.ReconOperationsService;

/**
 * Test Driver for testing multi-threading
 * Example: Modifies OIM Users using data from CSV file
 * @author rayedchan
 */
public class FlatFileUserModificationTestDriver 
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
      
            // Get OIM services
            ReconOperationsService reconOps = oimClient.getService(ReconOperationsService.class);
            EventMgmtService eventService = oimClient.getService(EventMgmtService.class);
            UserManager usrMgr = oimClient.getService(UserManager.class);
            
            // Parameters to change
            String keyAttrName = "User Login";
            String filePath = "/home/oracle/Desktop/users.csv";
            String delimiter = ",";
            int numOfThreads = 3;
                        
            // File Reader
            FileReader fReader = new FileReader(filePath);
            bReader = new BufferedReader(fReader);
            
            // Header Line
            String line = bReader.readLine();
            if(line == null || "".equalsIgnoreCase(line))
            {
                throw new Exception("Header must be provided as the first entry in file.");
            }
            String[] header = line.split(delimiter);
            System.out.println(Arrays.asList(header));
            
             // Create thread pool      
            ExecutorService threadExecutor = Executors.newFixedThreadPool(numOfThreads);
            
            // Initialize base configuration 
            UserProcessor.initializeConfig(header, delimiter, LOGGER, usrMgr, keyAttrName);
            
            // Process data entries using multi-threading
            line = bReader.readLine();
            while(line != null)
            {          
                threadExecutor.execute(new UserProcessor(line));
                line = bReader.readLine();
            }
            
             // Initate thread shutdown
            threadExecutor.shutdown();
            
            while(!threadExecutor.isTerminated())
            {
                // Wait for all event processor threads to complete
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
