package com.blogspot.oraclestack.testdriver;

import com.blogspot.oraclestack.utilities.EntitlementUtilities;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.platform.OIMClient;
import oracle.iam.platform.entitymgr.vo.SearchCriteria;
import oracle.iam.provisioning.api.ApplicationInstanceService;
import oracle.iam.provisioning.api.EntitlementService;
import oracle.iam.provisioning.api.ProvisioningConstants;
import oracle.iam.provisioning.api.ProvisioningService;
import oracle.iam.provisioning.vo.ApplicationInstance;
import oracle.iam.provisioning.vo.Entitlement;

/**
 * Test Driver for using entitlement API.
 * @author rayedchan
 */
public class EntitlementTestDriver 
{
    private static final ODLLogger logger = ODLLogger.getODLLogger(EntitlementTestDriver.class.getName());
    
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
            ApplicationInstanceService appInstServ = oimClient.getService(ApplicationInstanceService.class);
                    
            // Instantiate custom entitlement utils
            EntitlementUtilities entUtils = new EntitlementUtilities(provServOps, usrMgr, entServ);
            
            // Print all entitlement definitions
            // entUtils.printEntitlementDefinition();
            
            String displayName = "GridGuard";
            String entCode = "21~GridGuard";
            String entValue = "BadgeAccess~GridGuard";
            Long itResKey = 21L;
            Long objKey = 21L;
            Long formKey = 23L;
            Long formFieldKey = 74L;
            Long lookupKey = 1581L;
            // Create Entitlement
            //entUtils.createEntitlement(displayName, entCode, entValue, itResKey, objKey, formKey, formFieldKey, lookupKey);
            
            String userId = "3007";
            String resourceObjectName = "LDAP User";
            
            // Get application instance by resource object name
            SearchCriteria objectNameCriteria =  new SearchCriteria(ProvisioningConstants.AccountSearchAttribute.OBJ_NAME.getId(), resourceObjectName, SearchCriteria.Operator.EQUAL);
            HashMap<String,Object> configParams = new HashMap<String,Object>();
            List<ApplicationInstance> appInsts = appInstServ.findApplicationInstance(objectNameCriteria, configParams);
            ApplicationInstance applicationInstance = null;
            
            for(ApplicationInstance appInst : appInsts)
            {
                applicationInstance = appInst;
            }
            
            //throw ApplicationInstanceNotFoundException
            logger.log(ODLLevel.NOTIFICATION, "Found Application Instance: {0}", new Object[]{applicationInstance});
            
            // Get Entitlement Defintions
            Long resourceObjKey = applicationInstance.getObjectKey();
            SearchCriteria objKeyCriteria = new SearchCriteria(ProvisioningConstants.EntitlementSearchAttribute.OBJ_KEY.getId(),resourceObjKey, SearchCriteria.Operator.EQUAL);     
            SearchCriteria entCriteria = new SearchCriteria(ProvisioningConstants.EntitlementSearchAttribute.ENTITLEMENT_CODE.getId(), "42~cn=Engineer,ou=Groups,dc=my-domain,dc=com", SearchCriteria.Operator.EQUAL);     
            SearchCriteria entCriteria2 = new SearchCriteria(ProvisioningConstants.EntitlementSearchAttribute.ENTITLEMENT_CODE.getId(), "42~cn=Scientist,ou=Groups,dc=my-domain,dc=com", SearchCriteria.Operator.EQUAL);
            SearchCriteria entCriteria3 = new SearchCriteria(ProvisioningConstants.EntitlementSearchAttribute.ENTITLEMENT_VALUE.getId(), "DSEE Server~Administrator", SearchCriteria.Operator.EQUAL);
            SearchCriteria orEntCriteria = new SearchCriteria(entCriteria, entCriteria2, SearchCriteria.Operator.OR);
            SearchCriteria orEntCriteria2 = new SearchCriteria(orEntCriteria, entCriteria3, SearchCriteria.Operator.OR);
            SearchCriteria allCriteria = new SearchCriteria(objKeyCriteria, orEntCriteria2, SearchCriteria.Operator.AND);     
            
            List<Entitlement> entitlements = entServ.findEntitlements(allCriteria, configParams);
            logger.log(ODLLevel.NOTIFICATION, "Entitlement Definition List: {0}", new Object[]{entitlements});
            
            // Check if user has entitlement
            for(Entitlement ent : entitlements)
            {
                boolean isEntProvisioned = provServOps.isEntitlementProvisionedToUser(userId, ent);
                logger.log(ODLLevel.NOTIFICATION, "Is Entitlement [{0}] Provisioned? {1}", new Object[]{ent, isEntProvisioned});
            }
            
            // Disconnected Resource
            //String userLogin = "RSYNGAL";
            //String appInstName = "Laptop";
            //String entitlementCode = "Lenovo";
            //String entitlementDisplayName = "Lenovo"; 
            //HashMap<String, Object> entitlementAttributes = new HashMap<String,Object>();
            //entitlementAttributes.put("UD_LPTYPE_STARTDATE", new Date());
            //entitlementAttributes.put("UD_LPTYPE_HARDDRIVESPACE", "300GB");
                        
            // Connected Resource
            
            
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
