package com.blogspot.oraclestack.utilities;

import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.provisioning.api.ProvisioningService;
import oracle.iam.provisioning.vo.Entitlement;
import oracle.iam.provisioning.vo.EntitlementInstance;

/**
 * Contains methods related to entitlements.
 * @author rayedchan
 */
public class EntitlementUtilities 
{
    // OIM API Services
    private ProvisioningService provServOps;
    private UserManager userMgrOps;
    
    public EntitlementUtilities(ProvisioningService provServOps, UserManager userMgrOps)
    {
        this.provServOps = provServOps;
        this.userMgrOps = userMgrOps;
    }
    
    /*public void grantEntitlement()
    {
        EntitlementInstance entInst = new EntitlementInstance();
        Entitlement ent = new Entitlement();
        ent.setEntitlementKey(entitlementKey);
        entInst.
        
    }*/
}
