package project.rayedchan.testdriver;

import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.login.LoginException;
import oracle.core.ojdl.logging.ConsoleHandler;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.iam.identity.exception.UserSearchException;
import oracle.iam.platform.OIMClient;
import oracle.iam.platform.authz.exception.AccessDeniedException;
import oracle.iam.platformservice.exception.InvalidCacheCategoryException;
import project.rayedchan.constants.JarElementType;
import project.rayedchan.services.OracleIdentityManagerClient;
import project.rayedchan.utilities.PlatformServiceUtilities;
import project.rayedchan.utilities.RoleUtilities;

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
        OracleIdentityManagerClient oimClientWrapper = null;
        
        try 
        {
            // Test OIMClient by logging in with a user and querying all the Identities
            oimClientWrapper = new OracleIdentityManagerClient(OIM_ADMIN_USERNAME, OIM_ADMIN_PASSWORD, AUTHWL_PATH, APPSERVER_TYPE, FACTORY_INITIAL_TYPE, OIM_PROVIDER_URL, true, TRUST_KEYSTORE_FOR_SSL);
            //oimClientWrapper.test();
            OIMClient oimClient = oimClientWrapper.getOIMClient();
            
            
            // Test platform service utilities
            PlatformServiceUtilities platServUtil = new PlatformServiceUtilities(oimClient);
            //platServUtil.uploadJar(JarElementType.ThirdParty, "/home/oracle/Desktop/OIMUtilities/dist/OIMUtilities.jar");
            //platServUtil.deleteJar(JarElementType.ThirdParty, "OIMUtilities.jar");
            //platServUtil.purgeCache();
            
            
            // Test Role service utilities
            RoleUtilities roleUtils = new RoleUtilities(oimClient);
            //System.out.println(roleUtils.getAllRoleCategories());
            //roleUtils.createRoleCategory("Groups", "Maps to LDAP groups.");
            //roleUtils.bulkCreateRoleCategories("sample_csv_files/RoleCategories.csv", ';');
            //System.out.println(roleUtils.getAllRoles(new HashSet(Arrays.asList("Role Name","Role Category Name","Role Category Key"))));
            //System.out.println(roleUtils.getRoleCategoryKeyByName("Security & Compliance"));
            //roleUtils.createRole("engineer", "Groups", "engineer Group");
            //roleUtils.bulkCreateRoles("sample_csv_files/Roles.csv", ';');
        
        } 
        
        catch (Exception ex) 
        {
            Logger.getLogger(TestDriver.class.getName()).log(Level.SEVERE, null, ex);
        } 
         
        finally
        {
            oimClientWrapper.logout();
        } 
    }
    
    /**
     * Adjust log level for the console.
     */
    public static void adjustConsoleLogLevel()
    {
        // Set console log level at run time
        ConsoleHandler consoleHandler = new ConsoleHandler(); // Create console handler
        consoleHandler.setLevel(ODLLevel.TRACE); // Set runtime log level for console
        
        // Adjust specific loggers log level
        OracleIdentityManagerClient.logger.setLevel(ODLLevel.TRACE); // Set log level for specific logger
        
        // Add console handler to specific logger
        OracleIdentityManagerClient.logger.addHandler(consoleHandler); // Add console handler to specific logger 
    }
}
