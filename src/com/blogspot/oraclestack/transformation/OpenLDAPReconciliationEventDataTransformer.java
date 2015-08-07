package com.blogspot.oraclestack.transformation;

import java.util.HashMap;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;

/**
 * Reconciliation Transformation Example
 * Uses First Name and Last Name values from the target system
 * and constructs Display Name by concatenate both values.
 * @author rayedchan
 */
public class OpenLDAPReconciliationEventDataTransformer 
{
    // Logger
    private static final ODLLogger LOGGER = ODLLogger.getODLLogger(OpenLDAPReconciliationEventDataTransformer.class.getName());
           
    /**
     * Method for transforming the attributes
     * @param parentData    HashMap<String,Object> containing parent data details
     * @param childData     HashMap<String,Object> containing child data details
     * @param reconField    Name of reconciliation field being transformed
     * @return New value for reconciliation field being operated on
     */
    public Object transform(HashMap<String,Object> parentData, HashMap<String,Object> childData, String reconField)
    {
        LOGGER.log(ODLLevel.NOTIFICATION, "Parameters: Parent Data = {0}, Child Data = {1}, Field = {2}", new Object[]{parentData, childData, reconField});
        
        // Get values using the traget system data. Use reconciliation field name to fetch value.
        String firstName = (String) parentData.get("First Name");
        LOGGER.log(ODLLevel.NOTIFICATION, "First Name = {0}", new Object[]{firstName});
        String lastName = (String) parentData.get("Last Name");
        LOGGER.log(ODLLevel.NOTIFICATION, "Last Name = {0}", new Object[]{lastName});
        
        // Construct Display Name
        String displayName = firstName + "." + lastName;
        LOGGER.log(ODLLevel.NOTIFICATION, "Populating {1} with value = {0}", new Object[]{displayName, reconField});
        return displayName;
    }
}
