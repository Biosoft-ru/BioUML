package ru.biosoft.access.security;

import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.users.UserInfo;
import ru.biosoft.util.Pair;

/**
 * @author tolstyh
 *
 * Duplicate of bioumlsupport functionality
 * 
 * TODO: this class should be replaced with {@link SecurityProvider} methods
 */
public class SecurityAdminUtils
{
    protected static final Logger log = Logger.getLogger(SecurityAdminUtils.class.getName());

    public static final String ENTITY_USERS = "users";
    public static final String ENTITY_GROUPS = "groups";
    public static final String ENTITY_USER_GROUPS = "user_groups";
    public static final String ENTITY_HISTORY = "history";

    /**
     * Register new user
     * @param user user name
     * @param pass user password
     * @param email user email address
     * @param trial 1 - trial user, 0 - full right user
     * @param expirationDate time limit for account, 0 - no limit
     * @param out error messages writer
     * @return true if completed successfully, false otherwise
     */
    public static boolean registerUser(String user, String pass, String email, int trial, long expirationDate, Writer out) throws Exception
    {
        Map<String, Object> params = new HashMap<>();
        params.put("trial", trial);
        params.put("expirationDate", expirationDate);
        if( SecurityManager.getSecurityProvider().register(user, pass, email, params) )
        {
            Connection connection = GlobalDatabaseManager.getDatabaseConnection();
            //add history
            String userInfo = getUserInfoAsString( user, pass, connection );
            addHistoryRecord(connection, user, "user_created", "BioUML: SecurityAdminUtils.registerUser", userInfo);
            return true;
        }
        out.write("E-mail address '" + email + "' is already registered.");
        return false;
    }

