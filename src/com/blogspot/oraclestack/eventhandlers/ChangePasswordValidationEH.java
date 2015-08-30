package com.blogspot.oraclestack.eventhandlers;

import java.io.Serializable;
import java.util.HashMap;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.platform.context.ContextAware;
import oracle.iam.platform.kernel.ValidationFailedException;
import oracle.iam.platform.kernel.spi.ValidationHandler;
import oracle.iam.platform.kernel.vo.BulkOrchestration;
import oracle.iam.platform.kernel.vo.Orchestration;
import com.thortech.xl.crypto.tcCryptoUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import javax.sql.DataSource;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.api.UserManagerConstants;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.platform.Platform;
import oracle.iam.platform.context.ContextManager;
import oracle.iam.platform.kernel.ValidationException;

/**
 * Additional password rules which are not handled by the OOTB Password Policy.
 * Validate if the new password meets the custom password rules.
 * @author rayedchan
 */
public class ChangePasswordValidationEH implements ValidationHandler
{    
    // Logger
    private static final ODLLogger LOGGER = ODLLogger.getODLLogger(ChangePasswordValidationEH.class.getName());
    
    // OIM API Services
    // private static final UserManager USRMGR =  Platform.getService(UserManager.class);
    private static final UserManager USRMGR = Platform.getServiceForEventHandlers(UserManager.class, null, "ADMIN","ChangePasswordValidationEH", null);
    
    // SQL Query
    private static final String USER_ATTRS_SQL_QUERY = "SELECT usr_login, usr_middle_name, usr_email FROM usr where usr_key=?";
   
    @Override
    public void validate(long processId, long eventId, Orchestration orchestration)
    {
        LOGGER.log(ODLLevel.NOTIFICATION, "Version 1.0");
        LOGGER.log(ODLLevel.NOTIFICATION, "Enter validate() with parameters: Process Id = [{0}], Event Id = [{1}], Orchestration = [{2}]", new Object[]{processId, eventId, orchestration});
        
        Connection conn = null;
        PreparedStatement ps = null;
        User user = null;
        
        try
        {            
            // Get usr_key of target user
            String usrKey = orchestration.getTarget().getEntityId();
            LOGGER.log(ODLLevel.NOTIFICATION, "Target User USR_KEY: {0}", new Object[]{usrKey});
            
            // Get actor
            String actorLogin = ContextManager.getOIMUser(); 
            LOGGER.log(ODLLevel.NOTIFICATION, "Actor Login: {0}", new Object[]{actorLogin});
                        
            // Contains only the new values
            HashMap<String, Serializable> newParameters = orchestration.getParameters();
            LOGGER.log(ODLLevel.TRACE, "Parameters: {0}", new Object[]{newParameters});
            LOGGER.log(ODLLevel.TRACE, "InterEventData: {0}", new Object[]{orchestration.getInterEventData()}); // password policy info
            LOGGER.log(ODLLevel.TRACE, "Context: {0}", new Object[]{orchestration.getContextVal()});
            
            // Decrypt new password using the default secret key
            String newPasswordEncrypted = getParamaterValue(newParameters, "usr_password");
            String newPasswordDecrypted = tcCryptoUtil.decrypt(newPasswordEncrypted, "DBSecretKey");
            LOGGER.log(ODLLevel.TRACE, "New Password: {0}", new Object[]{newPasswordDecrypted}); // TODO: Remove
            
            // Anonymous user case E.g. Scenario Forget Password?
            /*if("<anonymous>".equalsIgnoreCase(actorLogin))
            {
                // Get OIM database connection from data source
                LOGGER.log(ODLLevel.NOTIFICATION, "Anonymous User");
                DataSource ds = Platform.getOperationalDS(); // Get OIM datasource
                conn = ds.getConnection(); // Get connection
                LOGGER.log(ODLLevel.TRACE, "Got database connection.");
                
                // Construct Prepared Statement
                ps = conn.prepareStatement(USER_ATTRS_SQL_QUERY);
                ps.setString(1, usrKey); // Set parametized value usr_key
                
                // Execute query
                ResultSet rs = ps.executeQuery();
                
                // Iterate record; should be only one since usr_key is a primary key
                while(rs.next())
                {
                    // Get data from record
                    String middleName = rs.getString("usr_middle_name");
                    String email = rs.getString("usr_email");
                    String userLogin = rs.getString("usr_login");
                    
                    // Construct user object
                    user = new User(usrKey);
                    user.setEmail(email);
                    user.setLogin(userLogin);
                    user.setMiddleName(middleName);
                }
                
            }
            
            // All other cases (E.g. Administrator, Self)
            else
            {*/
                // Get OIM User
                HashSet<String> attrs = new HashSet<String>();
                attrs.add(UserManagerConstants.AttributeName.MIDDLENAME.getId()); // Middle Name
                attrs.add(UserManagerConstants.AttributeName.EMAIL.getId()); // Email
                boolean useUserLogin = false;
                user = USRMGR.getDetails(usrKey, attrs, useUserLogin);
            //}
            
            LOGGER.log(ODLLevel.NOTIFICATION, "User: {0}", new Object[]{user});
           
            // Check password against custom rules
            boolean validatePassword = this.customPasswordPolicy(newPasswordDecrypted, user);
            LOGGER.log(ODLLevel.NOTIFICATION, "Is new password validate? {0}", new Object[]{validatePassword});
           
            // Validation failed
            if(!validatePassword)
            {
                throw new ValidationException("The following requirements have not been met. " + "(1) Must not contain middle name. (2) Must not contain email. ");
            }            
        }
        
        catch(Exception e)
        {
            LOGGER.log(ODLLevel.ERROR, "", e);
            throw new ValidationFailedException(e);
        }
        
        finally
        {
            // Close statement
            if(ps != null)
            {
                try 
                {
                    ps.close();
                } 
                
                catch (SQLException ex) 
                {
                     LOGGER.log(ODLLevel.ERROR, "", ex);
                }
            }
            
            // Close database connection
            if(conn != null)
            {
                try 
                {
                    conn.close();
                } 
                
                catch (SQLException ex)
                {
                    LOGGER.log(ODLLevel.ERROR, "", ex);
                }
            }
        }
    }
    
