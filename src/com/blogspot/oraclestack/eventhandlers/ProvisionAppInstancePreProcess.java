package com.blogspot.oraclestack.eventhandlers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.api.UserManagerConstants;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.platform.Platform;
import oracle.iam.platform.kernel.EventFailedException;
import oracle.iam.platform.kernel.spi.ConditionalEventHandler;
import oracle.iam.platform.kernel.spi.PreProcessHandler;
import oracle.iam.platform.kernel.vo.AbstractGenericOrchestration;
import oracle.iam.platform.kernel.vo.BulkEventResult;
import oracle.iam.platform.kernel.vo.BulkOrchestration;
import oracle.iam.platform.kernel.vo.EventResult;
import oracle.iam.platform.kernel.vo.Orchestration;
import oracle.iam.provisioning.vo.ChildTableRecord;
import oracle.iam.provisioning.vo.ChildTableRecord.ACTION;

/**
 * Populates the fields on a specific application instance form
 * on the initial provisioning stage.
 * @author rayedchan
 */
public class ProvisionAppInstancePreProcess implements PreProcessHandler, ConditionalEventHandler
{
    // TODO: Change variables accordingly 
    private static final String BADGE_ACCESS_DISCONNECTED_RESOURCE_APP_INST_KEY = "1"; // TODO: Environment dependent application key; create lookup to make dynamic
    private static final String BADGE_ACCESS_CHILD_FORM_BUILDING_ACCESS = "UD_BACCESS"; // Child table
    private static final String BUILDING_ACCESS_FIELD_NAME = "Name"; // Child form field
    private static final String BUILDING_ACCESS_FIELD_DESCRIPTION = "Description"; // Child form field
    private static final String DEFAULT_ACCESS_ONE = "Core"; // Name of entitlement to assign
    private static final String DEFAULT_ACCESS_TWO ="Computer Department"; // Name of entitlement to assign
    
    // Logger
    private static final ODLLogger LOGGER = ODLLogger.getODLLogger(ProvisionAppInstancePreProcess.class.getName());
    
    // OIM API Services
    private UserManager usrMgr = Platform.getService(UserManager.class);
    
