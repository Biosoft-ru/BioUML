package ru.biosoft.access.users;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import java.util.logging.Logger;
import org.jivesoftware.smack.util.StringUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.sql.Query;
import ru.biosoft.access.sql.SqlConnectionPool;
import ru.biosoft.access.sql.SqlUtil;

/**
 * {@link JabberProvider} implementation based on direct SQL access
 */
public class SQLJabberProvider extends JabberProvider
{
    private static final Query USER_QUERY = new Query("SELECT username FROM ofUser WHERE username=$user$");
    private static final Query GROUP_USERS_QUERY = new Query("SELECT gu.username FROM ofGroupUser gu WHERE gu.groupName=$group$");
    private static final Query OFFLINE_DATE_QUERY = new Query("SELECT p.offlineDate FROM ofUser u LEFT JOIN ofPresence p ON u.username=p.username WHERE u.username=$user$");
    
    public static final String JABBER_JDBC_DRIVER_PROPERTY = "jabberJdbcDriverClass";
    public static final String JABBER_JDBC_URL_PROPERTY = "jabberJdbcURL";
    public static final String JABBER_JDBC_USER_PROPERTY = "jabberJdbcUser";
    public static final String JABBER_JDBC_PASSWORD_PROPERTY = "jabberJdbcPassword";

    protected static final Logger log = Logger.getLogger(SQLJabberProvider.class.getName());

    protected Properties properties;

    public SQLJabberProvider(Properties properties)
    {
        this.properties = properties;
        try
        {
            // Load the SQL JDBC driver
            String driver = properties.getProperty(JABBER_JDBC_DRIVER_PROPERTY);
            if( driver != null )
            {
                Class.forName(driver);
                log.log(Level.FINE, "Used driver is " + driver);
            }
        }
        catch( ClassNotFoundException e )
        {
            log.log(Level.SEVERE, "Cannot load JDBC driver");
        }
    }

    @Override
    public List<UserInfo> getSupportUsers(DataCollection origin)
    {
        List<UserInfo> result = new ArrayList<>();
        try
        {
            Connection conn = getConnection();
            if( conn != null )
            {
                for(String userName : SqlUtil.queryStrings( conn, GROUP_USERS_QUERY.str( SUPPORT_GROUP_NAME ) ))
                {
                    result.add(new UserInfo(origin, userName));
                }
            }
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Cannot load user info", t);
        }
        return result;
    }

    @Override
    public boolean isUserOnline(String userName)
    {
        boolean result = false;
        Connection conn = null;
        try
        {
            /* Check if offline record exists in 'ofpresence' table*/
            conn = getConnection();
            if( conn != null )
            {
                if(SqlUtil.queryString( conn, OFFLINE_DATE_QUERY.str( StringUtils.escapeNode(userName) ) ) == null)
                {
                    result = true;
                }
            }
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Cannot check user status", t);
        }
        return result;
    }

    @Override
    public void createUser(String username, String password)
    {
        Connection conn = null;
        String jidUsername = StringUtils.escapeNode(username);
        try
        {
            conn = getConnection();
            if( conn != null )
            {
                if(SqlUtil.hasResult( conn, USER_QUERY.str( jidUsername ) ))
                {
                    String now = String.valueOf(System.currentTimeMillis());
                    while( now.length() < 15 )
                    {
                        now = '0' + now;
                    }

                    String sql = "INSERT INTO ofUser (username,plainPassword,creationDate,modificationDate) VALUES (?,?,?,?)";
                    try(PreparedStatement pstmt = conn.prepareStatement(sql))
                    {
                        pstmt.setString(1, jidUsername);
                        pstmt.setString(2, password);
                        pstmt.setString(3, now);
                        pstmt.setString(4, now);
                        pstmt.executeUpdate();
                    }

                    sql = "INSERT INTO ofPresence (username,offlineDate) VALUES (?,?)";
                    try(PreparedStatement pstmt = conn.prepareStatement(sql))
                    {
                        pstmt.setString(1, jidUsername);
                        pstmt.setString(2, now);
                        pstmt.executeUpdate();
                    }

                    //add user to special BioUML users group
                    sql = "INSERT INTO ofGroupUser (groupName, username,administrator) VALUES (?,?,0)";
                    try(PreparedStatement pstmt = conn.prepareStatement(sql))
                    {
                        pstmt.setString(1, USERS_GROUP_NAME);
                        pstmt.setString(2, jidUsername);
                        pstmt.executeUpdate();
                    }
                }
            }
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Cannot create jabber user", t);
        }
    }

    protected boolean connectionAvailable = true;
    protected Connection getConnection()
    {
        if( connectionAvailable )
        {
            try
            {
                return SqlConnectionPool.getPersistentConnection(properties.getProperty(JABBER_JDBC_URL_PROPERTY),
                        properties.getProperty(JABBER_JDBC_USER_PROPERTY), properties.getProperty(JABBER_JDBC_PASSWORD_PROPERTY));
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Jabber database is not available, check JDBC properties");
                connectionAvailable = false;
            }
        }
        return null;
    }
}
