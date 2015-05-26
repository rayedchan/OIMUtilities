package com.blogspot.oraclestack.testdriver;

import com.blogspot.oraclestack.utilities.ProvisioningUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.platform.OIMClient;
import oracle.iam.provisioning.vo.ChildTableRecord.ACTION;
import oracle.iam.provisioning.vo.ChildTableRecord;

/**
 * Test Driver fro Provisioning Utilities
 * @author rayedchan
 */
public class ProvisioningTestDriver 
{
    // Logger 
    private static final ODLLogger logger = ODLLogger.getODLLogger(ProvisioningTestDriver.class.getName());
    
    // Adjust constant variables according to you OIM environment
    public static final String OIM_HOSTNAME = "localhost";
    public static final String OIM_PORT = "14000"; // For SSL, use 14001; For non-SSL, use 14000
    public static final String OIM_PROVIDER_URL = "t3://"+ OIM_HOSTNAME + ":" + OIM_PORT; // For SSL, use t3s protocol; For non-SSL, use t3 protocol
    public static final String AUTHWL_PATH = "lib/config/authwl.conf";
    public static final String APPSERVER_TYPE = "wls";
    public static final String FACTORY_INITIAL_TYPE = "weblogic.jndi.WLInitialContextFactory";
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

            // Create an instance of OIMClient with OIM environment information 
            Hashtable<String, String> env = new Hashtable<String, String>();
            env.put(OIMClient.JAVA_NAMING_FACTORY_INITIAL, FACTORY_INITIAL_TYPE);
            env.put(OIMClient.JAVA_NAMING_PROVIDER_URL, OIM_PROVIDER_URL);
            
            // Establish an OIM Client
            oimClient = new OIMClient(env);
            
            // Login to OIM with System Administrator Credentials
            oimClient.login(OIM_ADMIN_USERNAME, OIM_ADMIN_PASSWORD.toCharArray());
                        
            // Instaniate custom utility
            ProvisioningUtils provUtils = new ProvisioningUtils(oimClient);
            
            String userLogin = "TSWIFT"; // TODO: Change this with your user
            String appInstName = "DBATUser"; // TODO: Change this with application name
            String CHILD_PROCESS_FORM_NAME = "UD_DBATRO"; // TODO: Change with child process form name
            String CHILD_ATTRIBUTE_NAME = "UD_DBATRO_ROLE"; // TODO: Change with child attribute name
            
            // ==========================================
            // Data to populate the parent process form
            // ==========================================
            Map<String,Object> parentData = new HashMap<String,Object>();
            parentData.put("UD_DBATUSR_FIRST_NAME", "Taylor");
            parentData.put("UD_DBATUSR_MIDDLE_NAME", "Alison");
            parentData.put("UD_DBATUSR_LAST_NAME", "Swift");
            parentData.put("UD_DBATUSR_USER_ID", "TSWIFT");
            parentData.put("UD_DBATUSR_UNIQUE_ID", "TSWIFT");
            parentData.put("UD_DBATUSR_TITLE", "Celebrity");
            
            // ==========================================
            // Data to populate the child form
            // ==========================================
            Map<String, ArrayList<ChildTableRecord>> childData = new HashMap<String, ArrayList<ChildTableRecord>>();
            ArrayList<ChildTableRecord> modRecords = new ArrayList<ChildTableRecord>();
             
            // First child record
            HashMap<String,Object> addRecordData = new HashMap<String,Object>();
            addRecordData.put(CHILD_ATTRIBUTE_NAME, "Singer");
            ChildTableRecord addRecord = new ChildTableRecord();
            addRecord.setAction(ACTION.Add);
            addRecord.setChildData(addRecordData);
            modRecords.add(addRecord);
            
            // Second child record
            HashMap<String,Object> addRecordData2 = new HashMap<String,Object>();
            addRecordData2.put(CHILD_ATTRIBUTE_NAME, "Actress");
            ChildTableRecord addRecord2 = new ChildTableRecord();
            addRecord2.setAction(ACTION.Add);
            addRecord2.setChildData(addRecordData2);
            modRecords.add(addRecord2);
            
            // Wrapper for child data provisioning
            childData.put(CHILD_PROCESS_FORM_NAME, modRecords); // Put Child Form Name and its modified child data

            // ==========================================
            // Call custom method to provision account
            // ==========================================
            Long oiuKey = provUtils.provisionResourceAccountToUser(userLogin, appInstName, parentData, childData);
            System.out.println(oiuKey);
        }
        
        finally
        {
            if( oimClient != null)
            {
                oimClient.logout();
            } 
        }
    }
    
}
