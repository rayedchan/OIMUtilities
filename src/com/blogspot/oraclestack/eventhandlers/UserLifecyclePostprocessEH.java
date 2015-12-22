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
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.identity.vo.Identity;
import oracle.iam.platform.Platform;
import oracle.iam.platform.entitymgr.vo.SearchCriteria;
import oracle.iam.platform.kernel.EventFailedException;
import oracle.iam.platform.kernel.spi.ConditionalEventHandler;
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
 * This event handler supports the following user life-cycle events:
 * Enable, Disable, Lock, Unlock
 * On each operation, the corresponding lookup definition is used to
 * determine which process tasks to call for the downstream resource accounts.
 * 
 * NOTE: LOCK operation might have to be in a separate event handler. On locking of a user
 * lock and modify operations happen. Preventing the modify operation stop this event handler
 * from continuing. It is recommended to use this event handler on a specific operation
 * rather than ANY operation.
 * 
 * @author rayedchan
 */
public class UserLifecyclePostprocessEH implements ConditionalEventHandler, PostProcessHandler
{
    // Logger
    private static final ODLLogger LOGGER = ODLLogger.getODLLogger(UserLifecyclePostprocessEH.class.getName());
    
    // OIM API Service
    private static final ProvisioningService PROV_SERVICE = Platform.getService(ProvisioningService.class);
    
    // Lookups Application Instance Display Name to Process Tasks Mapping
    private static final String LOOKUP_USER_LOCK_APPINST_DISPLAYNAME_PROCTASKS = "Lookup.User.Lock.AppInstDisplayNameToProcessTasks"; 
    private static final String LOOKUP_USER_UNLOCK_APPINST_DISPLAYNAME_PROCTASKS = "Lookup.User.Unlock.AppInstDisplayNameToProcessTasks";
    private static final String LOOKUP_USER_ENABLE_APPINST_DISPLAYNAME_PROCTASKS = "Lookup.User.Enable.AppInstDisplayNameToProcessTasks"; 
    private static final String LOOKUP_USER_DISABLE_APPINST_DISPLAYNAME_PROCTASKS = "Lookup.User.Disable.AppInstDisplayNameToProcessTasks";

    // Internal Map to map an operation to a lookup
    private HashMap<String,String> operationToLookup = new HashMap<String,String>();
    
    // Delimiter for separating multiple process tasks
    private static final String DELIMITER = ",";
    
    // Operations supported for this event handler
    String[] supportedOps = {"DISABLE","ENABLE","LOCK","UNLOCK", "MODIFY"}; // Used in isApplicable()
    
    /**
     * Determine to execute event handler if supported operation is provided.
     * @param ago   Orchestration
     * @return true to proceed execution of event handler; false to end execution of event handler
     */
    @Override
    public boolean isApplicable(AbstractGenericOrchestration ago) 
    {
        String operation = ago.getOperation();
        LOGGER.log(ODLLevel.NOTIFICATION, "Operation: {0}", new Object[]{operation});
        boolean proceed = false;
        
        for(String op : this.supportedOps)
        {
            if(op.equals(operation))
            {
                proceed = true;
            }
        }
        
        LOGGER.log(ODLLevel.NOTIFICATION, "Execute Event Handler: {0}", new Object[]{proceed});
        return proceed;
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
        this.operationToLookup.put("DISABLE", LOOKUP_USER_DISABLE_APPINST_DISPLAYNAME_PROCTASKS);
        this.operationToLookup.put("ENABLE", LOOKUP_USER_ENABLE_APPINST_DISPLAYNAME_PROCTASKS);
        this.operationToLookup.put("LOCK", LOOKUP_USER_LOCK_APPINST_DISPLAYNAME_PROCTASKS);
        this.operationToLookup.put("UNLOCK", LOOKUP_USER_UNLOCK_APPINST_DISPLAYNAME_PROCTASKS);
        
        LOGGER.log(ODLLevel.NOTIFICATION, "End Initialize: {0}", new Object[]{this.operationToLookup});
    }
        
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
            
