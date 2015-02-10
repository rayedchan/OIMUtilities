package com.blogspot.oraclestack.testdriver;

import com.blogspot.oraclestack.services.OracleIdentityManagerClient;
import com.blogspot.oraclestack.utilities.PlatformServiceUtilities;
import oracle.iam.platform.OIMClient;

/**
 * Test Driver for PlatformServiceUtilities. Use to upload, update, or remove
 * OIM JARS.
 * @author rayedchan
 */
public class JarUtilityTestDriver 
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
    public static final String JAR_PATH = "/home/oracle/NetBeansProjects/OIMUtilities/dist/OIMUtilities.jar"; // Absolute Path of JAR file on machine where OIM is running
    
    public static void main(String[] args) throws Exception
    {
        OracleIdentityManagerClient oimClientWrapper = null;
        
        try
        {
            // Establish an OIM Client
            oimClientWrapper = new OracleIdentityManagerClient(OIM_ADMIN_USERNAME, OIM_ADMIN_PASSWORD, AUTHWL_PATH, APPSERVER_TYPE, FACTORY_INITIAL_TYPE, OIM_PROVIDER_URL, false, null);
            OIMClient oimClient = oimClientWrapper.getOIMClient();
            
            // Test platform service utilities
            PlatformServiceUtilities platServUtil = new PlatformServiceUtilities(oimClient);
            
            // Upload JAR to OIM; the jar must exist on the machine where OIM is running
            // platServUtil.uploadJar(JarElementType.JavaTasks, JAR_PATH); // Specify JAR type and path of JAR on machine where OIM is running
            
            // Update an existing JAR in OIM
            //platServUtil.updateJar(JarElementType.JavaTasks, JAR_PATH);
            
            // Remove a JAR from OIM
            // platServUtil.deleteJar(JarElementType.JavaTasks, "OIMUtilities.jar");
            
            // Download a JAR from OIM
            // platServUtil.downloadJar(JarElementType.JavaTasks, "OIMUtilities.jar" , "/home/oracle/Desktop/");

            // Purge OIM Cache
            platServUtil.purgeCache();
        }
        
        finally
        {
            if( oimClientWrapper != null)
            {
                oimClientWrapper.logout();
            } 
        }
    }
    
}
