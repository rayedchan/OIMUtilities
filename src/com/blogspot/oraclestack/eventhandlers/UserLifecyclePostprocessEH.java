package com.blogspot.oraclestack.eventhandlers;

import Thor.API.Exceptions.tcAPIException;
import Thor.API.Exceptions.tcColumnNotFoundException;
import Thor.API.Exceptions.tcInvalidLookupException;
import Thor.API.Exceptions.tcTaskNotFoundException;
import Thor.API.Operations.TaskDefinitionOperationsIntf;
import Thor.API.Operations.tcLookupOperationsIntf;
import Thor.API.Operations.tcProvisioningOperationsIntf;
import Thor.API.tcResultSet;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.platform.Platform;
import oracle.iam.platform.entitymgr.vo.SearchCriteria;
import oracle.iam.platform.kernel.EventFailedException;
import oracle.iam.platform.kernel.spi.PostProcessHandler;
import oracle.iam.platform.kernel.vo.AbstractGenericOrchestration;
import oracle.iam.platform.kernel.vo.BulkEventResult;
import oracle.iam.platform.kernel.vo.BulkOrchestration;
import oracle.iam.platform.kernel.vo.EventResult;
import oracle.iam.platform.kernel.vo.Orchestration;
import oracle.iam.provisioning.api.ProvisioningConstants;
import oracle.iam.provisioning.api.ProvisioningService;
import oracle.iam.provisioning.exception.GenericProvisioningException;
import oracle.iam.provisioning.exception.UserNotFoundException;
import oracle.iam.provisioning.vo.Account;

/**
 * This event handler supports the following user lifecycle events:
 * Enable, Disable, Lock, Unlock
 * On each operation, the corresponding lookup definition is used to
 * determine which process tasks to call for the downstream resource accounts.
 * @author rayedchan
 */
public class UserLifecyclePostprocessEH implements PostProcessHandler
{
    // Logger
    private static final ODLLogger LOGGER = ODLLogger.getODLLogger(UserLifecyclePostprocessEH.class.getName());
    
    // OIM API Service
    private static final UserManager USR_MGR_SERVICE = Platform.getServiceForEventHandlers(UserManager.class, null, "ADMIN","UserLifecyclePostprocessEH", null);
    private static final ProvisioningService PROV_SERVICE = Platform.getService(ProvisioningService.class);
    
    // Lookups Application Instance to Process Tasks Mapping
    private static final String LOOKUP_USER_LOCK_APPINSTNAME_PROCTASKS = "Lookup.User.Lock.AppInstNameToProcessTasks"; 
    private static final String LOOKUP_USER_UNLOCK_APPINSTNAME_PROCTASKS = "Lookup.User.Unlock.AppInstNameToProcessTasks";
    private static final String LOOKUP_USER_ENABLE_APPINSTNAME_PROCTASKS = "Lookup.User.Enable.AppInstNameToProcessTasks"; 
    private static final String LOOKUP_USER_DISABLE_APPINSTNAME_PROCTASKS = "Lookup.User.Disable.AppInstNameToProcessTasks";

    // Internal Map to map an operation to a lookup
    public HashMap<String,String> operationToLookup = new HashMap<String,String>();
    
    // Delimiter for separating multiple process tasks
    public static final String DELIMITER = ",";
    
