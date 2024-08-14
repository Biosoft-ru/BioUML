package ru.biosoft.access.users;

import java.util.List;

import ru.biosoft.access.core.DataCollection;

/**
 * Functions to work with jabbed server
 */
public abstract class JabberProvider
{
    public static final String JABBER_PROVIDER = "jabberProvider";
    
    /** Names of jabber groups*/
    public static final String SUPPORT_GROUP_NAME = "support";
    public static final String USERS_GROUP_NAME = "users";

    /**
     * Get users of support team
     * @param origin
     * @return
     */
    public abstract List<UserInfo> getSupportUsers(DataCollection origin);
    
    /**
     * Check if user is online now
     * @param userName
     * @return
     */
    public abstract boolean isUserOnline(String userName);
    
    /**
     * Create new jabber user if not exists
     * @param username
     * @param password
     */
    public abstract void createUser(String username, String password);
}
