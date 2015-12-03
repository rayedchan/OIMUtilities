package com.blogspot.oraclestack.eventhandlers;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.HashMap;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.identity.usermgmt.api.UserManagerConstants;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.identity.vo.Identity;
import oracle.iam.platform.kernel.EventFailedException;
import oracle.iam.platform.kernel.spi.PreProcessHandler;
import oracle.iam.platform.kernel.vo.AbstractGenericOrchestration;
import oracle.iam.platform.kernel.vo.BulkEventResult;
import oracle.iam.platform.kernel.vo.BulkOrchestration;
import oracle.iam.platform.kernel.vo.EventResult;
import oracle.iam.platform.kernel.vo.Orchestration;

/**
 * Demonstrates implementing bulk execute method for a preprocess
 * event handler on user lock operation. This can also be used as a template for
 * implementing other preprocess event handlers. Place additional implementations/logic
 * in the executeEvent() method since both execute() methods make calls to it.
 * @author rayedchan
 */
public class UserLockPreprocessEH implements PreProcessHandler 
{
    // Logger
    private static final ODLLogger LOGGER = ODLLogger.getODLLogger(UserLockPreprocessEH.class.getName());

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
            this.executeEvent(user, modParams);
            LOGGER.log(ODLLevel.TRACE, "New Modified Parameters: {0}", new Object[]{modParams});
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
        
        // Get interEventData
        HashMap<String, Serializable> interEventData = bulkOrchestration.getInterEventData();
        
        // Get list of users
        Object curUsersObj = interEventData.get("CURRENT_USER");
        Identity[] users  = (Identity[]) curUsersObj;

        // Iterate each user
        for(int i = 0; i < entityIds.length; i++)
        {            
            // Get current user state
            User user = (User) users[i];
            LOGGER.log(ODLLevel.TRACE, "User: {0}", new Object[]{user});

            // Get the modified parameter of user
            HashMap<String, Serializable> modParams = bulkParameters[i];
            LOGGER.log(ODLLevel.TRACE, "User Modified Parameters: {0}", new Object[]{modParams});

            // Get USR_KEY of current user being modified
            String userKey = entityIds[i];
            LOGGER.log(ODLLevel.NOTIFICATION, "OIM User Key: {0}", new Object[]{userKey});
            
            // Get user login
            String userLogin = user.getLogin();
            LOGGER.log(ODLLevel.NOTIFICATION, "User Login: {0}", new Object[]{userLogin});
               
            try
            {
                // Modify the orchestration parameters for current user
                this.executeEvent(user, modParams);
                LOGGER.log(ODLLevel.TRACE, "New Modified Parameters: [{0}]", new Object[]{modParams});
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
     * Used to modify the orchestration user parameters. This can be called in
     * the single and bulk execute since this method processes one user at a time.
     * @param user  OIM User
     * @param orchParams   Orchestration parameters to add, remove, or update user attributes
     */
    private void executeEvent(User user, HashMap<String,Serializable> orchParams) 
    {
        String userLogin = user.getLogin();
        LOGGER.log(ODLLevel.INFO, "Preparing updates for {0}", new Object[]{userLogin});
        
        // Modify orchestration parameters so OIM can propagate changes to downstream processes (E.g. Postprocess event handler, Process Tasks)
        // TODO: Apply your changes here. Put user attributes you would like to change on the user.
        orchParams.put(UserManagerConstants.AttributeName.EMPLOYEE_NUMBER.getId(), "LOCKED" + user.getId()); // Employee Number
        orchParams.put(UserManagerConstants.AttributeName.DEPARTMENT_NUMBER.getId(), "LOCKED"); // Department Number
        orchParams.put(UserManagerConstants.AttributeName.TITLE.getId(), "LOCKED"); // Title
        
        LOGGER.log(ODLLevel.INFO, "Passed modified user attributes to orchestration for {0}", new Object[]{userLogin});
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
