package com.blogspot.oraclestack.eventhandlers;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.HashMap;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.identity.usermgmt.api.UserManagerConstants;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.platform.context.ContextAware;
import oracle.iam.platform.kernel.EventFailedException;
import oracle.iam.platform.kernel.spi.PreProcessHandler;
import oracle.iam.platform.kernel.vo.AbstractGenericOrchestration;
import oracle.iam.platform.kernel.vo.BulkEventResult;
import oracle.iam.platform.kernel.vo.BulkOrchestration;
import oracle.iam.platform.kernel.vo.EventResult;
import oracle.iam.platform.kernel.vo.Orchestration;

/**
 * Demonstrates implementing single and bulk execute method for a preprocess
 * event handler. This can also be used as a template for
 * implementing other preprocess event handlers. Place additional implementations/logic
 * in the executeEvent() method since both execute() methods make calls to it.
 * @author rayedchan
 */
public class UserCreatePreprocessEH implements PreProcessHandler 
{
    // Logger
    private static final ODLLogger LOGGER = ODLLogger.getODLLogger(UserCreatePreprocessEH.class.getName());

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

        // Get the modified parameters upon lock operation
        HashMap<String,Serializable> modParams = orchestration.getParameters();
        LOGGER.log(ODLLevel.TRACE, "Modified Paramerters: {0}", new Object[]{modParams});
   
        try 
        {
            // Modify the orchestration parameters for the target user
            this.executeEvent(modParams);
            LOGGER.log(ODLLevel.TRACE, "New Modified Parameters: {0}", new Object[]{modParams});
        } 
        
        catch (Exception e) 
        {
            LOGGER.log(ODLLevel.SEVERE, MessageFormat.format("Failed {0}", new Object[]{e.getMessage()}), e);
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
        
        try
        {
            // Get changes for all users
            HashMap<String, Serializable>[] bulkParameters = bulkOrchestration.getBulkParameters();
            int numEvents = bulkParameters.length;
            LOGGER.log(ODLLevel.NOTIFICATION, "Number of events: {0}", new Object[]{numEvents});

            // Iterate each user
            for(int i = 0; i < numEvents; i++)
            {            
                // Get the modified parameter of user
                HashMap<String, Serializable> modParams = bulkParameters[i];
                LOGGER.log(ODLLevel.TRACE, "User Modified Parameters: {0}", new Object[]{modParams});

                try
                {
                    // Modify the orchestration parameters for current user
                    this.executeEvent(modParams);
                    LOGGER.log(ODLLevel.TRACE, "New Modified Parameters: [{0}]", new Object[]{modParams});
                }

                catch (Exception e) 
                {
                    LOGGER.log(ODLLevel.SEVERE, MessageFormat.format("Failed {0}", new Object[]{e.getMessage()}), e);
                }
            }
        }
        
        catch (Exception e) 
        {
            LOGGER.log(ODLLevel.SEVERE, MessageFormat.format("Failed Bulk execute: {0}", new Object[]{e.getMessage()}), e);
            throw new EventFailedException(processId,"","","","", new Exception(e.getMessage()));
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
    private void executeEvent(HashMap<String,Serializable> orchParams) 
    {
        String userLogin = getParamaterValue(orchParams, UserManagerConstants.AttributeName.USER_LOGIN.getId());
        LOGGER.log(ODLLevel.INFO, "Preparing updates for {0}", new Object[]{userLogin});
        
        // Modify orchestration parameters so OIM can propagate changes to downstream processes (E.g. Postprocess event handler, Process Tasks)
        // TODO: Apply your changes here. Put user attributes you would like to change on the user.
        // Modifying the orchestration parameters applies to all users in orchestration for bulk operations
        orchParams.put(UserManagerConstants.AttributeName.DEPARTMENT_NUMBER.getId(), "CREATED"); // Department Number
        orchParams.put(UserManagerConstants.AttributeName.TITLE.getId(),  userLogin); // Title

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
    
      /**
     * Get an attribute value based on the attribute name.
     * @param parameters    User's attributes
     * @param key           Name of the attribute API name
     * @return              Value of the attribute to fetch
     */
    private String getParamaterValue(HashMap<String, Serializable> parameters, String key)
    {
        if(parameters.containsKey(key))
        {
            String value = (parameters.get(key) instanceof ContextAware) ? (String) ((ContextAware) parameters.get(key)).getObjectValue() : (String) parameters.get(key);
            return value;
        }
        else
        {
            return null;
        }
    }
}