    /**
     * Single event 
     * @param processId
     * @param eventId
     * @param orchestration
     * @return
     */
    @Override
    public EventResult execute(long processId, long eventId, Orchestration orchestration) 
    {
        LOGGER.log(ODLLevel.NOTIFICATION, "Enter execute() with parameters: Process Id = [{0}], Event Id = [{1}], Orchestration = [{2}]", new Object[]{processId, eventId, orchestration});
        String appInstToProcTaskLookup = null;
        
        // OIM tc* Services
        tcProvisioningOperationsIntf provOps = null;
        TaskDefinitionOperationsIntf taskDefOps = null;
        tcLookupOperationsIntf lookupOps = null;
        
        // Get Target Type
        String targetType = orchestration.getTarget().getType();
        LOGGER.log(ODLLevel.NOTIFICATION, "Target type: {0}", new Object[]{targetType});

        // Get Operation
        String operation = orchestration.getOperation();
        LOGGER.log(ODLLevel.NOTIFICATION, "Operation: {0}", new Object[]{operation});
            
        try
        {
            // Get tc* Services
            provOps = Platform.getService(tcProvisioningOperationsIntf.class);
            taskDefOps = Platform.getService(TaskDefinitionOperationsIntf.class);
            lookupOps = Platform.getService(tcLookupOperationsIntf.class);
                                                
            // Get corresponding lookup for operation
            appInstToProcTaskLookup = this.operationToLookup.get(operation);
            LOGGER.log(ODLLevel.NOTIFICATION, "Using Lookup: {0}", new Object[]{appInstToProcTaskLookup});
            
            // Get USR_KEY of current user being modified
            String userKey = orchestration.getTarget().getEntityId();
            LOGGER.log(ODLLevel.NOTIFICATION, "Target OIM User Key: {0}", new Object[]{userKey});
            
            // Contains old and new values of target user
            HashMap<String, Serializable> interEventData = orchestration.getInterEventData();
            LOGGER.log(ODLLevel.TRACE, "InterEventData: {0}", new Object[]{interEventData});
            
            // Get new user state
            User newUserState = (User) interEventData.get("NEW_USER_STATE");
            LOGGER.log(ODLLevel.TRACE, "User: {0}", new Object[]{newUserState});
            
            // Get old user state
            User oldUserState = (User) interEventData.get("CURRENT_USER");
            LOGGER.log(ODLLevel.TRACE, "Old User: {0}", new Object[]{oldUserState});
                        
            // Get Resource To Process Tasks Lookup; Code is Resource Object Name; Decode is String delimited Process Tasks
            HashMap<String,String> resourceToProcTasksMap = this.convertLookupToMap(appInstToProcTaskLookup, lookupOps);
            LOGGER.log(ODLLevel.INFO, "Resource To Process Tasks Mapping: [{0}]", new Object[]{resourceToProcTasksMap});
            
            // Construct criteria based on application instance display name given in lookup (code key)
            SearchCriteria criteria = this.constructOrCriteria(resourceToProcTasksMap.keySet(), ProvisioningConstants.AccountSearchAttribute.DISPLAY_NAME.getId());
            
            // Execute event
            this.callProcessTasksForUserResourceAccounts(newUserState, provOps, taskDefOps, PROV_SERVICE, resourceToProcTasksMap, criteria);       
        } 
        
        catch (tcInvalidLookupException e) 
        {
            LOGGER.log(ODLLevel.WARNING, MessageFormat.format("Continue {0} since lookup {1} does not exist: {2}.", new Object[]{operation, appInstToProcTaskLookup, e.getMessage()}), e);
        }
        
        catch (UserNotFoundException e) 
        {
            LOGGER.log(ODLLevel.ERROR, e.getMessage(), e);
            throw new EventFailedException(processId,"","","","", new Exception(e.getMessage()));
        } 
        
        catch (GenericProvisioningException e)
        {
            LOGGER.log(ODLLevel.ERROR, e.getMessage(), e);
            throw new EventFailedException(processId,"","","","", new Exception(e.getMessage()));
        } 
        
        catch (tcAPIException e) 
        {
            LOGGER.log(ODLLevel.ERROR, e.getMessage(), e);
            throw new EventFailedException(processId,"","","","", new Exception(e.getMessage()));
        } 
        
        catch (tcColumnNotFoundException e)
        {
            LOGGER.log(ODLLevel.ERROR, e.getMessage(), e);
            throw new EventFailedException(processId,"","","","", new Exception(e.getMessage()));
        } 
        
        catch (tcTaskNotFoundException e) 
        {
            LOGGER.log(ODLLevel.ERROR, e.getMessage(), e);
            throw new EventFailedException(processId,"","","","", new Exception(e.getMessage()));
        }

        catch(Exception e)
        {
            LOGGER.log(ODLLevel.ERROR, e.getMessage(), e);
            throw new EventFailedException(processId,"","","","", new Exception(e.getMessage()));
        }
        
        finally 
        {
            // Close tc* service
            if(provOps != null)
            {
                provOps.close();
            }
            
            if(taskDefOps != null)
            {
                taskDefOps.close();
            }
            
            if (lookupOps != null) 
            {
                lookupOps.close();
            }
        }
        
        return new EventResult();   
    }

