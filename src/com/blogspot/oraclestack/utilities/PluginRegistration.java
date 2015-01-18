package com.blogspot.oraclestack.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import oracle.iam.platform.OIMClient;
import oracle.iam.platform.pluginframework.PluginException;
import oracle.iam.platformservice.api.PlatformService;
import oracle.iam.platformservice.api.PlatformUtilsService;
import oracle.iam.platformservice.exception.InvalidCacheCategoryException;
import oracle.iam.platformservice.exception.PlatformServiceAccessDeniedException;

/**
 * Registers or unregisters an OIM Plug-in.
 * @author rayedchan
 */
public class PluginRegistration 
{
    private final PlatformService platformService;
    private final PlatformUtilsService platUtilOps;
    
    public PluginRegistration(OIMClient oimClient)
    {
        this.platformService = oimClient.getService(PlatformService.class);
        this.platUtilOps = oimClient.getService(PlatformUtilsService.class);
    }
    
    /**
     * Registers an plug-in to OIM
     * @param pathToPluginZipFile       Absolute Path to Plug-in zip file
     * @throws FileNotFoundException
     * @throws IOException
     * @throws PlatformServiceAccessDeniedException
     * @throws PluginException
     * @throws InvalidCacheCategoryException 
     */
    public void registerOIMPlugin(String pathToPluginZipFile) throws FileNotFoundException, IOException, PlatformServiceAccessDeniedException, PluginException, InvalidCacheCategoryException
    {
        FileInputStream fis = null;
        
        try
        {
            // Zip file conversion to byte
            File zipFile = new File(pathToPluginZipFile);
            fis = new FileInputStream(zipFile);
            int size = (int) zipFile.length();
            byte[] b = new byte[size];
            int bytesRead = fis.read(b, 0, size);
            
            while (bytesRead < size)
            {
                bytesRead += fis.read(b, bytesRead, size - bytesRead);
            }
            
            // Register Plugin to OIM
            this.platformService.registerPlugin(b);
            System.out.println("Successfully registered plugin.");
 
            // Purge Cache
            this.platUtilOps.purgeCache("ALL");
            System.out.println("Cache Purged.");
        }
        
        finally
        {
            if (fis != null)
            {
                fis.close(); 
            }
        }
    }
    
    /**
     * Unregister an OIM Plug-in
     * @param pluginId          PLUGIN_ID
     * @param pluginVersion     PLUGIN_VERSION
     * @throws PlatformServiceAccessDeniedException
     * @throws InvalidCacheCategoryException
     * @throws PluginException 
     */
    public void unRegisterOIMPlugin(String pluginId, String pluginVersion) throws PlatformServiceAccessDeniedException, InvalidCacheCategoryException, PluginException
    {
        // Remove single plugin
        this.platformService.unRegisterPlugin(pluginId, pluginVersion);
        System.out.println("Successfully unregistered plugin.");

        // Purge Cache;
        this.platUtilOps.purgeCache("ALL");
        System.out.println("Cache Purged.");
    }
}
