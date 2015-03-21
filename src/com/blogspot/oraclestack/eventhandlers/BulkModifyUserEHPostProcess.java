package com.blogspot.oraclestack.eventhandlers;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.identity.exception.NoSuchUserException;
import oracle.iam.identity.exception.UserLookupException;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.api.UserManagerConstants;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.identity.vo.Identity;
import oracle.iam.platform.Platform;
import oracle.iam.platform.authz.exception.AccessDeniedException;
import oracle.iam.platform.entitymgr.EntityManager;
import oracle.iam.platform.entitymgr.InvalidDataFormatException;
import oracle.iam.platform.entitymgr.InvalidDataTypeException;
import oracle.iam.platform.entitymgr.NoSuchEntityException;
import oracle.iam.platform.entitymgr.ProviderException;
import oracle.iam.platform.entitymgr.StaleEntityException;
import oracle.iam.platform.entitymgr.UnknownAttributeException;
import oracle.iam.platform.entitymgr.UnsupportedOperationException;
import oracle.iam.platform.kernel.EventFailedException;
import oracle.iam.platform.kernel.spi.ConditionalEventHandler;
import oracle.iam.platform.kernel.spi.PostProcessHandler;
import oracle.iam.platform.kernel.vo.AbstractGenericOrchestration;
import oracle.iam.platform.kernel.vo.BulkEventResult;
import oracle.iam.platform.kernel.vo.BulkOrchestration;
import oracle.iam.platform.kernel.vo.EventResult;
import oracle.iam.platform.kernel.vo.Orchestration;

/**
 * Bulk Event Handler Example
 * Operation = MODIFY
 * Perform recalculation of Department Number = "{Manager User Login}|{Manager USR Key}|{User Type}"
 * whenever User Type or Manager is changed for target user
 * @author rayedchan
 */
public class BulkModifyUserEHPostProcess implements PostProcessHandler, ConditionalEventHandler 
{
    private ODLLogger logger = ODLLogger.getODLLogger("BULK_MODIFY_USER");
    private EntityManager entMgr = null;
    private UserManager usrMgr = null;
            
    @Override
    public EventResult execute(long processId, long eventId, Orchestration orchestration)
    {
        logger.log(ODLLevel.NOTIFICATION, "Enter execute() with parameters: Process Id = [{0}], Event Id = [{1}], Orchestration = [{2}]", new Object[]{processId, eventId, orchestration});
        
        try
        {
            this.entMgr = Platform.getService(EntityManager.class);  
            this.usrMgr = Platform.getService(UserManager.class);  
            
             // Get the modified UDFs
            HashMap<String, Serializable> modUDFs = orchestration.getParameters();
            logger.log(ODLLevel.NOTIFICATION, "Modified UDFs: [{0}]", new Object[]{modUDFs});

            // Get USR_KEY of current userbeing modified
            String userKey = orchestration.getTarget().getEntityId();
            logger.log(ODLLevel.NOTIFICATION, "Target OIM User Key: [{0}]", new Object[]{userKey});

            // Get Target Type
            String targetType = orchestration.getTarget().getType();
            logger.log(ODLLevel.NOTIFICATION, "Target type: [{0}]", new Object[]{targetType});

            // Get new user state
            HashMap<String, Serializable> interEventData = orchestration.getInterEventData(); // Contains old and new values of user
            logger.log(ODLLevel.TRACE, "InterEventData: {0}", new Object[]{interEventData});
            User newUserState = (User) interEventData.get("NEW_USER_STATE");
            logger.log(ODLLevel.NOTIFICATION, "User: [{0}]", new Object[]{newUserState});
            
            // Perform attribute derivation of Department Number = "{Manager User Login}|{Manager USR Key}|{User Type}"
            executeEvent(newUserState, userKey, targetType);
        } 
        
        catch (Exception e) 
        {
            logger.log(ODLLevel.ERROR, e.getMessage(), e);
            throw new EventFailedException(processId,"","","","", new Exception(e.getMessage()));
        } 

        return new EventResult();
    }