            if(appInstToProcTaskLookup != null && !"".equalsIgnoreCase(appInstToProcTaskLookup))
            {
                 // Get USR_KEY of current user being modified
                String userKey = orchestration.getTarget().getEntityId();
                LOGGER.log(ODLLevel.NOTIFICATION, "Target OIM User Key: {0}", new Object[]{userKey});
                
                // Get modified parameters
                HashMap<String,Serializable> modParams = orchestration.getParameters();
                LOGGER.log(ODLLevel.NOTIFICATION, "Modified Parameters: {0}", new Object[]{modParams});

                // Contains old and new values of target user
                HashMap<String, Serializable> interEventData = orchestration.getInterEventData();
                LOGGER.log(ODLLevel.TRACE, "InterEventData: {0}", new Object[]{interEventData});

                // Get new user state
                User newUserState = (User) interEventData.get("NEW_USER_STATE");
                LOGGER.log(ODLLevel.TRACE, "User: {0}", new Object[]{newUserState});

                // Get old user state
                User oldUserState = (User) interEventData.get("CURRENT_USER");
                LOGGER.log(ODLLevel.TRACE, "Old User: {0}", new Object[]{oldUserState});

                // Get Resource To Process Tasks Lookup; Code is Application Instance Display Name Name; Decode is String delimited Process Tasks
                HashMap<String,String> appInstDisplayNameToProcTasksMap = this.convertLookupToMap(appInstToProcTaskLookup, lookupOps);
                LOGGER.log(ODLLevel.NOTIFICATION, "Application Instance Display Name To Process Tasks Mapping: {0}", new Object[]{appInstDisplayNameToProcTasksMap});

                // Construct criteria based on application instance display name given in lookup (code key)
                SearchCriteria criteria = this.constructOrCriteria(appInstDisplayNameToProcTasksMap.keySet(), ProvisioningConstants.AccountSearchAttribute.DISPLAY_NAME.getId());

                // Execute event
                this.callProcessTasksForUserResourceAccounts(newUserState, provOps, taskDefOps, PROV_SERVICE, appInstDisplayNameToProcTasksMap, criteria); 
            }
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
        LOGGER.log(ODLLevel.NOTIFICATION, "Enter execute() with parameters: Process Id = [{0}], Event Id = [{1}], Bulk Orchestration = [{2}]", new Object[]{processId, eventId, bulkOrchestration});
        String appInstToProcTaskLookup = null;
        
        // OIM tc* Services
        tcProvisioningOperationsIntf provOps = null;
        TaskDefinitionOperationsIntf taskDefOps = null;
        tcLookupOperationsIntf lookupOps = null;
        
        // Get Target Type
        String targetType = bulkOrchestration.getTarget().getType();
        LOGGER.log(ODLLevel.NOTIFICATION, "Target type: {0}", new Object[]{targetType});
        
        // Get Operation
        String operation = bulkOrchestration.getOperation();
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
            
            if(appInstToProcTaskLookup != null && !"".equalsIgnoreCase(appInstToProcTaskLookup))
            {
                // Get Resource To Process Tasks Lookup; Code is Application Instance Display Name Name; Decode is String delimited Process Tasks
                HashMap<String,String> appInstDisplayNameToProcTasksMap = this.convertLookupToMap(appInstToProcTaskLookup, lookupOps);
                LOGGER.log(ODLLevel.NOTIFICATION, "Application Instance Display Name To Process Tasks Mapping: {0}", new Object[]{appInstDisplayNameToProcTasksMap});

                // Construct criteria based on application instance display name given in lookup (code key)
                SearchCriteria criteria = this.constructOrCriteria(appInstDisplayNameToProcTasksMap.keySet(), ProvisioningConstants.AccountSearchAttribute.DISPLAY_NAME.getId());

                // Get the user records from the orchestration argument
                String[] entityIds = bulkOrchestration.getTarget().getAllEntityId();
                int numUsers = entityIds.length;
                LOGGER.log(ODLLevel.NOTIFICATION, "{0} user keys: {1}", new Object[]{numUsers, Arrays.toString(entityIds)});

                // Get bulk Parameters
                HashMap<String, Serializable>[] bulkParameters = bulkOrchestration.getBulkParameters();
                int numEvents = bulkParameters.length;
                LOGGER.log(ODLLevel.NOTIFICATION, "Number of Bulk Parameters Events: {0}", new Object[]{numEvents});
                LOGGER.log(ODLLevel.TRACE, "Bulk Parameters: {0}", new Object[]{Arrays.toString(bulkParameters)});
                
                // Get InterEventData
                HashMap<String, Serializable> interEventData = bulkOrchestration.getInterEventData();
                LOGGER.log(ODLLevel.TRACE, "InterEventData: {0}", new Object[]{interEventData});

                // Get the new state of all users
                Object usersObj = interEventData.get("NEW_USER_STATE");
                Identity[] users  = (Identity[]) usersObj;

                // Get the old state of all users
                Object prevUsersObj = interEventData.get("CURRENT_USER");
                Identity[] prevUsers  = (Identity[]) prevUsersObj;

                // Iterate each OIM user
                for(int i = 0; i < numUsers; i++) 
                {
                    // Get USR_KEY of current userbeing modified
                    String userKey = entityIds[i];
                    LOGGER.log(ODLLevel.NOTIFICATION, "Target OIM User Key: {0}", new Object[]{userKey});

                    // Get new user state
                    User newUserState = (User) users[i];
                    LOGGER.log(ODLLevel.TRACE, "New User State: {0}", new Object[]{newUserState});

                    // Get old user state
                    User oldUserState = (User) prevUsers[i];
                    LOGGER.log(ODLLevel.TRACE, "Old User State: {0}", new Object[]{oldUserState});

                    try
                    {                    
                        // Execute event
                        this.callProcessTasksForUserResourceAccounts(newUserState, provOps, taskDefOps, PROV_SERVICE, appInstDisplayNameToProcTasksMap, criteria);
                    }

                    catch (UserNotFoundException e) 
                    {
                        LOGGER.log(ODLLevel.ERROR, e.getMessage(), e);      
                    } 

                    catch (GenericProvisioningException e) 
                    {
                        LOGGER.log(ODLLevel.ERROR, e.getMessage(), e);    
                    }

                    catch (tcAPIException e) 
                    {
                        LOGGER.log(ODLLevel.ERROR, e.getMessage(), e);
                    } 

                    catch (tcColumnNotFoundException e)
                    {
                        LOGGER.log(ODLLevel.ERROR, e.getMessage(), e);
                    } 

                    catch (tcTaskNotFoundException e)
                    {
                        LOGGER.log(ODLLevel.ERROR, e.getMessage(), e);
                    }

                    catch(Exception e)
                    {
                        LOGGER.log(ODLLevel.ERROR, e.getMessage(), e);
                    }
                } 
            }
            
