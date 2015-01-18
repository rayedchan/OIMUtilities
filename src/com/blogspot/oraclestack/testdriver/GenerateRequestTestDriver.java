package com.blogspot.oraclestack.testdriver;

import com.blogspot.oraclestack.services.OracleIdentityManagerClient;
import com.blogspot.oraclestack.utilities.GenerateRequestUtilities;
import java.util.HashMap;
import oracle.iam.platform.OIMClient;
import oracle.iam.vo.OperationResult;

/**
 * Test Driver for GenerateRequestUtilities class
 * @author rayedchan
 */
public class GenerateRequestTestDriver 
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
        OracleIdentityManagerClient oimClientWrapper = null;
        
        try
        {
            // Establish an OIM Client
            oimClientWrapper = new OracleIdentityManagerClient(OIM_ADMIN_USERNAME, OIM_ADMIN_PASSWORD, AUTHWL_PATH, APPSERVER_TYPE, FACTORY_INITIAL_TYPE, OIM_PROVIDER_URL, false, null);
            OIMClient oimClient = oimClientWrapper.getOIMClient();
            
            // Instantiate Util Object
            GenerateRequestUtilities genReqUtil = new GenerateRequestUtilities(oimClient);
            
            // Input variables
            String userLogin = "JCICCHELLA";
            OperationResult response;
            HashMap<String,String> modAttrs = new HashMap<String,String>();
            modAttrs.put("First Name", "Justin2");
            modAttrs.put("Last Name", "Cicchella2");
            String entitlementValue = "Tech Lab"; // ENT_LIST.ENT_VALUE
            String entitlementKey = "5"; // ENT_LIST.ENT_LIST_KEY
            
            // Generate "Disable User" Request
            response = genReqUtil.requestToDisableUser(userLogin);
            System.out.printf("Request Id: {%s}, Entity Id: {%s}, Status: {%s}\n", response.getRequestID(), response.getEntityId(), response.getOperationStatus());
            
            // Generate "Modify User" Request
            response = genReqUtil.requestToModifyUserAttributes(userLogin, modAttrs);
            System.out.printf("Request Id: {%s}, Entity Id: {%s}, Status: {%s}\n", response.getRequestID(), response.getEntityId(), response.getOperationStatus());
            
            // Generate "Provision Entitlement" Request
            response = genReqUtil.requestToProvisionEntitlement(userLogin, entitlementValue, entitlementKey);
            System.out.printf("Request Id: {%s}, Entity Id: {%s}, Status: {%s}\n", response.getRequestID(), response.getEntityId(), response.getOperationStatus());
        }
        
        finally
        {
            oimClientWrapper.logout();
        }
    }
}
