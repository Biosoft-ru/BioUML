package ru.biosoft.access.security.biostore;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import one.util.streamex.StreamEx;

import com.developmentontheedge.application.ApplicationUtils;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

import ru.biosoft.access.exception.BiosoftNetworkException;
import ru.biosoft.util.NetworkConfigurator;
import ru.biosoft.util.TextUtil2;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility functions to communicate with biostore server
 */
public class BiostoreConnector
{
    protected static final Logger log = Logger.getLogger(BiostoreConnector.class.getName());

    protected Map<String, String> sessionCookies = new HashMap<>();

    protected String serverLink;
    
    protected String serverKey;
    
    public static final String BIOSTORE_DEFAULT_URL = "https://bio-store.org/biostore";

    public BiostoreConnector(String serverLink)
    {
        this.serverLink = serverLink;
    }

    public BiostoreConnector(String serverLink, String serverKey)
    {
        this.serverLink = serverLink;
        this.serverKey = serverKey;
    }

    /**
     * Check is session exist for user
     * @param username current user name
     * @return
     */
    public boolean checkSession(String username)
    {
        return sessionCookies.containsKey(username);
    }

    /**
     * Request biostore server using HTTPS protocol
     * @param username current user name
     * @param action name of biostore action
     * @param parameters action parameters
     * @return request result as JSON object
     */
    public @Nonnull JsonObject askServer(String username, String action, Map<String, String> parameters)
    {
        NetworkConfigurator.initNetworkConfiguration();
        String stringResponse = null;
        try
        {
            StringBuilder urlParameters = new StringBuilder();
            urlParameters.append( "action=" ).append( TextUtil2.encodeURL( action ) );
            if( serverKey != null )
            {
                urlParameters.append( "&serverName=" ).append( TextUtil2.encodeURL( serverKey ) );
            }
            if( parameters != null )
            {
                for( Map.Entry<String, String> entry : parameters.entrySet() )
                {
                    urlParameters.append( "&" ).append( entry.getKey() ).append( "=" ).append( TextUtil2.encodeURL( entry.getValue() ) );
                }
            }

            if( "removeProject".equals( action ) )
            {
                log.info( "Removing project via '" + serverLink + "', params =\n" + urlParameters );
            }

            HttpURLConnection urlc = (HttpURLConnection)new URL( serverLink ).openConnection();
            urlc.setRequestMethod( "POST" );

            urlc.setUseCaches(false); // Don't look at possibly cached data
            final int TIMEOUT_TEN_MINUTES = 10*60*1000;
            urlc.setConnectTimeout( TIMEOUT_TEN_MINUTES );
            urlc.setReadTimeout( TIMEOUT_TEN_MINUTES );
            String oldCookies = username == null ? null : sessionCookies.get(username);
            if( oldCookies != null )
            {
                //set cookie for session support
                urlc.setRequestProperty("Cookie", oldCookies);
            }
            urlc.setDoOutput( true );
            try( DataOutputStream wr = new DataOutputStream( urlc.getOutputStream() ) )
            {
                wr.writeBytes( urlParameters.toString() );
                wr.flush();
            }

            //read cookies from server response
            List<String> cookies = urlc.getHeaderFields().get("Set-Cookie");
            if( cookies != null )
            {
                String cookieHeader = StreamEx.of(cookies).map( cookie -> TextUtil2.split( cookie, ';' )[0] ).joining( "; " );
                if(username != null)
                    sessionCookies.put(username, cookieHeader);
            }

            stringResponse = ApplicationUtils.readAsString( urlc.getInputStream() ); 
            JsonObject jsonObj = Json.parse( stringResponse ).asObject();
            if( jsonObj == null )
            {   
                log.log(Level.SEVERE, "Cannot parse server response: " + stringResponse );
                throw new Exception( "Cannot parse server response: " + stringResponse );
            }
            return jsonObj;
        }
        catch( Exception e )
        {
            log.log( Level.SEVERE, "Unknown error (username: " + username + ", action: " + action + "). Response: " + stringResponse, e );
            throw new BiosoftNetworkException(e, serverLink);
        }
    }
}
