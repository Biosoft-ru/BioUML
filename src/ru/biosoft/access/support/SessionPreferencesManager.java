package ru.biosoft.access.support;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.developmentontheedge.beans.Preferences;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.security.GlobalDatabaseManager;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.security.SessionCache;
import ru.biosoft.access.security.SessionCacheManager;
import ru.biosoft.access.security.UserPermissions;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.util.TempFiles;

/**
 * User-specific preferences manager
 * 
 * Preferences are stores as xml dump created by DynamicPropertySetSerializer in user_preferences table
 * During session preferences are stored in session cache.
 * When preference is changed, user_preferences table is updated.
 * 
 * DROP TABLE IF EXISTS user_preferences;
 * CREATE TABLE user_preferences (user_name VARCHAR(50), preferences_value TEXT);

 * @author anna
 *
 */
public class SessionPreferencesManager
{
    public static final String USER_PREFERENCES = "beans/Preferences";
    protected static final Logger log = Logger.getLogger(SessionPreferencesManager.class.getName());

    public static Preferences getPreferences()
    {
        SessionCache sessionCache = null;
        sessionCache = SessionCacheManager.getSessionCache();
        if( sessionCache != null )
        {
            Object preferencesObj = sessionCache.getObject(USER_PREFERENCES);
            if( preferencesObj instanceof Preferences )
            {
                return (Preferences)preferencesObj;
            }
        }
        Preferences preferences = getCurrentSessionPreferences();
        preferences.addPropertyChangeListener(evt -> savePreferences());
        if( sessionCache != null )
            sessionCache.addObject(USER_PREFERENCES, preferences, true);
        return preferences;
    }

    public static void savePreferences()
    {
        SessionCache sessionCache = SessionCacheManager.getSessionCache();
        if( sessionCache != null )
        {
            Object preferencesObj = sessionCache.getObject(USER_PREFERENCES);
            if( preferencesObj instanceof Preferences )
            {
                saveCurrentSessionPreferences((Preferences)preferencesObj);
            }
        }
    }

    public static Preferences getCurrentSessionPreferences()
    {
        Preferences preferences = new Preferences();
        UserPermissions ps = SecurityManager.getCurrentUserPermission();
        String user = null;
        if( ps != null )
        {
            user = ps.getUser();
        }
        if( user == null || user.equals("") )
            return preferences;
        try
        {
            Connection connection = GlobalDatabaseManager.getDatabaseConnection();
            if( connection != null )
            {
                String propertiesStr = SqlUtil.queryString(connection, "SELECT preferences_value FROM user_preferences WHERE user_name="+SqlUtil.quoteString(user));
                if( propertiesStr != null && !propertiesStr.isEmpty() )
                {
                    ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();
                    Thread.currentThread().setContextClassLoader(ClassLoading.getClassLoader());

                    File tmpFile = TempFiles.file("activities.tmp", propertiesStr);

                    preferences.load(tmpFile.getAbsolutePath(), ClassLoading.getClassLoader());

                    tmpFile.delete();

                    Thread.currentThread().setContextClassLoader(oldContextClassLoader);
                }
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Connection error:", e);
        }
        return preferences;
    }

    public static void saveCurrentSessionPreferences(Preferences preferences)
    {
        UserPermissions ps = SecurityManager.getCurrentUserPermission();
        String user = null;
        if( ps != null )
        {
            user = ps.getUser();
        }
        if( user == null || user.equals("") )
            return;
        try
        {
            Connection connection = GlobalDatabaseManager.getDatabaseConnection();

            OutputStream output = new OutputStream()
            {
                private final StringBuilder string = new StringBuilder();
                @Override
                public void write(int b) throws IOException
                {
                    this.string.append( (char)b );
                }

                @Override
                public String toString()
                {
                    return this.string.toString();
                }

                @Override
                public void write(byte[] b) throws IOException
                {
                    String str = new String( b, "UTF-8" );
                    this.string.append( str );
                }
            };
            try
            {
                preferences.save( output );
            }
            catch( IOException e )
            {
                log.log(Level.SEVERE,  "can not write properties", e );
            }

            String sqlUpd = "UPDATE user_preferences SET preferences_value = ? WHERE user_name=?";
            int numRows = -1;
            try (PreparedStatement pstmt = connection.prepareStatement( sqlUpd ))
            {
                pstmt.setString( 2, user );
                pstmt.setString( 1, output.toString() );
                numRows = pstmt.executeUpdate();
            }

            if( numRows == 0 )
            {
                String sqlIns = "INSERT INTO user_preferences VALUES(?,?)";
                try (PreparedStatement pstmt = connection.prepareStatement( sqlIns ))
                {
                    pstmt.setString( 1, user );
                    pstmt.setString( 2, output.toString() );
                    pstmt.execute();
                }
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Connection error:", e);
        }
    }

    public static class SessionPreferencesChangeListener implements PropertyChangeListener
    {
        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            savePreferences();
        }
    }
}