    /**
     * Bulk events
     * @param processId
     * @param eventId
     * @param bulkOrchestration
     * @return
     */
    @Override
    public BulkEventResult execute(long processId, long eventId, BulkOrchestration bulkOrchestration) 
    {
        /*LOGGER.log(ODLLevel.NOTIFICATION, "Enter execute() with parameters: Process Id = [{0}], Event Id = [{1}], Bulk Orchestration = [{2}]", new Object[]{processId, eventId, bulkOrchestration});
        boolean failedProcess = false;
        boolean failedProcessPerUser = false;
        Exception exception = null;
        tcProvisioningOperationsIntf provOps = null;
        TaskDefinitionOperationsIntf taskDefOps = null; 
        
        try
        {
            // Get tc* Services
            provOps = Platform.getService(tcProvisioningOperationsIntf.class);
            taskDefOps = Platform.getService(TaskDefinitionOperationsIntf.class);
            
            // Get Resource To Process Tasks Lookup; Code is Resource Object Name; Decode is String delimited Process Tasks
            HashMap<String,String> resourceToProcTasksMap = ResourceAttributeCalculation.convertLookupToMap(LOOKUP_USER_LOCK_RESOURCE_PROCTASKS);
            LOGGER.log(ODLLevel.INFO, "Resource To Process Tasks Mapping: [{0}]", new Object[]{resourceToProcTasksMap});
            
            // Construct criteria based on resource object given in lookup
            int count = 0; // Used to construct criteria
            SearchCriteria resourceObjectsCriteria = null; // Used to filter for certain resource objects
            // Iterate all the resource objects given in lookup map to construct criteria
            for(String resourceObjectName: resourceToProcTasksMap.keySet()) 
            {
                // One criteria
                if(count == 0) 
                {
                    resourceObjectsCriteria = new SearchCriteria(ProvisioningConstants.AccountSearchAttribute.OBJ_NAME.getId(), resourceObjectName, SearchCriteria.Operator.EQUAL);     
                }
                
                // Appending OR criteria
                else 
                {
                    SearchCriteria entCriteria = new SearchCriteria(ProvisioningConstants.AccountSearchAttribute.OBJ_NAME.getId(), resourceObjectName, SearchCriteria.Operator.EQUAL);     
                    resourceObjectsCriteria = new SearchCriteria(resourceObjectsCriteria, entCriteria, SearchCriteria.Operator.OR);
                }
                
                count++;
            }
            
            // Get the user records from the orchestration argument
            String[] entityIds = bulkOrchestration.getTarget().getAllEntityId();
            
            // Get interParameters
            HashMap<String, Serializable> interParameters = bulkOrchestration.getInterEventData();
        
            // Get the new state of all users
            Object usersObj = interParameters.get("NEW_USER_STATE");
            Identity[] users  = (Identity[]) usersObj;
            
            // Get the old state of all users
            Object prevUsersObj = interParameters.get("CURRENT_USER");
            Identity[] prevUsers  = (Identity[]) prevUsersObj;
                                
            // Iterate each OIM user
            for (int i = 0; i < entityIds.length; i++) 
            {
                failedProcessPerUser = false;
                
                try
                {
                    // Get USR_KEY of current userbeing modified
                    String userKey = entityIds[i];
                    LOGGER.log(ODLLevel.NOTIFICATION, "Target OIM User Key: [{0}]", new Object[]{userKey});
        
                    // Get Target Type
                    String targetType = bulkOrchestration.getTarget().getType();
                    LOGGER.log(ODLLevel.NOTIFICATION, "Target type: [{0}]", new Object[]{targetType});
        
                    // Get new user state
                    User user = (User) users[i];
                    LOGGER.log(ODLLevel.TRACE, "New User State: [{0}]", new Object[]{user});
                    
                    // Get old user state
                    User oldUser = (User) prevUsers[i];
                    LOGGER.log(ODLLevel.TRACE, "Old User State: [{0}]", new Object[]{oldUser});
                    
                    // Check user access control type
                    String accessControlType = (String) user.getAttribute(UserDefinedFields.UDF_ACCESS_CONTROL_TYPE);
                    LOGGER.log(ODLLevel.NOTIFICATION, "Access Control Type: [{0}]", new Object[]{accessControlType});
                    
                    // Only process users whose access control type is SUS.
                    if ("SUS".equals(accessControlType))
                    {
                        // Process single event
                        this.executeEvent(user, provOps, taskDefOps, resourceToProcTasksMap, resourceObjectsCriteria);  
                    }
                    
                    else
                    {
                        LOGGER.log(ODLLevel.NOTIFICATION, "Access Control Type is not SUS; no action taken for {0}.", new Object[]{user.getLogin()});
                    }
                }
                
                catch (UserNotFoundException e) 
                {
                    LOGGER.log(ODLLevel.ERROR, e.getMessage(), e);
                    failedProcessPerUser = true;
                    exception = e;      
                } 
                
                catch (GenericProvisioningException e) 
                {
                    LOGGER.log(ODLLevel.ERROR, e.getMessage(), e);
                    failedProcessPerUser = true;
                    exception = e;      
                }
                
                catch (tcAPIException e) 
                {
                    LOGGER.log(ODLLevel.ERROR, e.getMessage(), e);
                    failedProcessPerUser = true;
                    exception = e;
                } 
                
                catch (tcColumnNotFoundException e)
                {
                    LOGGER.log(ODLLevel.ERROR, e.getMessage(), e);
                    failedProcessPerUser = true;
                    exception = e;
                } 
                
                catch (tcTaskNotFoundException e)
                {
                    LOGGER.log(ODLLevel.ERROR, e.getMessage(), e);
                    failedProcessPerUser = true;
                    exception = e;
                }

                catch(Exception e)
                {
                    LOGGER.log(ODLLevel.ERROR, e.getMessage(), e);
                    failedProcessPerUser = true;
                    exception = e;
                }
                
                finally 
                {
                    if(failedProcessPerUser == true) 
                    {   
                        LOGGER.log(ODLLevel.ERROR, "Send notification to administrator for failed user");
                        HashMap<String,Object> templateParameters = new HashMap<String,Object>();
                        templateParameters.put(NotificationParameters.PROCESS.toString(), PROCESS);
                        templateParameters.put(NotificationParameters.EXCEPTIONCODE.toString(), "N/A");
                        templateParameters.put(NotificationParameters.EXCEPTIONTEXT.toString(), exception.getMessage());
                        notification.sendNotificationByRole(NotificationConstants.ADMINISTRATORS.toString(), NotificationConstants.EXCEPTION_TEMPLATE.toString(), templateParameters);
                    }
                }
            } 
        } 
        
        catch(Exception e)
        {
            LOGGER.log(ODLLevel.ERROR, e.getMessage(), e);
            failedProcess = true;
            exception = e;
        }
        
        finally 
        {
            // Close tc* service
            if(provOps != null)
            {
                provOps.close();
            }
            
            if(taskDefOps != null)
            {
                taskDefOps.close();
            }
            
            if(failedProcess == true) 
            {
                LOGGER.log(ODLLevel.ERROR, "Send notification to administrator to retry scheduled task");
                HashMap<String,Object> templateParameters = new HashMap<String,Object>();
                templateParameters.put(NotificationParameters.PROCESS.toString(), PROCESS);
                templateParameters.put(NotificationParameters.EXCEPTIONCODE.toString(), "N/A");
                templateParameters.put(NotificationParameters.EXCEPTIONTEXT.toString(), exception.getMessage());
                notification.sendNotificationByRole(NotificationConstants.ADMINISTRATORS.toString(), NotificationConstants.EXCEPTION_TEMPLATE.toString(), templateParameters);
                throw new EventFailedException(processId,"","","","", new Exception(exception.getMessage()));
            }
        }*/
        
        return new BulkEventResult();
    }
    
