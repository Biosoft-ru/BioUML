package ru.biosoft.access.security;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.NamingEnumeration;

import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

/**
 * SecurityProvider based on LDAP data
 * See: https://stackoverflow.com/questions/12317205/ldap-authentication-using-java
 */
public class LdapSecurityProvider implements SecurityProvider
{
    String[] products;
    Hashtable<String, Object> env = new Hashtable<String, Object>();

    String securityProviderLink;
    String securityProviderSearchBase;
    String securityProviderLookup;   
    
    @Override
    public void init(Properties properties)
    {
        this.products = properties.getProperty( "products", "Server;Import" ).split( ";" );
        this.env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
        this.env.put( Context.PROVIDER_URL, securityProviderLink = properties.getProperty( "securityProviderLink" ) );
        String bindUser = properties.getProperty( "securityProviderPrincipal" );
        if( bindUser != null && bindUser.trim().length() > 0 )
        {
            this.env.put( Context.SECURITY_AUTHENTICATION, "simple" );
            this.env.put( Context.SECURITY_PRINCIPAL, bindUser );
            this.env.put( Context.SECURITY_CREDENTIALS, properties.getProperty( "securityProviderCredentials" ) );
        }
        else
        {
            this.env.put( Context.SECURITY_AUTHENTICATION, "none" );
        }

        this.securityProviderSearchBase = properties.getProperty( "securityProviderSearchBase" );
        this.securityProviderLookup = properties.getProperty( "securityProviderLookup" );
    }

    @Override
    public UserPermissions authorize(String username, String password, String remoteAddress, String jwToken) throws SecurityException
    {
        if( username.isEmpty() || "anonymous".equals( username ) )
        {
            return new UserPermissions( "", "", new String[] { "Jupyter", "R" } );
        } 

        SecurityException bad = null;
        try
        {
            InitialDirContext ctx = new InitialDirContext( env ); 

            SearchControls ctrls = new SearchControls();
            ctrls.setReturningAttributes( new String[] { "givenName", "sn", "memberOf" } );
            ctrls.setSearchScope( SearchControls.SUBTREE_SCOPE );

            NamingEnumeration<SearchResult> answers = ctx.search( securityProviderSearchBase, 
                securityProviderLookup.replace( "${user}", username ), ctrls );
            SearchResult result = answers.nextElement();

            String realUser = result.getNameInNamespace();

            try 
            {
                Properties props = new Properties();
                props.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
                props.put( Context.SECURITY_AUTHENTICATION, "simple" );
                props.put( Context.PROVIDER_URL, securityProviderLink );
                props.put( Context.SECURITY_PRINCIPAL, realUser );
                props.put( Context.SECURITY_CREDENTIALS, password );

                new InitialDirContext( props );
 
                return new UserPermissions( username, password, products, Collections.emptyMap() );
            } 
            catch( Exception e1 ) 
            {
                bad = new SecurityException( "Incorrect password", e1 );
            }
        }
        catch( Exception e2 ) 
        {
            bad =  new SecurityException( "Unknown user", e2 );
        }

        throw bad;
    }

    @Override
    public int getPermissions(String dataCollectionName, UserPermissions userInfo)
    {
        if( "".equals( userInfo.getUser() ) || "anonymous".equals( userInfo.getUser() ) )
        {
            return Permission.INFO | Permission.READ;
        }
        return Permission.ALL;
    }

    @Override
    public int getGuestPermissions(String dataCollectionName)
    {
        return Permission.INFO | Permission.READ;
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
