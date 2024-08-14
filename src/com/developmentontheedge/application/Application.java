package com.developmentontheedge.application;

import java.util.HashMap;
import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import com.developmentontheedge.beans.Preferences;
import com.developmentontheedge.application.action.ActionManager;

/**
 * ...
 *
 * Application has main ApplicationFrame, methods:
 * <ul>
 *   <li>{@link #getApplicationFrame()} - returns main application frame
 *   <li>{@link #registerApplicationFrame(ApplicationFrame)} - register application frame.
 * </ul>
 *
 * Additionally Application can have sevaral named frames,
 * <ul>
 *   <li>{@link #getApplicationFrame(String frameName)} - returns application frame with the specified name
 *   <li>{@link #registerApplicationFrame(String frmaeName, ApplicationFrame)} - register application frame with the sepcified name
 * </ul>
 *
 * <p>Some actions can be used by several frames. In this case it is needed to find active frame to which
 * the action should be applied. Method #getActiveApplicationFrame is used for this purpose.
 */
public class Application
{
    private static IExtensionRegistry registry = Platform.getExtensionRegistry();
    private static UIStrategy uiStrategy = new SwingUIStrategy();
    private static Preferences preferences;
    static public Preferences getPreferences()
    {
        return preferences;
    }
    static public void setPreferences(Preferences preferences)
    {
        SecurityManager sm = System.getSecurityManager();
        if( sm != null )
        {
            sm.checkPermission(new RuntimePermission("setupApplication"));
        }
        Application.preferences = preferences;
    }

    static public IExtensionRegistry getExtensionRegistry()
    {
        return registry;
    }

    static public void setExtensionRegistry(IExtensionRegistry registry)
    {
        SecurityManager sm = System.getSecurityManager();
        if( sm != null )
        {
            sm.checkPermission(new RuntimePermission("setupApplication"));
        }
        Application.registry = registry;
    }

    static public void setUIStrategy(UIStrategy uiStrategy)
    {
        SecurityManager sm = System.getSecurityManager();
        if( sm != null )
        {
            sm.checkPermission(new RuntimePermission("setupApplication"));
        }
        Application.uiStrategy = uiStrategy;
    }

    static public UIStrategy getUIStrategy()
    {
        return uiStrategy;
    }

    /** @return application constant by name.  */
    static public String getGlobalValue(String constantName)
    {
        return getGlobalValue(constantName, constantName);
    }

    /** @return application constant by name or defaultValue if property not found  */
    static public String getGlobalValue(String constantName, String defaultValue)
    {
    	Preferences preferences = getPreferences();
        if( preferences != null )
        {
            Preferences globalPreferences = (Preferences)preferences.getValue("Global");
            if( globalPreferences != null )
            {
                Object value = globalPreferences.getValue(constantName);
                if(value != null)
                {
                    return value.toString();
                }
            }
        }
        return defaultValue;
    }

    static private ApplicationFrame applicationFrame = null;

    /** @return main application frame.  */
    public static ApplicationFrame getApplicationFrame()
    {
        return getActiveApplicationFrame();
    }

    public static ApplicationFrame getMainApplicationFrame()
    {
        if( null == applicationFrame && !applicationFrameMap.isEmpty() )
        {
            return (ApplicationFrame)applicationFrameMap.values().toArray()[0];
        }
        return applicationFrame;
    }

    /** Registers main application frame.  */
    public static void registerApplicationFrame(ApplicationFrame frame)
    {
        SecurityManager sm = System.getSecurityManager();
        if( sm != null )
        {
            sm.checkPermission(new RuntimePermission("setupApplication"));
        }
        applicationFrame = frame;
    }

    static private Map<String, ApplicationFrame> applicationFrameMap = new HashMap<>();

    /** @return application frame with the specified name. */
    public static ApplicationFrame getApplicationFrame(String frameName)
    {
        if( null == frameName )
        {
            return getApplicationFrame();
        }
        return applicationFrameMap.get(frameName);
    }

    /** Registers application frame with the specified name. */
    public static void registerApplicationFrame(String name, ApplicationFrame frame)
    {
        SecurityManager sm = System.getSecurityManager();
        if( sm != null )
        {
            sm.checkPermission(new RuntimePermission("setupApplication"));
        }
        applicationFrameMap.put(name, frame);
    }

    /**
     * Returns currently active application frame.
     *
     * @pending this method may return null if currently active window is not application frame.
     */
    public static ApplicationFrame getActiveApplicationFrame()
    {
        for( ApplicationFrame frame : applicationFrameMap.values() )
        {
            if( frame.isActive() )
                return frame;
        }

        return getMainApplicationFrame();
    }

    static private ActionManager actionManager = null;
    static private Map<ApplicationFrame, ActionManager> actionManagersMap = new HashMap<>();
    /** @return instance of <code>ActionManager</code>.  */
    public static ActionManager getActionManager()
    {
        return getActiveActionManager();
    }
    public static ActionManager getActiveActionManager()
    {
        return getActionManager(getActiveApplicationFrame());
    }
    public static ActionManager getMainActionManager()
    {
        if( null == actionManager && !actionManagersMap.isEmpty() )
        {
            return (ActionManager)actionManagersMap.values().toArray()[0];
        }
        return actionManager;
    }
    public static void registerActionManager(ActionManager actionManager)
    {
        SecurityManager sm = System.getSecurityManager();
        if( sm != null )
        {
            sm.checkPermission(new RuntimePermission("setupApplication"));
        }
        Application.actionManager = actionManager;
    }
    public static ActionManager getActionManager(ApplicationFrame applicationFrame)
    {
        if( null == applicationFrame || Application.applicationFrame == applicationFrame )
        {
            return getMainActionManager();
        }
        return actionManagersMap.get(applicationFrame);
    }
    public static void registerActionManager(ActionManager actionManager, ApplicationFrame applicationFrame)
    {
        SecurityManager sm = System.getSecurityManager();
        if( sm != null )
        {
            sm.checkPermission(new RuntimePermission("setupApplication"));
        }
        Application.actionManagersMap.put(applicationFrame, actionManager);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Utility methods
    //

    static private Logger log = Logger.getLogger(Application.class.getName());
    public static void registerCategory(Logger c)
    {
        log = c;
    }

    static private ListResourceBundle resBundle;
    /**
     * @param name
     * @return ResourceBundle
     */
    static public ListResourceBundle registerMessageBundle(String name)
    {
        resBundle = (ListResourceBundle)ResourceBundle.getBundle(name, Locale.getDefault());
        return resBundle;
    }

    /**
     * Get an object from a ResourceBundle.
     *
     * @param key  an String key
     * @return Requested object or key if not found
     */
    public static Object getResource(String key)
    {
        try
        {
            return resBundle.getObject(key);
        }
        catch( Exception ex )
        {
            if( log != null )
                log.log(Level.SEVERE, "", ex);
        }
        return key;
    }

    /**
     * Get an String from a registered ResourceBundle.
     *
     * @param key  an key
     * @return Requested object or key if not found
     */
    public static String getString(String key)
    {
        try
        {
            return resBundle.getString(key);
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "", ex);
        }
        return key;
    }
}
