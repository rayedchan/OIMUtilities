package com.blogspot.oraclestack.validation;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;

/**
 * Reconciliation Event Data Validation Example
 * When validation fails, the reconciliation event will be skipped. OIM will not
 * create a reconciliation event.
 * @author rayedchan
 */
public class ReconciliationEventDataTelephoneNumberValidation
{
    // Logger
    private static final ODLLogger LOGGER = ODLLogger.getODLLogger(ReconciliationEventDataTelephoneNumberValidation.class.getName());
   
    /**
     * Validates if the incoming data has the proper phone number format:
     * +X-XXX-XXX-XXXX 
     * @param hmUserDetails         HashMap<String,Object> containing parent data details
     * @param hmEntitlementDetails  HashMap<String,Object> containing child data details
     * @param sField                Name of the reconciliation field being validated
     * @return true if target field passes validation; false otherwise
     */
    public boolean validate(HashMap<String,Object> hmUserDetails, HashMap<String,Object> hmEntitlementDetails, String sField) 
    {
        LOGGER.log(ODLLevel.NOTIFICATION, "Parameters: Parent Data = {0}, Child Data = {1}, Field = {2}", new Object[]{hmUserDetails, hmEntitlementDetails, sField});
        
        boolean valid = false;
        String phoneNumber = (String) hmUserDetails.get(sField); // Get value using the reconciliation field name
        
        // Empty value case
        if(phoneNumber == null || "".equalsIgnoreCase(phoneNumber))
        {
            LOGGER.log(ODLLevel.NOTIFICATION, "No data provided. Pass validation.");
            return true;
        }
        
        String phoneNumberRegex = "\\+\\d(-\\d{3}){2}-\\d{4}"; // Phone Number Regular Expression: +1-111-111-1111
        Pattern pattern = Pattern.compile(phoneNumberRegex);
        Matcher matcher = pattern.matcher(phoneNumber);
        valid = matcher.matches(); // Checks input against the regex
        
        LOGGER.log(ODLLevel.NOTIFICATION, "Is {0} = {1} valid? {2}", new Object[]{sField, phoneNumber, valid});
        return valid;
    }
}
