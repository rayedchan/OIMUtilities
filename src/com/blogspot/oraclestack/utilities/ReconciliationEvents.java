package com.blogspot.oraclestack.utilities;

import Thor.API.Exceptions.tcAPIException;
import java.util.Date;
import java.util.HashMap;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.platform.OIMClient;
import oracle.iam.reconciliation.api.ChangeType;
import oracle.iam.reconciliation.api.EventAttributes;
import oracle.iam.reconciliation.api.ReconOperationsService;

/**
 * Utilities to create reconciliation events in OIM.
 * @author rayedchan
 */
public class ReconciliationEvents 
{
    // Logger 
    private ODLLogger logger = ODLLogger.getODLLogger(ReconciliationEvents.class.getName());
    
    // OIM Services
    private final ReconOperationsService reconOps;
    
    /**
     * Constructor
     * @param oimClient OIM Client
     */
    public ReconciliationEvents(OIMClient oimClient)
    {
        this.reconOps = oimClient.getService(ReconOperationsService.class);
    }
    
    /**
     * Creates a reconciliation event and processes the event.
     * This method only handles parent data on the reconciliation event.
     * @param resourceObjName   Name of the Resource Object
     * @param reconFieldData    Map of the reconciliation field data
     * @throws tcAPIException 
     */
    public void makeReconciliationEvent(String resourceObjName, HashMap<String,Object> reconFieldData) throws tcAPIException
    {
        logger.log(ODLLevel.NOTIFICATION, "Enter makeReconciliationEvent() with parameters: Resource Object Name = [{0}], Reconciliation Data = [{1}]", new Object[]{resourceObjName, reconFieldData});
        
        // Setup Event Attributes
        EventAttributes evtAttrs = new EventAttributes();
        evtAttrs.setEventFinished(true); // Child is not going to be provided; Event will be in "Data Recieved" state
        evtAttrs.setActionDate(new Date()); // Use current date
        evtAttrs.setActionDate(null); // Processing is done instantly; no defering date
        evtAttrs.setChangeType(ChangeType.REGULAR); // For create and modify operations
        
        // Call OIM API to create reconciliation event 
        long reconEventKey = this.reconOps.createReconciliationEvent(resourceObjName, reconFieldData, evtAttrs);
        logger.log(ODLLevel.NOTIFICATION, "Reconciliation Event Key = [{0}]", new Object[]{reconEventKey});
        
        // Call OIM API to process reconciliation event (apply action and matching rules, and link to appropriate user, org, or process instance)
        this.reconOps.processReconciliationEvent(reconEventKey);
        logger.log(ODLLevel.NOTIFICATION, "Processed Recon Event.");
        
        // Close Event
        // this.reconOps.closeReconciliationEvent(reconEventKey);
        // logger.log(ODLLevel.NOTIFICATION, "Closed event.");
    }
}
