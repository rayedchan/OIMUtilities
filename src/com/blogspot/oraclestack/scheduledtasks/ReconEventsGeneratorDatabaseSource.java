package com.blogspot.oraclestack.scheduledtasks;

import Thor.API.Exceptions.tcAPIException;
import Thor.API.Exceptions.tcColumnNotFoundException;
import Thor.API.Exceptions.tcInvalidLookupException;
import Thor.API.Operations.tcLookupOperationsIntf;
import Thor.API.tcResultSet;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.platform.Platform;
import oracle.iam.reconciliation.api.BatchAttributes;
import oracle.iam.reconciliation.api.ChangeType;
import oracle.iam.reconciliation.api.InputData;
import oracle.iam.reconciliation.api.ReconOperationsService;
import oracle.iam.reconciliation.api.ReconciliationResult;
import oracle.iam.scheduler.vo.TaskSupport;

/**
 * A scheduled task to create reconciliation events of a specified resource object
 * using data from a database table.
 * @author rayedchan
 */
public class ReconEventsGeneratorDatabaseSource extends TaskSupport
{
    // Logger
    private static final ODLLogger LOGGER = ODLLogger.getODLLogger(ReconEventsGeneratorDatabaseSource.class.getName());
    
    // OIM API Services
    private ReconOperationsService reconOps = Platform.getService(ReconOperationsService.class);
   
    @Override
    public void execute(HashMap params) throws NamingException, SQLException, tcAPIException, tcInvalidLookupException, tcColumnNotFoundException
    {
        LOGGER.log(ODLLevel.NOTIFICATION, "Scheduled Job Parameters: {0}", new Object[]{params});
        Connection conn = null;
        tcLookupOperationsIntf lookupOps = Platform.getService(tcLookupOperationsIntf.class);
         
        try
        {
            // Get the parameters from the scheduled job
            String dataSource = (String) params.get("Data Source"); // JNDI Name for Data Source
            String resourceObjectName = (String) params.get("Resource Object Name"); // Reconciliation Profile Name
            String tableName = (String) params.get("Table Name");
            String filter = (String) params.get("Filter") == null ? "" : (String) params.get("Filter"); // WHERE clause
            String reconAttrMapLookup = (String) params.get("Recon Attribute Lookup");
            String keyAttrName = (String) params.get("Key Attribute Name"); // __UID__
            // TODO: Boolean if column names are identical to recon field names
            
            // Store all reconciliation events to be created
            List<InputData> allReconEvents = new ArrayList<InputData>();
            
            // Reconciliation events details
            Boolean eventFinished = true; // No child data provided; mark event to Data Received
            String dateFormat = "yyyy-MM-dd";
            Date actionDate = null; // Event to be processed immediately
            boolean ignoreDuplicate = true; // Identical to using IgnoreEvent API
            BatchAttributes batchAttrs = new BatchAttributes(resourceObjectName, dateFormat, ignoreDuplicate);
            
            // Get database connection from data source
            conn = getDatabaseConnection(dataSource);
            
            // Get Recon Attribute Mappings
            HashMap<String,String> reconAttrMap = convertLookupToMap(lookupOps, reconAttrMapLookup); 
            
            // SELECT SQL Query: Users Table
            String usersQuery = "SELECT * FROM " + tableName + " " + filter;
            PreparedStatement ps = conn.prepareStatement(usersQuery);
            ResultSet rs = ps.executeQuery();
            
            // Iterate each record
            while(rs.next())
            {
                // Store recon event data 
                HashMap<String, Serializable> reconEventData = new HashMap<String, Serializable>();
                
                // Iterate recon attribute lookup and populate map accordingly
                for(Map.Entry<String, String> entry : reconAttrMap.entrySet())
                {
                    String reconFieldName = entry.getKey(); // Code Key
                    String targetColumn = entry.getValue(); // Decode
                    reconEventData.put(reconFieldName, rs.getString(targetColumn));
                }
                
                InputData event = new InputData(reconEventData, null, eventFinished, ChangeType.CHANGELOG, actionDate);
                
                // Add recon event to list
                allReconEvents.add(event);
            }
            
            LOGGER.log(ODLLevel.NOTIFICATION, "Recon Events {0}: {1}", new Object[]{allReconEvents.size(), allReconEvents});
            InputData[] events = new InputData[allReconEvents.size()];
            allReconEvents.toArray(events);
            
            // Create reconciliation events in OIM and process them
            ReconciliationResult result = reconOps.createReconciliationEvents(batchAttrs, events);
            LOGGER.log(ODLLevel.NOTIFICATION, "Success result: {0}",  new Object[]{result.getSuccessResult()});
            LOGGER.log(ODLLevel.NOTIFICATION, "Success result: {0}",  new Object[]{result.getFailedResult()});
        } 
        
        catch (SQLException e) 
        {       
            LOGGER.log(ODLLevel.SEVERE, "Could not get database connection: ", e);
            throw e;
        } 
           
        catch (NamingException e) 
        {
            LOGGER.log(ODLLevel.SEVERE, "Could not get database connection: ", e);
            throw e;
        }
        
        catch (tcAPIException e)
        {
            LOGGER.log(ODLLevel.SEVERE, "Could not get Recon Attr Lookup: ", e);
            throw e;
        } 
        
        catch (tcInvalidLookupException e)
        {
            LOGGER.log(ODLLevel.SEVERE, "Could not get Recon Attr Lookup: ", e);
            throw e;
        } 
        
        catch (tcColumnNotFoundException e) 
        {
            LOGGER.log(ODLLevel.SEVERE, "Could not get Recon Attr Lookup: ", e);
            throw e;
        }

        finally
        {
            if(conn != null)
            {
                conn.close();
            }
            
            if(lookupOps != null)
            {
                lookupOps.close();
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
    
    /**
     * Get database connection from a data source
     * using the JNDI Name
     * @param jndiName  JNDI Name
     * @return Database Connection Object
     * @throws NamingException
     * @throws SQLException 
     **/
    private Connection getDatabaseConnection(String jndiName) throws NamingException, SQLException 
    {
        Context initContext = new InitialContext();
        DataSource ds = (DataSource)initContext.lookup(jndiName);
        Connection connection  = ds.getConnection();
        return connection;
    }
    
    /**
     * Converts a lookup definition into a Map. The Code Key column is used as
     * the key and the Decode column is used as the value.
     * @param lookupDefinitionName      Name of the lookup definition
     * @return Map of lookup values {Key = Code Key, Value = Decode}.
     * @throws tcAPIException
     * @throws tcInvalidLookupException
     * @throws tcColumnNotFoundException
     */
    public HashMap<String, String> convertLookupToMap(tcLookupOperationsIntf lookupOps, String lookupDefinitionName) throws tcAPIException, tcInvalidLookupException, tcColumnNotFoundException 
    {
        HashMap<String, String> lookupValues = new HashMap<String, String>();
        tcResultSet lookupValuesRs = lookupOps.getLookupValues(lookupDefinitionName); // Get lookup values
        int numRows = lookupValuesRs.getTotalRowCount();

        // Iterate lookup resultset and construct map
        for (int i = 0; i < numRows; i++) 
        {
            lookupValuesRs.goToRow(i);
            String codeKey = lookupValuesRs.getStringValue("Lookup Definition.Lookup Code Information.Code Key"); // Fetch Code Key
            String decode = lookupValuesRs.getStringValue("Lookup Definition.Lookup Code Information.Decode"); // Fetch Decode
            lookupValues.put(codeKey, decode);
        }

        return lookupValues;
    }
}


