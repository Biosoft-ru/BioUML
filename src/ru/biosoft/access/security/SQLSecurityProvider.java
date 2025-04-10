package ru.biosoft.access.security;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.sql.Query;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.util.TextUtil2;

/**
 * Implementation of {@link SecurityProvider} base on bioumlsupport2 SQL database
 */
public class SQLSecurityProvider implements SecurityProvider
{
    protected static final Logger log = Logger.getLogger(SQLSecurityProvider.class.getName());
    
    private static final String[] nonTrialProducts = new String[] {"Import", "Extended", "Experimental"};

    @Override
    public void init(Properties properties)
    {
    }

    @Override
    public UserPermissions authorize(String username, String password, String remoteAddress, String jwToken) throws SecurityException
    {
        if( username.isEmpty() )
        {
            return new UserPermissions( "", "" );
        } 
        UserPermissions result = null;
        long currentTime = System.currentTimeMillis();
        try
        {
            Connection connection = GlobalDatabaseManager.getDatabaseConnection();
            String sql = "SELECT expirationDate,trial FROM users WHERE user_name=? AND user_pass=? AND (expirationDate=0 OR expirationDate>?)";
            try( PreparedStatement pstmt = connection.prepareStatement( sql ) )
            {
                pstmt.setString( 1, username );
                pstmt.setString( 2, password );
                pstmt.setLong( 3, currentTime );
                try( ResultSet rs = pstmt.executeQuery() )
                {
                    if( rs.next() )
                        result = new UserPermissions( username, password, rs.getInt( 2 ) == 0 ? nonTrialProducts : null );
                }
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Connection error:", e);
            throw new SecurityException("Unable to connect to the security backend");
        }
        return result;
    }

    @Override
    public int getGuestPermissions(String dataCollectionName)
    {
        int result = Permission.INFO;
        try
        {
            Connection connection = GlobalDatabaseManager.getDatabaseConnection();
            String sql = "SELECT module,permission FROM permissions WHERE user=?";
            try( PreparedStatement pstmt = connection.prepareStatement( sql ) )
            {
                pstmt.setString( 1, "<guest>" );
                try( ResultSet rs = pstmt.executeQuery() )
                {
                    String currentCollection = "";
                    while( rs.next() )
                    {
                        String cName = rs.getString( 1 ).trim();
                        if( isPartOf( cName, dataCollectionName ) && cName.length() > currentCollection.length() )
                        {
                            currentCollection = cName;
                            result = rs.getInt( 2 );
                        }
                    }
                }
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "can not get guest permissions", e);
        }
        return result;
    }

    @Override
    public int getPermissions(String dataCollectionName, UserPermissions userInfo)
    {
        long currentTime = System.currentTimeMillis();
        int permissions = getGuestPermissions(dataCollectionName);

        try
        {
            String role = null;
            Connection connection = GlobalDatabaseManager.getDatabaseConnection();
            String sql = "SELECT ur.role_name FROM users u LEFT JOIN user_roles ur ON u.user_name=ur.user_name WHERE u.user_name=? AND u.user_pass=? AND (u.expirationDate=0 OR u.expirationDate>?)";
            try( PreparedStatement pstmt = connection.prepareStatement( sql ) )
            {
                pstmt.setString( 1, userInfo.getUser() );
                pstmt.setString( 2, userInfo.getPassword() );
                pstmt.setLong( 3, currentTime );
                try( ResultSet rs = pstmt.executeQuery() )
                {
                    if( rs.next() )
                        role = rs.getString( 1 );
                }
            }
            if( "Administrator".equals( role ) )
            {
                permissions = 0x17;
            }
            else
            {
                sql = "SELECT module,permission FROM permissions WHERE user=?";
                try( PreparedStatement pstmt = connection.prepareStatement( sql ) )
                {
                    pstmt.setString( 1, userInfo.getUser() );
                    try( ResultSet rs = pstmt.executeQuery() )
                    {
                        String currentCollection = "";
                        while( rs.next() )
                        {
                            String cName = rs.getString( 1 );
                            if( isPartOf( cName, dataCollectionName ) && cName.length() > currentCollection.length() )
                            {
                                currentCollection = cName;
                                permissions = rs.getInt( 2 );
                            }
                        }
                    }
                }
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "can not authorize user ", e);
        }

        return permissions;
    }

    @Override
    public boolean changePassword(String username, String oldPassword, String password)
    {
        try
        {
            Connection connection = GlobalDatabaseManager.getDatabaseConnection();
            SqlUtil.execute(connection, new Query("UPDATE users SET user_pass=$pass$ WHERE user_name=$user$").str("user", username).str("pass", password));
            return true;
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Connection error:", e);
        }
        return false;
    }

    @Override
    public boolean register(String username, String password, String email, Map<String, Object> props)
    {
        boolean result = false;
        try
        {
            Connection connection = GlobalDatabaseManager.getDatabaseConnection();
            if(SqlUtil.hasResult(connection, Query.byCondition("users", "user_name", email)))
                return false;

            //add 'users' record
            String sql = "INSERT INTO users(user_name, user_pass, emailAddress, trial, creationDate, expirationDate) VALUES(?,?,?,?,?,?)";
            try( PreparedStatement pstmt = connection.prepareStatement( sql ) )
            {
                pstmt.setString( 1, email );
                pstmt.setString( 2, password );
                pstmt.setString( 3, email );
                pstmt.setInt( 4, (Integer)props.get( "trial" ) );
                pstmt.setLong( 5, System.currentTimeMillis() );
                pstmt.setLong( 6, (Long)props.get( "expirationDate" ) );
                pstmt.executeUpdate();
            }

            //add 'user_roles' record
            sql = "INSERT INTO user_roles( user_name, role_name ) VALUES(?, 'User')";
            try( PreparedStatement pstmt = connection.prepareStatement( sql ) )
            {
                pstmt.setString( 1, email );
                pstmt.executeUpdate();
            }

            result = true;
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Cannot register new user:", e);
        }
        return result;
    }

    @Override
    public boolean deleteUser(String username)
    {
        boolean result = false;
        try
        {
            Connection connection = GlobalDatabaseManager.getDatabaseConnection();
            //remove group
            String groupID = null;
            try( PreparedStatement pstmt = connection.prepareStatement( "SELECT ID FROM groups WHERE name=?" ) )
            {
                pstmt.setString( 1, username );
                try( ResultSet rs = pstmt.executeQuery() )
                {
                    if( rs.next() )
                    {
                        groupID = rs.getString( 1 );
                    }
                }
            }
            if( groupID != null )
            {
                try( PreparedStatement pstmt = connection.prepareStatement( "DELETE FROM group_permissions WHERE groupID=?" ) )
                {
                    pstmt.setString( 1, groupID );
                    pstmt.executeUpdate();
                }

                try( PreparedStatement pstmt = connection.prepareStatement( "DELETE FROM user_groups WHERE groupID=?" ) )
                {
                    pstmt.setString( 1, groupID );
                    pstmt.executeUpdate();
                }

                try( PreparedStatement pstmt = connection.prepareStatement( "DELETE FROM groups WHERE ID=?" ) )
                {
                    pstmt.setString( 1, groupID );
                    pstmt.executeUpdate();
                }
            }

            //remove user
            try( PreparedStatement pstmt = connection.prepareStatement( "DELETE FROM users WHERE user_name=?" ) )
            {
                pstmt.setString( 1, username );
                pstmt.executeUpdate();
            }

            try( PreparedStatement pstmt = connection.prepareStatement( "DELETE FROM user_roles WHERE user_name=?" ) )
            {
                pstmt.setString( 1, username );
                pstmt.executeUpdate();
            }

            try( PreparedStatement pstmt = connection.prepareStatement( "DELETE FROM permissions WHERE user=?" ) )
            {
                pstmt.setString( 1, username );
                pstmt.executeUpdate();
            }

            result = true;
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Cannot remove user:", e);
        }
        return result;
    }

    @Override
    public Map<String, Object> getUserInfo(String username, String password)
    {
        Map<String, Object> result = new HashMap<>();
        try
        {
            Connection connection = GlobalDatabaseManager.getDatabaseConnection();
            String sql = "SELECT * FROM users u LEFT JOIN persons ui ON u.user_name=ui.user WHERE u.user_name=?";
            try( PreparedStatement pstmt = connection.prepareStatement( sql ) )
            {
                pstmt.setString( 1, username );
                try( ResultSet rs = pstmt.executeQuery() )
                {
                    if( rs.next() )
                    {
                        result.put( "expiration", rs.getLong( "u.expirationDate" ) );
                        result.put( "creation", rs.getLong( "u.creationDate" ) );
                        String email = rs.getString( "u.emailAddress" );
                        if( email != null )
                            result.put( "email", email );

                        String firstName = rs.getString( "ui.firstName" );
                        if( firstName != null )
                            result.put( "firstName", firstName );
                        String lastName = rs.getString( "ui.lastName" );
                        if( lastName != null )
                            result.put( "lastName", lastName );
                        String organization = rs.getString( "ui.organization" );
                        if( organization != null )
                            result.put( "organization", organization );
                        String countryID = rs.getString( "ui.countryID" );
                        if( countryID != null )
                            result.put( "country", countryID );
                        String city = rs.getString( "ui.city" );
                        if( city != null )
                            result.put( "city", city );
                        String address = rs.getString( "ui.address" );
                        if( address != null )
                            result.put( "address", address );
                        String zip = rs.getString( "ui.zip" );
                        if( zip != null )
                            result.put( "zip", zip );
                        String courtesy = rs.getString( "ui.courtesy" );
                        if( courtesy != null )
                            result.put( "courtesy", courtesy );
                        String organizationType = rs.getString( "ui.organizationType" );
                        if( organizationType != null )
                            result.put( "organizationType", organizationType );
                    }
                }
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Cannot get user info:", e);
        }
        return result;
    }

    @Override
    public boolean updateUserInfo(String username, String password, Map<String, String> parameters)
    {
        boolean result = false;
        try
        {
            Connection connection = GlobalDatabaseManager.getDatabaseConnection();
            String firstName = TextUtil2.nullToEmpty( parameters.get("firstName") );
            String lastName = TextUtil2.nullToEmpty( parameters.get("lastName") );

            String sql = "SELECT ID FROM persons WHERE user=?";
            String personID = null;
            try( PreparedStatement pstmt = connection.prepareStatement( sql ) )
            {
                pstmt.setString( 1, username );
                try( ResultSet rs = pstmt.executeQuery() )
                {
                    if( rs.next() )
                        personID = rs.getString( 1 );
                }
            }
            if( personID == null )
            {
                sql = "INSERT INTO persons(user,firstName,lastName) VALUES(?,?,?)";
                try( PreparedStatement pstmt = connection.prepareStatement( sql ) )
                {
                    pstmt.setString( 1, username );
                    pstmt.setString( 2, firstName );
                    pstmt.setString( 3, lastName );
                    pstmt.executeUpdate();
                }
                try( Statement st = connection.createStatement(); ResultSet rs2 = st.executeQuery( "SELECT LAST_INSERT_ID() AS \"lid\"" ) )
                {
                    if( rs2.next() )
                        personID = rs2.getString( 1 );
                }
            }

            sql = "UPDATE persons SET courtesy=?,middleName=?,organization=?,organizationType=?,countryID=?,city=?,address=?,zip=? WHERE ID="
                    + personID;
            try( PreparedStatement pstmt = connection.prepareStatement( sql ) )
            {
                pstmt.setString( 1, parameters.get( "courtesy" ) == null ? null : parameters.get( "courtesy" ) );
                pstmt.setString( 2, parameters.get( "middleName" ) == null ? "" : parameters.get( "middleName" ) );
                pstmt.setString( 3, parameters.get( "organization" ) == null ? "" : parameters.get( "organization" ) );
                pstmt.setString( 4, parameters.get( "organizationType" ) == null ? "" : parameters.get( "organizationType" ) );
                pstmt.setString( 5, parameters.get( "countryID" ) == null ? "" : parameters.get( "countryID" ) );
                pstmt.setString( 6, parameters.get( "city" ) == null ? "" : parameters.get( "city" ) );
                pstmt.setString( 7, parameters.get( "address" ) == null ? "" : parameters.get( "address" ) );
                pstmt.setString( 8, parameters.get( "zip" ) == null ? "" : parameters.get( "zip" ) );
                pstmt.executeUpdate();
            }
            result = true;
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Cannot update user info:", e);
        }
        return result;
    }

    @Override
    public void createGroup(String username, String groupName, boolean reuse) throws Exception
    {
        try
        {
            Connection connection = GlobalDatabaseManager.getDatabaseConnection();
            long groupId = SqlUtil.queryLong(connection, new Query("SELECT ID FROM groups WHERE name=$group$").str(groupName), -1);
            if(groupId == -1)
            {
                try (PreparedStatement pstmt = connection
                        .prepareStatement( "INSERT INTO groups(name, language, description) VALUES(?,?,?)" ))
                {
                    pstmt.setString( 1, groupName );
                    pstmt.setString( 2, "en" );
                    pstmt.setString( 3, "Autogenerated group for user " + username );
                    pstmt.executeUpdate();
                }

                groupId = SqlUtil.queryLong(connection, "SELECT LAST_INSERT_ID()");
            }
            else if( reuse )
            {
                //refresh user role for group
                try (PreparedStatement pstmt = connection.prepareStatement( "DELETE FROM user_groups WHERE user=? AND groupID=?" ))
                {
                    pstmt.setString( 1, username );
                    pstmt.setLong( 2, groupId );
                    pstmt.executeUpdate();
                }
            }
            else
            {
                throw new Exception("Group already exists");
            }
            try (PreparedStatement pstmt = connection
                    .prepareStatement( "INSERT INTO user_groups(user, groupID, role) VALUES(?,?,'Administrator')" ))
            {
                pstmt.setString( 1, username );
                pstmt.setLong( 2, groupId );
                pstmt.executeUpdate();
            }
        }
        catch( Exception e )
        {
            throw new Exception("Cannot create user group: "+e.getMessage(), e);
        }
    }

    @Override
    public boolean setGroupPermission(String username, String groupName, String collectionName, int permission)
    {
        boolean result = false;
        try
        {
            Connection connection = GlobalDatabaseManager.getDatabaseConnection();
            String groupID = SqlUtil.queryString(connection, new Query("SELECT ID FROM groups WHERE name=$group$").str(groupName));
            if(groupID == null)
                return false;
            try (PreparedStatement pstmt = connection.prepareStatement( "DELETE FROM group_permissions WHERE groupID=? AND module=?" ))
            {
                pstmt.setString( 1, groupID );
                pstmt.setString( 2, collectionName );
                pstmt.executeUpdate();
            }

            try (PreparedStatement pstmt = connection
                    .prepareStatement( "INSERT INTO group_permissions(groupID, module, permission) VALUES(?,?,?)" ))
            {
                pstmt.setString( 1, groupID );
                pstmt.setString( 2, collectionName );
                pstmt.setInt( 3, permission );
                pstmt.executeUpdate();
            }

            List<String> userList = SqlUtil.queryStrings(connection, new Query("SELECT user FROM user_groups WHERE groupID=$group$").str(groupID));

            try (PreparedStatement ps0 = connection
                    .prepareStatement( "DELETE FROM permissions WHERE user=? AND module=? AND permission<=" + permission );
                    PreparedStatement ps = connection
                            .prepareStatement( "INSERT INTO permissions(user, module, permission) VALUES(?,?,?)" );)
            {
                ps0.setString( 2, collectionName );

                ps.setString( 2, collectionName );
                ps.setInt( 3, permission );
                for( String user : userList )
                {
                    ps0.setString( 1, user );
                    ps0.executeUpdate();
                    ps.setString( 1, user );
                    ps.executeUpdate();
                }
            }
            result = true;
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Cannot add group permission:", e);
        }
        return result;
    }

    @Override
    public boolean checkUse(String username, String serviceName)
    {
        try
        {
            Connection connection = GlobalDatabaseManager.getDatabaseConnection();
            if( serviceName == null )
            {
                long expirationTime = SqlUtil.queryLong(connection, new Query("SELECT expirationDate FROM users WHERE user_name=$user$").str(username), 0);
                if( expirationTime < System.currentTimeMillis() )
                    return false;
            }
            else if( serviceName.equalsIgnoreCase("import") || serviceName.equalsIgnoreCase("extended") )
            {
                if(SqlUtil.queryInt(connection, new Query("SELECT trial FROM users WHERE user_name=$user$").str(username), 0) == 1)
                    return false;
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Connection error:", e);
        }
        return true;
    }

    /**
     * Check if collection is the part or equals base collection by name
     */
    protected static boolean isPartOf(String baseCollection, String childCollection)
    {
        return DataElementPath.create(childCollection).isDescendantOf(DataElementPath.create(baseCollection));
    }

    @Override
    public String getRegistrationURL()
    {
        return "";
    }

    @Override
    public List<String> getGroups()
    {
        try
        {
            Connection connection = GlobalDatabaseManager.getDatabaseConnection();
            return SqlUtil.queryStrings(connection, "SELECT name FROM groups");
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Cannot get group list", e);
        }
        return Collections.emptyList();
    }

    @Override
    public List<String> getGroupUsers(String group)
    {
        List<String> result = new ArrayList<>();
        try
        {
            Connection connection = GlobalDatabaseManager.getDatabaseConnection();
            String sql = "SELECT u.user_name FROM users u LEFT JOIN user_groups ug ON u.user_name=ug.user LEFT JOIN groups g ON g.ID=ug.groupID WHERE g.name=?";
            try( PreparedStatement pstmt = connection.prepareStatement( sql ) )
            {
                pstmt.setString( 1, group );
                try( ResultSet rs = pstmt.executeQuery() )
                {
                    while( rs.next() )
                    {
                        String username = rs.getString( 1 );
                        result.add( username );
                    }
                }
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Cannot get group list", e);
        }
        return result;
    }

    @Override
    public boolean isOnAllowedPath(File file) throws Exception
    {
        return false;
    }

    @Override
    public boolean removeProject(String groupName, String username, String password, String jwToken)
    {
        boolean result = false;
        try
        {
            Connection connection = GlobalDatabaseManager.getDatabaseConnection();
            String groupID = SqlUtil.queryString( connection, new Query( "SELECT ID FROM groups WHERE name=$group$" ).str( groupName ) );
            if( groupID == null )
                return false;

            List<String> userRoles = SqlUtil.queryStrings( connection,
                    new Query( "SELECT role FROM user_groups WHERE user=$user$ AND groupID=$groupID$" ).str( username ).str( groupID ) );
            if( !userRoles.contains( "Administrator" ) )
            {
                //user is not admin, just remove him/her from group
                try (PreparedStatement pstmt = connection.prepareStatement( "DELETE FROM user_groups WHERE user=? AND groupID=?" ))
                {
                    pstmt.setString( 1, username );
                    pstmt.setString( 2, groupID );
                    pstmt.executeUpdate();
                }
            }
            else
            {
                //user is admin, remove group
                try (PreparedStatement pstmt = connection.prepareStatement( "DELETE FROM user_groups WHERE groupID=?" ))
                {
                    pstmt.setString( 1, groupID );
                    pstmt.executeUpdate();
                }
                try (PreparedStatement pstmt = connection.prepareStatement( "DELETE FROM group_permissions WHERE groupID=?" ))
                {
                    pstmt.setString( 1, groupID );
                    pstmt.executeUpdate();
                }
            }
            result = true;
        }
        catch( Exception e )
        {
            log.log( Level.SEVERE, "Cannot remove group:", e );
        }
        return result;
    }
}