    @Override
    public boolean cancel(long l, long l1, AbstractGenericOrchestration ago) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Call to initialize event handler.
     * @param hm 
     */
    @Override
    public void initialize(HashMap<String, String> hm)
    {
        LOGGER.log(ODLLevel.NOTIFICATION, "Begin Initialize: {0}", new Object[]{hm});
        
        // Map operation-lookup pair
        operationToLookup.put("DISABLE", LOOKUP_USER_DISABLE_APPINSTNAME_PROCTASKS);
        operationToLookup.put("ENABLE", LOOKUP_USER_ENABLE_APPINSTNAME_PROCTASKS);
        operationToLookup.put("LOCK", LOOKUP_USER_LOCK_APPINSTNAME_PROCTASKS);
        operationToLookup.put("UNLOCK", LOOKUP_USER_UNLOCK_APPINSTNAME_PROCTASKS);
        
        LOGGER.log(ODLLevel.NOTIFICATION, "End Initialize.");
    }

    @Override
    public void compensate(long l, long l1, AbstractGenericOrchestration ago) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    /**
     * Converts a lookup definition into a Map. The Code Key column is used as
     * the key and the Decode column is used as the value (E.g. {Key= Code Key, Value= Decode}).
     * @param lookupDefinitionName      Name of the lookup definition
     * @param tcLookupOperationsIntf    Lookup API Service
     * @return Map of lookup values
     * @throws tcAPIException
     * @throws tcInvalidLookupException
     * @throws tcColumnNotFoundException
     */
    private HashMap<String,String> convertLookupToMap(String lookupDefinitionName, tcLookupOperationsIntf lookupOps) throws tcAPIException, tcInvalidLookupException, tcColumnNotFoundException
    {
        HashMap<String, String> lookupValues = new HashMap<String,String>();
        tcResultSet lookupValuesRs = lookupOps.getLookupValues(lookupDefinitionName); // Get lookup values
        int numRows = lookupValuesRs.getTotalRowCount();

        // Iterate lookup resultset and construct map 
        for(int i = 0; i < numRows; i++)
        {
            lookupValuesRs.goToRow(i); // move pointer in result set
            String codeKey = lookupValuesRs.getStringValue("Lookup Definition.Lookup Code Information.Code Key"); // Fetch Code Key
            String decode = lookupValuesRs.getStringValue("Lookup Definition.Lookup Code Information.Decode"); // Fetch Decode
            lookupValues.put(codeKey, decode); // add key-value pair to map
        }

        return lookupValues;
    }
    
