package com.blogspot.oraclestack.eventhandlers;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.HashMap;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.identity.exception.NoSuchUserException;
import oracle.iam.identity.exception.SearchKeyNotUniqueException;
import oracle.iam.identity.exception.UserModifyException;
import oracle.iam.identity.exception.ValidationFailedException;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.api.UserManagerConstants;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.identity.usermgmt.vo.UserManagerResult;
import oracle.iam.identity.vo.Identity;
import oracle.iam.platform.Platform;
import oracle.iam.platform.authz.exception.AccessDeniedException;
import oracle.iam.platform.kernel.EventFailedException;
import oracle.iam.platform.kernel.spi.PreProcessHandler;
import oracle.iam.platform.kernel.vo.AbstractGenericOrchestration;
import oracle.iam.platform.kernel.vo.BulkEventResult;
import oracle.iam.platform.kernel.vo.BulkOrchestration;
import oracle.iam.platform.kernel.vo.EventResult;
import oracle.iam.platform.kernel.vo.Orchestration;

/**
 * Demonstrates implementing single and bulk execute method for a preprocess
 * event handler on user lock operation. This can also be used as a template for
 * implementing other preprocess event handlers. Place additional implementations/logic
 * in the executeEvent() method since both execute() methods make calls to it.
 * 
 * Note: For bulk lock operation, modifying the parameters applies to all users
 * in the orchestration. When using this preprocess event handler on DISABLE and 
 * ENABLE operations, modifying the orchestration parameters do nothing.
 * 
 * @author rayedchan
 */
public class UserLockPreprocessEH implements PreProcessHandler 
{
    // Logger
    private static final ODLLogger LOGGER = ODLLogger.getODLLogger(UserLockPreprocessEH.class.getName());
    
    // OIM API Services
    private static final UserManager USR_MGR = Platform.getServiceForEventHandlers(UserManager.class, null, "ADMIN","UserLockPreprocessEH", null);

    /**
     * Executes the event handler on a single event operation.
     * @param processId Process Id
     * @param eventId   Event Id
     * @param orchestration Orchestration contains target user information
     * @return new event result
     */
    @Override
    public EventResult execute(long processId, long eventId, Orchestration orchestration) 
    {
        LOGGER.log(ODLLevel.NOTIFICATION, "Enter execute method: Process Id = {0}, Event Id = {1}", new Object[]{processId, eventId});
        
        // Get USR_KEY of target user from orchestration
        String userKey = orchestration.getTarget().getEntityId();
        LOGGER.log(ODLLevel.NOTIFICATION, "User Key: {0}", new Object[]{userKey});

        // Get Interevent data; only current User state avaliable    
        HashMap<String,Serializable> interEventMap = orchestration.getInterEventData();
        LOGGER.log(ODLLevel.TRACE, "InterEvent Data: {0}", new Object[]{interEventMap});

        // Get the modified parameters upon lock operation
        HashMap<String,Serializable> modParams = orchestration.getParameters();
        LOGGER.log(ODLLevel.TRACE, "Modified Paramerters: {0}", new Object[]{modParams});

        // Get current user state
        User user = (User) interEventMap.get("CURRENT_USER");
        LOGGER.log(ODLLevel.TRACE, "Current User: {0}", new Object[]{user});
        
        // Get user login
        String userLogin = user.getLogin();
        LOGGER.log(ODLLevel.NOTIFICATION, "User Login: {0}", new Object[]{userLogin});
        
        try 
        {
            // Modify the orchestration parameters for the target user
            this.updateOrchParams(modParams);
            LOGGER.log(ODLLevel.TRACE, "New Modified Parameters: {0}", new Object[]{modParams});
            
            // Apply additional modification to user. 
            // NOTE: These modification can also be changed in the orchestration.
            this.executeEvent(user);
        } 
        
        catch (Exception e) 
        {
            LOGGER.log(ODLLevel.SEVERE, MessageFormat.format("Failed for {0}", new Object[]{userLogin}), e);
            throw new EventFailedException(processId,"","","","", new Exception(e.getMessage()));
        } 

        LOGGER.log(ODLLevel.NOTIFICATION, "End execute method.");
        return new EventResult();
    }

