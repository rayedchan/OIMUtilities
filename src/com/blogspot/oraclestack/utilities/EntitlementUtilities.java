package com.blogspot.oraclestack.utilities;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.identity.exception.NoSuchUserException;
import oracle.iam.identity.exception.UserLookupException;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.api.UserManagerConstants;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.platform.entitymgr.vo.SearchCriteria;
import oracle.iam.provisioning.api.EntitlementService;
import oracle.iam.provisioning.api.ProvisioningConstants;
import oracle.iam.provisioning.api.ProvisioningService;
import oracle.iam.provisioning.exception.AccountNotFoundException;
import oracle.iam.provisioning.exception.DuplicateEntitlementException;
import oracle.iam.provisioning.exception.EntitlementAlreadyProvisionedException;
import oracle.iam.provisioning.exception.EntitlementNotFoundException;
import oracle.iam.provisioning.exception.EntitlementNotProvisionedException;
import oracle.iam.provisioning.exception.FormFieldNotFoundException;
import oracle.iam.provisioning.exception.FormNotFoundException;
import oracle.iam.provisioning.exception.GenericEntitlementServiceException;
import oracle.iam.provisioning.exception.GenericProvisioningException;
import oracle.iam.provisioning.exception.ITResourceNotFoundException;
import oracle.iam.provisioning.exception.ImproperAccountStateException;
import oracle.iam.provisioning.exception.LookupValueNotFoundException;
import oracle.iam.provisioning.exception.ObjectNotFoundException;
import oracle.iam.provisioning.exception.UserNotFoundException;
import oracle.iam.provisioning.vo.Account;
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
    private final ProvisioningService provServOps;
    private final UserManager userMgrOps;
    private final EntitlementService entServ;
    
    public EntitlementUtilities(ProvisioningService provServOps, UserManager userMgrOps, EntitlementService entServ)
    {
        this.provServOps = provServOps;
        this.userMgrOps = userMgrOps;
        this.entServ = entServ;
    }
        
    /**
     * Get all the entitlements from the OIM environment. 
     * @throws GenericEntitlementServiceException 
     */
    public void printEntitlementDefinition() throws GenericEntitlementServiceException
    {
        // Get all Entitlement Definitions
        SearchCriteria criteria = new SearchCriteria(ProvisioningConstants.EntitlementSearchAttribute.ENTITLEMENT_CODE.getId(),"*", SearchCriteria.Operator.EQUAL);
        HashMap<String,Object> configParams = new HashMap<String,Object>();
        List<Entitlement> entitlements = entServ.findEntitlements(criteria, configParams);
        logger.log(ODLLevel.NOTIFICATION, "Entitlement List: {0}", new Object[]{entitlements});
    }
    
    /**
     * Print all user's entitlement instances. 
     * @param userLogin OIM.USR_LOGIN
     * @throws GenericProvisioningException
     * @throws UserNotFoundException 
     */
    public void printUserEntitlementInstances(String userLogin) throws GenericProvisioningException, UserNotFoundException, NoSuchUserException, UserLookupException
    {
        // Get user's key
        String userKey = this.getUserKeyByUserLogin(userLogin);
        logger.log(ODLLevel.NOTIFICATION, "User key: {0}", new Object[]{userKey});
        
        // Get user's entitlements
        List<EntitlementInstance> userEntitlements =  this.provServOps.getEntitlementsForUser(userKey);
            
        // Iterate each entitlement and print to logs
        for(EntitlementInstance userEntitlement : userEntitlements )
        {
            Long accountId = userEntitlement.getAccountKey(); // OIU_KEY
            logger.log(ODLLevel.NOTIFICATION, "Entitlement Instance: {0}, Account ID (OIU_KEY): {1}", new Object[]{userEntitlement, accountId});
        }
    }
    
    /**
     * Add an entitlement to a user. Entitlements are stored 
     * in a resource account in the form of child data.
     * @param userLogin                 OIM User Login (USR_LOGIN)
     * @param appInstName               Application Instance Display Name
     * @param entitlementCode           Entitlement Code (ENT_LIST.ENT_CODE)
     * @param entitlementAttributes     Attributes on entitlement 
     * @throws NoSuchUserException
     * @throws UserLookupException
     * @throws UserNotFoundException
     * @throws GenericProvisioningException
     * @throws GenericEntitlementServiceException
     * @throws AccountNotFoundException
     * @throws ImproperAccountStateException
     * @throws EntitlementNotFoundException
     * @throws EntitlementAlreadyProvisionedException 
     */
    public void grantEntitlementToUser(String userLogin, String appInstName, String entitlementCode, HashMap<String,Object> entitlementAttributes) throws NoSuchUserException, UserLookupException, UserNotFoundException, GenericProvisioningException, GenericEntitlementServiceException, AccountNotFoundException, ImproperAccountStateException, EntitlementNotFoundException, EntitlementAlreadyProvisionedException
    {
        // Get user's key
        String userKey = this.getUserKeyByUserLogin(userLogin);
        logger.log(ODLLevel.NOTIFICATION, "User key: {0}", new Object[]{userKey});
        
        // Get user's account filtered by application instance display name
        boolean populateAcctData = false;
        SearchCriteria appInstCriteria = new SearchCriteria(ProvisioningConstants.AccountSearchAttribute.DISPLAY_NAME.getId(), appInstName, SearchCriteria.Operator.EQUAL);
        HashMap<String,Object> acctConfigParams = new HashMap<String,Object>();
        List<Account> userAccounts = this.provServOps.getAccountsProvisionedToUser(userKey, appInstCriteria, acctConfigParams, populateAcctData);
        logger.log(ODLLevel.NOTIFICATION, "User accounts fetched: {0}", new Object[]{userAccounts});
        
        // Get specific Entitlement Definitions
        SearchCriteria entDefCriteria = new SearchCriteria(ProvisioningConstants.EntitlementSearchAttribute.ENTITLEMENT_CODE.getId(), entitlementCode, SearchCriteria.Operator.EQUAL);
        HashMap<String,Object> entConfigParams = new HashMap<String,Object>();
        List<Entitlement> entitlements = entServ.findEntitlements(entDefCriteria, entConfigParams);
        logger.log(ODLLevel.NOTIFICATION, "Entitlement Definition Fetched: {0}", new Object[]{entitlements});
        
        // Ensure an entitlement can be added to a specific resource on a user
        if (userAccounts != null && !userAccounts.isEmpty() && entitlements != null && !entitlements.isEmpty())
        {
            // Get the first resource account 
            Account userAccount = userAccounts.get(0);
            String accountKey = userAccount.getAccountID(); // OIU_KEY
            logger.log(ODLLevel.NOTIFICATION, "Add entitlement to account: Account Key = {0}", new Object[]{accountKey});
            
            // Get first entitlement definition
            Entitlement entitlement = entitlements.get(0);
            logger.log(ODLLevel.NOTIFICATION, "Entitlement Definition: {0}", new Object[]{entitlement});
            
            // Instantiate Entitlement Instance Object
            EntitlementInstance grantEntInst = new EntitlementInstance();
            
            // Set required fields to grant entitlement
            grantEntInst.setEntitlement(entitlement); // **
            grantEntInst.setAccountKey(Long.parseLong(accountKey)); // ** OIU_KEY
            
            // Set attributes on entitlement if any 
            grantEntInst.setChildFormValues(entitlementAttributes);

            // Add entitlement for user 
            this.provServOps.grantEntitlement(grantEntInst); 
        }
        
        else
        {
            logger.log(ODLLevel.NOTIFICATION, "Did not grant entitlement to user.");
        }
    }
    
    /**
     * Removes an entitlement instance form a user based on the display name of the entitlement.
     * @param userLogin         Login of OIM user   (OIM.USR_LOGIN)
     * @param entitlementName   Entitlement to remove from user (OIM.ENT_LIST.ENT_DISPLAY_NAME)
     * @throws UserNotFoundException
     * @throws GenericProvisioningException
     * @throws AccountNotFoundException
     * @throws EntitlementNotProvisionedException
     * @throws NoSuchUserException
     * @throws UserLookupException 
     */
    public void revokeEntitlementFromUser(String userLogin, String entitlementName) throws UserNotFoundException, GenericProvisioningException, AccountNotFoundException, EntitlementNotProvisionedException, NoSuchUserException, UserLookupException
    {
        // Get user's key
        String userKey = this.getUserKeyByUserLogin(userLogin);
        logger.log(ODLLevel.NOTIFICATION, "User key: {0}", new Object[]{userKey});
        
        // Get specific entitlement from user filtered by Entitlement Display Name (ENT_LIST.ENT_DISPLAY_NAME)
        SearchCriteria criteria = new SearchCriteria(ProvisioningConstants.EntitlementSearchAttribute.ENTITLEMENT_DISPLAYNAME.getId(), entitlementName, SearchCriteria.Operator.EQUAL);
        HashMap<String,Object> configParams = new HashMap<String,Object>();
        List<EntitlementInstance> userEntitlementInsts =  this.provServOps.getEntitlementsForUser(userKey, criteria, configParams);
        logger.log(ODLLevel.NOTIFICATION, "Entitlement Instances Fetched: {0}", new Object[]{userEntitlementInsts});
        
        // Check if there is at least one entitlement
        if(userEntitlementInsts != null && !userEntitlementInsts.isEmpty())
        {
            // Get first item in user's entitlement list
            EntitlementInstance revokeEntInst = userEntitlementInsts.get(0);
            
            // Remove entitlement from user
            this.provServOps.revokeEntitlement(revokeEntInst); 
            logger.log(ODLLevel.NOTIFICATION, "Removed Entitlement Instance: {0}", new Object[]{revokeEntInst});
        }
        
        else
        {
            logger.log(ODLLevel.NOTIFICATION, "No such entitlement instance to remove: {0}", new Object[]{entitlementName});
        }
    }
    
    /**
     * Update an entitlement on a user.
     * @param userLogin                 User Login
     * @param entitlementName           Display name of entitlement
     * @param entitlementAttributes     Attributes to update on entitlement
     * @throws NoSuchUserException
     * @throws UserLookupException
     * @throws UserNotFoundException
     * @throws GenericProvisioningException
     * @throws AccountNotFoundException
     * @throws EntitlementNotFoundException 
     */
    public void updateEntitlementInstanceOnUser(String userLogin, String entitlementName, HashMap<String,Object> entitlementAttributes) throws NoSuchUserException, UserLookupException, UserNotFoundException, GenericProvisioningException, AccountNotFoundException, EntitlementNotFoundException
    {
        // Get user's key
        String userKey = this.getUserKeyByUserLogin(userLogin);
        logger.log(ODLLevel.NOTIFICATION, "User key: {0}", new Object[]{userKey});
        
        // Get specific entitlement from user filtered by Entitlement Display Name (ENT_LIST.ENT_DISPLAY_NAME)
        SearchCriteria criteria = new SearchCriteria(ProvisioningConstants.EntitlementSearchAttribute.ENTITLEMENT_DISPLAYNAME.getId(), entitlementName, SearchCriteria.Operator.EQUAL);
        HashMap<String,Object> configParams = new HashMap<String,Object>();
        List<EntitlementInstance> userEntitlementInsts =  this.provServOps.getEntitlementsForUser(userKey, criteria, configParams);
        logger.log(ODLLevel.NOTIFICATION, "Entitlement Instances Fetched: {0}", new Object[]{userEntitlementInsts});
        
        // Check if there is at least one entitlement
        if(userEntitlementInsts != null && !userEntitlementInsts.isEmpty())
        {
            // Get first item in user's entitlement list
            EntitlementInstance updateEntInst = userEntitlementInsts.get(0);
            
            // Stage updates for entitlement
            updateEntInst.setChildFormValues(entitlementAttributes);
            
            // Update entitlement instance on user
            this.provServOps.updateEntitlement(updateEntInst); 
            logger.log(ODLLevel.NOTIFICATION, "Update Entitlement Instance: {0}", new Object[]{updateEntInst});
        }
        
        else
        {
            logger.log(ODLLevel.NOTIFICATION, "No such entitlement instance to update: {0}", new Object[]{entitlementName});
        }
        
    }
    
     /**
     * Creates an entitlement in OIM.
     * DO NOT USE. Incorrect behavior in 11.1.2.0.0
     * The problem is with setting the lookup value key (LKV). The API does a validation against the LKU.
     * The API will create a new record in the ENT_LIST that has an incorrect key.
     * Also, there is no record added to the lookup (LKV).
     * 
     * @param displayName   Display Name of the entitlement
     * @param entCode       Entitlement Code 
     * @param entValue      Entitlement Value
     * @param itResKey
     * @param objKey
     * @param formKey
     * @param formFieldKey
     * @param lookupKey
     * @throws ITResourceNotFoundException
     * @throws ObjectNotFoundException
     * @throws DuplicateEntitlementException
     * @throws GenericEntitlementServiceException
     * @throws FormFieldNotFoundException
     * @throws LookupValueNotFoundException
     * @throws FormNotFoundException 
     */
    public void createEntitlement(String displayName, String entCode, String entValue, Long itResKey, Long objKey, Long formKey, Long formFieldKey, Long lookupKey) throws ITResourceNotFoundException, ObjectNotFoundException, DuplicateEntitlementException, GenericEntitlementServiceException, FormFieldNotFoundException, LookupValueNotFoundException, FormNotFoundException
    {
        // Setup Entitlement Definition
        Entitlement ent = new Entitlement();
        ent.setDisplayName(displayName); // ENT_DISPLAY_NAME
        ent.setEntitlementCode(entCode); // ENT_CODE
        ent.setEntitlementValue(entValue);// ENT_VALUE
        ent.setItResourceKey(itResKey); // SVR_KEY
        ent.setObjectKey(objKey); // OBJ_KEY
        ent.setFormKey(formKey); // SDK_KEY
        ent.setFormFieldKey(formFieldKey); // SDC_KEY  *Use Key lookup attribute
        ent.setLookupValueKey(lookupKey); // LKU_KEY *Look like a bug with OIM here; there should be a setter method for LKU
        
        // Call API to create the entitlement in OIM
        this.entServ.addEntitlement(ent); 
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
