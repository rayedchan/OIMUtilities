package com.blogspot.oraclestack.testdriver;

import com.blogspot.oraclestack.objects.UserProcessor;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Serializable;
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
import oracle.iam.reconciliation.api.BatchAttributes;
import oracle.iam.reconciliation.api.EventAttributes;
import oracle.iam.reconciliation.api.ChangeType;
import oracle.iam.reconciliation.api.EventMgmtService;
import oracle.iam.reconciliation.api.InputData;
import oracle.iam.reconciliation.api.ReconOperationsService;
import oracle.iam.reconciliation.api.ReconciliationResult;
import oracle.iam.reconciliation.vo.EventConstants;


/**
 * Generic Resource Reconciliation Event Creator
 * Source is from a flat file
 * Any resource can be used as long as the resource object and fields are correctly specified.
 * @author rayedchan
 */
public class GenericReconEventFlatFileFeedTestDriver 
{
    // Logger
    private static final ODLLogger LOGGER = ODLLogger.getODLLogger(GenericReconEventFlatFileFeedTestDriver.class.getName());
    
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
            
            // Input Parameters 
            String csvFilePath = "/home/oracle/Desktop/psft_hrms_users.csv";
            String delimiter = ",";
            String profileName = "Peoplesoft HRMS"; // Reconciliation Profile Name - Resource Object Name
            
            // Read File
            FileReader fReader = new FileReader(csvFilePath); 
            bReader = new BufferedReader(fReader);
            
            Boolean eventFinished = true; // No child data provided; mark event to Data Received
            String dateFormat = "yyyy-MM-dd";
            Date actionDate = null; // Event to be processed immediately
            boolean ignoreDuplicate = false; // Identical to using IgnoreEvent API
            BatchAttributes batchAttrs = new BatchAttributes(profileName, dateFormat, ignoreDuplicate);

            // Store all data from flat file into OIM object
            List<InputData> allReconEvents = new ArrayList<InputData>();
            
            // Header Line
            String line = bReader.readLine();
            if(line == null || "".equalsIgnoreCase(line))
            {
                throw new Exception("Header must be provided as the first entry in file.");
            }
            String[] header = line.split(delimiter);
            System.out.println(Arrays.asList(header));
            
            // Convert each line entry into a map object
            line = bReader.readLine();
            while(line != null)
            {
                HashMap<String, Serializable> reconEventData = new HashMap<String, Serializable>();
                String[] entryValues = line.split(delimiter);
                
                // One to one correspondence to header
                for(int i = 0; i < entryValues.length; i++)
                {
                    reconEventData.put(header[i],entryValues[i]);
                }
               
                InputData event = new InputData(reconEventData, null, eventFinished, ChangeType.CHANGELOG, actionDate);
                
                allReconEvents.add(event);
                
                // read next line
                line = bReader.readLine();
            }
            
            
            //System.out.println(allReconEvents);
            InputData[] events = new InputData[allReconEvents.size()];
            allReconEvents.toArray(events);
            
            //System.out.println(Arrays.asList(events));
            ReconciliationResult result = reconOps.createReconciliationEvents(batchAttrs, events);
            System.out.println(result.getSuccessResult());
            System.out.println(result.getFailedResult());
            
            
            
            
  /*         
   InputData[] input = new InputData[2];
 
   HashMap<String, Serializable> reconData1 = new HashMap<String, Serializable>();
   reconData1.put("First Name", "name1");
   
   List> allMultiValRecords = new ArrayList>(); 
   Map<String, List>> allMultiValAttribMap = new HashMap<String, List>>();
   allMultiValAttribMap.put("Member Of A", allMultiValRecords); 
   HashMap<String, Serializable> multiValRecord1 = new HashMap<String, Serializable>(); 
   multiValRecord1.put("Group Name", "mygroup1");
   allMultiValRecords.add(multiValRecord1);
   HashMap<String, Serializable> multiValRecord2 = new HashMap<String, Serializable>();
   multiValRecord2.put("Group Name", "mygroup2"); 
   allMultiValRecords.add(multiValRecord2); 
   input[0] = new InputData(reconData1, allMultiValAttribMap, true, ChangeType.CHANGELOG, null); 
   HashMap<String, Serializable> reconData2 = new HashMap<String, Serializable>(); 
   reconData2.put("First Name", "name2"); List> allMultiValRecords = new ArrayList>(); 
   Map<String, List>> allMultiValAttribMap = new HashMap<String, List>>(); 
   allMultiValAttribMap.put("Member Of B", allMultiValRecords); 
   HashMap<String, Serializable> multiValRecord1 = new HashMap<String, Serializable>(); 
   multiValRecord1.put("Group Name", "mygroup3"); 
   allMultiValRecords.add(multiValRecord1);
   HashMap<String, Serializable> multiValRecord2 = new HashMap<String, Serializable>(); multiValRecord2.put("Group Name", "mygroup2"); allMultiValRecords.add(multiValRecord2); input[1] = new InputData(reconData2, allMultiValAttribMap, false, ChangeType.REGULAR, null); BatchAttributes batchAttribs = new BatchAttributes("ResourceObjectName", "yyyy/MM/dd hh:mm:ss z"); ReconciliationResult result = reconOperationsService.createReconciliationEvents(batchAttribs, input); ArrayList<FailedInputData> failedResult = result.getFailedResult(); assertEquals(0, failedResult.size()); ArrayList<Serializable> batchIds = result.getSuccessResult(); Long batchId = (Long) batchIds.get(0);
  */
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