    @Override
    public BulkEventResult execute(long processId, long eventId, BulkOrchestration bulkOrchestration) 
    {
        logger.log(ODLLevel.NOTIFICATION, "Enter execute() with parameters: Process Id = [{0}], Event Id = [{1}], Bulk Orchestration = [{2}]", new Object[]{processId, eventId, bulkOrchestration});
        
        try
        {
            this.entMgr = Platform.getService(EntityManager.class);  
            this.usrMgr = Platform.getService(UserManager.class);

            // Get the user records from the orchestration argument
            String[] entityIds = bulkOrchestration.getTarget().getAllEntityId();

            // Get every changes from all users
            HashMap<String, Serializable>[] bulkParameters = bulkOrchestration.getBulkParameters();

            // Get interParameters
            HashMap<String, Serializable> interParameters = bulkOrchestration.getInterEventData();

            // Get the new state of all users
            Object newUsersObj = interParameters.get("NEW_USER_STATE");
            Identity[] newUsersState  = (Identity[]) newUsersObj;

            // Iterate each OIM user
            for (int i = 0; i < entityIds.length; i++) 
            {
                // Get the modified UDFs
                HashMap<String, Serializable> modUDFs = bulkParameters[i];
                logger.log(ODLLevel.NOTIFICATION, "Modified UDFs: [{0}]", new Object[]{modUDFs});

                // Get USR_KEY of current userbeing modified
                String userKey = entityIds[i];
                logger.log(ODLLevel.NOTIFICATION, "Target OIM User Key: [{0}]", new Object[]{userKey});

                // Get Target Type
                String targetType = bulkOrchestration.getTarget().getType();
                logger.log(ODLLevel.NOTIFICATION, "Target type: [{0}]", new Object[]{targetType});

                // Get new user state
                User newUserState = (User) newUsersState[i];
                logger.log(ODLLevel.NOTIFICATION, "User: [{0}]", new Object[]{newUserState});

                // Perform attribute derivation of Department Number = "{Manager User Login}|{Manager USR Key}|{User Type}"
                executeEvent(newUserState, userKey, targetType);

            }
        }
        
        catch (Exception e) 
        {
            logger.log(ODLLevel.ERROR, e.getMessage(), e);
            throw new EventFailedException(processId,"","","","", new Exception(e.getMessage()));
        } 
        
        return new BulkEventResult();
    }
    
    /**
     * Process a single event.
     * Populate Department Number with "{Manager User Login}|{Manager USR Key}|{User Type}"
     * @param newUserState  New User state
     * @param userKey       OIM USR_KEY
     * @param targetType    User
     * @throws AccessDeniedException
     * @throws NoSuchUserException
     * @throws UserLookupException
     * @throws InvalidDataTypeException
     * @throws InvalidDataFormatException
     * @throws NoSuchEntityException
     * @throws StaleEntityException
     * @throws UnsupportedOperationException
     * @throws UnknownAttributeException
     * @throws ProviderException 
     */
    public void executeEvent(User newUserState, String userKey, String targetType) throws AccessDeniedException, NoSuchUserException, UserLookupException, InvalidDataTypeException, InvalidDataFormatException, NoSuchEntityException, StaleEntityException, UnsupportedOperationException, UnknownAttributeException, ProviderException
    {
        // Fetch User attributes
        String managerKey = newUserState.getManagerKey();
        String userType = newUserState.getEmployeeType();
        String managerUserLogin = "";
        
        // Check existence of manager key
        if(managerKey != null)
        {
            User managerUser = this.usrMgr.getDetails(managerKey, new HashSet<String>(), false);
            managerUserLogin = managerUser.getLogin();
        }
        
        else
        {
            managerUserLogin = "NO_MANAGER";
        }
  
        logger.log(ODLLevel.NOTIFICATION, "Manager Key = {0}, Manager User Login = {1}, User Type ={2}", new Object[]{managerKey,managerUserLogin,userType});   
        
        // Populate Department Number with <Manager User Login>|<Manager USR Key>|<User Type>
        String result = managerKey + "|" + managerUserLogin + "|" + userType;
        logger.log(ODLLevel.NOTIFICATION, "Result = {0}", new Object[]{result});
        HashMap<String, Object> modAttrs = new HashMap<String, Object>();  
        modAttrs.put(UserManagerConstants.AttributeName.DEPARTMENT_NUMBER.getId(), result); 
        this.entMgr.modifyEntity(targetType, userKey, modAttrs); // prevents OIM from triggering a second orchestration event after the user gets updated
        logger.log(ODLLevel.NOTIFICATION, "Modify user successfully.");
    }

