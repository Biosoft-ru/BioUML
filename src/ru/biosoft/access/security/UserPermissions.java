package ru.biosoft.access.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

/**
 * Info about user and his permission
 */
public class UserPermissions
{
    /**
     * Timeout in milliseconds for
     */
    public static final int TIMEOUT = 3600000;

    /**
     * User name (login)
     */
    protected String user;
    /**
     * User password
     */
    protected String password;
    
    /**
     * Available products
     */
    protected Set<String> products;
    
    /**
     * User limits
     */
    protected Map<String, Long> limits;
    /**
     * Indicates that session in dead state
     * which means that after all session threads are finished, it will be wiped completely.
     */
    protected boolean dead;

    protected Hashtable<String, Permission> dbToPermission;

    /**
     * Groups with admin indicator
     */
    protected Map<String, Boolean> groups;

    /**
     * Time when this permission object should be expired
     */
    protected long expirationTime;

    public UserPermissions(String user, String password)
    {
        this(user, password, null, null);
    }
    
    public UserPermissions(String user, String password, String[] products)
    {
        this(user, password, products, null);
    }
    
    public UserPermissions(String user, String password, String[] products, Map<String, Long> limits)
    {
        this( user, password, products, limits, null );
    }

    public UserPermissions(String user, String password, String[] products, Map<String, Long> limits, Map<String, Boolean> groups)
    {
        this.user = user;
        this.password = password;
        this.dbToPermission = new Hashtable<>();
        this.products = products == null ? Collections.<String>emptySet() : new HashSet<>(Arrays.asList(products));
        this.limits = limits == null ? Collections.<String, Long>emptyMap() : new HashMap<>(limits);
        this.groups = groups == null?Collections.<String, Boolean>emptyMap() : new HashMap<>(groups);
        updateExpirationTime();
    }

    /**
     * Call this when user shows some activity to prolong session
     */
    public void updateExpirationTime()
    {
        expirationTime = System.currentTimeMillis() + TIMEOUT;
    }

    /**
     * Check whether permissions object is expired
     * @return true if permissions object is expired
     */
    public boolean isExpired()
    {
        return System.currentTimeMillis() > expirationTime;
    }

    public String getUser()
    {
        return user;
    }

    public void setUser(String user)
    {
        this.user = user;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public Hashtable<String, Permission> getDbToPermission()
    {
        return dbToPermission;
    }

    public boolean isDead()
    {
        return dead;
    }

    public void setDead(boolean dead)
    {
        this.dead = dead;
    }
    
    public boolean isProductAvailable(String productName)
    {
        return products.contains(productName);
    }

    /**
     * @param limitName
     * @return limit value or null if no such limit is available
     */
    public Long getLimit(String limitName)
    {
        return limits.get(limitName);
    }

    public Boolean isGroupAdmin(String projectName)
    {
        return groups.getOrDefault( projectName, false );
    }
}
