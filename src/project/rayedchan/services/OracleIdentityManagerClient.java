package project.rayedchan.services;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import javax.security.auth.login.LoginException;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.identity.exception.UserSearchException;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.platform.OIMClient;
import oracle.iam.platform.authz.exception.AccessDeniedException;
import oracle.iam.platform.entitymgr.vo.SearchCriteria;

/**
 * This class uses the OIMClient to access the API services of an Oracle
 * Identity Manager environment.
 * @author rayedchan
 */
public class OracleIdentityManagerClient 
{
    // Logger
    public static ODLLogger logger = ODLLogger.getODLLogger(OracleIdentityManagerClient.class.getName());
    
    // Instance Variables
    private OIMClient oimClient; // OIM Client to use API services
    
    /**
     * This constructor initializes the OIMClient by logging in as an
     * OIM Identity. A system administrator is necessary to perform
     * all the OIM API services.
     * @param username               User Login of the OIM Identity
     * @param password               Plain text password of the OIM Identity
     * @param authwlPath             Path to "authwl.conf" file. This can be found in "$MW_HOME/Oracle_IDM1/designconsole/config".
     * @param appServerType          Type of application server OIM is deployed on. For WebLogic, this value should be "wls".
     * @param factoryInitialType     Type of factory initial. For WebLogic, this value should be "weblogic.jndi.WLInitialContextFactory".
     * @param oimProviderURL         The OIM provider URL. For non-SSL protocol, the value should be "t3://<oimhostname>:<oimport>". For SSL protocol, the value should be "t3s://<oimhostname>:<oimport>".
     * @param isSSL                  Set to true if SSL protocol is in use.
     * @param trustKeystorePath      Set path to trust key store if SSL is being used.
     */
    public OracleIdentityManagerClient(String username, String password, String authwlPath, String appServerType, String factoryInitialType, String oimProviderURL, boolean isSSL, String trustKeystorePath) throws LoginException
    {
        // Initializes OIMClient with environment information 
        this.initializeOIMClient(authwlPath, appServerType, factoryInitialType, oimProviderURL, isSSL, trustKeystorePath);
        
        // Login to OIM with System Administrator Credentials
        oimClient.login(username, password.toCharArray());
    }
    
    /**
    * Setup the necessary system properties and environment information in 
    * order to use the OIM Client.
    * @param authwlPath             Path to "authwl.conf" file. This can be found in "$MW_HOME/Oracle_IDM1/designconsole/config".
    * @param appServerType          Type of application server OIM is deployed on. For WebLogic, this value should be "wls".
    * @param factoryInitialType     Type of factory initial. For WebLogic, this value should be "weblogic.jndi.WLInitialContextFactory".
    * @param oimProviderURL         The OIM provider URL. For non-SSL protocol, the value should be "t3://<oimhostname>:<oimport>". For SSL protocol, the value should be "t3s://<oimhostname>:<oimport>".
    * @param isSSL                  Set to true if SSL is in use.
    * @param trustKeystorePath      Set path to trust key store if SSL is being used.
    */
    private void initializeOIMClient(String authwlPath, String appServerType, String factoryInitialType, String oimProviderURL, boolean isSSL, String trustKeystorePath)
    {        
        // Set system properties required for OIMClient
        System.setProperty("java.security.auth.login.config", authwlPath);
        System.setProperty("APPSERVER_TYPE", appServerType);
        
        // Set SSL argument on runtime to point to trusted key store
        if(isSSL)
        {
            System.setProperty("weblogic.security.SSL.trustedCAKeyStore", trustKeystorePath);
        }

        // Create an instance of OIMClient with OIM environment information 
        Hashtable env = new Hashtable();
        env.put(OIMClient.JAVA_NAMING_FACTORY_INITIAL, factoryInitialType);
        env.put(OIMClient.JAVA_NAMING_PROVIDER_URL, oimProviderURL);
        this.oimClient = new OIMClient(env);
    }
    
    /**
     * Log out user from OIMClient.
     */
    public void logout()
    {
        if(oimClient != null)
        {
            oimClient.logout();
            logger.log(ODLLevel.TRACE, "Logout user from OIMClient.");
        }
    }
    
    /**
     * Method to test the OIMClient. All the Identities (users) are queried from
     * OIM environment.
     * @param args 
     */
    public void test() throws AccessDeniedException, UserSearchException
    {        
        // Lookup User Manager service
        UserManager usermgr = oimClient.getService(UserManager.class);
        
        // Only fetch attributes defined in HashSet 
        HashSet attrQuery = new HashSet();
        attrQuery.add("usr_key");
        attrQuery.add("usr_login");
        attrQuery.add("Display Name");
        attrQuery.add("First Name");
        attrQuery.add("Last Name");
    
        // Call a method from User Manager service
        List<User> users = usermgr.search(new SearchCriteria("User Login", "*", SearchCriteria.Operator.EQUAL), attrQuery, new HashMap());
        logger.log(ODLLevel.NOTIFICATION, "OIM Users: {0}", new Object[]{users});
    }
}