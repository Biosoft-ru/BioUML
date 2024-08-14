package ru.biosoft.access.security.biostore;

import java.util.logging.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import java.util.logging.Logger;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.users.JabberProvider;
import ru.biosoft.access.users.UserInfo;

/**
 * Remote Jabber server provider
 */
public class RemoteJabberProvider extends JabberProvider
{
    protected static final Logger log = Logger.getLogger(RemoteJabberProvider.class.getName());

    public static final String JABBER_SERVER_LINK = "jabberServer";

    public static final String ATTR_VALUE = "value";

    protected Properties properties;
    protected BiostoreConnector biostore;

    protected static final String guestUserName = "anonymous";

    public RemoteJabberProvider(Properties properties)
    {
        this.properties = properties;
        this.biostore = new BiostoreConnector(properties.getProperty(JABBER_SERVER_LINK));
    }

    @Override
    public List<UserInfo> getSupportUsers(DataCollection origin)
    {
        List<UserInfo> result = new ArrayList<>();
        JsonObject response = biostore.askServer(guestUserName, "getSupport", null);
        if( response != null )
        {
            try
            {
                String status = response.get(RemoteSecurityProvider.ATTR_TYPE).asString();
                if( status.equals(RemoteSecurityProvider.TYPE_OK) )
                {
                    for( JsonValue val : response.get(ATTR_VALUE).asArray() )
                    {
                        result.add(new UserInfo(origin, val.asString()));
                    }
                }
                else
                {
                    log.log(Level.SEVERE, response.get(RemoteSecurityProvider.ATTR_MESSAGE).toString());
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
    public boolean isUserOnline(String userName)
    {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("username", userName);
        JsonObject response = biostore.askServer(guestUserName, "isOnline", parameters);
        if( response != null )
        {
            try
            {
                String status = response.get(RemoteSecurityProvider.ATTR_TYPE).asString();
                if( status.equals(RemoteSecurityProvider.TYPE_OK) )
                {
                    return Boolean.TRUE.equals( response.get( ATTR_VALUE ).asBoolean() );
                }
                else
                {
                    log.log(Level.SEVERE, response.get(RemoteSecurityProvider.ATTR_MESSAGE).toString());
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
    public void createUser(String username, String password)
    {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("username", username);
        parameters.put("password", password);
        JsonObject response = biostore.askServer(guestUserName, "createUser", parameters);
        if( response != null )
        {
            try
            {
                String status = response.get(RemoteSecurityProvider.ATTR_TYPE).asString();
                if( !status.equals(RemoteSecurityProvider.TYPE_OK) )
                {
                    log.log(Level.SEVERE, response.get(RemoteSecurityProvider.ATTR_MESSAGE).toString());
                }
            }
            catch( UnsupportedOperationException e )
            {
                log.log(Level.SEVERE, "Invalid JSON response", e);
            }
        }
    }
}
