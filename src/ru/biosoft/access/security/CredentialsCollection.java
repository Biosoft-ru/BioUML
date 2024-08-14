package ru.biosoft.access.security;

import ru.biosoft.access.exception.CollectionLoginException;

/**
 * Collection which may need user interaction (e.g. login and password) to work correctly
 * @author lan
 *
 * @param <T>
 */
public interface CredentialsCollection
{
    public boolean needCredentials();
    
    public Object getCredentialsBean();
    
    /**
     * Process login
     * @param bean bean previously obtained from getCredentialsBean()
     * @throws CollectionLoginException if login failed
     */
    public void processCredentialsBean(Object bean) throws CollectionLoginException;
}
