package com.blogspot.oraclestack.testdriver;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.platform.OIMClient;
import oracle.iam.reconciliation.api.BatchAttributes;
import oracle.iam.reconciliation.api.ChangeType;
import oracle.iam.reconciliation.api.EventMgmtService;
import oracle.iam.reconciliation.api.InputData;
import oracle.iam.reconciliation.api.ReconOperationsService;
import oracle.iam.reconciliation.api.ReconciliationResult;

/**
 * Generic Resource Reconciliation Event Creator
 * Source is from a flat file or database table
 * Any resource can be used as long as the resource object and fields are correctly specified.
 * @author rayedchan
 */
public class GenericReconEventSourceGeneratorDriver 
{
    // Logger
    private static final ODLLogger LOGGER = ODLLogger.getODLLogger(GenericReconEventSourceGeneratorDriver.class.getName());
    
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
        Connection conn = null;
          
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
            
            // TODO: Input parameters
            String profileName = "AD User Trusted"; // Resource Object Name
            String tableName = "MY_ADUSER_RE_FEED"; // Target Table
            String filter = ""; // WHERE Clause
            
            // Get database connection via Driver Manager
            conn = null;
            Class.forName("oracle.jdbc.driver.OracleDriver"); // Load Oracle database driver class
            String dbUrl = "jdbc:oracle:thin:@localhost:1521/orcl"; // Oracle Database URL
            Properties connectionProp = new Properties();
            connectionProp.put("user", "DEV_OIM");
            connectionProp.put("password", "Password1");
            conn = DriverManager.getConnection(dbUrl, connectionProp);
            LOGGER.log(ODLLevel.NOTIFICATION, "Established database connection.");
            
            // Recon Event Details
            Boolean eventFinished = true; // No child data provided; mark event to Data Received
            String dateFormat = "yyyy-MM-dd";
            Date actionDate = null; // Event to be processed immediately
            boolean ignoreDuplicate = false; // Identical to using IgnoreEvent API
            BatchAttributes batchAttrs = new BatchAttributes(profileName, dateFormat, ignoreDuplicate);
            
            // Construct list of reconciliation event to be created reading from source database table
            List<InputData> allReconEvents = constructReconciliationEventList(conn, tableName, filter, eventFinished, actionDate);
            LOGGER.log(ODLLevel.NOTIFICATION, "Recon Events {0}: {1}", new Object[]{allReconEvents.size(), allReconEvents});
            InputData[] events = new InputData[allReconEvents.size()];
            allReconEvents.toArray(events);
            
            // Create reconciliation events in OIM and process them
            ReconciliationResult result = reconOps.createReconciliationEvents(batchAttrs, events);
            LOGGER.log(ODLLevel.NOTIFICATION, "Success result: {0}",  new Object[]{result.getSuccessResult()});
            LOGGER.log(ODLLevel.NOTIFICATION, "Success result: {0}",  new Object[]{result.getFailedResult()});

            
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
        } 
    }  
    
     /**
     * Construct a list of reconciliation events staging to be created
     * @param conn  Database connection
     * @param tableName Source table name
     * @param filter    WHERE clause to be appended to SQL query
     * @param eventFinished Determine if child data needs to be added
     * @param actionDate For deferring events
     * @return List of events to be created
     * @throws SQLException 
     */
    public static List<InputData> constructReconciliationEventList(Connection conn, String tableName, String filter, Boolean eventFinished, Date actionDate) throws SQLException
    {
        List<InputData> allReconEvents = new ArrayList<InputData>();
        
        // SELECT SQL Query on source table
        String usersQuery = "SELECT * FROM " + tableName + (filter == null || "".equals(filter) ? "" : " " + filter);
        PreparedStatement ps = conn.prepareStatement(usersQuery);
        ResultSet rs = ps.executeQuery();

        // Get the result set metadata
        ResultSetMetaData rsmd = rs.getMetaData();
        int columnCount = rsmd.getColumnCount();
        LOGGER.log(ODLLevel.NOTIFICATION, "Column count: {0}", new Object[]{columnCount});

        // Iterate each record
        while(rs.next())
        {
            // Store recon event data 
            HashMap<String, Serializable> reconEventData = new HashMap<String, Serializable>();

            // Iterate recon attribute lookup and populate map accordingly
            for(int i = 1; i <= columnCount; i++)
            {
                String reconFieldName = rsmd.getColumnName(i); // Get column name
                String value = rs.getString(reconFieldName); // Get column value
                reconEventData.put(reconFieldName, value);
            }

            LOGGER.log(ODLLevel.NOTIFICATION, "Recon Event Data: {0}", new Object[]{reconEventData});
            InputData event = new InputData(reconEventData, null, eventFinished, ChangeType.CHANGELOG, actionDate);

            // Add recon event to list
            allReconEvents.add(event);
        }
        
        return allReconEvents;
    }
    
    /**
     * Create list of reconciliation events using data from a flat file
     * @param reconOps  OIM Reconciliation Service
     * @throws FileNotFoundException
     * @throws IOException
     * @throws Exception 
     */
    public static void flatFileFeed(ReconOperationsService reconOps) throws FileNotFoundException, IOException, Exception
    {
        BufferedReader bReader = null;
                
        // Input Parameters 
        String csvFilePath = "/home/oracle/Desktop/psft_hrms_users.csv";
        String delimiter = ",";
        String profileName = "Peoplesoft HRMS"; // Reconciliation Profile Name - Resource Object Name

        try
        {
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
        }
        
        finally
        {
            if(bReader != null)
            {
                bReader.close();
            }
        } 
    }
}