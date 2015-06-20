package com.blogspot.oraclestack.testdriver;

import com.blogspot.oraclestack.utilities.EntitlementUtilities;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.platform.OIMClient;
import oracle.iam.provisioning.api.EntitlementService;
import oracle.iam.provisioning.api.ProvisioningService;
import oracle.iam.provisioning.vo.Entitlement;

/**
 * Test Driver for using entitlement API.
 * @author rayedchan
 */
public class EntitlementTestDriver 
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
            
            // Get OIM Services
            UserManager usrMgr = oimClient.getService(UserManager.class);
            ProvisioningService provServOps = oimClient.getService(ProvisioningService.class);
            EntitlementService entServ = oimClient.getService(EntitlementService.class);
            
            // Instantiate custom entitlement utils
            EntitlementUtilities entUtils = new EntitlementUtilities(provServOps, usrMgr, entServ);
            
            // Print all entitlement definitions
            entUtils.printEntitlementDefinition();
            
            Entitlement ent = new Entitlement();
            ent.setDisplayName("GridGuard"); // ENT_DISPLAY_NAME
            ent.setEntitlementCode("21~GridGuard"); // ENT_CODE
            ent.setEntitlementValue("BadgeAccess~GridGuard");// ENT_VALUE
            ent.setItResourceKey(21L); // SVR_KEY
            ent.setObjectKey(21L); // OBJ_KEY
            ent.setFormKey(23L); // SDK_KEY
            ent.setFormFieldKey(74L); // SDC_KEY  *Use Key lookup attribute
            ent.setLookupValueKey(1570L); // LKU_KEY
            //entServ.addEntitlement(ent);
            
            String displayName = "GridGuard";
            String entCode = "21~GridGuard";
            String entValue = "BadgeAccess~GridGuard";
            Long itResKey = 21L;
            Long objKey = 21L;
            Long formKey = 23L;
            Long formFieldKey = 74L;
            Long lookupKey = 1581L;
            entUtils.createEntitlement(displayName, entCode, entValue, itResKey, objKey, formKey, formFieldKey, lookupKey);
            
            String userLogin = "RSYNGAL";
            String appInstName = "Laptop";
            String entitlementCode = "Lenovo";
            String entitlementDisplayName = "Lenovo"; 
            HashMap<String, Object> entitlementAttributes = new HashMap<String,Object>();
            entitlementAttributes.put("UD_LPTYPE_STARTDATE", new Date());
            entitlementAttributes.put("UD_LPTYPE_HARDDRIVESPACE", "300GB");
                        
            // Print user's entitlements
            //entUtils.printUserEntitlementInstances(userLogin);
                              
            // Grant Entitlement to user
            //entUtils.grantEntitlementToUser(userLogin, appInstName, entitlementCode, entitlementAttributes);
            
            // Update Entitlement on user
            //entUtils.updateEntitlementInstanceOnUser(userLogin, entitlementDisplayName, entitlementAttributes);
            
            // Revoke an entitlement from user
            //entUtils.revokeEntitlementFromUser(userLogin, entitlementDisplayName);
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
