package com.blogspot.oraclestack.testdriver;

import java.util.ArrayList;
import java.util.Hashtable;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.vo.UserManagerResult;
import oracle.iam.platform.OIMClient;

/**
 * A test driver for testing UserLockPreprocessEH event handler on single
 * and bulk lock operations.
 * @author rayedchan
 */
public class UserLockPreprocessEHTestDriver 
{
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
            
            // Get OIM services
            UserManager usrMgr = oimClient.getService(UserManager.class);
        
            boolean isUserLogin = true;
            
            // Single user lock operation
            String userLogin = "PPARKER";
            UserManagerResult result = usrMgr.lock(userLogin, isUserLogin);
            System.out.println("Entity ID: " + result.getEntityId());
            System.out.println("Failed Results: " + result.getFailedResults());
            System.out.println("Status: " + result.getStatus());
            System.out.println("Succeeded Results: " + result.getSucceededResults());
            System.out.println();
            
            // Bulk user lock operation
            ArrayList<String> userLogins = new ArrayList<String>();
            userLogins.add("FHARDY");
            userLogins.add("HOSBORN");
            UserManagerResult bulkResults = usrMgr.lock(userLogins, isUserLogin);
            System.out.println("Entity ID: " + bulkResults.getEntityId());
            System.out.println("Failed Results: " + bulkResults.getFailedResults());
            System.out.println("Status: " + bulkResults.getStatus());
            System.out.println("Succeeded Results: " + bulkResults.getSucceededResults());
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
}
