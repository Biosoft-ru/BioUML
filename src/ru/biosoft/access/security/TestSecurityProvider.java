package ru.biosoft.access.security;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * SecurityProvider for development purposes
 * 
 * Every user has admin privileges
 * 
 * @author lan
 */
public class TestSecurityProvider implements SecurityProvider
{
    String[] products;
    
    @Override
    public void init(Properties properties)
    {
        this.products = properties.getProperty( "products", "Server;Import" ).split( ";" );
    }

    @Override
    public UserPermissions authorize(String username, String password, String remoteAddress, String jwToken) throws SecurityException
    {
        return new UserPermissions(username, password, products, Collections.emptyMap());
    }

    @Override
    public int getPermissions(String dataCollectionName, UserPermissions userInfo)
    {
        return Permission.ALL;
    }

    @Override
    public int getGuestPermissions(String dataCollectionName)
    {
        return Permission.ALL;
    }

    @Override
    public boolean register(String username, String password, String email, Map<String, Object> props)
    {
        return false;
    }

    @Override
    public boolean deleteUser(String username)
    {
        return false;
    }

    @Override
    public boolean changePassword(String username, String oldPassword, String password)
    {
        return false;
    }

    @Override
    public Map<String, Object> getUserInfo(String username, String password)
    {
        return Collections.singletonMap( "name", username );
    }

    @Override
    public boolean updateUserInfo(String username, String password, Map<String, String> parameters)
    {
        return false;
    }

    @Override
    public void createGroup(String username, String groupName, boolean reuse) throws Exception
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean setGroupPermission(String username, String groupName, String collectionName, int permission)
    {
        return false;
    }

    @Override
    public String getRegistrationURL()
    {
        return null;
    }

    @Override
    public List<String> getGroups()
    {
        return Arrays.asList( "support" );
    }

    @Override
    public List<String> getGroupUsers(String group)
    {
        return Collections.emptyList();
    }

    @Override
    public boolean checkUse(String username, String serviceName)
    {
        return true;
    }

    @Override
    public boolean isOnAllowedPath(File file) throws Exception
    {
        return false;
    }

    @Override
    public boolean removeProject(String groupName, String username, String password, String jwToken)
    {
        return true;
    }
}
