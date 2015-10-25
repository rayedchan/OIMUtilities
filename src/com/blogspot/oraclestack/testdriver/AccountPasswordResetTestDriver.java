package com.blogspot.oraclestack.testdriver;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.api.UserManagerConstants;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.passwordmgmt.api.PasswordMgmtService;
import oracle.iam.passwordmgmt.vo.ValidationResult;
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
    
    // Change variables accordingly
    public static final String OIM_HOSTNAME = "localhost";
    public static final String OIM_PORT = "14000"; // For SSL, use 14001; For non-SSL, use 14000
    public static final String OIM_PROVIDER_URL = "t3://"+ OIM_HOSTNAME + ":" + OIM_PORT; // For SSL, use t3s protocol; For non-SSL, use t3 protocol
    public static final String AUTHWL_PATH = "lib/config/authwl.conf";
    public static final String APPSERVER_TYPE = "wls"; // WebLogic Server
    public static final String FACTORY_INITIAL_TYPE = "weblogic.jndi.WLInitialContextFactory";
    public static final String OIM_ADMIN_USERNAME = "xelsysadm";
    public static final String OIM_ADMIN_PASSWORD = "Password1";
    
    public static void main(String[] args) throws Exception
    {        
        // OIM Client
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
            
            // Get API Service
            UserManager usrMgr = oimClient.getService(UserManager.class);
            ProvisioningService provService = oimClient.getService(ProvisioningService.class);
            PasswordMgmtService pwdMgmtService = oimClient.getService(PasswordMgmtService.class); 
            
            // Change variable values accordinly
            String userLogin = "ASKYWALKER"; // OIM User Login
            String resourceObjectName = "LDAP User";  // Resource Object Name
            char[] newPassword = "#LightSaber7".toCharArray();
            
            // Get user's details
            boolean useUserLogin = true;
            HashSet<String> retAttrs = new HashSet<String>();
            retAttrs.add(UserManagerConstants.AttributeName.USER_KEY.getId()); // usr_key
            User user = usrMgr.getDetails(userLogin, retAttrs, useUserLogin);
            LOG.log(ODLLevel.INFO, "User: {0}", new Object[]{user});
            String userKey = user.getId(); // Get usr_key

            // Get user's resource accounts
            SearchCriteria criteria = new SearchCriteria(ProvisioningConstants.AccountSearchAttribute.OBJ_NAME.getId(), resourceObjectName, SearchCriteria.Operator.EQUAL);       
            List<Account> accounts = provService.getAccountsProvisionedToUser(userKey, criteria, null, useUserLogin);
            LOG.log(ODLLevel.INFO, "User''s Accounts: {0}", new Object[]{accounts});
            Account resourceAcct = accounts.isEmpty() ? null : accounts.get(0); // Grab first item

            if(resourceAcct != null)
            {        
                String accountId = resourceAcct.getAccountID(); // oiu_key
                String procInstFormKey = resourceAcct.getProcessInstanceKey(); // Process Form Instance Key
                String appInstName = resourceAcct.getAppInstance().getApplicationInstanceName();
                LOG.log(ODLLevel.INFO, "Account Id: {0}", new Object[]{accountId});
                LOG.log(ODLLevel.INFO, "Process Instance Form Key: {0}", new Object[]{procInstFormKey});
                LOG.log(ODLLevel.INFO, "Application Instance Name: {0}", new Object[]{appInstName});
                
                // Validate new password against account password policy
                ValidationResult  vr = pwdMgmtService.validatePasswordAgainstPolicy(newPassword, user, appInstName, Locale.getDefault());
                boolean isNewPasswordValid = vr.isPasswordValid();
                LOG.log(ODLLevel.INFO, "Passes Account Password Policy? {0}", new Object[]{isNewPasswordValid});
                
                // Perfrom account password change if account password policy passes
                if(isNewPasswordValid)
                {
                    // TODO: Account Password History being bypassed in validatePasswordAgainstPolicy. 
                    
                    // Change resource account password
                    provService.changeAccountPassword(Long.valueOf(accountId), newPassword);
                    LOG.log(ODLLevel.INFO, "Successfully changed resource account password.");

                    // Confirm resource account password; This checks if the password on the process form is identical to the supplied value
                    boolean confirmAcctPwd =  provService.confirmAccountPassword(Long.valueOf(accountId), newPassword);
                    LOG.log(ODLLevel.INFO, "Confirm Account Password? {0}", new Object[]{confirmAcctPwd});
                }
            }
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
