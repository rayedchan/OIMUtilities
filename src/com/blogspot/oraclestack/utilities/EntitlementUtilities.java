package com.blogspot.oraclestack.utilities;

import java.util.HashSet;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.identity.exception.NoSuchUserException;
import oracle.iam.identity.exception.UserLookupException;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.api.UserManagerConstants;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.provisioning.api.ProvisioningService;
import oracle.iam.provisioning.vo.Entitlement;
import oracle.iam.provisioning.vo.EntitlementInstance;

/**
 * Contains methods related to entitlements.
 * @author rayedchan
 */
public class EntitlementUtilities 
{
    // Logger
    private static final ODLLogger logger = ODLLogger.getODLLogger(EntitlementUtilities.class.getName());
    
    // OIM API Services
    private ProvisioningService provServOps;
    private UserManager userMgrOps;
    
    public EntitlementUtilities(ProvisioningService provServOps, UserManager userMgrOps)
    {
        this.provServOps = provServOps;
        this.userMgrOps = userMgrOps;
    }
    
    public void grantEntitlementToUser(String userLogin)
    {
        // Instantiate Entitlement Instance Object
        EntitlementInstance grantEntInst = new EntitlementInstance();
        
        
        
        /*Entitlement grantEnt = new Entitlement();
            grantEnt.setItResourceKey(42L);
            grantEnt.setObjectKey(42L);
            grantEnt.setEntitlementKey(7L);
            grantEnt.setEntitlementCode("IBM");
            grantEnt.setEntitlementValue("IBM");
            grantEnt.setFormName("UD_LPTYPE");
            grantEnt.setFormKey(45L);
            grantEnt.setFormFieldKey(348L);
            grantEntInst.setEntitlement(grantEnt);
            grantEntInst.setAccountKey(55L);
            grantEntInst.setUsrKey(41L);
            provServOps.grantEntitlement(grantEntInst);*/

        
    }
    
     /**
     * Get the OIM User's USR_KEY
     * @param   userLogin     OIM.User Login (USR_LOGIN)
     * @return value of USR_KEY
     * @throws NoSuchUserException
     * @throws UserLookupException
     */
    private String getUserKeyByUserLogin(String userLogin) throws NoSuchUserException, UserLookupException
    {
        boolean userLoginUsed = true;
        HashSet<String> attrsToFetch = new HashSet<String>();
        attrsToFetch.add(UserManagerConstants.AttributeName.USER_KEY.getId());
        attrsToFetch.add(UserManagerConstants.AttributeName.USER_LOGIN.getId());
        User user = userMgrOps.getDetails(userLogin, attrsToFetch, userLoginUsed);
        logger.log(ODLLevel.NOTIFICATION, "User Details: {0}", new Object[]{user});
        return user.getEntityId();
    }
}