            else
            {
                LOGGER.log(ODLLevel.WARNING,"Skipping event handler on unsupported operation: {0}", new Object[]{operation});
            }
        }
        
        catch (tcColumnNotFoundException e) 
        { 
            LOGGER.log(ODLLevel.ERROR, e.getMessage(), e);
            throw new EventFailedException(processId,"","","","", new Exception(e.getMessage()));
        }
        
        catch (tcAPIException e) 
        {        
            LOGGER.log(ODLLevel.ERROR, e.getMessage(), e);
            throw new EventFailedException(processId,"","","","", new Exception(e.getMessage()));
        } 
        
        catch (tcInvalidLookupException e) 
        {
            LOGGER.log(ODLLevel.WARNING, MessageFormat.format("Continue {0} since lookup {1} does not exist: {2}.", new Object[]{operation, appInstToProcTaskLookup, e.getMessage()}), e);
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
        
        return new BulkEventResult();
    }
    
    @Override
    public boolean cancel(long l, long l1, AbstractGenericOrchestration ago) 
    {
        return false;
    }

    @Override
    public void compensate(long l, long l1, AbstractGenericOrchestration ago) 
    {
        
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
     * The code key is the application instance display name and the decode value is a comma-delimited value of each
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
           String appInstDisplayName = resourceAcct.getAppInstance().getDisplayName(); // Application Instance Name
           String status = resourceAcct.getAccountStatus();
           LOGGER.log(ODLLevel.NOTIFICATION, "Account Id: {0}", new Object[]{accountId});
           LOGGER.log(ODLLevel.NOTIFICATION, "Process Instance Form Key: {0}", new Object[]{procInstFormKey});
           LOGGER.log(ODLLevel.NOTIFICATION, "Application Instance Name: {0}", new Object[]{appInstName});
           LOGGER.log(ODLLevel.NOTIFICATION, "Object Name: {0}", new Object[]{resourceObjectName});
           LOGGER.log(ODLLevel.NOTIFICATION, "Application Instance Display Name: {0}", new Object[]{appInstDisplayName});
           LOGGER.log(ODLLevel.NOTIFICATION, "Account Status: {0}", new Object[]{status});
           
           // Get delimited process tasks from lookup
           String delimitedProcTasks = resourceToProcTasksMap.get(appInstDisplayName);
           String[] procTaskNames = delimitedProcTasks.split(DELIMITER);
           LOGGER.log(ODLLevel.TRACE, "Application Instance Display Name: {0}, Process Tasks: {1}", new Object[]{appInstDisplayName, Arrays.asList(procTaskNames)});
           
           // Handle resource accounts that been written to UD table.
           // This excludes resources in Waiting state, Revoked state, and Provisioning 
           if(procInstFormKey != null && !procInstFormKey.equalsIgnoreCase("0") && !ProvisioningConstants.ObjectStatus.REVOKED.getId().equalsIgnoreCase(status) && !ProvisioningConstants.ObjectStatus.PROVISIONING.getId().equalsIgnoreCase(status))
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
               LOGGER.log(ODLLevel.NOTIFICATION, "Skip resource {0}. Status = {1}", new Object[]{appInstDisplayName, status});
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