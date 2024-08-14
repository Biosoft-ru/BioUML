package ru.biosoft.access.security.biostore;

import java.io.File;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONException;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonObject.Member;

import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.security.Permission;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.security.SecurityProvider;
import ru.biosoft.access.security.UserPermissions;
import ru.biosoft.util.JsonUtils;
import ru.biosoft.util.TextUtil;

/**
 * Security provider to work with remote Biostore server
 */
public class RemoteSecurityProvider implements SecurityProvider
{
    protected static final Logger log = Logger.getLogger(RemoteSecurityProvider.class.getName());

    public static final String TYPE_OK = "ok";
    public static final String TYPE_ERROR = "error";
    public static final String TYPE_NEED_LOGIN = "unauthorized";

    public static final String ATTR_TYPE = "type";
    public static final String ATTR_MESSAGE = "message";
    public static final String ATTR_PERMISSION = "permission";
    public static final String ATTR_INVALIDATE = "invalidate";
    public static final String ATTR_INFO = "info";

    protected static final String guestUserName = "anonymous";

    public static final String PROVIDER_SERVER_LINK_PROPERTY = "securityProviderLink";
    public static final String PROVIDER_SERVER_NAME_PROPERTY = "securityProviderServer";

    private static final long MAX_PERMISSION_TIME = 1000L*60*60*24*365;  // 365 days

    protected String serverName;
    protected String biostoreLink;
    protected BiostoreConnector biostore;

    @Override
    public void init(Properties properties)
    {
        serverName = properties.getProperty(PROVIDER_SERVER_NAME_PROPERTY);
        biostoreLink = properties.getProperty(PROVIDER_SERVER_LINK_PROPERTY);
        biostore = new BiostoreConnector(biostoreLink+"/permission", serverName);
    }

    @Override
    public String getServerName()
    {
        return serverName;
    }

    public BiostoreConnector getConnector()
    {
        return biostore;
    }

    @Override
    public UserPermissions authorize(String username, String password, String remoteAddress, String jwToken) throws SecurityException
    {
        UserPermissions result = null;
        Map<String, String> parameters = new HashMap<>();
        String targetUser = username;
        if( jwToken != null )
        {
            parameters.put( "jwtoken", jwToken );
        }
        else
        {
            String[] fields = username.split( "\\$" );
            parameters.put( "username", fields[0] );
            parameters.put( "password", password );
            if( fields.length > 1 )
            {
                parameters.put( "sudo", fields[1] );
                targetUser = fields[1];
            }
        }
        if(remoteAddress != null)
        { 
            parameters.put("ip", remoteAddress);
        }  
        JsonObject response = biostore.askServer(username, "login", parameters);
        try
        {
            String status = response.get(ATTR_TYPE).asString();
            if( status.equals(TYPE_OK) )
            {
                String[] products = getProducts(response).toArray(String[]::new);
                result = new UserPermissions( targetUser, password, products, getLimits( response ), getGroups( response ) );
                initPermissions(result, response);
            }
            else
            {
                if( response.get(ATTR_MESSAGE) != null )
                {
                    String msg = response.get(ATTR_MESSAGE).asString();
                    String unameStr = username;
                    if( msg.indexOf( "Incorrect email or password" ) >= 0 )
                    {
                        unameStr += "/" + password;
                    } 
                    log.log( Level.SEVERE, "While authorizing " + unameStr + " ("+remoteAddress+"): " + msg );
                    throw new SecurityException( msg );
                } 
                else
                {
                    throw new SecurityException(response.toString());
                }
            }
        }
        catch( UnsupportedOperationException e )
        {
            log.log(Level.SEVERE, "Invalid JSON response", e);
            throw new SecurityException("Error communicating to authentication server");
        }
        return result;
    }

    /**
     * @param response
     * @return map limitName -> limitValue
     */
    private Map<String, Long> getLimits(JsonObject response)
    {
        return JsonUtils.arrayOfObjects( response.get("limits") ).toMap(
                limit -> limit.get("name").asString(),
                limit -> limit.get("value").asLong());
    }