    /**
     * Manually call process tasks on a user's resource accounts based on a lookup definition or a constructed Map.
     * The code key is the resource object name and the decode value is a comma-delimited value of each
     * process task name to call.
     * @param user                      OIM User
     * @param provOps                   Provisioning Service
     * @param taskDefOps                Task Definition Service
     * @param provService               Provisioning Service
     * @param resourceToProcTasksMap    Mapping of Resource Object to Process Tasks
     * @param resourceObjectsCriteria   Used to specify which resource accounts to inspect
     * @throws UserNotFoundException
     * @throws GenericProvisioningException
     * @throws tcAPIException
     * @throws tcColumnNotFoundException
     * @throws tcTaskNotFoundException
     */
    private void callProcessTasksForUserResourceAccounts(User user, tcProvisioningOperationsIntf provOps, TaskDefinitionOperationsIntf taskDefOps, ProvisioningService provService, HashMap<String,String> resourceToProcTasksMap, SearchCriteria resourceObjectsCriteria) throws UserNotFoundException, GenericProvisioningException, tcAPIException, tcAPIException, tcColumnNotFoundException, tcTaskNotFoundException 
    {
        String userLogin = user.getLogin();
        String userKey = user.getId(); // Get usr_key
        boolean populateAccountData = true;
        HashMap<String,Object> configParams = null;
        LOGGER.log(ODLLevel.INFO, "Begin event for user: USR_KEY = {0}, User Login = {1}", new Object[]{userKey, userLogin}); 
        
        //ProvisioningConstants.AccountSearchAttribute.APPINST_NAME
        // Get user's resource accounts based on criteria     
        List<Account> accounts = provService.getAccountsProvisionedToUser(userKey, resourceObjectsCriteria, configParams, populateAccountData); // API will return nothing if null criteria is provided 
        LOGGER.log(ODLLevel.INFO, "Total Accounts to Process: {0}", new Object[]{accounts.size()});
        
        // Iterate User's accounts of a specific resource object
        for(Account resourceAcct: accounts)
        {
           String accountId = resourceAcct.getAccountID(); // OIU_KEY
           String procInstFormKey = resourceAcct.getProcessInstanceKey(); // (ORC_KEY) Process Form Instance Key 
           String appInstName = resourceAcct.getAppInstance().getApplicationInstanceName(); // Application Instance Name
           String resourceObjectName = resourceAcct.getAppInstance().getObjectName(); // Resource Object Name
           LOGGER.log(ODLLevel.NOTIFICATION, "Account Id: {0}", new Object[]{accountId});
           LOGGER.log(ODLLevel.NOTIFICATION, "Process Instance Form Key: {0}", new Object[]{procInstFormKey});
           LOGGER.log(ODLLevel.NOTIFICATION, "Application Instance Name: {0}", new Object[]{appInstName});
           LOGGER.log(ODLLevel.NOTIFICATION, "Object Name: {0}", new Object[]{resourceObjectName});
           
           // Get delimited process tasks from lookup
           String delimitedProcTasks = resourceToProcTasksMap.get(resourceObjectName);
           String[] procTaskNames = delimitedProcTasks.split(DELIMITER);
           LOGGER.log(ODLLevel.TRACE, "Resource Object: {0}, Process Tasks: {1}", new Object[]{resourceObjectName, Arrays.asList(procTaskNames)});
           
           // Handle resource accounts that been written to UD table.
           // This excludes resources in Waiting State
           if(procInstFormKey != null && !procInstFormKey.equalsIgnoreCase("0"))
           {
               // Call each process task instance for given resource account
               for(String procTaskName: procTaskNames) 
               {
                   // Get a specific process task 
                   HashMap<String,String> filter = new HashMap<String,String>();
                   filter.put("Process Definition.Tasks.Task Name", procTaskName);
                   tcResultSet results = taskDefOps.getTaskDetail(Long.valueOf(procInstFormKey), filter);
                   int rows = results.getTotalRowCount();
                   String procDefTaskKey = null;
                   
                   // Should only be one since Process Task Name is unique
                   for(int i = 0; i < rows; i++)
                   {
                       results.goToRow(i);
                       procDefTaskKey = results.getStringValue("Process Definition.Tasks.Key"); // MIL_KEY
                   }
                   
                   LOGGER.log(ODLLevel.NOTIFICATION, "Process Definition Task Key: {0}", new Object[]{procDefTaskKey});
                   
                   if(procDefTaskKey != null)
                   {         
                       // Call a process task directly on an application instance
                       long schKey = provOps.addProcessTaskInstance(Long.valueOf(procDefTaskKey), Long.valueOf(procInstFormKey));
                       LOGGER.log(ODLLevel.NOTIFICATION, "Called Process Task: User = {1}, Application Name = {3}, Task Name = {2}, Task Instance Key = {0}", new Object[]{schKey, userLogin, procTaskName, appInstName});
                   }
                   
                   else 
                   {
                       LOGGER.log(ODLLevel.WARNING, "Process Task Instance Name Not Found: {0}", new Object[]{procTaskName});
                   }
               }
           }
            
           else 
           {
               LOGGER.log(ODLLevel.NOTIFICATION, "Skip resource {0}. Status = {1}", new Object[]{appInstName, resourceAcct.getAccountStatus()});
           }
        }
                
        LOGGER.log(ODLLevel.NOTIFICATION, "Finished event for user: USR_KEY = {0}, User Login = {1}", new Object[]{userKey, userLogin}); 
    }
    
    /**
     * Construct an OR criteria
     * E.g. element1 || element2 || element3  
     * @param elements String elements to construct criteria
     * @param searchAttribute   Search Attribute (E.g. ProvisioningConstants.AccountSearchAttribute.DISPLAY_NAME.getId())
     * @return constructed criteria object 
     */
    private SearchCriteria constructOrCriteria(Set<String> elements, String searchAttribute)
    {
        // Used to construct criteria
        int count = 0;
        
        // Used to build the final criteria
        SearchCriteria allCriteria = null; 
        
        // Iterate each element in the set
        for(String element: elements) 
        {
            // One criteria
            if(count == 0) 
            {
                allCriteria = new SearchCriteria(searchAttribute, element, SearchCriteria.Operator.EQUAL);     
            }

            // Appending OR criteria
            else 
            {
                SearchCriteria concatCriteria = new SearchCriteria(searchAttribute, element, SearchCriteria.Operator.EQUAL);     
                allCriteria = new SearchCriteria(allCriteria, concatCriteria, SearchCriteria.Operator.OR);
            }

            count++;
        }
        
        return allCriteria;
    }
}