    @Override
    public void compensate(long l, long l1, AbstractGenericOrchestration ago) 
    {
        
    }

    @Override
    public boolean cancel(long l, long l1, AbstractGenericOrchestration ago) 
    {
       return false;
    }

    @Override
    public void initialize(HashMap<String, String> hm) 
    {
        logger.log(ODLLevel.NOTIFICATION, "Enter initialize with parameter: [{0}]", new Object[]{hm});
    }

    /**
     * Determines if this event handler should be triggered.
     * Condition for Execution: Manager or User Type user attribute is changed
     * @param abstractGenericOrchestration Orchestration is used for single event. BulkOrchestration is used for bulk events.
     * @return true to execute event handler or false to skip event handler
     */
    @Override
    public boolean isApplicable(AbstractGenericOrchestration abstractGenericOrchestration) 
    {
        logger.log(ODLLevel.NOTIFICATION, "Enter isApplicable() with parameter: AbstractGenericOrchestration = {0}", new Object[]{abstractGenericOrchestration});
        boolean isApplicable = false; 
        
        String operationType = abstractGenericOrchestration.getOperation();
        logger.log(ODLLevel.NOTIFICATION, "Operation: {0}", new Object[]{operationType});
        
        HashMap<String, Serializable> modParams = abstractGenericOrchestration.getParameters();
        logger.log(ODLLevel.NOTIFICATION, "Modified Parameters: {0}", new Object[]{modParams});
        
        HashMap<String, Serializable> interEventData = abstractGenericOrchestration.getInterEventData();
        logger.log(ODLLevel.NOTIFICATION, "InterEventData: {0}", new Object[]{interEventData}); // null
        
        // Single Orchestration
        if(abstractGenericOrchestration instanceof Orchestration) 
        {            
            String entityId = abstractGenericOrchestration.getTarget().getEntityId();
            logger.log(ODLLevel.NOTIFICATION, "Entity Id: {0}", new Object[]{entityId});
            
            String[] entityIds = abstractGenericOrchestration.getTarget().getAllEntityId();
            
            for(String userId : entityIds) 
            {
                logger.log(ODLLevel.NOTIFICATION, "Entity Ids: {0}", new Object[]{userId});
            }
        }
        
        // Bulk Orchestration
        else if (abstractGenericOrchestration instanceof BulkOrchestration)
        {
            BulkOrchestration bulkOrchestration = (BulkOrchestration) abstractGenericOrchestration;
            
            // Get the user records from the orchestration argument
            String[] entityIds = bulkOrchestration.getTarget().getAllEntityId();
            
            // Get every changes from all users
            HashMap<String, Serializable>[] bulkParameters = bulkOrchestration.getBulkParameters();
            logger.log(ODLLevel.NOTIFICATION, "All Modified Bulk Parameters: {0}", new Object[]{bulkParameters});
            
            // Get interParameters
            HashMap<String, Serializable> interParameters = bulkOrchestration.getInterEventData();
            logger.log(ODLLevel.NOTIFICATION, "Bulk InterEventData: {0}", new Object[]{interParameters}); // No interdata in conditional stage
            
            // Iterate each OIM user
            for (int i = 0; i < entityIds.length; i++)
            {
                // Get the modified UDFs
                HashMap<String, Serializable> modUDFs = bulkParameters[i];
                
                // Get USR_KEY of current userbeing modified
                String userKey = entityIds[i];
                logger.log(ODLLevel.NOTIFICATION, "Target OIM User Key = [{0}], Modified UDFs = [{1}]", new Object[]{userKey, modUDFs});
            }
        }
        
        // Check if User Type (Role) or Manager (usr_manager_key) user attribute is changed 
        isApplicable = modParams.containsKey(UserManagerConstants.AttributeName.MANAGER_KEY.getId()) || modParams.containsKey(UserManagerConstants.AttributeName.EMPTYPE.getId()) ; 
        logger.log(ODLLevel.NOTIFICATION, "Trigger event handler: {0}", new Object[]{isApplicable});;
        return isApplicable;
    }
}