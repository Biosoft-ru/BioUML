package ru.biosoft.access.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Iterator;

import org.apache.commons.codec.binary.Base64;

import ru.biosoft.access.core.DataElementPath;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Preferences;
import com.developmentontheedge.application.Application;

/**
 * Support SSO operations
 */
public class SingleSignOnSupport
{
    public static final String PREFERENCES_SSO = "Single Sign On";
    public static final String PREFERENCES_USE_SSO = "use";
    public static final String PREFERENCES_USER_NAME = "name";
    public static final String PREFERENCES_USER_PASSWORD = "password";

    /**
     * Check if SSO option should used
     */
    public static boolean isSSOUsed()
    {
        try
        {
            Preferences ssoPreferences = getSSOPreferences();
            Boolean value = (Boolean)ssoPreferences.getValue(PREFERENCES_USE_SSO);
            if( value == null )
            {
                value = false;
                ssoPreferences
                        .add(new DynamicProperty(PREFERENCES_USE_SSO, PREFERENCES_USE_SSO, "use Single Sign On", Boolean.class, value));
            }
            return value.booleanValue();
        }
        catch( Exception e )
        {
            return false;
        }
    }

    private static String activeUser = null;
    private static String activeUserPassword = null;

    /**
     * Returns active user name or null otherwise
     */
    public static String getActiveUser()
    {
        return activeUser;
    }

    /**
     * Login to BioUML
     */
    public static boolean login(String username, String password)
    {
        try
        {
            Preferences userPreferences = getSSOPreferences().getPreferencesValue(username);
            if( userPreferences != null )
            {
                String currentPassword = userPreferences.getStringValue(PREFERENCES_USER_PASSWORD, "");
                if( getMD5(password).equals(currentPassword) )
                {
                    SingleSignOnSupport.activeUser = username;
                    SingleSignOnSupport.activeUserPassword = password;
                    refreshHiddenProperties(username);
                    return true;
                }
            }
        }
        catch( Exception e )
        {
            //nothing to do, just return false
        }
        return false;
    }

    /**
     * Logout from BioUML
     */
    public static void logout()
    {
        SingleSignOnSupport.activeUser = null;
        SingleSignOnSupport.activeUserPassword = null;
        refreshHiddenProperties(null);
    }

    /**
     * Add new BioUML user
     */
    public static void addUser(String username, String password) throws Exception
    {
        Preferences ssoPreferences = getSSOPreferences();
        if( ssoPreferences.getPreferencesValue(username) != null )
        {
            throw new Exception("User with the same name is already exists");
        }

        Preferences userPreferences = new Preferences();
        DynamicProperty userPassword = new DynamicProperty(PREFERENCES_USER_PASSWORD, PREFERENCES_USER_PASSWORD, "user password",
                String.class, getMD5(password));
        userPassword.setHidden(true);
        userPreferences.add(userPassword);

        DynamicProperty userBlock = new DynamicProperty(username, username, username, Preferences.class, userPreferences);
        userBlock.setHidden(true);
        ssoPreferences.add(userBlock);
        ssoPreferences.firePropertyChange("*", null, null);
    }

