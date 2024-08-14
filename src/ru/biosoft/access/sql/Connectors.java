package ru.biosoft.access.sql;

import java.io.File;
import java.net.URI;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Nonnull;

import org.json.JSONObject;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.exception.InternalException;
import ru.biosoft.util.ExProperties;
import ru.biosoft.util.LazyValue;

public class Connectors
{
    public static class ConnectionInfo
    {
        private final String host, db, user, password;
        private final String base;

        public ConnectionInfo(String base, String host, String db, String user, String password)
        {
            this.base = base;
            this.host = host;
            this.db = db;
            this.user = user;
            this.password = password;
        }

        public String getHost()
        {
            return host == null && base != null ? getConnectionInfo( base ).getHost() : host;
        }

        public String getDb()
        {
            return db == null && base != null ? getConnectionInfo( base ).getDb() : db;
        }

        public String getUser()
        {
            return user == null && base != null ? getConnectionInfo( base ).getUser() : user;
        }

        public String getPassword()
        {
            return password == null && base != null ? getConnectionInfo( base ).getPassword() : password;
        }

        public String getConnectionUrl()
        {
            return "jdbc:mysql://" + getHost() + ":" + getPort() + "/" + getDb() + "?allowLoadLocalInfile=true";
        }

        public int getPort()
        {
            // TODO: add port setting
            return 3306;
        }

        public Properties toProperites()
        {
            Properties properties = new ExProperties();
            properties.put( SqlDataCollection.JDBC_URL_PROPERTY, getConnectionUrl() );
            properties.put( SqlDataCollection.JDBC_USER_PROPERTY, getUser() );
            properties.put( SqlDataCollection.JDBC_PASSWORD_PROPERTY, getPassword() );
            return properties;
        }
    }

    private static final LazyValue<Map<String, Connectors.ConnectionInfo>> connectors = new LazyValue<Map<String, Connectors.ConnectionInfo>>() {
        @Override
        protected Map<String, ConnectionInfo> doGet() throws Exception
        {
            File connectors_json = new File( "connectors.json" );
            if( System.getProperty( "biouml.server.path" ) != null )
            {
                connectors_json = new File( System.getProperty( "biouml.server.path" ), "connectors.json" );               
            }           

            JSONObject json = new JSONObject( ApplicationUtils.readAsString( connectors_json ) );
            Map<String, ConnectionInfo> result = new HashMap<>();
            @SuppressWarnings ( "unchecked" )
            Iterator<String> keys = json.keys();
            while(keys.hasNext()) {
                String key = keys.next();
                JSONObject profile = json.getJSONObject( key );
                String host = profile.optString( "host", null );
                String user = profile.optString( "user", null );
                String password = profile.optString( "password", null );
                String db = profile.optString( "db", null );
                String base = profile.optString( "super" );
                result.put( key, new ConnectionInfo( base, host, db, user, password ) );
            }
            return result;
        }
    };

    public static @Nonnull ConnectionInfo getConnectionInfo(Properties properties)
    {
        String url = properties.getProperty( SqlDataCollection.JDBC_URL_PROPERTY );
        if(url.startsWith( "jdbc:" ))
        {
            URI uri = URI.create(url.substring("jdbc:".length()));
            return new ConnectionInfo( null, uri.getHost(), uri.getPath().substring( 1 ),
                    properties.getProperty( SqlDataCollection.JDBC_USER_PROPERTY ),
                    properties.getProperty( SqlDataCollection.JDBC_PASSWORD_PROPERTY ) );
        }
        return getConnectionInfo( url );
    }

    public static @Nonnull ConnectionInfo getConnectionInfo(String name)
    {
        ConnectionInfo info = connectors.get().get( name );
        if(info == null) {
            throw new InternalException( "Invalid connection requested: "+name );
        }
        return info;
    }

    public static @Nonnull Connection getConnection(String name)
    {
        ConnectionInfo info = getConnectionInfo( name );
        return SqlConnectionPool.getPersistentConnection( info.getConnectionUrl(), info.getUser(), info.getPassword() );
    }
}
