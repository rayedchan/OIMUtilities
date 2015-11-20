package com.blogspot.oraclestack.testdriver;

import Thor.API.Exceptions.tcAPIException;
import Thor.API.Exceptions.tcColumnNotFoundException;
import Thor.API.Operations.TaskDefinitionOperationsIntf;
import Thor.API.Operations.tcProvisioningOperationsIntf;
import Thor.API.tcResultSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.api.UserManagerConstants;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.platform.OIMClient;
import oracle.iam.platform.entitymgr.vo.SearchCriteria;
import oracle.iam.provisioning.api.ProvisioningConstants;
import oracle.iam.provisioning.api.ProvisioningService;
import oracle.iam.provisioning.vo.Account;

/**
 * Uses OIM APIs to manually call a process task instance on a given application
 * instance on a user.
 * @author rayedchan
 */
public class CallProvisioningTaskTestDriver 
{
    // Logger
    private static final ODLLogger LOG = ODLLogger.getODLLogger(CallProvisioningTaskTestDriver.class.getName());
    
    // Change variables accordingly
    public static final String OIM_HOSTNAME = "localhost";
    public static final String OIM_PORT = "14000"; // For SSL, use 14001; For non-SSL, use 14000
    public static final String OIM_PROVIDER_URL = "t3://"+ OIM_HOSTNAME + ":" + OIM_PORT; // For SSL, use t3s protocol; For non-SSL, use t3 protocol
    public static final String AUTHWL_PATH = "lib/config/authwl.conf";
    public static final String APPSERVER_TYPE = "wls"; // WebLogic Server
    public static final String FACTORY_INITIAL_TYPE = "weblogic.jndi.WLInitialContextFactory";
    public static final String OIM_ADMIN_USERNAME = "xelsysadm";
    public static final String OIM_ADMIN_PASSWORD = "Password1";
    
    public static void main(String[] args)
    {        
        // OIM Client
        OIMClient oimClient = null;
        tcProvisioningOperationsIntf provOps = null;
        TaskDefinitionOperationsIntf taskDefOps =null; 
        
        try
        {
            // Set system properties required for OIMClient
            System.setProperty("java.security.auth.login.config", AUTHWL_PATH);
            System.setProperty("APPSERVER_TYPE", APPSERVER_TYPE);

            // Create an instance of OIMClient with OIM environment information 
            Hashtable<String, String> env = new Hashtable<String, String>();
            env.put(OIMClient.JAVA_NAMING_FACTORY_INITIAL, FACTORY_INITIAL_TYPE);
            env.put(OIMClient.JAVA_NAMING_PROVIDER_URL, OIM_PROVIDER_URL);
            
            // Establish an OIM Client
            oimClient = new OIMClient(env);
            
            // Login to OIM with System Administrator Credentials
            oimClient.login(OIM_ADMIN_USERNAME, OIM_ADMIN_PASSWORD.toCharArray());
            
            // Get API Service
            UserManager usrMgr = oimClient.getService(UserManager.class);
            ProvisioningService provService = oimClient.getService(ProvisioningService.class);
            provOps = oimClient.getService(tcProvisioningOperationsIntf.class);
            taskDefOps = oimClient.getService(TaskDefinitionOperationsIntf.class);
            
            // Change variables accordingly
            String userLogin = "LSTILLMAN"; // OIM User Login
            String resourceObjectName = "LDAP User";  // Resource Object Name
            String procTaskName = "Set Department To Some Value";
                                                
            // Get user's details
            boolean useUserLogin = true;
            HashSet<String> retAttrs = new HashSet<String>();
            retAttrs.add(UserManagerConstants.AttributeName.USER_KEY.getId()); // usr_key
            User user = usrMgr.getDetails(userLogin, retAttrs, useUserLogin);
            LOG.log(ODLLevel.INFO, "User: {0}", new Object[]{user});
            String userKey = user.getId(); // Get usr_key

            // Get user's resource accounts for a specific resource object
            SearchCriteria criteria = new SearchCriteria(ProvisioningConstants.AccountSearchAttribute.OBJ_NAME.getId(), resourceObjectName, SearchCriteria.Operator.EQUAL);       
            List<Account> accounts = provService.getAccountsProvisionedToUser(userKey, criteria, null, useUserLogin);
            LOG.log(ODLLevel.INFO, "Total Accounts: {0}", new Object[]{accounts.size()});
            
            // Iterate User's accounts of a specific resource object
            for(Account resourceAcct: accounts)
            {
                String accountId = resourceAcct.getAccountID(); // OIU_KEY
                String procInstFormKey = resourceAcct.getProcessInstanceKey(); // (ORC_KEY) Process Form Instance Key 
                String appInstName = resourceAcct.getAppInstance().getApplicationInstanceName(); // Application Instance Name
                LOG.log(ODLLevel.INFO, "Account Id: {0}", new Object[]{accountId});
                LOG.log(ODLLevel.INFO, "Process Instance Form Key: {0}", new Object[]{procInstFormKey});
                LOG.log(ODLLevel.INFO, "Application Instance Name: {0}", new Object[]{appInstName});
                
                // Get a specific process task 
                HashMap<String,String> filter = new HashMap<String,String>();
                filter.put("Process Definition.Tasks.Task Name", procTaskName);
                tcResultSet results = taskDefOps.getTaskDetail(Long.valueOf(procInstFormKey), filter);
                int rows = results.getTotalRowCount();
                String procDefTaskKey = null;
                
                // Should only be one since Process Task Name is unique
                for(int i = 0; i < rows; i++)
                {
                    results.goToRow(i);
                    procDefTaskKey = results.getStringValue("Process Definition.Tasks.Key"); // MIL_KEY
                }
                
                LOG.log(ODLLevel.INFO, "Process Definition Task Key: {0}", new Object[]{procDefTaskKey});
                
                if(procDefTaskKey != null)
                {         
                    // Call a process task directly on an application instance
                    long schKey = provOps.addProcessTaskInstance(Long.valueOf(procDefTaskKey), Long.valueOf(procInstFormKey));
                    LOG.log(ODLLevel.INFO, "Process Task Instance Key: {0}", new Object[]{schKey});
                }
            }
        } 
        
        catch (Exception ex) 
        {
            LOG.log(ODLLevel.SEVERE, "", ex);
        } 
        
        finally
        {
            // Close tc* service
            if(provOps != null)
            {
                provOps.close();
            }
            
            if(taskDefOps != null)
            {
                taskDefOps.close();
            }
            
            // Logout user from OIM client
            if( oimClient != null)
            {
                oimClient.logout();
            } 
        }
    }
    
    /**
     * Prints the records of a tcResultSet.
     * @param  tcResultSetObj  tcResultSetObject
     * @throws tcAPIException
     * @throws tcColumnNotFoundException 
     */
    public static void printTcResultSetRecords(tcResultSet tcResultSetObj) throws tcAPIException, tcColumnNotFoundException
    {
        String[] columnNames = tcResultSetObj.getColumnNames();
        int numRows = tcResultSetObj.getTotalRowCount();
        
        for(int i = 0; i < numRows; i++)
        {
            HashMap<String,String> record = new HashMap<String,String>();
            tcResultSetObj.goToRow(i);
            for(String columnName: columnNames)
            {
                record.put(columnName, tcResultSetObj.getStringValue(columnName));
            }
            
            LOG.log(ODLLevel.INFO, "{0}", new Object[]{record});
        }
    }
}