    /**
     * Add or replace module properties for current user
     */
    public static void setModuleProperties(ModuleProperties moduleProperties)
    {
        try
        {
            if( activeUser != null )
            {
                Preferences userPreferences = getSSOPreferences().getPreferencesValue(activeUser);
                if( userPreferences != null )
                {
                    //remove old preferences for this module
                    userPreferences.remove(moduleProperties.getDcName());

                    Preferences modulePreferences = new Preferences();

                    DynamicProperty userProperty = new DynamicProperty(PREFERENCES_USER_NAME, PREFERENCES_USER_NAME, "user name",
                            String.class, moduleProperties.getUsername());
                    userProperty.setReadOnly(true);
                    modulePreferences.add(userProperty);

                    DynamicProperty passwordProperty = new DynamicProperty(PREFERENCES_USER_PASSWORD, PREFERENCES_USER_PASSWORD,
                            "user password", String.class, encryptString(moduleProperties.getPassword(), activeUserPassword));
                    passwordProperty.setHidden(true);
                    modulePreferences.add(passwordProperty);

                    String dcNameKey = moduleProperties.getDcName().replaceAll("/", "_");
                    userPreferences.add(new DynamicProperty(dcNameKey, moduleProperties.getDcName(), moduleProperties.getDcName(),
                            Preferences.class, modulePreferences));
                }
            }
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
    }

    /**
     * Get username and password values for active user and given data collection
     */
    public static ModuleProperties getModuleProperties(DataElementPath path)
    {
        String username = "";
        String password = "";
        String currentDCName = "";
        try
        {
            if( activeUser != null )
            {
                Preferences userPreferences = getSSOPreferences().getPreferencesValue(activeUser);
                if( userPreferences != null )
                {
                    Iterator<String> iter = userPreferences.nameIterator();
                    while( iter.hasNext() )
                    {
                        String propertyName = iter.next();

                        //skip password property
                        if( propertyName.equals(PREFERENCES_USER_PASSWORD) )
                            continue;

                        if( path.toString().replaceAll("/", "_").startsWith(propertyName) && propertyName.length() > currentDCName.length() )
                        {
                            currentDCName = propertyName;
                            Preferences p = userPreferences.getPreferencesValue(propertyName);
                            username = p.getStringValue(PREFERENCES_USER_NAME, "");
                            password = decryptString(p.getStringValue(PREFERENCES_USER_PASSWORD, ""), activeUserPassword);
                        }
                    }
                }
            }
        }
        catch( Exception e )
        {
            //nothing to do, just return empty username and password
        }
        return new ModuleProperties(path.toString(), username, password);
    }
    /**
     * Returns SSO preferences
     */
    protected static Preferences getSSOPreferences() throws Exception
    {
        Preferences preferences = Application.getPreferences();
        Preferences ssoPreferences = (Preferences)preferences.getValue(PREFERENCES_SSO);
        if( ssoPreferences == null )
        {
            ssoPreferences = new Preferences();
            preferences.add(new DynamicProperty(PREFERENCES_SSO, PREFERENCES_SSO, "Single Sign On preferences", Preferences.class,
                    ssoPreferences));
        }
        return ssoPreferences;
    }

    /**
     * Calculate MD5 for string
     */
    protected static String getMD5(String input)
    {
        try
        {
            StringBuffer code = new StringBuffer();
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte bytes[] = input.getBytes(StandardCharsets.UTF_8);
            byte digest[] = messageDigest.digest(bytes);
            for( byte b : digest )
            {
                code.append(Integer.toHexString(0x0100 + ( b & 0x00FF )).substring(1));
            }
            return code.toString();
        }
        catch( Exception e )
        {
            return input;
        }
    }

    /**
     * Encrypt input string with given key
     */
    protected static String encryptString(String input, String key)
    {
        byte inputBytes[] = input.getBytes(StandardCharsets.UTF_8);
        byte keyBytes[] = key.getBytes(StandardCharsets.UTF_8);
        byte outputBytes[] = new byte[inputBytes.length];
        for( int i = 0; i < inputBytes.length; i++ )
        {
            outputBytes[i] = (byte) ( inputBytes[i] ^ keyBytes[i % key.length()] );
        }
        return Base64.encodeBase64String(outputBytes);
    }

    /**
     * Decrypt input string with given key
     */
    protected static String decryptString(String input, String key)
    {
        byte inputBytes[] = Base64.decodeBase64(input);
        byte keyBytes[] = key.getBytes(StandardCharsets.UTF_8);
        byte outputBytes[] = new byte[inputBytes.length];
        for( int i = 0; i < inputBytes.length; i++ )
        {
            outputBytes[i] = (byte) ( inputBytes[i] ^ keyBytes[i % key.length()] );
        }
        return new String(outputBytes, StandardCharsets.UTF_8);
    }

    /**
     * Refresh hidden state for properties
     */
    protected static void refreshHiddenProperties(String activeUser)
    {
        try
        {
            Preferences ssoPreferences = getSSOPreferences();
            Iterator<String> iter = ssoPreferences.nameIterator();
            while( iter.hasNext() )
            {
                String propertyName = iter.next();
                if( propertyName.equals(PREFERENCES_USE_SSO) )
                    continue;

                if( activeUser != null && propertyName.equals(activeUser) )
                {
                    ssoPreferences.getProperty(propertyName).setHidden(false);
                }
                else
                {
                    ssoPreferences.getProperty(propertyName).setHidden(true);
                }
            }
        }
        catch( Exception e )
        {
            //nothing to do
        }
    }

    public static class ModuleProperties
    {
        protected String dcName;
        protected String username;
        protected String password;

        public ModuleProperties(String dcName, String username, String password)
        {
            this.dcName = dcName;
            this.username = username;
            this.password = password;
        }
        public String getDcName()
        {
            return dcName;
        }
        public String getPassword()
        {
            return password;
        }
        public String getUsername()
        {
            return username;
        }
    }
}
