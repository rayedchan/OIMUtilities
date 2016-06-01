package com.blogspot.oraclestack.utilities;

import Thor.API.Exceptions.tcAPIException;
import Thor.API.Exceptions.tcAttributeNotFoundException;
import Thor.API.Exceptions.tcEventDataReceivedException;
import Thor.API.Exceptions.tcEventNotFoundException;
import Thor.API.Exceptions.tcObjectNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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
     * @param reconFieldData    Map of the reconciliation field data. Key is the name of the reconciliation field and value is the data.
     * @throws tcAPIException 
     */
    public void makeReconciliationEvent(String resourceObjName, HashMap<String,Object> reconFieldData) throws tcAPIException, tcObjectNotFoundException
    {
        logger.log(ODLLevel.NOTIFICATION, "Enter makeReconciliationEvent() with parameters: Resource Object Name = [{0}], Reconciliation Data = [{1}]", new Object[]{resourceObjName, reconFieldData});
        
        // Setup Event Attributes
        EventAttributes evtAttrs = new EventAttributes();
        evtAttrs.setEventFinished(true); // Child is not going to be provided; Event will be in "Data Recieved" state
        evtAttrs.setActionDate(null); // Processing is done instantly; no defering date
        evtAttrs.setChangeType(ChangeType.CHANGELOG); // For create and modify operations
        
        // Determine if event needs to be ignored (E.g. No change)
        boolean ignoreEvent = this.reconOps.ignoreEvent(resourceObjName, reconFieldData);
        logger.log(ODLLevel.NOTIFICATION, "Ignore event? {0}", new Object[]{ignoreEvent});
       
        if(!ignoreEvent)
        {
            // Call OIM API to create reconciliation event 
            long reconEventKey = this.reconOps.createReconciliationEvent(resourceObjName, reconFieldData, evtAttrs);
            logger.log(ODLLevel.NOTIFICATION, "Reconciliation Event Key = [{0}]", new Object[]{reconEventKey});
        
            // Call OIM API to process reconciliation event (apply action and matching rules, and link to appropriate user, org, or process instance)
            //this.reconOps.processReconciliationEvent(reconEventKey);
            //logger.log(ODLLevel.NOTIFICATION, "Processed Recon Event.");
        }
                
        // Close Event
        // this.reconOps.closeReconciliationEvent(reconEventKey);
        // logger.log(ODLLevel.NOTIFICATION, "Closed event.");
    }
    
    /**
     * Creates a reconciliation event and processes the event.
     * This method handles parent data with child data on the reconciliation event.
     * @param resourceObjName   Name of the Resource Object
     * @param reconFieldData    Map of the reconciliation field data
     * @throws tcAPIException 
     */
    public void makeReconciliationEventWithChildData(String resourceObjName, HashMap<String,Object> parentData, HashMap<String, ArrayList<HashMap<String,Object>>> childData) throws tcAPIException, tcEventNotFoundException, tcEventDataReceivedException, tcAttributeNotFoundException
    {
        logger.log(ODLLevel.NOTIFICATION, "Enter makeReconciliationEventWithChildData() with parameters: Resource Object Name = [{0}], Parent Data = [{1}], Child Data = [{2}] ", new Object[]{resourceObjName, parentData, childData});
        
        // Setup Event Attributes
        EventAttributes evtAttrs = new EventAttributes();
        evtAttrs.setEventFinished(false); // Child is going to be provided; Event will be in "Event Recieved" state
        evtAttrs.setActionDate(null); // Processing is done instantly; no defering date
        evtAttrs.setChangeType(ChangeType.CHANGELOG); // For create and modify operations with incomplete or just required dataset.
        
        // Call OIM API to create reconciliation event 
        long reconEventKey = this.reconOps.createReconciliationEvent(resourceObjName, parentData, evtAttrs);
        logger.log(ODLLevel.NOTIFICATION, "Reconciliation Event Key = [{0}]", new Object[]{reconEventKey});
        
        // Setup child data on reconciliation event
        for (Map.Entry<String, ArrayList<HashMap<String,Object>>> entry : childData.entrySet()) 
        {
            String reconFieldChildMapName = entry.getKey();
            ArrayList<HashMap<String,Object>> childTableEntries = entry.getValue();
            logger.log(ODLLevel.NOTIFICATION, "Recon Field Multivalued Name = [{0}], Child Data = [{1}]", new Object[]{reconFieldChildMapName, childTableEntries});
            
            // Add each child recotrd
            for(HashMap<String,Object> childEntry: childTableEntries)
            {
                this.reconOps.addMultiAttributeData(reconEventKey, reconFieldChildMapName, childEntry);
            }
        }
        
        // Marks the status of a reconciliation event as 'Data Received' which was left in status 'Event Received' to allow additional data (child table data) to be added
        this.reconOps.finishReconciliationEvent(reconEventKey); 

        // Call OIM API to process reconciliation event (apply action and matching rules, and link to appropriate user, org, or process instance)
        this.reconOps.processReconciliationEvent(reconEventKey);
        logger.log(ODLLevel.NOTIFICATION, "Processed Recon Event.");
        
        // Close Event
        // this.reconOps.closeReconciliationEvent(reconEventKey);
        // logger.log(ODLLevel.NOTIFICATION, "Closed event.");
    }
}
