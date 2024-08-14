package biouml.plugins.server.access;

import java.util.logging.Level;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import one.util.streamex.StreamEx;

import java.util.logging.Logger;

import ru.biosoft.access.exception.BiosoftNetworkException;
import ru.biosoft.access.security.biostore.BiostoreConnector;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Preferences;
import com.developmentontheedge.application.Application;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonObject.Member;

/**
 * Collection of servers with login status
 */
public class ServerRegistry
{
    protected static final Logger log = Logger.getLogger(ServerRegistry.class.getName());

    public static final String PREFERENCES_SERVER = "Servers";
    public static final String PREFERENCES_ADDR_PORT_LIST = "Server addresses";
    static volatile Map<String, SessionInfo> serverAddrs = null;
    private static final BiostoreConnector connector = new BiostoreConnector(BiostoreConnector.BIOSTORE_DEFAULT_URL+"/client");

    /**
     * Get server names as array of strings
     */
    public static String[] getServerHosts(String userName, String password)
    {
        init();
        Map<String, String> parameters = new HashMap<>();
        if(userName == null) userName = "";
        if(!userName.isEmpty())
        {
            parameters.put("username", userName);
            parameters.put("password", password);
        }
        JsonObject biostoreServers = connector.askServer(userName, "servers", parameters);
        if(!biostoreServers.getString("type", "ok").equals("ok"))
            throw new BiosoftNetworkException(BiostoreConnector.BIOSTORE_DEFAULT_URL, biostoreServers);
        Set<String> result = new LinkedHashSet<>();
        for(Member m : biostoreServers)
        {
            JsonObject serverRecord = m.getValue().asObject();
            String address = serverRecord.get("address").asString();
            String url = serverRecord.get( "url" ).asString();
            if( url == null )
                url = address;
            result.add( serverRecord.get( "name" ).asString() + " (" + url + ")" );
        }
        result.addAll(serverAddrs.keySet());
        return result.toArray(new String[result.size()]);
    }
    
    private static final Pattern namedServerPattern = Pattern.compile(".+ \\((.+)\\)");
    /**
     * Converts server host returned by getServerHosts to server address ready to connect
     * @param server like "BioUML server (ie.biouml.org)"
     * @return URL like "ie.biouml.org:80/biouml/"
     */
    public static String getServerURL(String server)
    {
        if( server == null )
            return null;
        String result = server.trim();
        Matcher matcher = namedServerPattern.matcher(result);
        if( matcher.matches() )
            result = matcher.group(1);
        if( !result.contains("/") )
        {
            if( !result.contains(":") )
                result = result + ":80";
            result = result + "/biouml/";
        }
        if( !result.endsWith("/") )
            result += "/";
        return result;
    }

    /**
     * Add new server to registry
     */
    public static void addServer(String host)
    {
        init();
        if( !serverAddrs.containsKey(host) )
        {
            serverAddrs.put(host, new SessionInfo());
        }
    }

    /**
     * Set session ID for server
     */
    public static void setServerSession(String host, String session)
    {
        init();
        if( serverAddrs.containsKey(host) )
        {
            SessionInfo info = serverAddrs.get(host);
            info.connected = true;
            info.sessionId = session;
        }
    }

    /**
     * Get session ID for server
     */
    public static String getServerSession(String host)
    {
        init();
        if( serverAddrs.containsKey(host) )
        {
            return serverAddrs.get(host).sessionId;
        }
        return null;
    }

    /**
     * Save servers info to application preferences
     */
    public static void saveServers()
    {
        String list = String.join(";", serverAddrs.keySet());

        MessageBundle resources = new MessageBundle();
        Preferences preferences = Application.getPreferences();
        Preferences serverPreferences = (Preferences)preferences.getValue(PREFERENCES_SERVER);
        if( serverPreferences == null )
        {
            try
            {
                serverPreferences = new Preferences();
                preferences.add(new DynamicProperty(PREFERENCES_SERVER, resources.getResourceString("SERVER_PREFERENCES_PN"), resources
                        .getResourceString("SERVER_PREFERENCES_PD"), Preferences.class, serverPreferences));
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Can not create server properties: " + e.getMessage());
            }
        }
        if( serverPreferences == null )
            return;

        String key = PREFERENCES_ADDR_PORT_LIST;
        if( serverPreferences.getProperty(key) != null )
            serverPreferences.setValue(key, list);
        else
        {
            try
            {
                serverPreferences.add(new DynamicProperty(key, resources
                        .getResourceString("LOAD_DATABASE_DIALOG_PREFERENCES_SERVERLIST_PN"), resources
                        .getResourceString("LOAD_DATABASE_DIALOG_PREFERENCES_SERVERLIST_PD"), String.class, list));
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Can not save server list: " + e.getMessage());
            }
        }
    }

    protected static void init()
    {
        if( serverAddrs == null )
        {
            synchronized( ServerRegistry.class )
            {
                if( serverAddrs == null )
                {
                    String addrPortList = "";

                    Preferences preferences = getServersSettings();
                    if( ( preferences != null ) && ( preferences.getProperty(PREFERENCES_ADDR_PORT_LIST) != null ) )
                    {
                        addrPortList = preferences.getValueAsString(PREFERENCES_ADDR_PORT_LIST);
                    }

                    serverAddrs = StreamEx.split( addrPortList, ';' )
                        .mapToEntry( host -> new SessionInfo() ).toCustomMap( LinkedHashMap::new );
                }
            }
        }
    }

    protected static Preferences getServersSettings()
    {
        Preferences preferences = Application.getPreferences();
        Preferences proxyPreferences = (Preferences)preferences.getValue(PREFERENCES_SERVER);
        return proxyPreferences;
    }

    protected static class SessionInfo
    {
        public boolean connected = false;
        public String sessionId = null;
    }
}
