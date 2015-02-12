package com.blogspot.oraclestack.adapters;

import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;

/**
 * Contain methods for prepopulate adapters. These adapters are used to populate
 * the process form instance of a resource account. In Oracle Identity Manager,
 * Prepopulate adapters are executed on the initial assignment of a resource account to a user.
 * @author rayedchan
 */
public class PrepopulateAdapters 
{
    // Logger
    private static final ODLLogger LOGGER = ODLLogger.getODLLogger(PrepopulateAdapters.class.getName());
    
    /**
     * Concatenate two strings with a pipe as a separator.
     * @param left      Beginning of string
     * @param right     Gets appended to left string
     * @return combination two string with a pipe delimiter
     */
    public static String concatenateString(String left, String right)
    {
        LOGGER.log(ODLLevel.TRACE, "Enter concatenateString() with parameters: [ Left: [{0}]], [ Right: [{1}]]", new Object[]{left, right});
        String result = left + "|" + right;
        LOGGER.log(ODLLevel.TRACE, "Result: {0}", new Object[]{result});
        return result;
    }
    
    /**
     * Calculates a fictitious code based on the User Type attribute on the OIM user profile.
     * The code key from Lookup.Users.Role is passed to this method.
     * @param userType Pass OIM user attribute "User Type" 
     * @return code
     */
    public static String calculateUserTypeCode(String userType)
    {
        String result = "";
        LOGGER.log(ODLLevel.TRACE, "Enter calculateUserTypeCode() with parameter: [ User Type: [{0}]]", new Object[]{userType});
        
        if("Full-Time".equals(userType))
        {
            result = "1000";
        }
        
        else if("EMP".equals(userType))
        {
            result = "2000";
        }
        
        else if("Consultant".equalsIgnoreCase(userType))
        {
            result = "3000";
        }
        
        else
        {
            result = "9999";
        }
        
        LOGGER.log(ODLLevel.TRACE, "Result: {0}", new Object[]{result});
        return result;
    }
}
