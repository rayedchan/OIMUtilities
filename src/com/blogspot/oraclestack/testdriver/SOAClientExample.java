package com.blogspot.oraclestack.testdriver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import oracle.bpel.services.workflow.StaleObjectException;
import oracle.bpel.services.workflow.WorkflowException;
import oracle.bpel.services.workflow.client.IWorkflowServiceClient;
import oracle.bpel.services.workflow.client.IWorkflowServiceClientConstants;
import oracle.bpel.services.workflow.client.WorkflowServiceClientFactory;
import oracle.bpel.services.workflow.query.ITaskQueryService;
import oracle.bpel.services.workflow.task.ITaskAssignee;
import oracle.bpel.services.workflow.task.ITaskService;
import oracle.bpel.services.workflow.task.impl.TaskAssignee;
import oracle.bpel.services.workflow.task.model.Task;
import oracle.bpel.services.workflow.verification.IWorkflowContext;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;

/**
 * Demonstrates how to setup a remote SOA Workflow Service client and use its APIs.
 * Fetching all the requests of a given user and reassigning a task to a different user
 * are demonstrated as examples.
 * @author rayedchan
 */
public class SOAClientExample 
{
    private static final ODLLogger LOGGER = ODLLogger.getODLLogger(SOAClientExample.class.getName());
            
    public static void main(String[] args)
    {
        // TODO: Change accordingly
        String userid = "weblogic";  // User whose task list needs to be queries
        String password = "Password1"; // Password for the user
        String ejbHost = "t3://localhost:8001"; // SOA URL E.g. t3://localhost:8001
        
        // Properties for SOA Client
        Map properties = new HashMap();
        properties.put(IWorkflowServiceClientConstants.CONNECTION_PROPERTY.CLIENT_TYPE, WorkflowServiceClientFactory.REMOTE_CLIENT);
        properties.put(IWorkflowServiceClientConstants.CONNECTION_PROPERTY.EJB_PROVIDER_URL, ejbHost);
        properties.put(IWorkflowServiceClientConstants.CONNECTION_PROPERTY.EJB_INITIAL_CONTEXT_FACTORY, "weblogic.jndi.WLInitialContextFactory");

        try
        {
            // Get workflow service client
            IWorkflowServiceClient wfSvcClient = WorkflowServiceClientFactory.getWorkflowServiceClient(properties, null);

            // Get the workflow context; Authenticate as user whose task list needs to be queried
            IWorkflowContext wfCtx = wfSvcClient.getTaskQueryService().authenticate(userid, password.toCharArray(), null);
            LOGGER.log(ODLLevel.NOTIFICATION, "Authentication for client succeeded.");

            // Get SOA API Services 
            ITaskQueryService taskQueryService = wfSvcClient.getTaskQueryService();
            ITaskService taskService = wfSvcClient.getTaskService();
            
            // Call helper to fetch all the requests in a given context
            fetchAllRequests(taskQueryService, wfCtx);
            
            // Reassign task
            // TODO: Change values accordingly
            String taskId = "9246420c-a26c-4f69-a70d-f6024209a309";
            boolean useId = true;
            List<String> assignees = new ArrayList<String>();
            assignees.add("hsolo"); // use user login 
            assignees.add("kren");
            //reassignTask(taskService, taskQueryService, wfCtx, taskId, useId, assignees); // call helper method
        } 
        
        catch (Exception ex)
        {
            LOGGER.log(ODLLevel.ERROR, ex.getMessage(), ex);
        }
    }
    
