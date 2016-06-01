package com.blogspot.oraclestack.testdriver;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.api.UserManagerConstants;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.identity.usermgmt.vo.UserManagerResult;
import oracle.iam.platform.OIMClient;
import oracle.iam.platform.Platform;
import oracle.iam.platform.entitymgr.vo.SearchCriteria;
import oracle.iam.provisioning.api.ProvisioningConstants;
import oracle.iam.provisioning.api.ProvisioningService;
import oracle.iam.provisioning.vo.Account;

/**
 * A test driver for testing UserLockPreprocessEH event handler on single
 * and bulk lock operations.
 * @author rayedchan
 */
public class UserCreatePreprocessEHTestDriver 
{
    private static final ODLLogger LOGGER = ODLLogger.getODLLogger(UserCreatePreprocessEHTestDriver.class.getName());
    
    
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
            ProvisioningService provService = oimClient.getService(ProvisioningService.class);
            
            String userLogin = "ASKYWALKER";
            String userKey = usrMgr.getDetails(userLogin, null, true).getId();
           
            /*User newUser = new User(userLogin);
            newUser.setAttribute(UserManagerConstants.AttributeName.LASTNAME.getId(), "Rhodes");
            newUser.setAttribute(UserManagerConstants.AttributeName.FIRSTNAME.getId(), "James");
            newUser.setAttribute(UserManagerConstants.AttributeName.USER_LOGIN.getId(), userLogin);;
            newUser.setOrganizationKey("1"); // ACT_KEY; Xellerate Users
            newUser.setUserType("End-User");
            newUser.setEmployeeType("EMP");*/
            
            // Single user lock operation
            //UserManagerResult result = usrMgr.create(newUser);
            UserManagerResult result = usrMgr.lock(userLogin, true);
            System.out.println("Entity ID: " + result.getEntityId());
            System.out.println("Failed Results: " + result.getFailedResults());
            System.out.println("Status: " + result.getStatus());
            System.out.println("Succeeded Results: " + result.getSucceededResults());
            System.out.println();
            
            /*SearchCriteria resourceObjectsCriteria = new SearchCriteria(ProvisioningConstants.AccountSearchAttribute.DISPLAY_NAME.getId(), "OpenLDAP", SearchCriteria.Operator.EQUAL);     
            List<Account> accounts = provService.getAccountsProvisionedToUser(userKey, resourceObjectsCriteria, null, true);
            
             // Iterate User's accounts of a specific resource object
            for(Account resourceAcct: accounts)
            {
               String accountId = resourceAcct.getAccountID(); // OIU_KEY
               String procInstFormKey = resourceAcct.getProcessInstanceKey(); // (ORC_KEY) Process Form Instance Key 
               String appInstName = resourceAcct.getAppInstance().getApplicationInstanceName(); // Application Instance Name
               String resourceObjectName = resourceAcct.getAppInstance().getObjectName(); // Resource Object Name
               LOGGER.log(ODLLevel.INFO, "Account Id: {0}", new Object[]{accountId});
               LOGGER.log(ODLLevel.INFO, "Process Instance Form Key: {0}", new Object[]{procInstFormKey});
               LOGGER.log(ODLLevel.INFO, "Application Instance Name: {0}", new Object[]{appInstName});
               LOGGER.log(ODLLevel.INFO, "Object Name: {0}", new Object[]{resourceObjectName});

            }*/
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
