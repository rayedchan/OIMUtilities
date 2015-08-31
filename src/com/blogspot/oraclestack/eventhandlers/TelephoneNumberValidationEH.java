package com.blogspot.oraclestack.eventhandlers;

import java.io.Serializable;
import java.util.HashMap;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.identity.usermgmt.api.UserManagerConstants;
import oracle.iam.identity.usermgmt.utils.UserManagerUtils;
import oracle.iam.platform.context.ContextAware;
import oracle.iam.platform.kernel.ValidationException;
import oracle.iam.platform.kernel.ValidationFailedException;
import oracle.iam.platform.kernel.spi.ConditionalEventHandler;
import oracle.iam.platform.kernel.spi.ValidationHandler;
import oracle.iam.platform.kernel.vo.AbstractGenericOrchestration;
import oracle.iam.platform.kernel.vo.BulkOrchestration;
import oracle.iam.platform.kernel.vo.Orchestration;

/**
 * Validates if Telephone Number is in one of the following formats:
 * <ul>
 *      <li>###-###-####</li>
 *      <li>###-####</li>
 *      <li>##########</li>
 * </ul>
 * 
 * #Custom Message
 * UIAM-1058202 = {0} must conform to one of the following formats: {1}
 * @author rayedchan
 */
public class TelephoneNumberValidationEH implements ConditionalEventHandler, ValidationHandler
{
    // Logger
    private static final ODLLogger LOGGER = ODLLogger.getODLLogger(TelephoneNumberValidationEH.class.getName()); 
    
    /**
     * Executes the validate method if Telephone Number is being modified.
     * @param ago   Orchestration Object
     * @return true for executing event handler; false for not executing event handler
     */
    @Override
    public boolean isApplicable(AbstractGenericOrchestration ago) 
    {
        LOGGER.log(ODLLevel.NOTIFICATION, "Enter isApplicable() with parameter: {0}", new Object[]{ago});
        boolean retValue = false;
        
        // Get changed parameters 
        HashMap<String,Serializable> modParams = ago.getParameters();
        LOGGER.log(ODLLevel.NOTIFICATION, "Modified Parameters: {0}", new Object[]{modParams});
        
        if(modParams != null)
        {
            // Determine if user's Telephone Number changed
            retValue = modParams.containsKey(UserManagerConstants.AttributeName.PHONE_NUMBER.getId()); // Telephone Number
        }
        
        LOGGER.log(ODLLevel.NOTIFICATION, "Execute TelephoneNumberValidationEH? {0}", new Object[]{retValue});
        return retValue;
    }

    /**
     * Performs validation.
     * @param processId
     * @param eventId
     * @param orchestration
     * @throws ValidationException
     * @throws ValidationFailedException 
     */
    @Override
    public void validate(long processId, long eventId, Orchestration orchestration) throws ValidationException, ValidationFailedException 
    {
        LOGGER.log(ODLLevel.NOTIFICATION, "Enter execute() with parameters: Process Id = [{0}], Event Id = [{1}], Orchestration = [{2}]", new Object[]{processId, eventId, orchestration});
        
        // Get the parameters that changed
        HashMap<String,Serializable> modParams = orchestration.getParameters();
        LOGGER.log(ODLLevel.NOTIFICATION, "Modified Parameters: {0}", new Object[]{modParams});
        
        // Get Telephone Number
        String telephoneNumber = this.getParamaterValue(modParams, UserManagerConstants.AttributeName.PHONE_NUMBER.getId());
        LOGGER.log(ODLLevel.NOTIFICATION, "New Telephone Number: {0}", new Object[]{telephoneNumber});
        
        // Empty telephone passes validation
        if(telephoneNumber == null || "".equalsIgnoreCase(telephoneNumber))
        {
             LOGGER.log(ODLLevel.NOTIFICATION, "Validation Passed: Empty telephone number.");
        }
        
        else
        {
            String phoneNumberRegex = "(\\d{3}-){1,2}\\d{4}|\\d{10}"; // ###-###-####, ###-####,##########
            boolean isValid = telephoneNumber.matches(phoneNumberRegex); // Perform regular expression check
            LOGGER.log(ODLLevel.NOTIFICATION, "Is Telephone Number Valid? {0}", new Object[]{isValid});
            
            if(!isValid)
            {
                String errorKey = "UIAM-1058202";
                String formats = "(1)###-###-#### (2)###-#### (3)##########";
                ValidationFailedException ex = UserManagerUtils.createValidationFailedException(errorKey, new Object[]{UserManagerConstants.AttributeName.PHONE_NUMBER.getId(), formats});
                LOGGER.log(ODLLevel.ERROR, "", ex);
                throw ex;
            }
        }
    }

    @Override
    public void validate(long processId, long eventId, BulkOrchestration bulkOrchestration) throws ValidationException, ValidationFailedException 
    {
        LOGGER.log(ODLLevel.NOTIFICATION, "Enter execute() with parameters: Process Id = [{0}], Event Id = [{1}], Bulk Orchestration = [{2}]", new Object[]{processId, eventId, bulkOrchestration});
    }

    @Override
    public void initialize(HashMap<String, String> hm)
    {
        LOGGER.log(ODLLevel.NOTIFICATION, "Enter initialize() with parameter: {0}", new Object[]{hm});
    }
    
    /**
    * ContextAware object is obtained when the actor is a regular user.
    * If the actor is an administrator, the exact value of the attribute is obtained.
    * @param parameters    parameters from the orchestration object
    * @param key   name of User Attribute in OIM Profile or column in USR table
    * @return value of the corresponding key in parameters
    */
    private String getParamaterValue(HashMap<String, Serializable> parameters, String key)
    {
        String value = (parameters.get(key) instanceof ContextAware)
        ? (String) ((ContextAware) parameters.get(key)).getObjectValue()
        : (String) parameters.get(key);
        return value;
    }
}
