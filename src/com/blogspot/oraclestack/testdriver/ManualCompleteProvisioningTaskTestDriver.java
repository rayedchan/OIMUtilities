package com.blogspot.oraclestack.testdriver;

import Thor.API.Exceptions.tcAPIException;
import Thor.API.Exceptions.tcAwaitingApprovalDataCompletionException;
import Thor.API.Exceptions.tcAwaitingObjectDataCompletionException;
import Thor.API.Exceptions.tcBulkException;
import Thor.API.Exceptions.tcColumnNotFoundException;
import Thor.API.Exceptions.tcStaleDataUpdateException;
import Thor.API.Exceptions.tcTaskNotFoundException;
import Thor.API.Operations.tcProvisioningOperationsIntf;
import Thor.API.Security.XLClientSecurityAssociation;
import com.thortech.xl.client.dataobj.tcDataBaseClient;
import com.thortech.xl.dataaccess.tcDataProvider;
import com.thortech.xl.dataaccess.tcDataSet;
import com.thortech.xl.dataaccess.tcDataSetException;
import com.thortech.xl.dataobj.PreparedStatementUtil;
import com.thortech.xl.orb.dataaccess.tcDataAccessException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.platform.OIMClient;
import Thor.API.tcResultSet;

/**
 * Test Driver to manual complete provisioning tasks.
 * @author rayedchan
 */
public class ManualCompleteProvisioningTaskTestDriver 
{
    // LOGGER
    public static final ODLLogger LOGGER = ODLLogger.getODLLogger(ManualCompleteProvisioningTaskTestDriver.class.getName());
    
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
            
            // Manual complete provisioning tasks for a given process task in a specific process definition
            String processDefinationName = "LDAP User"; // PKG.PKG_NAME
            String processTaskName = "Disable User"; // MIL.MIL_NAME
            manualCompleteProvisioningTasks(processDefinationName, processTaskName, oimClient); // Call helper method
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
     * Manually complete provisioning tasks for a given process definition.
     * When a task is marked for manual completion, the value for SCH.SCH_STATUS 
     * is changed to 'MC'.
     * @param processDefinitionName     PKG.PKG_NAME
     * @param processTaskName           MIL.MIL_NAME
     * @param oimClient                 OIM Client
     * @throws tcDataSetException
     * @throws tcDataAccessException
     * @throws tcTaskNotFoundException
     * @throws tcBulkException
     * @throws tcAPIException
     */
   public static void manualCompleteProvisioningTasks(String processDefinitionName, String processTaskName, OIMClient oimClient) throws tcDataSetException, tcDataAccessException, tcTaskNotFoundException, tcBulkException, tcAPIException, tcColumnNotFoundException, tcAwaitingObjectDataCompletionException, tcAwaitingApprovalDataCompletionException, tcStaleDataUpdateException
   {
       tcProvisioningOperationsIntf provOps = null;
       
       try
       {
           // Get OIM tcProvisioningOperations service
           provOps = oimClient.getService(tcProvisioningOperationsIntf.class);
           
           // Establish database connection to OIM Schema through the OIMClient
           XLClientSecurityAssociation.setClientHandle(oimClient);
           tcDataProvider dbProvider = new tcDataBaseClient();
           
           // Setup query to fetch 'Rejected', 'Pending', and 'Uncompleted' provisioning tasks for a given process task and process definition
           String query = "select sch.sch_key from sch inner join osi on sch.sch_key = osi.sch_key inner join mil on osi.mil_key = mil.mil_key inner join pkg on pkg.pkg_key = osi.pkg_key where pkg.pkg_name = ? and mil.mil_name = ?  and sch_status in ('R', 'P', 'UC') order by sch.sch_key";
           PreparedStatementUtil ps = new PreparedStatementUtil();
           ps.setStatement(dbProvider, query);
           ps.setString(1, processDefinitionName); // PKG_NAME
           ps.setString(2, processTaskName); // MIL_NAME
           ps.execute(); // Execute query
           LOGGER.log(ODLLevel.NOTIFICATION, "Executed Statement: {0}", new Object[]{ps.getStatement()});
         
           // Provisioning Tasks Result set
           tcDataSet tasksDataSet = ps.getDataSet();
           int numRecords = tasksDataSet.getTotalRowCount();
           long[] schKeys = new long[numRecords];
           LOGGER.log(ODLLevel.NOTIFICATION, "Total Provisioning Tasks to Update: {0}", new Object[]{numRecords});
           
           // Iterate through each record in result set
           for(int i = 0; i < numRecords; i++)
           {               
               tasksDataSet.goToRow(i); // Move cursor to next record in result set
               Long id = tasksDataSet.getLong("sch_key"); // Get key from record
               schKeys[i] = id; // Add to array
               LOGGER.log(ODLLevel.NOTIFICATION, "Provisioning Task ID: {0}", new Object[]{id});
               
               tcResultSet rs = provOps.getProvisioningTaskDetails(id);
               printTcResultSetRecords(rs);
             
               byte[] taskInstanceRowVer = rs.getByteArrayValue("Process Instance.Task Details.Row Version");
               HashMap<String,Object> phAttributeList = new HashMap<String,Object>();
               phAttributeList.put("Process Instance.Task Details.Status", "Success");
               provOps.updateTask(id, taskInstanceRowVer, phAttributeList);
               break;
           }
           
           LOGGER.log(ODLLevel.NOTIFICATION, "Provisioning Tasks to Complete: {0}", new Object[]{Arrays.toString(schKeys)});
           //provOps.setTasksCompletedManually(schKeys); // Bulk Manual Complete provisioning tasks
       }
       
       finally 
       {
           // Close tc* service
           if(provOps != null) 
           {
               provOps.close();   
           }
           
           // Clear session
           XLClientSecurityAssociation.clearThreadLoginSession();
       }
   }
   
    /**
     * Prints the records of a tcResultSet.
     * @param   tcResultSetObj  tcResultSetObject
     */
    public static void printTcResultSetRecords(tcResultSet tcResultSetObj) throws tcAPIException, tcColumnNotFoundException
    {
        String[] columnNames = tcResultSetObj.getColumnNames();
        int numRows = tcResultSetObj.getTotalRowCount();
        
        for(int i = 0; i < numRows; i++)
        {
            tcResultSetObj.goToRow(i);
            for(String columnName: columnNames)
            {
                System.out.println(columnName + " = " + tcResultSetObj.getStringValue(columnName));
            }
            System.out.println();
        }
    }
    
    /**
     * Converts tcResultSet to a Map.
     * @param   tcResultSetObj  tcResultSetObject
     */
    public static HashMap<String,String> covertTcResultSetToMap(tcResultSet tcResultSetObj) throws tcAPIException, tcColumnNotFoundException
    {
        String[] columnNames = tcResultSetObj.getColumnNames();
        int numRows = tcResultSetObj.getTotalRowCount();
        HashMap<String,String> map = new HashMap<String,String>();
        
        for(int i = 0; i < numRows; i++)
        {
            tcResultSetObj.goToRow(i);
            for(String columnName: columnNames)
            {
                map.put(columnName, tcResultSetObj.getStringValue(columnName));
            }
        }
        
        return map;
    }
}
