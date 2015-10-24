package com.blogspot.oraclestack.testdriver;

import com.blogspot.oraclestack.utilities.EntitlementUtilities;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;
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
 * Test driver for resource account password reset.
 * @author rayedchan
 */
public class AccountPasswordResetTestDriver
{
    // Logger
    private static final ODLLogger LOG = ODLLogger.getODLLogger(AccountPasswordResetTestDriver.class.getName());
    
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
            ProvisioningService provService = oimClient.getService(ProvisioningService.class);
            
            String userLogin = "ASKYWALKER";
            boolean useUserLogin = true;
            HashSet<String> retAttrs = new HashSet<String>();
            retAttrs.add(UserManagerConstants.AttributeName.USER_KEY.getId()); // usr_key
            
            // Get user's key
            User user = usrMgr.getDetails(userLogin, retAttrs, useUserLogin);
            LOG.log(ODLLevel.INFO, "User: {0}", new Object[]{user});
            String userKey = user.getId(); // Get usr_key
            
            // Get user's resource accounts
            String resourcObjectName = "LDAP User"; 
            SearchCriteria criteria = new SearchCriteria(ProvisioningConstants.AccountSearchAttribute.OBJ_NAME.getId(), resourcObjectName, SearchCriteria.Operator.EQUAL);       
            List<Account> accounts = provService.getAccountsProvisionedToUser(userKey, criteria, null, useUserLogin);
            LOG.log(ODLLevel.INFO, "User''s Accounts: {0}", new Object[]{accounts});
            
            Account resourceAcct = accounts.get(0);
            String accountId = resourceAcct.getAccountID(); // oiu_key
            Long procInstFormKey = resourceAcct.getAppInstance().getAccountForm().getFormKey(); //
            LOG.log(ODLLevel.INFO, "Account Id: {0}", new Object[]{accountId});
            LOG.log(ODLLevel.INFO, "Process Instance Form Key: {0}", new Object[]{procInstFormKey});
            
            // Reset resource account password
            char[] newPassword = "LightSaber".toCharArray();
            provService.changeAccountPassword(Long.valueOf(accountId), newPassword);
            LOG.log(ODLLevel.INFO, "Successfully changed resource account password.");
            
            // Confirm resource account password; This checks if the password on the process form is identical to the supplied value
            boolean confirmAcctPwd =  provService.confirmAccountPassword(Long.valueOf(accountId), newPassword);
            LOG.log(ODLLevel.INFO, "Confirm Account Password? {0}", new Object[]{confirmAcctPwd});
            
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
