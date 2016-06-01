package com.blogspot.oraclestack.testdriver;

import Thor.API.Exceptions.tcAPIException;
import com.blogspot.oraclestack.objects.UserProcessor;
import java.io.BufferedReader;
import java.io.FileReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.identity.exception.NoSuchUserException;
import oracle.iam.identity.exception.SearchKeyNotUniqueException;
import oracle.iam.identity.exception.UserModifyException;
import oracle.iam.identity.exception.ValidationFailedException;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.platform.OIMClient;
import oracle.iam.platform.authz.exception.AccessDeniedException;
import oracle.iam.reconciliation.api.EventAttributes;
import oracle.iam.reconciliation.api.ChangeType;
import oracle.iam.reconciliation.api.EventMgmtService;
import oracle.iam.reconciliation.api.ReconOperationsService;
import oracle.iam.reconciliation.vo.EventConstants;

/**
 * Example of Multi-threading
 * - Reads a CSV file containing data for a particular target or trusted resource
 * - Each thread creates and process a reconciliation event
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
      
            // Get OIM services
            ReconOperationsService reconOps = oimClient.getService(ReconOperationsService.class);
            EventMgmtService eventService = oimClient.getService(EventMgmtService.class);
            UserManager usrMgr = oimClient.getService(UserManager.class);
            
            //User user = usrMgr.getDetails("AWINDRUNNER", null, true);
            //System.out.println(user);
            
            //String keyAttrName = "usr_key";
            //User modUser = new User("");
            //modUser.setAttribute("Employee Number", "1234");
            //usrMgr.modify(keyAttrName, "3018", modUser);
            
            //String keyAttrName = "User Login";
            //User modUser = new User("");
            //modUser.setAttribute("Employee Number", null);
            //usrMgr.modify(keyAttrName, "AWINDRUNNER", modUser);
            
            String keyAttrName = "User Login";
            String filePath = "/home/oracle/Desktop/users.csv";
            FileReader fReader = new FileReader(filePath);
            bReader = new BufferedReader(fReader);
            String delimiter = ",";
            
            // Header Line
            String line = bReader.readLine();
            if(line == null || "".equalsIgnoreCase(line))
            {
                throw new Exception("Header must be provided as the first entry in file.");
            }
            String[] header = line.split(delimiter);
            System.out.println(Arrays.asList(header));
            
             // Create thread pool      
            int numOfThreads = 3;
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
            
            
            /*String csvFilePath = "/home/oracle/Desktop/psft_hrms_users.csv";
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
            
            // Create thread pool      
            int numOfThreads = 3;
            ExecutorService threadExecutor = Executors.newFixedThreadPool(numOfThreads);
            
            // Initialize base configuration 
            EventProcessor.initializeConfig(header, delimiter, resourceObject, evtAttr, LOGGER, reconOps, eventService);
            
            // Process data entries using multi-threading
            line = bReader.readLine();
            while(line != null)
            {           
                threadExecutor.execute(new EventProcessor(line));
                line = bReader.readLine();
            }
            
            // Initate thread shutdown
            threadExecutor.shutdown();
            
            while(!threadExecutor.isTerminated())
            {
                // Wait for all event processor threads to complete
            }*/
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
    // Fields needed for every event
    private static String[] header;
    private static String delimiter;
    private static ODLLogger logger;
    private static String resourceObjectName;
    private static EventAttributes evtAttr;
    private static ReconOperationsService reconOps;
    private static EventMgmtService eventService;
    
    // Single entry in file
    private String line;
    
    /**
     * Constructor
     * @param line Reconciliation event data
     */
    public EventProcessor(String line)
    {
        this.line = line;
    }
    
    /**
     * Initialize required parameters for processing events
     * @param header    Header line of CSV file
     * @param delimiter Delimiter that separates each attribute
     */
    public static void initializeConfig(String[] header, String delimiter, String resourceObjectName, EventAttributes evtAttr, ODLLogger logger, ReconOperationsService reconOps, EventMgmtService eventService)
    {
        EventProcessor.header = header;
        EventProcessor.delimiter = delimiter;
        EventProcessor.logger = logger;
        EventProcessor.resourceObjectName = resourceObjectName;
        EventProcessor.evtAttr = evtAttr;
        EventProcessor.reconOps = reconOps;
        EventProcessor.eventService = eventService;
    }
    
    public void run()
    {
        String[] entry = line.split(delimiter);
        HashMap<String,Object> reconEventData = new HashMap<String,Object>();

        // Iterate entry to create mapping of attribute name and attribute value
        for(int i = 0; i < entry.length; i++)
        {
            String attributeName = header[i];
            String attributeValue = entry[i];
            reconEventData.put(attributeName, attributeValue);
        }
        
        // TODO: Use Batch Method to create events
        // Create Reconciliation Event
        long reconKey = reconOps.createReconciliationEvent(resourceObjectName, reconEventData, evtAttr);
        logger.log(ODLLevel.NOTIFICATION, "Created reconciliation event: Type = {0}, Event Id = {1}, Data = {2}", new Object[]{resourceObjectName, reconKey, reconEventData});
        
        try 
        {
            // Process Reconciliation Event
            // Using this API with threading may cause the following exception which results in a reconciliation event to be stuck in "Single User Matched" status: 
            // Thor.API.Exceptions.tcAPIException: An exception occurred: oracle.iam.platform.utils.SuperRuntimeException: Exception [EclipseLink-5006] (Eclipse Persistence Services - 2.3.1.v20111018-r10243): org.eclipse.persistence.exceptions.OptimisticLockException
            // Exception Description: The object [oracle.iam.reconciliation.dao.event.ReconBatch@3aa4c94] cannot be updated because it has changed or been deleted since it was las
            //reconOps.processReconciliationEvent(reconKey);
            
            String actionName = EventConstants.RECON_EVENT_ACTION_REEVAL; // Re-evalute
            HashMap<Object,Object> actionParams = new HashMap<Object,Object>();
            List<Long> reconEventIds = new ArrayList<Long>();
            reconEventIds.add(reconKey);
            String actionPerformed = "Process Recon Event: " + reconKey;
            eventService.performBulkAction(actionName, actionParams, reconEventIds, actionPerformed);
            logger.log(ODLLevel.NOTIFICATION, "Processed reconciliation event {0}", new Object[]{reconKey});
        } 
        
        catch (Exception ex) 
        {
            logger.log(ODLLevel.SEVERE, MessageFormat.format("Failed to process reconciliation event {0}", new Object[]{reconKey}), ex);
        }
    }
}