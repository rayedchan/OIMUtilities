package com.blogspot.oraclestack.testdriver;

import Thor.API.Exceptions.tcAPIException;
import Thor.API.Operations.tcUserOperationsIntf;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import javax.security.auth.login.LoginException;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.platform.OIMClient;
import oracle.iam.platform.authopss.exception.AccessDeniedException;
import oracle.iam.platform.entitymgr.vo.SearchCriteria;
import oracle.iam.provisioning.api.ProvisioningConstants;
import oracle.iam.provisioning.api.ProvisioningService;
import oracle.iam.provisioning.exception.AccountNotFoundException;
import oracle.iam.provisioning.exception.GenericProvisioningException;
import oracle.iam.provisioning.vo.Account;

/**
 * Converts all resource accounts of a single application instance into service accounts.
 * @author rayedchan
 */
public class ResourceServiceAccountTestDriver 
{
    private static final ODLLogger LOGGER = ODLLogger.getODLLogger(ResourceServiceAccountTestDriver.class.getName());
    
    // Adjust constant variables according to you OIM environment
    public static final String OIM_HOSTNAME = "localhost";
    public static final String OIM_PORT = "14000"; // For SSL, use 14001; For non-SSL, use 14000
    public static final String OIM_PROVIDER_URL = "t3://"+ OIM_HOSTNAME + ":" + OIM_PORT; // For SSL, use t3s protocol; For non-SSL, use t3 protocol
    public static final String AUTHWL_PATH = "lib/config/authwl.conf";
    public static final String APPSERVER_TYPE = "wls";
    public static final String FACTORY_INITIAL_TYPE = "weblogic.jndi.WLInitialContextFactory";
    public static final String OIM_ADMIN_USERNAME = "xelsysadm";
    public static final String OIM_ADMIN_PASSWORD = "Password1";
    public static final String JAR_PATH = "/home/oracle/NetBeansProjects/OIMUtilities/dist/OIMUtilities.jar"; // Absolute Path of JAR file on machine where OIM is running
    
    public static void main(String[] args) throws LoginException, GenericProvisioningException, AccessDeniedException, AccountNotFoundException, tcAPIException
    {
        OIMClient oimClient = null;
        tcUserOperationsIntf usrOps = null;
        
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
            
            // Get OIM Services
            ProvisioningService provService = oimClient.getService(ProvisioningService.class);
            usrOps = oimClient.getService(tcUserOperationsIntf.class);
            
            // TODO: Change accordingly
            String appInstName = "BadgeAccess";
            Account.ACCOUNT_TYPE accountType = Account.ACCOUNT_TYPE.ServiceAccount;
            
            // Make call to change the account type on all accounts of a specific application instance
            changeAccountTypeOnAllAccounts(provService, usrOps, appInstName, accountType);
        }
        
        finally
        {
            if( usrOps != null)
            {
                usrOps.close();
            }
                        
            if( oimClient != null)
            {
                oimClient.logout();
            } 
        }
        
    }
    
    /**
     * Change the account type on all resource accounts of an application instance.
     * @param provService   Provisioning API Service
     * @param usrOps        tcUserOperationsIntf API service
     * @param appInstName   Application Instance name 
     * @param accountType   Account type to change to
     * @throws GenericProvisioningException
     * @throws tcAPIException 
     */
    private static void changeAccountTypeOnAllAccounts(ProvisioningService provService, tcUserOperationsIntf usrOps, String appInstName, Account.ACCOUNT_TYPE accountType) throws GenericProvisioningException, tcAPIException
    {
         // Filter for accounts with status "Provisioned"
        SearchCriteria provisonedCriterion = new SearchCriteria(ProvisioningConstants.AccountSearchAttribute.ACCOUNT_STATUS.getId(), ProvisioningConstants.ObjectStatus.PROVISIONED.getId(), SearchCriteria.Operator.EQUAL);

        // Filter for accounts that are not account type you are changing to
        SearchCriteria typeCriterion = new SearchCriteria(ProvisioningConstants.AccountSearchAttribute.ACCOUNT_TYPE.getId(), accountType.getId(), SearchCriteria.Operator.NOT_EQUAL);

        // Combine criteria: Not account type changing to and is provisioned
        SearchCriteria criteria = new SearchCriteria(typeCriterion, provisonedCriterion, SearchCriteria.Operator.AND);

        // Get resource accounts based on criteria
        HashMap<String,Object> configParams = new HashMap<String,Object>();
        List<Account> accounts = provService.getProvisionedAccountsForAppInstance(appInstName, criteria, configParams);

        // Iterate accounts
        for(Account account: accounts)
        {
            String usrKey = account.getUserKey();
            String procInstKey = account.getProcessInstanceKey();
            String oiuKey = account.getAccountID();
            String curAccountType = account.getAccountType().getId();
            LOGGER.log(ODLLevel.NOTIFICATION, "User Key: {2}, Process Instance Key: {0}, OIU Key = {1}, Account = {3}", new Object[]{procInstKey, oiuKey, usrKey,curAccountType});

            if(Account.ACCOUNT_TYPE.ServiceAccount.getId().equals(accountType.getId()))
            {
                // Convert to service account
                usrOps.changeToServiceAccount(Long.parseLong(oiuKey));
            }

            else if(curAccountType.equals(Account.ACCOUNT_TYPE.ServiceAccount.getId()))
            {
                // Convert service account to regular account
                usrOps.changeFromServiceAccount(Long.parseLong(oiuKey));
            }
        }
    }
    
}
