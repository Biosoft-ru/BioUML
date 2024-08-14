package biouml.plugins.server.access;

import java.util.List;

import one.util.streamex.StreamEx;

import ru.biosoft.util.LazyValue;
import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Preferences;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.application.Application;

/**
 * SQL databases connection registry
 */
public class SQLRegistry
{
    /**
     * Name of preferences property
     */
    public static final String SQL_SERVERS_PREFERENCES = "SQL servers";

    /**
     * Default SQL server list
     */
    public static final String DEFAULT_SQL_SERVERS = "mysql:localhost:3306:biouml:root:";

    protected static final LazyValue<List<SQLInfo>> serverList = new LazyValue<List<SQLInfo>>("SQL servers list")
    {
        @Override
        protected List<SQLInfo> doGet() throws Exception
        {
            return StreamEx.split(Application.getPreferences().getStringValue(
            ServerRegistry.PREFERENCES_SERVER + "/" + SQL_SERVERS_PREFERENCES, DEFAULT_SQL_SERVERS ), ';')
                    .remove( String::isEmpty ).map( SQLInfo::new ).toList();
        }
    };

    /**
     * Get SQL connection info list
     */
    public static List<SQLInfo> getSQLServersList()
    {
        return serverList.get();
    }

    /**
     * Add new SQL server
     */
    public static void addSQLServer(String type, String host, String port, String database, String username, String password)
    {
        serverList.get().add(new SQLInfo(type, host, port, database, username, password));
        saveSQLServerList();
    }

    /**
     * Remove SQL info record
     */
    public static void removeSQLServer(SQLInfo sqlInfo)
    {
        serverList.get().remove(sqlInfo);
        saveSQLServerList();
    }

    protected static void saveSQLServerList()
    {
        if( serverList.isInitialized() )
        {
            Preferences preferences = Application.getPreferences();
            try
            {
                Preferences properties = preferences.getPreferencesValue(ServerRegistry.PREFERENCES_SERVER);
                if( properties == null )
                {
                    properties = new Preferences();
                    preferences.add(new DynamicProperty(ServerRegistry.PREFERENCES_SERVER, Preferences.class, properties));
                }
                String servers = StreamEx.of( serverList.get() ).map( SQLInfo::serialize ).joining( ";" );
                properties.add( new DynamicProperty( SQL_SERVERS_PREFERENCES, String.class, servers ) );
            }
            catch( Exception e )
            {
            }
        }
    }

    /**
     * SQL connection properties element
     */
    public static class SQLInfo
    {
        protected String type;
        protected String host;
        protected String port;
        protected String database;
        protected String username;
        protected String password;

        public SQLInfo(String type, String host, String port, String database, String username, String password)
        {
            this.type = type;
            this.host = host;
            this.port = port;
            this.database = database;
            this.username = username;
            this.password = password;
        }

        public SQLInfo(String str)
        {
            String[] elements = str.split(":", 6);
            if( elements.length == 6 )
            {
                this.type = elements[0];
                this.host = elements[1];
                this.port = elements[2];
                this.database = elements[3];
                this.username = elements[4];
                this.password = elements[5];
            }
        }

        public String getType()
        {
            return type;
        }

        public String getHost()
        {
            return host;
        }

        public String getPort()
        {
            return port;
        }

        public String getDatabase()
        {
            return database;
        }

        public String getUsername()
        {
            return username;
        }

        public String getPassword()
        {
            return password;
        }

        public String serialize()
        {
            return type + ":" + host + ":" + port + ":" + database + ":" + username + ":" + password;
        }

        public String getJdbcUrl()
        {
            return "jdbc:" + type + "://" + host + ":" + port + "/" + database;
        }

        @Override
        public String toString()
        {
            return username + "@" + host + ":" + port + "/" + database;
        }
    }

    /**
     * BeanInfo for {@link SQLInfo}
     */
    public static class SQLInfoBeanInfo extends BeanInfoEx
    {
        public SQLInfoBeanInfo()
        {
            super(SQLInfo.class, MessageBundle.class.getName());
            beanDescriptor.setDisplayName(getResourceString("CN_SQLINFO_DESCRIPTOR"));
            beanDescriptor.setShortDescription(getResourceString("CD_SQLINFO_DESCRIPTOR"));
        }

        @Override
        public void initProperties() throws Exception
        {
            PropertyDescriptorEx pde;

            pde = new PropertyDescriptorEx("type", beanClass, "getType", null);
            add(pde, getResourceString("PN_SQLINFO_TYPE"), getResourceString("PD_SQLINFO_TYPE"));

            pde = new PropertyDescriptorEx("host", beanClass, "getHost", null);
            add(pde, getResourceString("PN_SQLINFO_HOST"), getResourceString("PD_SQLINFO_HOST"));

            pde = new PropertyDescriptorEx("port", beanClass, "getPort", null);
            add(pde, getResourceString("PN_SQLINFO_PORT"), getResourceString("PD_SQLINFO_PORT"));

            pde = new PropertyDescriptorEx("database", beanClass, "getDatabase", null);
            add(pde, getResourceString("PN_SQLINFO_DATABASE"), getResourceString("PD_SQLINFO_DATABASE"));

            pde = new PropertyDescriptorEx("username", beanClass, "getUsername", null);
            add(pde, getResourceString("PN_SQLINFO_USERNAME"), getResourceString("PD_SQLINFO_USERNAME"));
        }
    }
}
