package com.blogspot.oraclestack.objects;

import java.text.MessageFormat;
import java.util.Arrays;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.identity.exception.NoSuchUserException;
import oracle.iam.identity.exception.SearchKeyNotUniqueException;
import oracle.iam.identity.exception.UserModifyException;
import oracle.iam.identity.exception.ValidationFailedException;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.platform.authz.exception.AccessDeniedException;

/**
 * Each thread modifies a user
 * @author rayechan
 */
public class UserProcessor implements Runnable
{
    // Class Fields needed for every thread
    private static String[] header;
    private static String delimiter;
    private static ODLLogger logger;
    private static UserManager usrMgr;
    private static String keyAttrName;
    
    // Row in a file
    private String userEntryLine;
    
    /**
     * Initializes the class variables needed to process each row
     * @param header    Header row from CSV file
     * @param delimiter Separator for file
     * @param logger    Logger
     * @param usrMgr    OIM User Manager Service
     * @param keyAttrName Key User Attribute in order to identify OIM user
     */
    public static void initializeConfig(String[] header, String delimiter, ODLLogger logger, UserManager usrMgr, String keyAttrName)
    {
        UserProcessor.header = header;
        UserProcessor.delimiter = delimiter;
        UserProcessor.logger = logger;
        UserProcessor.usrMgr = usrMgr;
        UserProcessor.keyAttrName = keyAttrName;
    }
    
    /**
     * Constructor 
     * @param line Line from CSV file
     */
    public UserProcessor(String line)
    {
        this.userEntryLine = line;
    }
    
    /**
     * Execution method for thread
     */
    @Override
    public void run() 
    {
        try 
        {
            String[] entry = userEntryLine.split(delimiter);
            logger.log(ODLLevel.NOTIFICATION,"Start processing line: {0}", new Object[]{Arrays.asList(userEntryLine)});
            User modUser = new User("");
            String attrKeyValue = null;

            // Iterate entry columns adding attribute to modify on given user
            for(int i = 0; i < entry.length; i++)
            {
                // One to One correlation with header row and data entry row
                String attributeName = header[i];
                String attributeValue = entry[i];
                    
                // Get key user attribute in order identify OIM user to modify
                if(attributeName.equals(keyAttrName))
                {
                    attrKeyValue = attributeValue;
                }
                
                // Regular attribute to modify on user
                else
                {
                    // Add attribute to modification user object
                    modUser.setAttribute(attributeName, attributeValue);
                }
            }
            
            usrMgr.modify(keyAttrName, attrKeyValue, modUser); // Apply changes to OIM user
            logger.log(ODLLevel.NOTIFICATION,"Processed {0} = {1} with {2}", new Object[]{keyAttrName, attrKeyValue, modUser});
        } 
        
        catch (ValidationFailedException ex) 
        {
            logger.log(ODLLevel.SEVERE, MessageFormat.format("Failed to process entry: {0}", new Object[]{userEntryLine}), ex);
        } 
        
        catch (AccessDeniedException ex)
        {
            logger.log(ODLLevel.SEVERE, MessageFormat.format("Failed to process entry: {0}", new Object[]{userEntryLine}), ex);
        } 
        
        catch (UserModifyException ex)
        {
            logger.log(ODLLevel.SEVERE, MessageFormat.format("Failed to process entry: {0}", new Object[]{userEntryLine}), ex);
        } 
        
        catch (NoSuchUserException ex)
        {
            logger.log(ODLLevel.SEVERE, MessageFormat.format("Failed to process entry: {0}", new Object[]{userEntryLine}), ex);
        } 
        
        catch (SearchKeyNotUniqueException ex) 
        {
            logger.log(ODLLevel.SEVERE, MessageFormat.format("Failed to process entry: {0}", new Object[]{userEntryLine}), ex);
        }
        
        catch (Exception ex) 
        {
            logger.log(ODLLevel.SEVERE, MessageFormat.format("Failed to process entry: {0}", new Object[]{userEntryLine}), ex);
        }
    }   
}
