package project.rayedchan.utilities;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.identity.exception.RoleCategoryAlreadyExistsException;
import oracle.iam.identity.exception.RoleCategoryBrowseException;
import oracle.iam.identity.exception.RoleCategoryCreateException;
import oracle.iam.identity.exception.ValidationFailedException;
import oracle.iam.identity.rolemgmt.api.RoleCategoryManager;
import oracle.iam.identity.rolemgmt.api.RoleManagerConstants.RoleCategoryAttributeName;
import oracle.iam.identity.rolemgmt.vo.RoleCategory;
import oracle.iam.platform.OIMClient;
import oracle.iam.platform.authz.exception.AccessDeniedException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * Contains utilities related to OIM roles.
 * @author rayedchan
 */
public class RoleUtilities 
{
    // Logger
    public static ODLLogger logger = ODLLogger.getODLLogger(RoleUtilities.class.getName());
    
    // Service instance variables
    private RoleCategoryManager roleCategoryMgrOps;
    
    /**
     * Constructor
     * Precondition: A system administrator is logged in with the OIM client
     * @param oimClient 
     */
    public RoleUtilities(OIMClient oimClient)
    {
        this.roleCategoryMgrOps = oimClient.getService(RoleCategoryManager.class);
    }
    
    /**
     * Creates a single role category. A record is inserted into OIM.ROLE_CATEGORY table.
     * @param roleCategoryName  Name of the role category
     * @param description       Description of the role category
     * @throws ValidationFailedException
     * @throws AccessDeniedException
     * @throws RoleCategoryAlreadyExistsException
     * @throws RoleCategoryCreateException 
     */
    public void createRoleCategory(String roleCategoryName, String description) throws ValidationFailedException, AccessDeniedException, RoleCategoryAlreadyExistsException, RoleCategoryCreateException
    {
        // Attribute Names for role category
        String roleCategoryNameAttrId = RoleCategoryAttributeName.NAME.getId(); // API Id = Role Category Name , Column Name = ROLE_CATEGORY_NAME
        String roleCategoryDescriptionAttrId = RoleCategoryAttributeName.DESCRIPTION.getId();  // API Id = Role Category Description, Column Name = ROLE_CATEGORY_DESC
        
        // Set up attribute values for a role category
        HashMap<String,Object> roleCategoryAttrs = new HashMap<String,Object>();
        roleCategoryAttrs.put(roleCategoryNameAttrId, roleCategoryName); 
        roleCategoryAttrs.put(roleCategoryDescriptionAttrId, description);
        
        // Create RoleCategory
        RoleCategory roleCategory = new RoleCategory(roleCategoryAttrs);
        
        // API to create role category
        roleCategoryMgrOps.create(roleCategory);
        logger.log(ODLLevel.NOTIFICATION, "Role Category created: {0} = \"{1}\", {2} = \"{3}\"", new Object[]{roleCategoryNameAttrId,roleCategoryName, roleCategoryDescriptionAttrId, description});
    }
    
    /**
     * Bulk create role categories from provided CSV file. If a role category fails
     * to be created, the bulk creation process still proceed.
     * Precondition: CSV file must have the proper CSV header
     * ROLE_CATEGORY_NAME<delimiter>ROLE_CATEGORY_DESC
     * <name><delimiter><description>
     * @param csvFilePath   Path to CSV file
     * @param delimiter     Delimiter to parse an entry in a file
     * @throws IOException 
     */
    public void bulkCreateRoleCatgory(String csvFilePath, char delimiter) throws IOException
    {
        CSVParser csvParser = null;
        
        try
        {
            // Objects for parsing CSV file     
            CSVFormat format = CSVFormat.DEFAULT.withHeader().withDelimiter(delimiter); // Indicate format for csv file; Specify csv file has header and use specific delimiter for parsing
            csvParser = new CSVParser(new FileReader(csvFilePath), format);

            // Iterate each entry in csv file excluding the header entry
            for (CSVRecord record: csvParser)
            {
                // Get entry (row) values via header names (columns)
                String roleCategoryName = record.get("ROLE_CATEGORY_NAME");
                String roleCategoryDescription = record.get("ROLE_CATEGORY_DESC");
                long entryNumber = record.getRecordNumber();
                
                try 
                {
                    // Method call to create a single role category
                    createRoleCategory(roleCategoryName, roleCategoryDescription);
                    //logger.log(ODLLevel.NOTIFICATION, "Added Role Category entry {0}: Name = \"{1}\", Description = \"{2}\"", new Object[]{entryNumber, roleCategoryName, roleCategoryDescription});
                }
                
                catch (ValidationFailedException ex) 
                {
                    logger.log(Level.WARNING, "Failed to add Role Category entry {0}: Name = \"{1}\", Description = \"{2}\"", new Object[]{entryNumber, roleCategoryName, roleCategoryDescription}, ex);
                } 
                
                catch (AccessDeniedException ex)
                {
                    logger.log(Level.WARNING, "Failed to add Role Category entry {0}: Name = \"{1}\", Description = \"{2}\"", new Object[]{entryNumber, roleCategoryName, roleCategoryDescription}, ex);   
                }
                
                catch (RoleCategoryAlreadyExistsException ex) 
                {
                    logger.log(Level.WARNING, "Failed to add Role Category entry {0}: Name = \"{1}\", Description = \"{2}\"", new Object[]{entryNumber, roleCategoryName, roleCategoryDescription}, ex);
                }
                
                catch (RoleCategoryCreateException ex) 
                {
                    logger.log(Level.WARNING, "Failed to add Role Category entry {0}: Name = \"{1}\", Description = \"{2}\"", new Object[]{entryNumber, roleCategoryName, roleCategoryDescription}, ex);
                }
            } // end for loop
        } // end try statement
        
        finally
        {
            // Close parser
            if(csvParser != null)
            {
                csvParser.close();
            }
        }
    }
        
    
    /**
     * Obtains all the role categories in OIM. The OIM.ROLE_CATEGORY table contains all the
     * data about role categories.
     * @return List of all the role categories
     * @throws AccessDeniedException
     * @throws RoleCategoryBrowseException 
     */
    public List<RoleCategory> getAllRoleCategories() throws AccessDeniedException, RoleCategoryBrowseException
    {
        return roleCategoryMgrOps.browse(new HashSet(), new HashMap());
    }
}
