package com.blogspot.oraclestack.utilities;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.api.OIMService;
import oracle.iam.exception.OIMServiceException;
import oracle.iam.identity.exception.NoSuchUserException;
import oracle.iam.identity.exception.UserLookupException;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.api.UserManagerConstants;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.platform.OIMClient;
import oracle.iam.platform.utils.vo.OIMType;
import oracle.iam.request.vo.Beneficiary;
import oracle.iam.request.vo.RequestBeneficiaryEntity;
import oracle.iam.request.vo.RequestConstants;
import oracle.iam.request.vo.RequestData;
import oracle.iam.request.vo.RequestEntity;
import oracle.iam.request.vo.RequestEntityAttribute;
import oracle.iam.vo.OperationResult;

/**
 * Generate a request in Oracle Identity Manager for the specific operation (E.g.
 * Create User, Modify User, Create Role, Provision Account, Revoke Entitlement).
 * The request must be approved in order for the changes be applied in OIM.
 * Refer to oracle.iam.api.OIMService for more information.
 * @author rayedchan
 */
public class GenerateRequestUtilities 
{
    // Logger 
    private ODLLogger logger = ODLLogger.getODLLogger(GenerateRequestUtilities.class.getName());
    
    // OIM Service for API calls
    private final OIMService oimService;
    private final UserManager usrMgrOps;
    
    /**
     * Constructor
     * @param oimService OIMService 
     */
    public GenerateRequestUtilities(OIMClient oimClient)
    {
        this.oimService = oimClient.getService(OIMService.class);
        this.usrMgrOps = oimClient.getService(UserManager.class);
    }
    
    /**
     * 
     * @param userLogin OIM User Login
     * @return 
     * @throws NoSuchUserException
     * @throws UserLookupException
     * @throws OIMServiceException 
     */
    public OperationResult requestToDisableUser(String userLogin) throws NoSuchUserException, UserLookupException, OIMServiceException
    {
        // Call helper method to get usr_key by User Login 
        String usrKey = getUserKeyByUserLogin(userLogin);
        
        // Setup Request Entity
        RequestEntity reqEntity = new RequestEntity();
        reqEntity.setRequestEntityType(OIMType.User); // Specify entity type to User
        reqEntity.setEntityKey(usrKey); // Specify target user's usr_key
        reqEntity.setOperation(RequestConstants.MODEL_DISABLE_OPERATION); // Specify DISABLE operation to perform
        
        // Add single request entity to list
        List<RequestEntity>  entities = new ArrayList<RequestEntity>();
        entities.add(reqEntity);
        
        // Setup Request Data
        RequestData reqData = new RequestData();
        reqData.setTargetEntities(entities); // Set list of request entity
        
        // Invoke request operation in OIM
        OperationResult result = oimService.doOperation(reqData, OIMService.Intent.REQUEST);
        
        return result;
    }
    
    /**
     * Generate a request to OIM to modify User Profile
     * @param userLogin OIM User Login
     * @param modAttrs  User Profile attributes to modify
     * @return response
     * @throws NoSuchUserException
     * @throws UserLookupException
     * @throws OIMServiceException 
     */
    public OperationResult requestToModifyUserAttributes(String userLogin, Map<String,String> modAttrs) throws NoSuchUserException, UserLookupException, OIMServiceException
    {
        // Call helper method to get usr_key by User Login 
        String usrKey = getUserKeyByUserLogin(userLogin);
        
        // Generate a list of request entity attribute to modify
        List<RequestEntityAttribute> reqModAttrs = new ArrayList<RequestEntityAttribute>();
        for(Map.Entry<String,String> entry : modAttrs.entrySet())
        {
            RequestEntityAttribute modAttr = new RequestEntityAttribute(entry.getKey(), entry.getValue(), RequestEntityAttribute.TYPE.String);
            reqModAttrs.add(modAttr);
        }
        
        // Setup Request Entity
        RequestEntity reqEntity = new RequestEntity();
        reqEntity.setRequestEntityType(OIMType.User);
        reqEntity.setEntityKey(usrKey);
        reqEntity.setOperation(RequestConstants.MODEL_MODIFY_OPERATION);
        reqEntity.setEntityData(reqModAttrs);
        
        // Add single request entity to list
        List<RequestEntity>  entities = new ArrayList<RequestEntity>();
        entities.add(reqEntity);
        
        // Setup Request Data
        RequestData reqData = new RequestData();
        reqData.setTargetEntities(entities); // Set list of request entity
        
        // Invoke request operation in OIM
        OperationResult result = oimService.doOperation(reqData, OIMService.Intent.REQUEST);
        
        return result;
    }
    
    public OperationResult requestToProvisionEntitlement(String userLogin, String entitlementName, String entKey) throws NoSuchUserException, UserLookupException, OIMServiceException
    {
        // Call helper method to get usr_key by User Login 
        String usrKey = getUserKeyByUserLogin(userLogin);
                
        // Setup Request Entity
        RequestBeneficiaryEntity reqBenefEntity = new RequestBeneficiaryEntity();
        reqBenefEntity.setRequestEntityType(OIMType.Entitlement);
        reqBenefEntity.setEntitySubType(entitlementName);
        reqBenefEntity.setEntityKey(entKey);
        reqBenefEntity.setOperation(RequestConstants.MODEL_PROVISION_ENTITLEMENT_OPERATION);
        
        // Add single request entity to list
        List<RequestBeneficiaryEntity>  entities = new ArrayList<RequestBeneficiaryEntity>();
        entities.add(reqBenefEntity);
        
        // Setup beneficiary to grant entitlement
        Beneficiary beneficiary = new Beneficiary();
        beneficiary.setBeneficiaryKey(usrKey);
        beneficiary.setBeneficiaryType(Beneficiary.USER_BENEFICIARY);
        beneficiary.setTargetEntities(entities);
        
        // Add single beneficiary to list
        List<Beneficiary> beneficiaries = new ArrayList<Beneficiary>();
        beneficiaries.add(beneficiary);
        
        // Setup Request Data
        RequestData reqData = new RequestData();
        reqData.setBeneficiaries(beneficiaries); // Set list of request entity
        
        // Invoke request operation in OIM
        OperationResult result = oimService.doOperation(reqData, OIMService.Intent.REQUEST);
        
        return result;
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
        User user = usrMgrOps.getDetails(userLogin, attrsToFetch, userLoginUsed);
        logger.log(ODLLevel.NOTIFICATION, "User Details: {0}", new Object[]{user});
        return user.getEntityId();
    }
}