    /**
     * Update user registration properties
     * @param user user name
     * @param params additional parameters (firstName, lastName, ...)
     * @param trial 1 - trial user, 0 - full right user
     * @param expirationDate time limit for account, 0 - no limit
     * @param out error messages writer
     * @return true if completed successfully, false otherwise
     */
    public static boolean updateRegistration(String user, String password, Map<String, String> params, int trial, long expirationDate,
            Writer out) throws Exception
    {
        try
        {
            Connection connection = GlobalDatabaseManager.getDatabaseConnection();
            String sql = "SELECT * FROM " + ENTITY_USERS + " WHERE user_name=? AND trial=0";
            try (PreparedStatement pstmt = connection.prepareStatement( sql ))
            {
                pstmt.setString( 1, user );
                try( ResultSet rs = pstmt.executeQuery() )
                {
                    if( rs.next() )
                    {
                        out.write( "User '" + user + "' already upgrated" );
                        return false;
                    }
                }
            }

            String firstName = params.get("firstName");
            if( ( firstName == null ) || firstName.trim().isEmpty() )
            {
                out.write("Incorrect first name");
                return false;
            }
            String lastName = params.get("lastName");
            if( ( lastName == null ) || lastName.trim().isEmpty() )
            {
                out.write("Incorrect last name");
                return false;
            }

            boolean infoUpdated = updateUserInfo( user, password, params, out );
            if( !infoUpdated )
                return false;

            sql = "UPDATE " + ENTITY_USERS + " SET trial=?,expirationDate=? WHERE user_name=?";
            try (PreparedStatement pstmt = connection.prepareStatement( sql ))
            {
                pstmt.setInt( 1, trial );
                pstmt.setLong( 2, expirationDate );
                pstmt.setString( 3, user );
                pstmt.executeUpdate();
            }

            //add history
            String userInfo = getUserInfoAsString( user, password, connection );
            addHistoryRecord(connection, user, "user_updated", "BioUML: SecurityAdminUtils.updateRegistration", userInfo);

            return true;
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Cannot update registration:", e);
            out.write("Registration error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Update user registration properties
     * @param user user name
     * @param params additional parameters (firstName, lastName, ...)
     * @param out error messages writer
     * @return true if completed successfully, false otherwise
     */
    public static boolean updateUserInfo(String user, String password, Map<String, String> params, Writer out) throws Exception
    {
        if( SecurityManager.getSecurityProvider().updateUserInfo( user, password, params ) )
        {
            Connection connection = GlobalDatabaseManager.getDatabaseConnection();
            //add history
            String userInfo = getUserInfoAsString( user, password, connection );
            addHistoryRecord(connection, user, "user_created", "BioUML: SecurityAdminUtils.registerUser", userInfo);
            return true;
        }
        out.write("Cannot update user information");
        return false;
    }


    /**
     * Change user password
     * @param user
     * @param oldPassword
     * @param newPassword
     * @return
     * @throws Exception
     */
    public static boolean changeUserPassword(String user, String oldPassword, String newPassword) throws Exception
    {
        if( SecurityManager.getSecurityProvider().changePassword(user, oldPassword, newPassword) )
        {
            Connection connection = GlobalDatabaseManager.getDatabaseConnection();
            //add to history
            addHistoryRecord(connection, user, "user_password_changed", "BioUML: SecurityAdminUtils.changeUserPassword", "");
            return true;
        }
        return false;
    }

    /**
     * Create new group with administrator user
     * @param user name of group administrator
     * @param groupName name of group
     * @param reuse reuse group if exists
     * @param out error messages writer
     * @return true if completed successfully, false otherwise
     */
    public static boolean createUserGroup(String user, String groupName, boolean reuse, Writer out) throws Exception
    {
        try
        {
            SecurityManager.getSecurityProvider().createGroup(user, groupName, reuse);
            return true;
        }
        catch(Exception e)
        {
            out.write(e.getMessage());
        }
        return false;
    }

    /**
     * Add collection permission for group
     * @param groupName name of group
     * @param collectionName full collection path
     * @param permission permission value
     * @param out error messages writer
     * @return true if completed successfully, false otherwise
     * @throws Exception
     */
    public static boolean addGroupPermission(String username, String groupName, DataElementPath collectionName, int permission, Writer out)
            throws Exception
    {
        if( SecurityManager.getSecurityProvider().setGroupPermission(username, groupName, collectionName.toString(), permission) )
        {
            return true;
        }
        out.write("Cannot set group permission: " + groupName);
        return false;
    }

    /**
     * Remove user with group
     * @param username
     * @return
     */
    public static boolean removeUser(String username, String password) throws Exception
    {
        boolean result = false;
        try
        {
            Connection connection = GlobalDatabaseManager.getDatabaseConnection();
            String userInfo = getUserInfoAsString( username, password, connection );

            if( SecurityManager.getSecurityProvider().deleteUser(username) )
            {
                //add history
                addHistoryRecord(connection, username, "user_removed", "BioUML: SecurityAdminUtils.removeUser", userInfo);

                result = true;
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Cannot remove user:", e);
        }

        return result;
    }

    /**
     * Get dictionary values from database
     * @param tableName name of database table
     * @param keyColumn name on column with keys
     * @param valueColumn name of column with values
     * @param out  error messages writer
     * @return list of (key,value) pairs
     * @throws Exception
     */
    public static List<Pair<String, String>> getDictionary(String tableName, String keyColumn, String valueColumn, Writer out)
            throws Exception
    {
        Connection connection = GlobalDatabaseManager.getDatabaseConnection();
        try (Statement st = connection.createStatement();
                ResultSet rs = st
                        .executeQuery( "SELECT " + keyColumn + "," + valueColumn + " FROM " + tableName + " ORDER BY " + valueColumn ))
        {
            List<Pair<String, String>> result = new ArrayList<>();
            while( rs.next() )
            {
                result.add(new Pair<>(rs.getString(1), rs.getString(2)));
            }
            return result;
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Cannot get dictionary:", e);
            out.write("Get dictionary error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Get list of all group names
     * @return {@link List} of group names
     * @throws Exception
     */
    public static List<String> getGroupNameList() throws Exception
    {
        return SecurityManager.getSecurityProvider().getGroups();
    }

    /**
     * Get user list for selected group name
     * @param groupName name of group
     * @return
     * @throws Exception
     */
    public static List<UserInfo> getGroupUsers(String groupName, DataCollection<?> origin) throws Exception
    {
        List<String> users = SecurityManager.getSecurityProvider().getGroupUsers(groupName);
        return StreamEx.of( users ).map( user -> new UserInfo( origin, user ) ).toList();
    }

    //
    // protected methods
    //

    /**
     * Add history record
     */
    protected static void addHistoryRecord(Connection connection, String user, String action, String source, String message)
    {
        try (PreparedStatement pstmt = connection
                .prepareStatement( "INSERT INTO " + ENTITY_HISTORY + "(created, action, user, source, message) values(?,?,?,?,?)" ))
        {
            pstmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            pstmt.setString(2, action);
            pstmt.setString(3, user);
            pstmt.setString(4, source);
            pstmt.setString(5, message);
            pstmt.executeUpdate();
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Cannot write history: " + e.getMessage());
        }
    }

    protected static String getUserInfoAsString(String user, String password, Connection connection) throws Exception
    {
        StringBuilder userInfo = new StringBuilder();
        try (PreparedStatement pstmt = connection
                .prepareStatement( "SELECT u.emailAddress,u.trial FROM " + ENTITY_USERS + " u WHERE u.user_name=?" ))
        {
            pstmt.setString(1, user);
            try (ResultSet rs = pstmt.executeQuery())
            {
                if( rs.next() )
                {
                    Map<String, Object> params = new HashMap<>();
                    params.put( "Email", rs.getString( 1 ) );
                    String trial = "false";
                    try
                    {
                        if( Integer.parseInt( rs.getString( 2 ) ) == 1 )
                            trial = "true";
                    }
                    catch( NumberFormatException e )
                    {
                    }
                    params.put( "Trial", trial );

                    Map<String, Object> userInfoMap = SecurityManager.getSecurityProvider().getUserInfo( user, password );
                    if( userInfoMap != null )
                    {
                        params.put( "Courtesy", userInfoMap.get( "courtesy" ) );
                        params.put( "First name", userInfoMap.get( "firstName" ) );
                        params.put( "Last name", userInfoMap.get( "lastName" ) );
                        params.put( "Organization", userInfoMap.get( "organization" ) );
                        params.put( "Organization type", userInfoMap.get( "organizationType" ) );
                        params.put( "Country", userInfoMap.get( "country" ) );
                        params.put( "City", userInfoMap.get( "city" ) );
                        params.put( "Address", userInfoMap.get( "address" ) );
                        params.put( "Zip", userInfoMap.get( "zip" ) );
                        long creation = (Long)userInfoMap.get( "creation" );
                        DateFormat dateFormat = new SimpleDateFormat( "yyyy.MM.dd HH:mm:ss" );
                        if( creation > 0 )
                        {
                            params.put( "Creation", dateFormat.format( new Date( creation ) ) );
                        }
                        long expiration = (Long)userInfoMap.get( "expiration" );
                        if( expiration > 0 )
                        {
                            params.put( "Expiration", dateFormat.format( new Date( expiration ) ) );
                        }
                    }

                    userInfo.append( "<p>User name: " );
                    userInfo.append( user );
                    if( !params.isEmpty() )
                    {
                        userInfo.append( "<ul>" );
                        for( Map.Entry<String, Object> entry : params.entrySet() )
                        {
                            Object value = entry.getValue();
                            if( value != null )
                            {
                                userInfo.append( "<li>" + entry.getKey() + ": " + value.toString() + "</li>" );
                            }
                        }
                        userInfo.append( "</ul>" );
                    }
                }
            }
        }
        catch( Exception e )
        {
            // TODO:
        }
        return userInfo.toString();
    }

    /**
     * Remove user group
     * if user is not administrator, just remove user from group
     * @param user name of group administrator
     * @param groupName name of group
     * @return true if completed successfully, false otherwise
     */
    public static boolean removeUserProject(String groupName, String user, String pass, String jwToken, Writer out) throws Exception
    {
        try
        {
            log.info( "Removing project '" + groupName + "'..." );
            return SecurityManager.getSecurityProvider().removeProject( groupName, user, pass, jwToken );
        }
        catch( Exception e )
        {
            log.log( Level.SEVERE, "Cannot remove project from Security Provider", e );
            out.write( e.getMessage() );
        }
        return false;
    }
}