    /**
     * Executes the event handler on bulk event operation.
     * @param processId Process Id
     * @param eventId   Event Id
     * @param bulkOrchestration Bulk Orchestration contains target users information
     * @return A new BulkEventResult
     */
    @Override
    public BulkEventResult execute(long processId, long eventId, BulkOrchestration bulkOrchestration)
    {    
        LOGGER.log(ODLLevel.NOTIFICATION, "Enter bulk execute method: Process Id = {0}, Event Id = {1}", new Object[]{processId, eventId});
        
        // Get the user keys from the orchestration
        String[] entityIds = bulkOrchestration.getTarget().getAllEntityId();
        
        // Get changes for all users
        HashMap<String, Serializable>[] bulkParameters = bulkOrchestration.getBulkParameters();
        int numEvents = bulkParameters.length;
        LOGGER.log(ODLLevel.NOTIFICATION, "Number of Bulk Parameters: {0}", new Object[]{numEvents});
        
        // Modifying the orchestration parameters applies to all users
        if(numEvents > 0)
        {
            // Get the modified parameters; Shared among the users
            HashMap<String, Serializable> modParams = bulkParameters[0];
            LOGGER.log(ODLLevel.TRACE, "Modified Parameters: {0}", new Object[]{modParams});
            
            // Call method to modify the orchestration parameters
            this.updateOrchParams(modParams);
            LOGGER.log(ODLLevel.TRACE, "New Modified Parameters: {0}", new Object[]{modParams});
        }
        
        // Get interEventData
        HashMap<String, Serializable> interEventData = bulkOrchestration.getInterEventData();
        
        // Get list of users
        Object curUsersObj = interEventData.get("CURRENT_USER");
        Identity[] users  = (Identity[]) curUsersObj;
        int numUsers = users.length;
        LOGGER.log(ODLLevel.NOTIFICATION, "Number of users to process: {0}", new Object[]{numUsers});
        
        // Iterate each user; apply changes to each user
        for(int i = 0; i < numUsers; i++)
        {            
            // Get current user state
            User user = (User) users[i];
            LOGGER.log(ODLLevel.TRACE, "User: {0}", new Object[]{user});
            
            // Get USR_KEY of current user being modified
            String userKey = entityIds[i];
            LOGGER.log(ODLLevel.NOTIFICATION, "OIM User Key: {0}", new Object[]{userKey});
            
            // Get user login
            String userLogin = user.getLogin();
            LOGGER.log(ODLLevel.NOTIFICATION, "User Login: {0}", new Object[]{userLogin});
               
            try
            {
                // Modify the user attributes
                this.executeEvent(user);
            }
            
            catch (Exception e) 
            {
                LOGGER.log(ODLLevel.SEVERE, MessageFormat.format("Failed for {0}", new Object[]{userLogin}), e);
            }
        }
            
        LOGGER.log(ODLLevel.INFO, "End bulk execute.");
        return new BulkEventResult();
    }
    
    /**
     * Used to modify the orchestration parameters. This can be called in
     * the single and bulk execute. For bulk lock operation, modifying the 
     * orchestration parameters applies to all users given in the orchestration.
     * @param user  OIM User
     * @param orchParams   Orchestration parameters to add, remove, or update user attributes
     */
    private void updateOrchParams(HashMap<String,Serializable> orchParams) 
    {        
        // Modify orchestration parameters so OIM can propagate changes to downstream processes (E.g. Postprocess event handler, Process Tasks)
        // TODO: Apply your changes here.
        orchParams.put(UserManagerConstants.AttributeName.DEPARTMENT_NUMBER.getId(), "LOCKED"); // Department Number
        orchParams.put(UserManagerConstants.AttributeName.TITLE.getId(), "LOCKED"); // Title
        orchParams.put(UserManagerConstants.AttributeName.EMPLOYEE_NUMBER.getId(), "LOCKED"); // Employee Number
    }
    
    /**
     * Used if different attribute values are to be applied for each user. This can be called in
     * the single and bulk execute since this method processes one user at a time.
     * @param user  OIM User
     */
    private void executeEvent(User user) throws ValidationFailedException, AccessDeniedException, UserModifyException, NoSuchUserException, SearchKeyNotUniqueException 
    {
        String userLogin = user.getLogin();
        LOGGER.log(ODLLevel.NOTIFICATION, "Preparing updates for {0}", new Object[]{userLogin});
        HashMap<String,Object> attrs = new HashMap<String,Object>();
            
        // Update user via API
        // TODO: Apply your changes here.
        attrs.put(UserManagerConstants.AttributeName.STREET.getId(), "LOCKED: " + user.getLogin());
        attrs.put(UserManagerConstants.AttributeName.POSTAL_ADDRESS.getId(), "LOCKED: " + user.getId());
        User modUser = new User(userLogin, attrs);
        UserManagerResult result = USR_MGR.modify("User Login", userLogin, modUser);
        
        LOGGER.log(ODLLevel.NOTIFICATION, "Entity ID: {0}", new Object[]{result.getEntityId()});
        LOGGER.log(ODLLevel.NOTIFICATION, "Failed Results: {0}", new Object[]{result.getFailedResults()});
        LOGGER.log(ODLLevel.NOTIFICATION, "Status: {0}", new Object[]{result.getStatus()});
        LOGGER.log(ODLLevel.NOTIFICATION, "Succeeded Results: {0}", new Object[]{result.getSucceededResults()});
        LOGGER.log(ODLLevel.NOTIFICATION, "Updated {0} with {1}", new Object[]{userLogin, attrs});
    }

    @Override
    public void initialize(HashMap<String, String> arg0)
    {    
    }
    
    @Override
    public boolean cancel(long processId, long eventId, AbstractGenericOrchestration orchestration) 
    {
        return false;
    }

    @Override
    public void compensate(long processId, long eventId, AbstractGenericOrchestration orchestration) 
    {  
    }  
}
