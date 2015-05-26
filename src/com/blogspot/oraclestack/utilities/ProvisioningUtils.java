package com.blogspot.oraclestack.utilities;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.identity.exception.NoSuchUserException;
import oracle.iam.identity.exception.UserLookupException;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.platform.OIMClient;
import oracle.iam.platform.authz.exception.AccessDeniedException;
import oracle.iam.provisioning.api.ApplicationInstanceService;
import oracle.iam.provisioning.api.ProvisioningService;
import oracle.iam.provisioning.exception.ApplicationInstanceNotFoundException;
import oracle.iam.provisioning.exception.GenericAppInstanceServiceException;
import oracle.iam.provisioning.exception.GenericProvisioningException;
import oracle.iam.provisioning.exception.UserNotFoundException;
import oracle.iam.provisioning.vo.Account;
import oracle.iam.provisioning.vo.AccountData;
import oracle.iam.provisioning.vo.ApplicationInstance;
import oracle.iam.provisioning.vo.ChildTableRecord;

/**
 * Contains methods to provision application instances
 * @author rayedchan
 */
public class ProvisioningUtils 
{
    // Logger 
    private static final ODLLogger logger = ODLLogger.getODLLogger(ProvisioningUtils.class.getName());
    
    // Get OIM API services
    private final UserManager usrMgr;
    private final ApplicationInstanceService appInstService;
    private final ProvisioningService provService;
    
    /**
     * Constructor
     * @param oimClient OIMClient with administrator logged in
     */
    public ProvisioningUtils(OIMClient oimClient)
    {
        this.usrMgr = oimClient.getService(UserManager.class);
        this.appInstService = oimClient.getService(ApplicationInstanceService.class);
        this.provService = oimClient.getService(ProvisioningService.class);
    }
    
    /**
     * Provision a resource account to a user.
     * @param userLogin     OIM User Login (USR.USR_LOGIN)
     * @param appInstName   Name of application instance (APP_INSTANCE.APP_INSTANCE_NAME)
     * @param parentData    Data to populate the parent process form
     * @param childData     Data to populate the child process form(s)
     * @return Account Id (OIU_KEY)
     * @throws AccessDeniedException
     * @throws NoSuchUserException
     * @throws UserLookupException
     * @throws ApplicationInstanceNotFoundException
     * @throws GenericAppInstanceServiceException
     * @throws UserNotFoundException
     * @throws oracle.iam.platform.authopss.exception.AccessDeniedException
     * @throws GenericProvisioningException 
     */
    public Long provisionResourceAccountToUser(String userLogin, String appInstName, Map<String,Object> parentData, Map<String, ArrayList<ChildTableRecord>> childData) throws AccessDeniedException, NoSuchUserException, UserLookupException, ApplicationInstanceNotFoundException, GenericAppInstanceServiceException, UserNotFoundException, oracle.iam.platform.authopss.exception.AccessDeniedException, GenericProvisioningException
    {
        // Get OIM User searching by User Login (USR.USR_LOGIN)
        boolean isUserLogin = true; // True for searching by User Login; False for searching by USR_KEY 
        Set<String> retAttrs = null; // Return attributes; Null implies returning every attributes on user 
        User user = usrMgr.getDetails(userLogin, retAttrs, isUserLogin); // Get OIM User
        logger.log(ODLLevel.NOTIFICATION, "User: {0}", new Object[]{user});

        // Get application instance by name (APP_INSTANCE.APP_INSTANCE_NAME)
        ApplicationInstance appInst = appInstService.findApplicationInstanceByName(appInstName);
        logger.log(ODLLevel.NOTIFICATION, "Application Instance: {0}", new Object[]{appInst});

        // Get information required provisioning resource account
        String usrKey = user.getId(); // Get usr_key of OIM User
        Long resourceFormKey = appInst.getAccountForm().getFormKey(); // Get Process Form Key (SDK_KEY)
        logger.log(ODLLevel.NOTIFICATION, "Resource Process Form Key: {0}", new Object[]{resourceFormKey});
        String udTablePrimaryKey = null;
        
        // Construct-Stage Resource Account 
        AccountData accountData = new AccountData(String.valueOf(resourceFormKey), udTablePrimaryKey, parentData);
        accountData.setChildData(childData);
        Account resAccount = new Account(appInst, accountData);
        
        // Provision resource account to user
        Long accountId = provService.provision(usrKey, resAccount); // Account Key = OIU_KEY
        logger.log(ODLLevel.NOTIFICATION, "Provisioning Account Id: {0}", new Object[]{accountId});
        
        return accountId;
    }
}