    /**
     * Determines if Badge Access application instance is being provisioned.
     * If no, do not trigger the event handler execute() method.
     * @param abstractGenericOrchestration  Orchestration object
     * @return true to execute the event handler execute() method; otherwise false to not trigger event handler
     */
    @Override
    public boolean isApplicable(AbstractGenericOrchestration abstractGenericOrchestration) 
    {
        LOGGER.log(ODLLevel.NOTIFICATION, "Enter isApplicable() with parameter: AbstractGenericOrchestration = {0}", new Object[]{abstractGenericOrchestration});
        boolean isApplicable = false; 
        
        String operationType = abstractGenericOrchestration.getOperation();
        LOGGER.log(ODLLevel.NOTIFICATION, "Operation: {0}", new Object[]{operationType}); // PROVISION
        
        HashMap<String, Serializable> modParams = abstractGenericOrchestration.getParameters();
        LOGGER.log(ODLLevel.NOTIFICATION, "Modified Parameters: {0}", new Object[]{modParams}); // {ParentData={Account Login=TSWIFT2, serviceaccount=false, ITResource=0}, BeneficiaryKey=22, AppInstanceKey=1, ParentRequestId=, ChildData={}}
        
        HashMap<String, Serializable> interEventData = abstractGenericOrchestration.getInterEventData();
        LOGGER.log(ODLLevel.NOTIFICATION, "InterEventData: {0}", new Object[]{interEventData}); // null
        
        // Single Orchestration
        if(abstractGenericOrchestration instanceof Orchestration) 
        {            
            String entityId = abstractGenericOrchestration.getTarget().getEntityId();
            LOGGER.log(ODLLevel.NOTIFICATION, "Entity Id: {0}", new Object[]{entityId}); // Application Instance Key
            
            String[] entityIds = abstractGenericOrchestration.getTarget().getAllEntityId();
            
            for(String userId : entityIds) 
            {
                LOGGER.log(ODLLevel.NOTIFICATION, "Entity Ids: {0}", new Object[]{userId});
            }
            
            // Get application instance key from parameters
            String appInstKey = (String) modParams.get("AppInstanceKey");
            LOGGER.log(ODLLevel.NOTIFICATION, "Application Instance Key: {0}", new Object[]{appInstKey});
            
            // True for application instance key equal to 1; otherwise false 
            isApplicable = (BADGE_ACCESS_DISCONNECTED_RESOURCE_APP_INST_KEY.equalsIgnoreCase(appInstKey))? true: false;
        }
        
        // Bulk Orchestration
        else if (abstractGenericOrchestration instanceof BulkOrchestration)
        {
            BulkOrchestration bulkOrchestration = (BulkOrchestration) abstractGenericOrchestration;
            
            // Get the user records from the orchestration argument
            String[] entityIds = bulkOrchestration.getTarget().getAllEntityId();
            
            // Get every changes from all users
            HashMap<String, Serializable>[] bulkParameters = bulkOrchestration.getBulkParameters();
            LOGGER.log(ODLLevel.NOTIFICATION, "All Modified Bulk Parameters: {0}", new Object[]{bulkParameters});
            
            // Get interParameters
            HashMap<String, Serializable> interParameters = bulkOrchestration.getInterEventData();
            LOGGER.log(ODLLevel.NOTIFICATION, "Bulk InterEventData: {0}", new Object[]{interParameters}); // No interdata in conditional stage
            
            // Iterate each OIM user
            for (int i = 0; i < entityIds.length; i++)
            {
                // Get the modified UDFs
                HashMap<String, Serializable> modUDFs = bulkParameters[i];
                
                // Get USR_KEY of current userbeing modified
                String userKey = entityIds[i];
                LOGGER.log(ODLLevel.NOTIFICATION, "Target OIM User Key = [{0}], Modified UDFs = [{1}]", new Object[]{userKey, modUDFs});
            }
            
            // Always return true on bulk
            isApplicable = true;
        }
          
        LOGGER.log(ODLLevel.NOTIFICATION, "Trigger event handler: {0}", new Object[]{isApplicable});;
        return isApplicable;
    }
    
