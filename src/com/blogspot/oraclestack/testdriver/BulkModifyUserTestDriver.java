package com.blogspot.oraclestack.testdriver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.api.UserManagerConstants;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.platform.OIMClient;

/**
 * Test Driver for BulkModifyUserEHPostProcess class
 * @author rayedchan
 */
public class BulkModifyUserTestDriver
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
            
            // Get User Manager API Service
            UserManager usrMgr = oimClient.getService(UserManager.class);
            
            // Stage bulk modification data - Note user attribute must be have "Bulk Update" set to true.
            HashMap<String,Object> modAttrs = new HashMap<String,Object>();
            //modAttrs.put(UserManagerConstants.AttributeName.EMPTYPE.getId(), "EMP"); // Set User Type to Employee
            modAttrs.put(UserManagerConstants.AttributeName.EMPTYPE.getId(), "Full-Time");
            //modAttrs.put(UserManagerConstants.AttributeName.USERTYPE.getId(), "End-User Administrator");
            //modAttrs.put(UserManagerConstants.AttributeName.USERTYPE.getId(), "End-User");
            ArrayList<String> userIds = new ArrayList<String>();
            userIds.add("DDUMA");
            userIds.add("BLEVANDOWSKI");
            userIds.add("KCHESTER");

            // Perform bulk modifications across multiple users
            boolean useUserLogin = true;
            usrMgr.modify(userIds, modAttrs, useUserLogin);
           
            // Single user modification
            //User modUser = new User("DDUMA");
            //modUser.setManagerKey("71");
            //modUser.setEmployeeType("Consultant");
            //modUser.setFirstName("Dan");
            //usrMgr.modify("User Login", "DDUMA" , modUser);
            
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
