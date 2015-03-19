package com.blogspot.oraclestack.testdriver;

import com.blogspot.oraclestack.utilities.ReconciliationEvents;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import oracle.iam.platform.OIMClient;

/**
 * Test driver for ReconciliationEvents class.
 * @author rayedchan
 */
public class ReconciliationEventsTestDriver 
{
     // Adjust constant variables according to you OIM environment
    public static final String OIM_HOSTNAME = "localhost";
    public static final String OIM_PORT = "14000"; // For SSL, use 14001; For non-SSL, use 14000
    public static final String OIM_PROVIDER_URL = "t3://"+ OIM_HOSTNAME + ":" + OIM_PORT; // For SSL, use t3s protocol; For non-SSL, use t3 protocol
    public static final String AUTHWL_PATH = "lib/config/authwl.conf";
    public static final String APPSERVER_TYPE = "wls";
    public static final String FACTORY_INITIAL_TYPE = "weblogic.jndi.WLInitialContextFactory";
    public static final String OIM_ADMIN_USERNAME = "xelsysadm";
    public static final String OIM_ADMIN_PASSWORD = "Password1";
    
    // Adjust input
    public static final String INPUT_RESOURCE_OBJECT = "DBAT User";
    public static final String INPUT_IT_RESOURCE = "DBAT";
    
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
        
            // Test reconciliation event utility
            ReconciliationEvents reconEvtUtil = new ReconciliationEvents(oimClient);
            
            // Stage reconciliation parent data; maps to reconciliation field on resource object
            HashMap<String,Object> reconData = new HashMap<String,Object>(); // Key = Recon Field Name, Value = data
            
            // Stage reconciliation child data
            HashMap<String,ArrayList<HashMap<String,Object>>> childReconData = new HashMap<String,ArrayList<HashMap<String,Object>>>(); // {Key = Child Recon Field Map Name, Value = {Key = Child Recon Field Name, Value = data}} 
            ArrayList<HashMap<String,Object>> childRoleEntries = new ArrayList<HashMap<String,Object>>();
            HashMap<String,Object> childTableRoleData1 = new HashMap<String, Object>();
            HashMap<String,Object> childTableRoleData2 = new HashMap<String, Object>();
            ArrayList<HashMap<String,Object>> childGroupEntries = new ArrayList<HashMap<String,Object>>();
            HashMap<String,Object> childTableGroupData1 = new HashMap<String, Object>();
            HashMap<String,Object> childTableGroupData2 = new HashMap<String, Object>();
            
            // DBAT Target
            reconData.put("Unique Id", "DDUMA"); // __UID__ attribute
            reconData.put("User Id", "DDUMA"); // __NAME__ attribute
            reconData.put("Status", "Enabled"); // __ENABLE__
            reconData.put("IT Resource Name", INPUT_IT_RESOURCE);
            reconData.put("Middle Name", "B"); 
            
            childTableRoleData1.put("Role", "Engineer");
            childTableRoleData2.put("Role", "Developer");
            childRoleEntries.add(childTableRoleData1);
            childRoleEntries.add(childTableRoleData2);
            
            childTableGroupData1.put("Group Name", "Fresh");
            childTableGroupData2.put("Group Name", "Rubix");
            childGroupEntries.add(childTableGroupData1);
            childGroupEntries.add(childTableGroupData2);
            
            childReconData.put("RO", childRoleEntries);
            childReconData.put("GRP", childGroupEntries);
            
            // AD Trusted; No IT Resource needed
            /*reconData.put("objectGUID", "NTAYLOR"); // __UID__ attribute
            reconData.put("User Id", "NTAYLOR"); // __NAME__ attribute
            reconData.put("TrustedStatus", "Disabled"); // __ENABLE__
            reconData.put("Last Name", "Taylor"); 
            reconData.put("Employee Type", "Full-Time"); 
            reconData.put("Organization", "Xellerate Users"); 
            reconData.put("Middle Name", "A"); 
            reconData.put("First Name", "Nick");*/
            
            // Create a reconciliation event and process it
            //reconEvtUtil.makeReconciliationEvent(INPUT_RESOURCE_OBJECT, reconData);
            
            // Create a reconciliation event and process it
            reconEvtUtil.makeReconciliationEventWithChildData(INPUT_RESOURCE_OBJECT, reconData, childReconData);
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
