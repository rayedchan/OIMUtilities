package project.rayedchan.testdriver;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.login.LoginException;
import oracle.core.ojdl.logging.ConsoleHandler;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.iam.identity.exception.UserSearchException;
import oracle.iam.platform.authz.exception.AccessDeniedException;
import project.rayedchan.services.OracleIdentityManagerClient;

/**
 * Used for testing purposes.
 * @author rayedchan
 */
public class TestDriver 
{
    // Adjust constant variables according to you OIM environment
    public static final String OIM_HOSTNAME = "localhost";
    public static final String OIM_PORT = "14001"; // For SSL, use 14001; For non-SSL, use 14000
    public static final String OIM_PROVIDER_URL = "t3s://"+ OIM_HOSTNAME + ":" + OIM_PORT; // For SSL, use t3s protocol; For non-SSL, use t3 protocol
    public static final String AUTHWL_PATH = "lib/config/authwl.conf";
    public static final String APPSERVER_TYPE = "wls";
    public static final String FACTORY_INITIAL_TYPE = "weblogic.jndi.WLInitialContextFactory";
   
    // Use if using SSL connection for OIMClient
    public static final String TRUST_KEYSTORE_FOR_SSL = "/home/oracle/Oracle/Middleware/wlserver_10.3/server/lib/DemoTrust.jks";
    
    // OIM Administrator Credentials
    public static final String OIM_ADMIN_USERNAME = "xelsysadm";
    public static final String OIM_ADMIN_PASSWORD = "Password1";
    
    public static void main(String[] args)
    {
        // Set log level at run time
        //ConsoleHandler consoleHandler = new ConsoleHandler(); // Create console handler
        //consoleHandler.setLevel(ODLLevel.TRACE); // Set runtime log level for console
        //OracleIdentityManagerClient.logger.setLevel(ODLLevel.TRACE); // Set log level for specific logger
        //OracleIdentityManagerClient.logger.addHandler(consoleHandler); // Add console handler to specific logger
        
        OracleIdentityManagerClient oimClientWrapper = null;
        
        try 
        {
            oimClientWrapper = new OracleIdentityManagerClient(OIM_ADMIN_USERNAME, OIM_ADMIN_PASSWORD, AUTHWL_PATH, APPSERVER_TYPE, FACTORY_INITIAL_TYPE, OIM_PROVIDER_URL, true, TRUST_KEYSTORE_FOR_SSL);
            oimClientWrapper.test();
        } 
        
        catch (AccessDeniedException ex) 
        {
            Logger.getLogger(TestDriver.class.getName()).log(Level.SEVERE, null, ex);
        } 
        
        catch (UserSearchException ex) 
        {
            Logger.getLogger(TestDriver.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        catch (LoginException ex) 
        {
            Logger.getLogger(TestDriver.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        finally
        {
            oimClientWrapper.logout();
        } 
    }
}