    private StreamEx<String> getProducts(JsonObject response)
    {
        return JsonUtils.arrayOfObjects( response.get( "products" ) ).map( val -> val.get( "name" ).asString() );
    }

    private Map<String, Boolean> getGroups(JsonObject response)
    {
        return JsonUtils.arrayOfObjects( response.get( "groups" ) ).toMap( 
                group -> group.get( "name" ).asString(),
                group -> group.getBoolean( "isAdmin", false ) );
    }

    private void initPermissions(UserPermissions userPermissions, JsonObject response)
    {
        Hashtable<String, Permission> dbToPermission = userPermissions.getDbToPermission();
        long time = System.currentTimeMillis()+MAX_PERMISSION_TIME;
         
        if(response.getBoolean("admin", false))
        {
            dbToPermission.put("/", new Permission(Permission.ADMIN, userPermissions.getUser(), "", time));
            JsonUtils.arrayOfObjects( response.get( "permissions" ) ).forEach( obj -> {
                String path = obj.get( "path" ).asString(); 
                if( path.startsWith( "analyses/Jupyter/" ) || path.startsWith( "analyses/Docker/" ) )
                {
                    if( path.startsWith( "analyses/Docker/" ) )
                    {
                        path = TextUtil.subst( path, "analyses/Docker/", "analyses/Docker/store/" );
                    } 
                    dbToPermission.put(
                        path,
                        new Permission( obj.get( "permissions" ).asInt(), userPermissions.getUser(), "", time ) );
                }
            });
        } 
        else
        {
            JsonUtils.arrayOfObjects( response.get( "permissions" ) ).forEach( obj -> {
                String path = obj.get( "path" ).asString(); 
                if( path.startsWith( "analyses/Docker/" ) )
                {
                    path = TextUtil.subst( path, "analyses/Docker/", "analyses/Docker/store/" );
                } 
                dbToPermission.put(
                    path,
                    new Permission( obj.get( "permissions" ).asInt(), userPermissions.getUser(), "", time ) );
            });
        }

        JsonUtils.arrayOfObjects( response.get( "groups" ) )
                .map( val -> val.get( "name" ).asString() ).forEach(name ->
                    dbToPermission.put( DataElementPath.create( "groups", name ).toString(),
                            new Permission(Permission.READ, userPermissions.getUser(), "", time ) ) );
    }

