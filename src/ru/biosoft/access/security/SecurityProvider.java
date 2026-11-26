package ru.biosoft.access.security;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Special interface for security provider objects.
 * Possible implementations:
 *  - based on SQL bioumlsupport2 database provider
 *  - based on Biostore server provider
 */
public interface SecurityProvider
{
    public void init(Properties properties);
    public UserPermissions authorize(String username, String password, String remoteAddress, String jwToken) throws SecurityException;
    public default UserPermissions reauthorize(UserPermissions permissions) throws SecurityException
    {
        String password = permissions.getPassword();
        return password == null ? null : authorize(permissions.getUser(), password, null, null);
    }
    public int getPermissions(String dataCollectionName, UserPermissions userInfo);
    public int getGuestPermissions(String dataCollectionName);
    public boolean register(String username, String password, String email, Map<String, Object> props);
    public boolean deleteUser(String username);
    public boolean changePassword(String username, String oldPassword, String password);
    public Map<String, Object> getUserInfo(String username, String password);
    public default Map<String, Object> getUserInfo(UserPermissions permissions)
    {
        return getUserInfo(permissions.getUser(), permissions.getPassword());
    }
    public boolean updateUserInfo(String username, String password, Map<String, String> parameters);
    public void createGroup(String username, String groupName, boolean reuse) throws Exception;
    public boolean setGroupPermission(String username, String groupName, String collectionName, int permission);
    public default String getServerName() {return "localhost:8080";};
    public String getRegistrationURL();
    public default String getForgetPasswordURL() {return "";}
    public default String getLoginURL(String addParams) {return "";}
    public default String getReinitURL(String addParams) {return getLoginURL(addParams);}
    public default String getLogoutURL() {return "";}
    public List<String> getGroups();
    public List<String> getGroupUsers(String group);
    
    public boolean isOnAllowedPath(File file) throws Exception;
    
    // TODO: refactor this method (change semantic or remove)
    public boolean checkUse(String username, String serviceName);
    enum ProjectGroupStatus { PRESENT, ABSENT, UNKNOWN }

    /** ABSENT requires an authoritative lookup. A failed or unsupported lookup must not report absence. */
    default ProjectGroupStatus getProjectGroupStatus(String groupName) throws Exception
    {
        return ProjectGroupStatus.UNKNOWN;
    }

    /** Treat an authoritatively absent group as already removed. */
    public boolean removeProject(String groupName, String username, String password, String jwToken) throws Exception;
    public default void sendEmail(String username, String subject, String message, Map<String, String> parameters) throws Exception {};
}