    /**
     * Populates the application instance form.
     * - Get values from UDFs to populate form fields (parent data)
     * - Assign default access or groups (child data)
     * @param processId
     * @param eventId
     * @param orchestration
     * @return 
     */
    @Override
    public EventResult execute(long processId, long eventId, Orchestration orchestration) 
    {
        LOGGER.log(ODLLevel.NOTIFICATION, "Enter execute() with parameters: Process Id = [{0}], Event Id = [{1}], Orchestration = [{2}]", new Object[]{processId, eventId, orchestration});
        
        try
        {            
            // Get the modified parameters of application instance
            HashMap<String, Serializable> modParams = orchestration.getParameters();
            LOGGER.log(ODLLevel.NOTIFICATION, "Modified Orchestration Params: [{0}]", new Object[]{modParams});

            // Get USR_KEY of current userbeing modified
            String appInstKey = orchestration.getTarget().getEntityId();
            LOGGER.log(ODLLevel.NOTIFICATION, "Application Instance Key: [{0}]", new Object[]{appInstKey});

            // Get Target Type
            String targetType = orchestration.getTarget().getType();
            LOGGER.log(ODLLevel.NOTIFICATION, "Target type: [{0}]", new Object[]{targetType}); // ApplicationInstance

            // Get Target User Key
            String usrKey = (String) modParams.get("BeneficiaryKey");
            LOGGER.log(ODLLevel.NOTIFICATION, "User Key: {0}", new Object[]{usrKey});
            
            // Get Target User Profile
            boolean isUserLogin = false;
            Set<String> retAttrs = new HashSet<String>(); // return attribute data to be on user object
            retAttrs.add(UserManagerConstants.AttributeName.FIRSTNAME.getId());
            retAttrs.add(UserManagerConstants.AttributeName.LASTNAME.getId());
            retAttrs.add(UserManagerConstants.AttributeName.EMPTYPE.getId());
            retAttrs.add(UserManagerConstants.AttributeName.USER_LOGIN.getId());
            User user = usrMgr.getDetails(usrKey, retAttrs, isUserLogin);
            LOGGER.log(ODLLevel.NOTIFICATION, "User: {0}", new Object[]{user});
            
            // Get Parent Data from application instance
            HashMap<String, Serializable> parentData = (HashMap<String, Serializable>) modParams.get("ParentData");
            HashMap<String, Serializable> childData = (HashMap<String, Serializable>) modParams.get("ChildData");
            
            // Get individual data from application instance form (ParentData)
            String appInstFirstName = (String) parentData.get("First Name");
            String appInstLastName = (String) parentData.get("Last Name");
            String appInstUserType = (String) parentData.get("User Type");
            String appInstAccountLogin = (String) parentData.get("Account Login");
            String appInstAccountID = (String) parentData.get("Account ID");
            LOGGER.log(ODLLevel.TRACE, "Values on form: First Name = [{0}], Last Name = [{1}], User Type = [{2}], Account Login = [{3}], Account ID = [{4}]", new Object[]{appInstFirstName, appInstLastName, appInstUserType, appInstAccountLogin, appInstAccountID});
            
            // Get child data on a particular child form
            ArrayList<ChildTableRecord> childRecords = (ArrayList<ChildTableRecord>) childData.get(BADGE_ACCESS_CHILD_FORM_BUILDING_ACCESS);
            
            // Populate application form field if there is no data providied for field
            if(appInstFirstName == null || appInstFirstName.equalsIgnoreCase(""))
            {
                parentData.put("First Name", user.getFirstName());
            }
            
            if(appInstLastName == null || appInstLastName.equalsIgnoreCase(""))
            {
                parentData.put("Last Name", user.getLastName());
            }
            
            if(appInstUserType == null || appInstUserType.equalsIgnoreCase(""))
            {
                parentData.put("User Type", user.getEmployeeType());
            }
            
            if(appInstAccountLogin == null || appInstAccountLogin.equalsIgnoreCase(""))
            {
                parentData.put("Account Login", user.getLogin());
            }
            
            if(appInstAccountID == null || appInstAccountID.equalsIgnoreCase(""))
            {
                parentData.put("Account ID", user.getLogin());
            }
            
            // parentData.put("serviceaccount", true); // Mark as a service account
            
            // Populate Child Form
            // No child records provided
            if(childRecords == null || childRecords.isEmpty())
            {
                // Assign default access or groups
                // Top level objects to store child records
                childRecords = new ArrayList<ChildTableRecord>();
                
                // Child Record One
                HashMap<String,Object> addRecordData = new HashMap<String,Object>();
                addRecordData.put(BUILDING_ACCESS_FIELD_NAME, DEFAULT_ACCESS_ONE);
                addRecordData.put(BUILDING_ACCESS_FIELD_DESCRIPTION, "Default Access One");
                ChildTableRecord addRecord = new ChildTableRecord();
                addRecord.setAction(ACTION.Add);
                addRecord.setChildData(addRecordData);
                
                // Child Record Two
                HashMap<String,Object> addRecordData2 = new HashMap<String,Object>();
                addRecordData2.put(BUILDING_ACCESS_FIELD_NAME, DEFAULT_ACCESS_TWO);
                addRecordData2.put(BUILDING_ACCESS_FIELD_DESCRIPTION, "Default Access Two");
                ChildTableRecord addRecord2 = new ChildTableRecord();
                addRecord2.setAction(ACTION.Add);
                addRecord2.setChildData(addRecordData2);
                
                // Add child data to top level objects
                childRecords.add(addRecord);
                childRecords.add(addRecord2);
                childData.put(BADGE_ACCESS_CHILD_FORM_BUILDING_ACCESS, childRecords);  
            }
            
            // Child records provided
            else
            {
                // Used to determine if the default access has been added
                boolean foundDefaultOne = false;
                boolean foundDefaultTwo = false;
                
                // Inspect exisiting child records (data provided by user on initial provisioning)
                for(ChildTableRecord childRecord: childRecords)
                {
                    Map<String,Object> cData = childRecord.getChildData();
                    String cAction = childRecord.getAction().toString();
                    String cRowKey = childRecord.getRowKey();
                    String cName = (String) cData.get(BUILDING_ACCESS_FIELD_NAME); // Key attribute on entitlement
                    LOGGER.log(ODLLevel.TRACE, "Child Record Data: {0}, Action: {1}, Row Key:{2}", new Object[]{cData, cAction, cRowKey});
                    
                    // Check if default access exists in form 
                    if(DEFAULT_ACCESS_ONE.equals(cName))
                    {
                        foundDefaultOne = true;
                    }
                    
                    else if(DEFAULT_ACCESS_TWO.equals(cName))
                    {
                        foundDefaultTwo = true;
                    }
                }
                
                // Add default access one
                if(!foundDefaultOne)
                {
                    // Child Record One
                    HashMap<String,Object> addRecordData = new HashMap<String,Object>();
                    addRecordData.put(BUILDING_ACCESS_FIELD_NAME, DEFAULT_ACCESS_ONE);
                    addRecordData.put(BUILDING_ACCESS_FIELD_DESCRIPTION, "Default Access One");
                    ChildTableRecord addRecord = new ChildTableRecord();
                    addRecord.setAction(ACTION.Add);
                    addRecord.setChildData(addRecordData);
                    childRecords.add(addRecord);
                }
                
                // Add default access two
                if(!foundDefaultTwo)
                {
                    // Child Record Two
                    HashMap<String,Object> addRecordData2 = new HashMap<String,Object>();
                    addRecordData2.put(BUILDING_ACCESS_FIELD_NAME, DEFAULT_ACCESS_TWO);
                    addRecordData2.put(BUILDING_ACCESS_FIELD_DESCRIPTION, "Default Access Two");
                    ChildTableRecord addRecord2 = new ChildTableRecord();
                    addRecord2.setAction(ACTION.Add);
                    addRecord2.setChildData(addRecordData2);
                    childRecords.add(addRecord2);
                }
                
                // Replace child data (maybe the same or may contain additional records)
                childData.put(BADGE_ACCESS_CHILD_FORM_BUILDING_ACCESS, childRecords);   
            }
            
            // Overwrite parameters in orchestration 
            modParams.put("ParentData", parentData); // Use to overwrite "ParentData" field in orchestration
            modParams.put("ChildData", childData); // Use to overwrite "ChildData" field in orchestration
            LOGGER.log(ODLLevel.TRACE, "New orchestration parameters: {0}", new Object[]{modParams});
            
            // Set changes in orchestration
            orchestration.setParameter(modParams);
            LOGGER.log(ODLLevel.TRACE, "Successfully modifed orchestration parameters.");
        } 
        
        catch (Exception e) 
        {
            LOGGER.log(ODLLevel.ERROR, e.getMessage(), e);
            throw new EventFailedException(processId,"","","","", new Exception(e.getMessage()));
        } 

        return new EventResult();
    }

    @Override
    public BulkEventResult execute(long l, long l1, BulkOrchestration bo) 
    {
        return new BulkEventResult();
    }

    @Override
    public void compensate(long l, long l1, AbstractGenericOrchestration ago) 
    {

    }

    @Override
    public boolean cancel(long l, long l1, AbstractGenericOrchestration ago)
    {
       return false;
    }

    @Override
    public void initialize(HashMap<String, String> hm)
    {
    
    }
}