    @Override
    public int getPermissions(String dataCollectionName, UserPermissions userInfo)
    {
        Hashtable<String, Permission> dbToPermission = userInfo.getDbToPermission();
        if(userInfo.getUser().isEmpty() && dbToPermission.isEmpty())
        {
            try
            {
                Map<String, String> parameters = new HashMap<>();
                parameters.put("username", "");
                parameters.put("password", "");
                JsonObject response = biostore.askServer("", "login", parameters);
                if( response != null )
                {
                    String status = response.get(ATTR_TYPE).asString();
                    if( status.equals(TYPE_OK) )
                    {
                        initPermissions(userInfo, response);
                    }
                }
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "getPermissions", e);
            }
        }
        if(dbToPermission.containsKey("/") && ((dbToPermission.get("/").getPermissions() & Permission.ADMIN) != 0))
            return Permission.ALL;
        DataElementPath originalPath = DataElementPath.create(dataCollectionName);
        DataElementPath path = originalPath;
        while(!path.isEmpty())
        {
            if(dbToPermission.containsKey(path.toString()))
            {
                return dbToPermission.get(path.toString()).getPermissions();
            }
            path = path.getParentPath();
        }
        // Provide info access for all the parents of accessible paths
        for(Entry<String, Permission> entry: dbToPermission.entrySet())
        {
            if( entry.getValue().isInfoAllowed() && originalPath.isAncestorOf(DataElementPath.create(entry.getKey())) )
                return Permission.INFO;
        }
        return 0;   // no permissions
    }

    @Override
    public int getGuestPermissions(String dataCollectionName)
    {
        UserPermissions userPermissions = authorize( "", "", null, null );
        Hashtable<String, Permission> dbToPermission = userPermissions.getDbToPermission();
        DataElementPath path = DataElementPath.create(dataCollectionName);
        while(!path.isEmpty())
        {
            if(dbToPermission.containsKey(path.toString()))
            {
                return dbToPermission.get(path.toString()).getPermissions();
            }
            path = path.getParentPath();
        }
        return 0;   // no permissions
    }

    @Override
    public boolean changePassword(String username, String oldPassword, String password)
    {
        if( !biostore.checkSession(username) )
        {
            authorize( username, oldPassword, null, null );
        }

        Map<String, String> parameters = new HashMap<>();
        parameters.put("oldPassword", oldPassword);
        parameters.put("password", password);
        JsonObject response = biostore.askServer(username, "changePassword", parameters);
        if( response != null )
        {
            try
            {
                if( response.get(ATTR_INVALIDATE) != null )
                {
                    //invalidate permissions
                    SecurityManager.invalidatePermissions();
                }

                String status = response.get(ATTR_TYPE).asString();
                if( status.equals(TYPE_OK) )
                {
                    return true;
                }
                else
                {
                    log.log(Level.SEVERE, response.get(ATTR_MESSAGE).toString());
                }
            }
            catch( UnsupportedOperationException e )
            {
                log.log(Level.SEVERE, "Invalid JSON response", e);
            }
        }
        return false;
    }

    @Override
    public boolean register(String username, String password, String email, Map<String, Object> props)
    {
        boolean result = false;
        Map<String, String> parameters = new HashMap<>();
        parameters.put("username", username);
        parameters.put("password", password);
        parameters.put("email", email);
        JsonObject response = biostore.askServer(username, "register", parameters);
        if( response != null )
        {
            try
            {
                String status = response.get(ATTR_TYPE).asString();
                if( status.equals(TYPE_OK) )
                {
                    result = true;
                }
            }
            catch( UnsupportedOperationException e )
            {
                log.log(Level.SEVERE, "Invalid JSON response", e);
            }
        }
        return result;
    }

    @Override
    public boolean deleteUser(String username)
    {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Map<String, Object> getUserInfo(String username, String password)
    {
        Map<String, String> parameters = new HashMap<>();
        parameters.put( "username", username );
        parameters.put( "password", password );
        JsonObject response = biostore.askServer(username, "getUser", parameters);
        try
        {
            String status = response.get( ATTR_TYPE ).asString();
            if( status.equals( TYPE_OK ) )
            {
                Map<String, Object> result = new HashMap<>();
                JsonObject info = response.get( ATTR_INFO ).asObject();
                for( Member m : info )
                {
                    result.put( m.getName(), JsonUtils.toOrgJson( m.getValue() ) );
                }
                return result;
            }
            else
            {
                String prefix = username == null ? "" : "(user: " + username + ") ";
                log.log( Level.SEVERE, prefix + response.get( ATTR_MESSAGE ).toString() );
            }
        }
        catch( JSONException | UnsupportedOperationException e )
        {
            log.log( Level.SEVERE, "Invalid JSON response", e );
        }
        return null;
    }

    @Override
    public boolean updateUserInfo(String username, String password, Map<String, String> parameters)
    {
        parameters.put( "username", username );
        parameters.put( "password", password );
        JsonObject response = biostore.askServer(username, "updateUser", parameters);
        try
        {
            checkResponse( response );
            return true;
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "While updating user info for "+username+": "+e.getMessage());
        }
        return false;
    }

    @Override
    public void createGroup(String username, String groupName, boolean reuse) throws Exception
    {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("group", groupName);
        parameters.put("user", username);
        JsonObject response = biostore.askServer(username, "createGroup", parameters);
        try
        {
            checkResponse( response );
        }
        catch( Exception e )
        {
            throw new Exception("While creating group "+groupName+": "+e.getMessage(), e);
        }
    }

    @Override
    public boolean setGroupPermission(String username, String groupName, String collectionName, int permission)
    {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("group", groupName);
        parameters.put("module", collectionName);
        parameters.put("permission", String.valueOf(permission));
        JsonObject response = biostore.askServer(username, "setGroupPermission", parameters);
        try
        {
            checkResponse( response );
            return true;
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE,  "While adding permissions to " + collectionName + " for " + groupName + ": " + e.getMessage() );
        }
        return false;
    }

    private void checkResponse(JsonObject response) throws Exception
    {
        if( !response.get( ATTR_TYPE ).asString().equals( TYPE_OK ) )
            throw new Exception( response.get( ATTR_MESSAGE ).asString() );
    }

    @Override
    public boolean checkUse(String username, String serviceName)
    {
        boolean result = false;
        Map<String, String> parameters = new HashMap<>();
        if( serviceName == null )
        {
            serviceName = "Server";
        }
        parameters.put("productName", serviceName);
        JsonObject response = biostore.askServer(username, "checkUse", parameters);
        if( response != null )
        {
            try
            {
                String status = response.get(ATTR_TYPE).asString();
                if( status.equals(TYPE_OK) )
                {
                    result = true;
                }
            }
            catch( UnsupportedOperationException e )
            {
                log.log(Level.SEVERE, "Invalid JSON response", e);
            }
        }
        return result;
    }

    @Override
    public String getRegistrationURL()
    {
        //        return biostoreLink + "#o?_t_=users&_on_=Registration&server="+TextUtil.encodeURL( serverName );
        return biostoreLink + "#!form/users/All%20records/Registration";//TODO: send server name
    }

    @Override
    public String getForgetPasswordURL()
    {
        return biostoreLink + "#!form/users/All%20records/Reset%20password";
    }

    @Override
    public String getLoginURL(String addParams)
    {
        String link = biostoreLink.replaceFirst( "api$", "" );
        return link + "#!serverLogin/"+TextUtil.encodeURL( serverName )+"/"+TextUtil.encodeURL( addParams );
    }

    @Override
    public String getReinitURL(String addParams)
    {
        return getLoginURL( addParams ) + "/auto";
    }

    @Override
    public String getLogoutURL()
    {
        String link = biostoreLink.replaceFirst( "api$", "" );
        return link + "#!serverLogin/"+TextUtil.encodeURL( serverName )+"//logout";
    }

    @Override
    public List<String> getGroups()
    {
        Map<String, String> parameters = new HashMap<>();
        JsonObject response = biostore.askServer(null, "groups", parameters);
        return JsonUtils.arrayOfStrings( response.get("groups") ).toList();
    }

    @Override
    public List<String> getGroupUsers(String group)
    {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("group", group);
        JsonObject response = biostore.askServer(null, "groupUsers", parameters);
        return JsonUtils.arrayOfStrings( response.get("groupUsers") ).toList();
    }

    @Override
    public boolean isOnAllowedPath(File file) throws Exception
    {
        return false;
    }

    @Override
    public boolean removeProject(String projectName, String username, String password, String jwToken) throws Exception
    {
        Map<String, String> parameters = new HashMap<>();
        parameters.put( "projectName", projectName );
        if( jwToken != null )
            parameters.put( "jwtoken", jwToken );
        else
        {
            parameters.put( "username", username );
            parameters.put( "password", password );
        }
        log.info( "Removing project with BioStore, params = " + parameters );
        JsonObject response = biostore.askServer( username, "removeProject", parameters );
        try
        {
            checkResponse( response );
            return true;
        }
        catch( Exception e )
        {
            throw new Exception( "While removing project " + projectName + ": " + e.getMessage(), e );
        }
    }

    @Override
    public void sendEmail(String username, String subject, String message, Map<String, String> parameters) throws Exception
    {
        UserPermissions userPermissions = SecurityManager.getCurrentUserPermission();
        if( userPermissions != null )
        {
            String user = userPermissions.getUser();
            if( user != null && !user.isEmpty() && user.equals( username ) )
            {
                parameters.put( "subject", subject );
                if( message != null )
                    parameters.put( "body", message );
                parameters.put( "username", username );
                parameters.put( "password", userPermissions.getPassword() );
                JsonObject response = biostore.askServer( username, "sendEmail", parameters );
                try
                {
                    checkResponse( response );
                    return;
                }
                catch( Exception e )
                {
                    throw new Exception( "While sending message to user " + username + ": " + e.getMessage(), e );
                }
            }
            else
                throw new Exception( "While sending message to user " + username + ": not a valid user" );
        }

    }
}
