package com.blogspot.oraclestack.scheduledtasks;

import com.blogspot.oraclestack.objects.UserProcessor;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.platform.Platform;
import oracle.iam.scheduler.vo.TaskSupport;

/**
 * An example of a multi-threaded scheduled task.
 * The scheduled task applies changes to the OIM users
 * using data given from a CSV file. A thread is created per row
 * in CSV file excluding the header row.
 * @author rayedchan
 */
public class FlatFileUserModification extends TaskSupport
{
    // Logger
    private static final ODLLogger LOGGER = ODLLogger.getODLLogger(FlatFileUserModification.class.getName());
    
    // OIM Services
    // private UserManager usrMgr = Platform.getService(UserManager.class); // Getting a NullPointer Exception when using service in a threading context
    private UserManager usrMgr = Platform.getServiceForEventHandlers(UserManager.class, null, "ADMIN","FlatFileUserModification", null);
    
    /**
     * Main method for scheduled job execution
     * @param hm Map of the scheduled job parameters
     * @throws Exception 
     */
    @Override
    public void execute(HashMap hm) throws Exception 
    {
        BufferedReader bReader = null;
        
        try
        {
            // Get the parameters from the scheduled job
            String keyAttrName = (String) hm.get("Key Attribute Name"); // Key Attribute Name to identify OIM User
            String filePath = (String) hm.get("File Path");
            String delimiter = (String) hm.get("Delimiter");
            int numThreads = ((Long) hm.get("Number of Threads")).intValue();
            LOGGER.log(ODLLevel.NOTIFICATION, "Scheduled Job Parameters: Key Attribute Name = {0}, File Path = {1}, Delimiter = {2}, Number of Threads = {3}", new Object[]{keyAttrName, filePath, delimiter, numThreads});
            
            if(numThreads <= 0) 
            {
                LOGGER.log(ODLLevel.SEVERE, "Threads Parameter is not valid. Value must be greater than 0.");
                throw new Exception("Task Mode Parameter is not valid. Value must be greater than 0.");
            }
            
            // Load CSV file for reading
            FileReader fReader = new FileReader(filePath);
            bReader = new BufferedReader(fReader);
            
            // Get Header Line
            String line = bReader.readLine();
            if(line == null || "".equalsIgnoreCase(line))
            {
                throw new Exception("Header must be provided as the first entry in file.");
            }
            String[] header = line.split(delimiter);
            LOGGER.log(ODLLevel.NOTIFICATION, "Header: {0}", new Object[]{Arrays.asList(header)});
            
            // Create Thread Pool
            ExecutorService threadExecutor = Executors.newFixedThreadPool(numThreads);
            
            // Initialize base configuration 
            UserProcessor.initializeConfig(header, delimiter, LOGGER, usrMgr, keyAttrName);
            
            // Process data entries using multi-threading
            line = bReader.readLine();
            while(line != null)
            {          
                threadExecutor.execute(new UserProcessor(line)); // Create new thread to process line
                line = bReader.readLine(); // read next line
            }
            
            // Initate thread shutdown
            threadExecutor.shutdown();
            
            while(!threadExecutor.isTerminated())
            {
                // Wait for all event processor threads to complete
            }
            
            LOGGER.log(ODLLevel.NOTIFICATION, "Finished scheduled job.");
        }
        
        catch(Exception ex)
        {
            LOGGER.log(ODLLevel.SEVERE, "", ex);
        }
        
        finally
        {
            if(bReader != null)
            {
                bReader.close();
            }
        }
    }

    @Override
    public HashMap getAttributes() 
    {
        return null;
    }

    @Override
    public void setAttributes()
    {
        
    }   
}
