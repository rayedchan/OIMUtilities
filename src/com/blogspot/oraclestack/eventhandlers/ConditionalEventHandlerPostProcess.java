package com.blogspot.oraclestack.eventhandlers;

import java.util.HashMap;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.identity.usermgmt.api.UserManagerConstants;
import oracle.iam.platform.Platform;
import oracle.iam.platform.entitymgr.EntityManager;
import oracle.iam.platform.kernel.EventFailedException;
import oracle.iam.platform.kernel.spi.ConditionalEventHandler;
import oracle.iam.platform.kernel.spi.PostProcessHandler;
import oracle.iam.platform.kernel.vo.AbstractGenericOrchestration;
import oracle.iam.platform.kernel.vo.BulkEventResult;
import oracle.iam.platform.kernel.vo.BulkOrchestration;
import oracle.iam.platform.kernel.vo.EventResult;
import oracle.iam.platform.kernel.vo.Orchestration;

/**
 * Conditional Post Process Event Handler
 * The ConditionalEventHandler interface has a method called isApplicable(), which
 * determines whether the event handler should be executed in the current context.
 * In this example, the event handler is triggered on the creation of a user who is an employee.
 * The post process event handler populates the Employee Number attribute with USR_KEY if the condition
 * is satisfied.
 * 
 * Side Note: Validation and preprocess event handlers can also be conditional. Inspect 
 * the ORCHEVENTS table in the OIM Schema to see the trigger sequences of the event handler.
 * 
 * @author rayedchan
 */
public class ConditionalEventHandlerPostProcess implements ConditionalEventHandler, PostProcessHandler 
{
    private static final ODLLogger LOGGER = ODLLogger.getODLLogger(ConditionalEventHandlerPostProcess.class.getName());
    
    @Override
    public EventResult execute(long l, long l1, Orchestration o) 
    {
        LOGGER.log(ODLLevel.NOTIFICATION, "Enter execute() V1 with parameters: [{0}], [{1}], [{2}]", new Object[]{l, l1, o});
        
        try
        {
            // Get the user attributes that were filled out on creation
            HashMap<String, String> params = o.getParameters();
            LOGGER.log(ODLLevel.NOTIFICATION, "Incoming User Attributes on create: {0}", new Object[]{params});

            // Get the target user's USR_KEY
            String usrKey = o.getTarget().getEntityId();
            LOGGER.log(ODLLevel.NOTIFICATION, "Target User Key: {0}", new Object[]{usrKey});

            // Get Entity Type (Should be User)
            String entityType = o.getTarget().getType();
            LOGGER.log(ODLLevel.NOTIFICATION, "Entity Type: {0}", new Object[]{entityType});

            // Get Entity Manager Service
            EntityManager entMgrService = Platform.getService(EntityManager.class); // use this instead of User Manager API in order to prevent event handlers(modify) from triggering again

            // Populate Employee Number with USR_KEY
            HashMap<String,Object> modAttrs = new HashMap<String,Object>();
            modAttrs.put(UserManagerConstants.AttributeName.EMPLOYEE_NUMBER.getId(), usrKey);
            entMgrService.modifyEntity(entityType, usrKey, modAttrs);
            LOGGER.log(ODLLevel.NOTIFICATION, "Successfully updated Employee Number");
        }
        
        catch(Exception ex)
        {
            LOGGER.log(ODLLevel.ERROR, "Exception in execute(): ", ex);
            throw new EventFailedException("", null, ex);
        }
        
        return new EventResult();
    }

    @Override
    public BulkEventResult execute(long l, long l1, BulkOrchestration bo)
    {
        LOGGER.log(ODLLevel.NOTIFICATION, "Enter Bulk execute() with parameters: [{0}], [{1}], [{2}]", new Object[]{l, l1, bo});
        return new BulkEventResult();
    }

    @Override
    public void compensate(long l, long l1, AbstractGenericOrchestration ago)
    {
        LOGGER.log(ODLLevel.NOTIFICATION, "Enter compensate() with parameters: [{0}], [{1}], [{2}]", new Object[]{l, l1, ago});
    }

    @Override
    public boolean cancel(long l, long l1, AbstractGenericOrchestration ago) 
    {
        LOGGER.log(ODLLevel.NOTIFICATION, "Enter cancel() with parameters: [{0}], [{1}], [{2}]", new Object[]{l, l1, ago});
        return false;
    }

    @Override
    public void initialize(HashMap<String, String> hm)
    {
        LOGGER.log(ODLLevel.NOTIFICATION, "Enter initialize() with parameter: [{0}]", new Object[]{hm});
    }

    /**
     * Triggered on the conditional phase of the orchestration stage.
     * Method returns true if target user is an employee, and returns false otherwise
     * @param ago Orchestration
     * @return if true the event handler will execute
     */
    @Override
    public boolean isApplicable(AbstractGenericOrchestration ago) 
    {
        LOGGER.log(ODLLevel.NOTIFICATION, "Enter isApplicable() with parameter: [{0}]", new Object[]{ago});
        
        // Get the user attributes that were filled out on creation
        HashMap<String, String> params = ago.getParameters();
        LOGGER.log(ODLLevel.NOTIFICATION, "Incoming User Attributes on create: {0}", new Object[]{params});
        
        // Get User Type attribute
        String userType = params.get(UserManagerConstants.AttributeName.EMPTYPE.getId());
        LOGGER.log(ODLLevel.NOTIFICATION, "User Type: {0}", new Object[]{userType});
        
        // Check if user is an Employee
        boolean isApplicable = "EMP".equalsIgnoreCase(userType);
        LOGGER.log(ODLLevel.NOTIFICATION, "is Applicable?: {0}", new Object[]{isApplicable});
        
        // True if user is employee, false otherwise 
        return isApplicable;
    }
}
