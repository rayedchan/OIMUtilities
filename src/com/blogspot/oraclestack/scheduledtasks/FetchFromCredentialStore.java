package com.blogspot.oraclestack.scheduledtasks;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import oracle.core.ojdl.logging.ODLLevel;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.scheduler.vo.TaskSupport;
import oracle.security.jps.JpsException;
import oracle.security.jps.service.JpsServiceLocator;
import oracle.security.jps.service.ServiceLocator;
import oracle.security.jps.service.credstore.CredentialStore;
import oracle.security.jps.service.credstore.PasswordCredential;

/**
 * Fetch credentials from the Credential Store
 * References:
 * http://docs.oracle.com/cd/E40329_01/apirefs.1112/e27155/toc.htm
 * http://www.redheap.com/2013/06/secure-credentials-in-adf-application.html
 * https://thecattlecrew.wordpress.com/2013/12/17/using-credentials-store-when-communicating-with-oracle-human-workflow-api/
 * http://docs.oracle.com/cd/E23943_01/core.1111/e10043/devcsf.htm#JISEC3675
 * 
 * @author rayedchan
 */
public class FetchFromCredentialStore extends TaskSupport
{
    // Logger
    private static final ODLLogger LOGGER = ODLLogger.getODLLogger(FetchFromCredentialStore.class.getName());
            
    @Override
    public void execute(HashMap params)  
    {
        LOGGER.log(ODLLevel.NOTIFICATION, "Enter execute() with parameters: {0}", new Object[]{params});
        
        // Get values from scheduled task parameters
        String map = (String) params.get("Map");
        String key = (String) params.get("Key");
        LOGGER.log(ODLLevel.NOTIFICATION, "Map: {0}, Key: {1}", new Object[]{map, key});
        
        // Call method to get password from credential store
        PasswordCredential pwdCred = this.readCredentials(map, key);
        String userName = (pwdCred != null) ? pwdCred.getName().toString() : "";
        String password = (pwdCred != null) ? pwdCred.getPassword().toString() : "";
        LOGGER.log(ODLLevel.NOTIFICATION, "User Name: {0}, Password: {1}", new Object[]{userName, password});
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
     * Retrieves credentials from the Credential Store where the current
     * application UID is used as the name of the credential map.
     * This method must be called through AccessController.doPrivileged
     * @param key name of the key in the credential map to retrieve
     * @return PasswordCredential if exists, null otherwise
     * @throws JpsException
     */
    private PasswordCredential _readCredentials(String map, String key) throws JpsException 
    {
        ServiceLocator locator = JpsServiceLocator.getServiceLocator();
        CredentialStore store = locator.lookup(CredentialStore.class);
        // always use application UID as name for the credential map to ensure
        // each application uses its own map and credentials aren't shared
        // String map = ADFContext.getCurrent().getADFApplicationUID();
        return (PasswordCredential)store.getCredential(map, key);
    }
    
    /**
     * Retrieves credentials from the Credential Store by invoking
     * {@link #_readCredentials} as a privileged action.
     * @param key name of the key in the credential map to retrieve
     * @return PasswordCredential if exists, null otherwise
     * @throws JpsException
     */
    private PasswordCredential readCredentials(final String map, final String key) 
    {
        PasswordCredential credentials;
        PrivilegedExceptionAction<PasswordCredential> action = new PrivilegedExceptionAction<PasswordCredential>() 
        {
            @Override
            public PasswordCredential run() throws JpsException 
            {
                return _readCredentials(map, key);
            }
        };
        
        try 
        {
            credentials = AccessController.doPrivileged(action);
        } 
        
        catch (PrivilegedActionException e)
        {
            throw new RuntimeException(e);
        }
        
        return credentials;
    }
}