    /**
     * Fetch all existing requests via SOA Workflow Service API. 
     * @param taskQueryService  Task Query Service
     * @param wfCtx             Workflow Context
     * @throws WorkflowException
     */
    public static void fetchAllRequests(ITaskQueryService taskQueryService, IWorkflowContext wfCtx) throws WorkflowException 
    {
        // Predicate used for filtering tasks
        //Predicate statePredicate=  new Predicate(TableConstants.WFTASK_STATE_COLUMN,Predicate.OP_EQ,IWorkflowConstants.TASK_STATE_ASSIGNED); // Only get ASSIGNED tasks
        //Predicate idKeyPredicate=  new Predicate(TableConstants.WFTASK_IDENTIFICATIONKEY_COLUMN,Predicate.OP_EQ, requestId); // Only get specific task with request id
        //Predicate stateAndIdKeyPredicate =  new Predicate(statePredicate,Predicate.AND, idKeyPredicate);

        // Display Column List
        List<String> queryColumns = new ArrayList<String>();
        queryColumns.add("TASKNUMBER");
        queryColumns.add("TASKID");
        queryColumns.add("TITLE");
        queryColumns.add("OUTCOME");
        queryColumns.add("STATE");
        queryColumns.add("PRIORITY");
        queryColumns.add("IDENTIFICATIONKEY"); // Request ID
        queryColumns.add("TASKDEFINITIONNAME"); // Composite Name

        // Optional Column
        List optionalInfo = new ArrayList();      
        optionalInfo.add("Comments");
        optionalInfo.add("Payload");   

        // Call query Task method
        List taskList = taskQueryService.queryTasks(wfCtx,
           queryColumns, // Custom Defined QueryColumns list
           optionalInfo, // Do not query additional info
           ITaskQueryService.AssignmentFilter.ALL,
           null, // No keywords
           null, // Custom Defined Predicate (E.g. stateAndIdKeyPredicate)
           null, // No Task Ordering ordering
           0,0);  // Do not page the query result

        // Number of tasks queried
        int numTasks = taskList.size();
        LOGGER.log(ODLLevel.NOTIFICATION, "Total tasks found: {0}", new Object[]{numTasks});

        // Print each task information
        for (int i = 0; i < numTasks; i++)
        {
            Task task = (Task) taskList.get(i);
            //LOGGER.log(ODLLevel.NOTIFICATION, "Task Number: {0}", new Object[]{task.getSystemAttributes().getTaskNumber()});
            //LOGGER.log(ODLLevel.NOTIFICATION, "Task Id: {0}", new Object[]{task.getSystemAttributes().getTaskId()});
            //LOGGER.log(ODLLevel.NOTIFICATION, "Title: {0}", new Object[]{task.getTitle()});
            //LOGGER.log(ODLLevel.NOTIFICATION, "Priority: {0}", new Object[]{task.getPriority()});
            //LOGGER.log(ODLLevel.NOTIFICATION, "State: {0}", new Object[]{task.getSystemAttributes().getState()});
            
            System.out.println("Task Number: " + task.getSystemAttributes().getTaskNumber());
            System.out.println("Task Id: " + task.getSystemAttributes().getTaskId());
            System.out.println("Title: " + task.getTitle());
            System.out.println("Priority: " + task.getPriority());
            System.out.println("State: " + task.getSystemAttributes().getState());
            System.out.println();
        } 
    }
    
    /**
     * Reassigns a task to different users.
     * @param taskService           Task Service
     * @param taskQueryService      Task Query Service
     * @param wfCtx                 Workflow Context
     * @param taskId                Supply the task id or the task number depending on the useId parameter.
     * @param useId                 If true, use task id. Otherwise use task number.
     * @param assignees             List of assignees to reassign task to
     * @throws WorkflowException
     * @throws StaleObjectException 
     */
    public static void reassignTask(ITaskService taskService, ITaskQueryService taskQueryService, IWorkflowContext wfCtx, String taskId, boolean useId, List<String> assignees) throws WorkflowException, StaleObjectException
    {
        // Get a specific task
        Task currentTask = null;
        
        // Fetch task
        if(useId)
        {
            currentTask = taskQueryService.getTaskDetailsById(wfCtx, taskId); // E.g. taskId = "e941fc3a-f47e-4dfd-a10b-720e6bf840ab"
            LOGGER.log(ODLLevel.NOTIFICATION, "Fetched task by id.");
        }
        
        else
        {
            currentTask = taskQueryService.getTaskDetailsByNumber(wfCtx, Integer.parseInt(taskId)); // E.g. taskId = 200770
            LOGGER.log(ODLLevel.NOTIFICATION, "Fetched task by number.");
        }

        // New assignees of found task
        List<ITaskAssignee> users = new ArrayList<ITaskAssignee>();
        for(String userLogin : assignees)
        {
            ITaskAssignee assignee = new TaskAssignee(userLogin.toLowerCase(), "user");
            users.add(assignee);
        }

        // Reassign a task to different users
        taskService.reassignTask(wfCtx, currentTask, users); 
    }
}
