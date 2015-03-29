package com.blogspot.oraclestack.testdriver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.api.UserManagerConstants;
import oracle.iam.platform.OIMClient;
import oracle.iam.platform.entitymgr.vo.SearchCriteria;
import oracle.iam.provisioning.api.EntitlementService;
import oracle.iam.provisioning.api.ProvisioningConstants;
import oracle.iam.provisioning.api.ProvisioningService;
import oracle.iam.provisioning.vo.Entitlement;
import oracle.iam.provisioning.vo.EntitlementInstance;

/**
 *
 * @author oracle
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
            
            //Entitlement oneEnt = entServ.findEntitlement(1L);
            //System.out.println(oneEnt.toString());
           

            // Get Entitlement Definition (Use for in granting)
            SearchCriteria criteria = new SearchCriteria(ProvisioningConstants.EntitlementSearchAttribute.ENTITLEMENT_CODE.getId(),"*", SearchCriteria.Operator.EQUAL);
            HashMap<String,Object> configParams = new HashMap<String,Object>();
            List<Entitlement> entitlements = entServ.findEntitlements(criteria, configParams);
            Entitlement revokeEnt = new Entitlement();
            
            for(Entitlement entitlement: entitlements)
            {
                if(entitlement.getEntitlementCode().equalsIgnoreCase("IBM"))
                {
                    revokeEnt = entitlement;
                    break;
                }
            }
            
            
            // Get user's entitlements (Can be used for deletion)
            String userKey = "41" ;
            List<EntitlementInstance> userEntitlements =  provServOps.getEntitlementsForUser(userKey);
            //System.out.println(userEntitlements);
            
            for(EntitlementInstance ei : userEntitlements )
            {
                System.out.println(ei);
            }
            
            
            EntitlementInstance grantEntInst = new EntitlementInstance();
            //Entitlement grantEnt = new Entitlement();
            //grantEnt.setItResourceKey(42L);
            //grantEnt.setObjectKey(42L);
            //grantEnt.setEntitlementKey(7L);
            //grantEnt.setEntitlementCode("IBM");
            //grantEnt.setEntitlementValue("IBM");
            //grantEnt.setFormName("UD_LPTYPE");
            //grantEnt.setFormKey(45L);
            //grantEnt.setFormFieldKey(348L);
            grantEntInst.setEntitlement(revokeEnt); // **
            grantEntInst.setAccountKey(55L); // ** OIU_KEY
            //grantEntInst.setUsrKey(41L);
            //provServOps.grantEntitlement(grantEntInst);
            
            EntitlementInstance revokeEntInst = new EntitlementInstance();
            //Entitlement revokeEnt = new Entitlement();
            //revokeEnt.setItResourceKey(42L);
            //revokeEnt.setObjectKey(42L);
            //revokeEnt.setEntitlementKey(7L);
            //revokeEnt.setEntitlementCode("IBM");
            //revokeEnt.setEntitlementValue("IBM");
            //revokeEnt.setFormName("UD_LPTYPE");
            //revokeEnt.setFormKey(45L);
            //revokeEnt.setFormFieldKey(348L);
            revokeEntInst.setEntitlement(revokeEnt); // **
            revokeEntInst.setAccountKey(55L); // **
            //revokeEntInst.setUsrKey(41L);
            //revokeEntInst.setEntitlementInstanceKey(26L);
            revokeEntInst.setChildTablePrimaryKey(25L); // **
            //provServOps.revokeEntitlement(revokeEntInst);  
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
