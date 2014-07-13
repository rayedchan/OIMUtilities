package project.rayedchan.utilities;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.identity.exception.*;
import oracle.iam.identity.rolemgmt.api.RoleCategoryManager;
import oracle.iam.identity.rolemgmt.api.RoleManager;
import oracle.iam.identity.rolemgmt.api.RoleManagerConstants;
import oracle.iam.identity.rolemgmt.api.RoleManagerConstants.RoleCategoryAttributeName;
import oracle.iam.identity.rolemgmt.vo.Role;
import oracle.iam.identity.rolemgmt.vo.RoleCategory;
import oracle.iam.platform.OIMClient;
import oracle.iam.platform.authz.exception.AccessDeniedException;
import oracle.iam.platform.entitymgr.vo.SearchCriteria;
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
    private RoleManager roleMgrOps;
    
    /**
     * Constructor
     * Precondition: A system administrator is logged in with the OIM client
     * @param oimClient 
     */
    public RoleUtilities(OIMClient oimClient)
    {
        this.roleCategoryMgrOps = oimClient.getService(RoleCategoryManager.class);
        this.roleMgrOps = oimClient.getService(RoleManager.class);
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
    public void bulkCreateRoleCategories(String csvFilePath, char delimiter) throws IOException
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
    
    /**
     * Get all the OIM roles. All roles can stored in OIM.UPG table. 
     * All possible role attributes: {Role Display Name, Role Unique Name, Owner Login
     * ugp_createby, ugp_create, Role Owner Key, Role Description, Role Name, ugp_update,
     * Owner Email, Role Namespace, Owner Display Name, Role Key, LDAP GUID, ugp_updateby,
     * Role Category Key, Owner Last Name, ugp_data_level, Role Email, LDAP DN, Owner First Name,
     * Role Category Name}
     * @param returnAttrs   Contains the role attributes to query for. 
     * @return List of Roles
     * @throws AccessDeniedException
     * @throws RoleSearchException 
     */
    public List<Role> getAllRoles(HashSet returnAttrs) throws AccessDeniedException, RoleSearchException
    {
        // Query based on "Role Name" attribute with any value
        SearchCriteria criteria = new SearchCriteria(RoleManagerConstants.RoleAttributeName.NAME.getId(), "*", SearchCriteria.Operator.EQUAL);
        return roleMgrOps.search(criteria, returnAttrs, new HashMap());
    }
    
    /**
     * Get the role category key by role category name .  
     * @param roleCategoryName  Name of the role category (ROLE_CATEGORY.ROLE_CATEGORY_NAME)
     * @return Role category key (ROLE_CATEGORY.ROLE_CATEGORY_KEY)
     * @throws SearchKeyNotUniqueException
     * @throws AccessDeniedException
     * @throws NoSuchRoleCategoryException
     * @throws RoleCategoryLookupException 
     */
    public Long getRoleCategoryKeyByName(String roleCategoryName) throws SearchKeyNotUniqueException, AccessDeniedException, NoSuchRoleCategoryException, RoleCategoryLookupException
    {
        // Only query for "Role Category Key"
        HashSet retAttrs = new HashSet();
        retAttrs.add(RoleManagerConstants.RoleCategoryAttributeName.KEY.getId());
        
        // Get role category key by role category name
        RoleCategory roleCategory = roleCategoryMgrOps.getDetails(RoleManagerConstants.RoleCategoryAttributeName.NAME.getId(), roleCategoryName, retAttrs);
        
        return Long.parseLong(roleCategory.getEntityId());
    }
    
    /**
     * Create a single role in OIM. In the backend, the API inserts a record into
     * the OIM.UGP table.
     * @param roleName          Name of role to be created
     * @param categoryName      Role Category the new role should be place into 
     * @param description       Description of the new role
     * @throws ValidationFailedException
     * @throws AccessDeniedException
     * @throws RoleAlreadyExistsException
     * @throws RoleCreateException
     * @throws SearchKeyNotUniqueException
     * @throws NoSuchRoleCategoryException
     * @throws RoleCategoryLookupException 
     */
    public void createRole(String roleName, String categoryName, String description) throws ValidationFailedException, AccessDeniedException, RoleAlreadyExistsException, RoleCreateException, SearchKeyNotUniqueException, NoSuchRoleCategoryException, RoleCategoryLookupException
    {
        // Get the role category key by role category name
        Long categoryKey = getRoleCategoryKeyByName(categoryName);
        
        // Set the attributes for a Role
        HashMap attrs = new HashMap();
        attrs.put(RoleManagerConstants.RoleAttributeName.NAME.getId(), roleName); // Set "Role Name"
        attrs.put(RoleManagerConstants.RoleAttributeName.CATEGORY_KEY.getId(), categoryKey); // Set "Role Category Key"
        attrs.put(RoleManagerConstants.RoleAttributeName.DESCRIPTION.getId(), description); // Set "Role Description"
            
        // Create Role object
        Role newRole = new Role(attrs);
        
        // Use OIM API to create role 
        roleMgrOps.create(newRole);
        logger.log(ODLLevel.NOTIFICATION, "Role created: {0}, Role Category Name = {1}", new Object[]{newRole, categoryName});
    }
    
    /**
     * Bulk create roles given in a CSV file.
     * Precondition: CSV file must have proper format. The first line is the header.
     * UGP_ROLENAME<delimiter>ROLE_CATEGORY_NAME<delimiter>UGP_DESCRIPTION
     * roleName<delimiter>categoryName<delimiter>description
     * @param csvFilePath   Path to CSV file that contains role data
     * @param delimiter     A character used for separating the values in an entry
     * @throws IOException 
     */
    public void bulkCreateRoles(String csvFilePath, char delimiter) throws IOException
    {    
        CSVParser csvParser = null;
        
        try
        {
            // Objects for parsing CSV file     
            CSVFormat format = CSVFormat.DEFAULT.withHeader().withDelimiter(delimiter); // Indicate format for csv file; Specify csv file has header and use specific delimiter for parsing
            csvParser = new CSVParser(new FileReader(csvFilePath), format);
            
            // Get CSV header
            Map header = csvParser.getHeaderMap();
            
            // Iterate each entry in csv file excluding the header entry
            for (CSVRecord record: csvParser)
            {
                // Get entry (row) values via header names (columns)
                String roleName = record.get("UGP_ROLENAME");
                String roleCategoryName = record.get("ROLE_CATEGORY_NAME");
                String roleDescription = record.get("UGP_DESCRIPTION");
                long entryNumber = record.getRecordNumber();

                try 
                {
                    // Method call to create a single role
                    createRole(roleName, roleCategoryName, roleDescription);
                } 
                
                catch (ValidationFailedException ex) 
                {
                    logger.log(Level.WARNING, "Failed to add Role entry {0}: Header = {1}, Values = {2}", new Object[]{entryNumber, header, record}, ex);
                } 
                
                catch (AccessDeniedException ex) 
                {
                    logger.log(Level.WARNING, "Failed to add Role entry {0}: Header = {1}, Values = {2}", new Object[]{entryNumber, header, record}, ex);                   
                } 
                
                catch (RoleAlreadyExistsException ex) 
                {
                    logger.log(Level.WARNING, "Failed to add Role entry {0}: Header = {1}, Values = {2}", new Object[]{entryNumber, header, record}, ex);
                } 
                
                catch (RoleCreateException ex)
                {
                    logger.log(Level.WARNING, "Failed to add Role entry {0}: Header = {1}, Values = {2}", new Object[]{entryNumber, header, record}, ex);
                } 
                
                catch (SearchKeyNotUniqueException ex) 
                {
                    logger.log(Level.WARNING, "Failed to add Role entry {0}: Header = {1}, Values = {2}", new Object[]{entryNumber, header, record}, ex);
                } 
                
                catch (NoSuchRoleCategoryException ex) 
                {
                    logger.log(Level.WARNING, "Failed to add Role entry {0}: Header = {1}, Values = {2}", new Object[]{entryNumber, header, record}, ex);
                } 
                
                catch (RoleCategoryLookupException ex) 
                {
                    logger.log(Level.WARNING, "Failed to add Role entry {0}: Header = {1}, Values = {2}", new Object[]{entryNumber, header, record}, ex);
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
}