    @Override
    public void validate(long processId, long eventId, BulkOrchestration bulkOrchestration)
    { 
        LOGGER.log(ODLLevel.NOTIFICATION, "[NOT SUPPORTED] Enter validate() with parameters: Process Id = [{0}], Event Id = [{1}], Bulk Orchestration = [{2}]", new Object[]{processId, eventId, bulkOrchestration});  
    }

    @Override
    public void initialize(HashMap<String, String> hm)
    {
        LOGGER.log(ODLLevel.NOTIFICATION, "Enter initialize: {0}", new Object[]{hm});
    }
    
    /**
    * ContextAware object is obtained when the actor is a regular user.
    * If the actor is an administrator, the exact value of the attribute is obtained.
    * @param parameters    parameters from the orchestration object
    * @param key   name of User Attribute in OIM Profile or column in USR table
    * @return value of the corresponding key in parameters
    */
    private String getParamaterValue(HashMap<String, Serializable> parameters, String key)
    {
        String value = (parameters.get(key) instanceof ContextAware)
        ? (String) ((ContextAware) parameters.get(key)).getObjectValue()
        : (String) parameters.get(key);
        return value;
    }
    
    /**
     * Custom Password Policy
     * - Does not contain middle name
     * - Does not contain email
     * @param password  Plain text password to validate
     * @param user  OIM User
     * @return true if password requirements are met; false otherwise
     */
    private boolean customPasswordPolicy(String password, User user)
    {   
        // Fetch user's attributes
        String middleName = user.getMiddleName(); // Get user's middle name
        String email = user.getEmail(); // Get user's email
        
        // Construct Regular Expression
        String middleNameRegex = (middleName == null || "".equalsIgnoreCase(middleName)) ? "" : ".*(?i)" + middleName  + ".*"; // contains, ignore case
        String emailRegex = (email == null || "".equalsIgnoreCase(email)) ? "" : ".*(?i)" + email  + ".*"; // contains, ignore case
        
        // Check if password valid
        boolean containsMiddleName = password.matches(middleNameRegex);
        boolean containsEmail = password.matches(emailRegex);
        boolean isValidatePassword = (!containsMiddleName) && (!containsEmail);
        
        LOGGER.log(ODLLevel.TRACE, "Password contains middle name? {0}", new Object[]{containsMiddleName});
        LOGGER.log(ODLLevel.TRACE, "Password contains email? {0}", new Object[]{containsEmail});
        
        return isValidatePassword;
    }
}
