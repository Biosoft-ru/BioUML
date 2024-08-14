package ru.biosoft.workbench;

import java.awt.Frame;
import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;

import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import one.util.streamex.StreamEx;

import java.util.logging.Logger;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;

import ru.biosoft.access.BundleDelegatingClassLoader;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Preferences;
import com.developmentontheedge.beans.editors.StringTagEditorSupport;
import com.developmentontheedge.application.Application;

public class LookAndFeelManager
{
    public static final String PREFERENCES_LOOK_AND_FEEL = "Look and feel";
    public static final String PREFERENCES_LOOK_AND_FEEL_THEME = "Look and feel theme";

    private static final Logger log = Logger.getLogger(LookAndFeelManager.class.getName());
    protected static final MessageBundle messageBundle = (MessageBundle)ResourceBundle.getBundle("ru.biosoft.workbench.MessageBundle");
    private static boolean extensionsLoaded;
    private static boolean uiUpdateEnabled = true;

    /**
     * Map of installed Look and Feels.
     * key - LookAndFeel name, value - LookAndFeelManager.LookAndFeelInfo.
     */
    private static Map<String, LookAndFeelManager.LookAndFeelInfo> lookAndFeelsMap = new TreeMap<>();

    /**
     *
     */
    public static void loadLookAndFeel(String lookAndFeelClassName)
    {
        if( !extensionsLoaded )
            loadExtensions();

        Preferences preferences = Application.getPreferences();
        if( preferences == null )
        {
            if(lookAndFeelClassName != null)
            {
                try
                {
                    UIManager.setLookAndFeel(lookAndFeelClassName);
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE, "Could not set up LookAndFeel, class=" + lookAndFeelClassName, e);
                }
            }
            return;
        }

        // load Look & Feel from preferences
        String lookAndFeelName = preferences.getStringValue(PREFERENCES_LOOK_AND_FEEL, null);
        String themeName = preferences.getStringValue(PREFERENCES_LOOK_AND_FEEL_THEME, null);
        // warranty Look & Feel properties
        try
        {
            if( preferences.getProperty(PREFERENCES_LOOK_AND_FEEL) == null )
            {
                preferences.add(new DynamicProperty(PREFERENCES_LOOK_AND_FEEL, messageBundle
                        .getResourceString("PREFERENCES_LOOK_AND_FEEL_PN"),
                        messageBundle.getResourceString("PREFERENCES_LOOK_AND_FEEL_PD"), String.class, lookAndFeelName));
            }

            if( preferences.getProperty(PREFERENCES_LOOK_AND_FEEL_THEME) == null )
            {
                preferences.add(new DynamicProperty(PREFERENCES_LOOK_AND_FEEL_THEME, messageBundle
                        .getResourceString("PREFERENCES_LOOK_AND_FEEL_THEME_PN"), messageBundle
                        .getResourceString("PREFERENCES_LOOK_AND_FEEL_THEME_PD"), String.class, themeName));
            }
        }
        catch( Exception t )
        {
            log.log(Level.SEVERE, "Could not create Look & Feel preferences, error: " + t.toString(), t);
        }

        if( preferences.getPropertyDescriptor(PREFERENCES_LOOK_AND_FEEL).getPropertyEditorClass() == null )
            preferences.getPropertyDescriptor(PREFERENCES_LOOK_AND_FEEL).setPropertyEditorClass(LookAndFeelTagEditor.class);

        if( preferences.getPropertyDescriptor(PREFERENCES_LOOK_AND_FEEL_THEME).getPropertyEditorClass() == null )
            preferences.getPropertyDescriptor(PREFERENCES_LOOK_AND_FEEL_THEME).setPropertyEditorClass(ThemeTagEditor.class);

        // synchronize current look and feel and preferences values
        uiUpdateEnabled = false;
        if( lookAndFeelName == null || !setLookAndFeelByName(lookAndFeelName) )
        {
            if( lookAndFeelClassName != null )
            {
                try
                {
                    UIManager.setLookAndFeel(lookAndFeelClassName);
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE, "Could not set up LookAndFeel, class=" + lookAndFeelClassName, e);
                }
            }

            LookAndFeel current = UIManager.getLookAndFeel();
            if( current != null )
            {
                String className = current.getClass().getName();
                for(LookAndFeelInfo info : lookAndFeelsMap.values())
                {
                    if( className.equals(info.getClassName()) )
                    {
                        lookAndFeelName = info.getName();
                        if( preferences.getProperty(PREFERENCES_LOOK_AND_FEEL) != null )
                            preferences.setValue(PREFERENCES_LOOK_AND_FEEL, lookAndFeelName);
                        break;
                    }
                }
            }
        }

        // synchronize current theme and preferences values
        LookAndFeelInfo info = lookAndFeelsMap.get(lookAndFeelName);
        if( themeName == null || !setThemeByName(themeName) )
        {
            if( info.themesMap == null )
                preferences.setValue(PREFERENCES_LOOK_AND_FEEL_THEME, null);
            else
                synchronizeThemeName(info);
        }

        uiUpdateEnabled = true;
        updateAllUIs(info.lookAndFeelLoader);

        LookAndFeelListener listener = new LookAndFeelListener();
        preferences.addPropertyChangeListener(PREFERENCES_LOOK_AND_FEEL, listener);
        preferences.addPropertyChangeListener(PREFERENCES_LOOK_AND_FEEL_THEME, listener);
    }

    public static boolean setLookAndFeelByName(String name)
    {
        if( name == null )
        {
            log.log(Level.SEVERE, "Error: look and feel name is null.");
            return false;
        }

        // get look and feel from registry
        LookAndFeelInfo info = lookAndFeelsMap.get(name);
        if( info == null )
        {
            log.log(Level.SEVERE, "Can not find registered look and feel with name '" + name + "'.");
            return false;
        }

        // check whether it is already used
        LookAndFeel currentLookAndFeel = UIManager.getLookAndFeel();
        if( currentLookAndFeel != null && info.getClassName().equals(currentLookAndFeel.getClass().getName()) )
            return true;

        // try to set up look and feel
        uiUpdateEnabled = false;
        try
        {
            try
            {
                UIManager.setLookAndFeel(info.getClassName());
            }
            catch( ClassNotFoundException e )
            {
                Class<? extends LookAndFeel> c = info.lookAndFeelLoader.loadClass(info.getClassName()).asSubclass( LookAndFeel.class );
                UIManager.setLookAndFeel(c.newInstance());
            }

            Application.getPreferences().setValue(PREFERENCES_LOOK_AND_FEEL, name);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Could not set up LookAndFeel, name=" + info.getName() + ", class=" + info.getClassName(), e);
            return false;
        }

        // set up default theme
        if( info.themesMap != null && info.defaultTheme != null )
        {
            setThemeByName(info.defaultTheme);
        }
        else if( info.setDefaultThemeScript != null )
        {
            executeThemeScript(info.setDefaultThemeScript, null, null);
            try
            {
                UIManager.setLookAndFeel(UIManager.getLookAndFeel());
            }
            catch( Exception e )
            {
            }
        }

        synchronizeThemeName(info);

        uiUpdateEnabled = true;
        updateAllUIs(info.lookAndFeelLoader);

        return true;
    }

    protected static void synchronizeThemeName(LookAndFeelInfo info)
    {
        Preferences preferences = Application.getPreferences();
        if( preferences == null )
            return;

        if( info.themesMap == null )
        {
            preferences.setValue(PREFERENCES_LOOK_AND_FEEL_THEME, null);
            return;
        }

        Object theme = executeThemeScript(info.getThemeScript, null, null);
        if( theme instanceof Wrapper )
            theme = ( (Wrapper)theme ).unwrap();

        if( theme instanceof String )
            preferences.setValue(PREFERENCES_LOOK_AND_FEEL_THEME, theme);
        else if( theme != null )
        {
            String className = theme.getClass().getName();
            StreamEx.ofKeys( info.themesMap, className::equals ).findAny()
                    .ifPresent( name -> preferences.setValue( PREFERENCES_LOOK_AND_FEEL_THEME, name ) );
        }
    }

    public static boolean setThemeByName(String themeName)
    {
        if( themeName == null )
        {
            log.log(Level.SEVERE, "Error: theme name is null.");
            return false;
        }

        // get current look and feel info
        Preferences preferences = Application.getPreferences();
        String lookAndFeelName = preferences.getStringValue(PREFERENCES_LOOK_AND_FEEL, null);
        LookAndFeelInfo info = lookAndFeelsMap.get(lookAndFeelName);
        if( info == null )
        {
            log.log(Level.SEVERE, "Can not find look and feel with name '" + themeName + "'.");
            return false;
        }

        if( info.themesMap == null )
        {
            log.log(Level.SEVERE, "No themes are registred for current look and feel with name '" + info.getName() + "'.");
            return false;
        }

        if( !info.themesMap.containsKey(themeName) )
        {
            log.log(Level.SEVERE, "This theme is not for current look and feel, theme=" + themeName + ", look and feel=" + info.getName() + "'.");
            return false;
        }

        if( info.setThemeScript == null )
        {
            log.log(Level.SEVERE, "There is no JavaScript to set up theme, look and feel=" + info.getName() + "'.");
            return false;
        }

        // try to set up look and feel
        String className = info.themesMap.get(themeName);
        try
        {
            // check whether selected theme is already installed
            Object currentTheme = executeThemeScript(info.getThemeScript, null, null);
            if( currentTheme instanceof Wrapper )
                currentTheme = ( (Wrapper)currentTheme ).unwrap();

            if( currentTheme != null && ( themeName.equals(currentTheme) || className.equals(currentTheme.getClass().getName()) ) )
                return true;

            Class<?> c = info.lookAndFeelLoader.loadClass(className);
            executeThemeScript(info.setThemeScript, themeName, c.newInstance());
            preferences.setValue(PREFERENCES_LOOK_AND_FEEL_THEME, themeName);

            // reinstall look and feel
            UIManager.setLookAndFeel(UIManager.getLookAndFeel());

            updateAllUIs(info.lookAndFeelLoader);
            return true;
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Could not set up theme, name=" + themeName + ", class=" + className + ", look and feel=" + info.getName(), e);
        }

        return false;
    }

    protected static Object executeThemeScript(String script, String themeName, Object theme)
    {
        if( script == null )
        {
            log.log(Level.SEVERE, "JavaScript error: script is empty");
            return null;
        }
        Object result = null;
        try
        {
            Context cx = Context.enter();
            Scriptable scope = cx.initStandardObjects(null);

            scope.put("lookAndFeel", scope, Context.toObject(UIManager.getLookAndFeel(), scope));

            if( themeName != null )
                scope.put("themeName", scope, Context.toObject(themeName, scope));

            if( theme != null )
                scope.put("theme", scope, Context.toObject(theme, scope));

            result = cx.evaluateString(scope, script, "theme script", 1, null);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "JavaScript error: " + e + "\nScript: \n" + script);
        }
        finally
        {
            Context.exit();
        }

        return result;
    }

    public static void updateAllUIs(ClassLoader classLoader)
    {
        UIDefaults defaults = UIManager.getDefaults();
        defaults.put("ClassLoader", classLoader);

        if( uiUpdateEnabled )
        {
            Frame[] frames = Frame.getFrames();
            if( frames != null )
                for( Frame frame : frames )
                    updateWindowUI(frame);
        }
    }

    protected static void updateWindowUI(Window window)
    {
        try
        {
            SwingUtilities.updateComponentTreeUI(window);
        }
        catch( Exception e )
        {
        }

        Window[] windows = window.getOwnedWindows();
        if( windows != null )
            for( Window ownedWindow : windows )
                updateWindowUI(ownedWindow);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Utility classes
    //

    public static class LookAndFeelInfo extends UIManager.LookAndFeelInfo
    {
        public LookAndFeelInfo(String name, String className)
        {
            super(name, className);
        }

        public LookAndFeelInfo(UIManager.LookAndFeelInfo info)
        {
            super(info.getName(), info.getClassName());
        }

        protected ClassLoader lookAndFeelLoader;
        protected String defaultTheme;
        protected Map<String, String> themesMap;

        protected String initScript;
        protected String setDefaultThemeScript;
        protected String getThemeScript;
        protected String setThemeScript;
    }

    public static class LookAndFeelTagEditor extends StringTagEditorSupport
    {
        public LookAndFeelTagEditor()
        {
            super(null);

            values = lookAndFeelsMap.keySet().toArray( new String[lookAndFeelsMap.size()] );
        }
    }

    public static class ThemeTagEditor extends StringTagEditorSupport
    {
        public ThemeTagEditor()
        {
            super(null);

            Preferences preferences = Application.getPreferences();
            String name = preferences.getStringValue(PREFERENCES_LOOK_AND_FEEL, null);
            LookAndFeelInfo info = lookAndFeelsMap.get(name);

            if( info.themesMap == null )
                values = new String[] {};
            else
            {
                values = info.themesMap.keySet().toArray( new String[info.themesMap.size()] );
            }
        }
    }

    protected static class LookAndFeelListener implements PropertyChangeListener
    {
        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            if( PREFERENCES_LOOK_AND_FEEL.equals(evt.getPropertyName()) )
                LookAndFeelManager.setLookAndFeelByName(Application.getPreferences().getStringValue(PREFERENCES_LOOK_AND_FEEL, null));

            else if( PREFERENCES_LOOK_AND_FEEL_THEME.equals(evt.getPropertyName()) )
            {
                String theme = Application.getPreferences().getStringValue(PREFERENCES_LOOK_AND_FEEL_THEME, null);
                if( theme != null )
                    LookAndFeelManager.setThemeByName(theme);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    //

    public static final String LOOK_AND_FEEL_ELEMENT = "lookAndFeel";
    public static final String THEME_ELEMENT = "theme";
    public static final String SCRIPT_ELEMENT = "script";

    public static final String NAME_ATTR = "name";
    public static final String CLASS_ATTR = "class";
    public static final String DEFAULT_THEME_ATTR = "defaultTheme";
    public static final String THEME_ADAPTER_ATTR = "themeAdapter";

    /**
     * Load extensions both
     *
     */
    protected static void loadExtensions()
    {
        extensionsLoaded = true;

        // load look & feels installed by system
        UIManager.LookAndFeelInfo[] infos = UIManager.getInstalledLookAndFeels();
        for( javax.swing.UIManager.LookAndFeelInfo info : infos )
        {
            LookAndFeelInfo lfInfo = new LookAndFeelInfo(info);
            lfInfo.lookAndFeelLoader = Thread.currentThread().getContextClassLoader();
            lookAndFeelsMap.put(info.getName(), lfInfo);
        }


        // load look and feel extensions
        IExtensionRegistry registry = Application.getExtensionRegistry();
        IConfigurationElement[] extensions = registry.getConfigurationElementsFor("ru.biosoft.workbench.lookAndFeel");
        for( IConfigurationElement extension : extensions )
        {
            String pluginId = extension.getNamespaceIdentifier();
            try
            {
                String name = extension.getAttribute(NAME_ATTR);
                if( name == null )
                    throw new Exception("Look and Feel name is missing");

                ClassLoader cl = new BundleDelegatingClassLoader(Platform.getBundle(pluginId), null);
                Class<?> lookAndFeelClass = null;
                if( !lookAndFeelsMap.containsKey(name) )
                {
                    String className = extension.getAttribute(CLASS_ATTR);
                    if( className == null )
                        throw new Exception("Look and Feel class name is missing");

                    lookAndFeelClass = cl.loadClass(className);
                    if( !LookAndFeel.class.isAssignableFrom(lookAndFeelClass) )
                        throw new Exception("Look and Feel class should be derived from javax.swing.LookAndFeel class");

                    UIManager.installLookAndFeel(name, className);
                    LookAndFeelInfo lookAndFeelInfo = new LookAndFeelInfo(name, className);
                    lookAndFeelInfo.lookAndFeelLoader = cl;
                    lookAndFeelsMap.put(name, lookAndFeelInfo);

                    if( log.isLoggable( Level.FINE ) )
                        log.log(Level.FINE, "Install Look and Feel: " + name + "(" + className + ").");
                }
                else
                {
                    lookAndFeelsMap.get(name).lookAndFeelLoader = cl;
                }

                LookAndFeelInfo info = lookAndFeelsMap.get(name);

                String defaultTheme = extension.getAttribute(DEFAULT_THEME_ATTR);
                if( defaultTheme != null )
                    info.defaultTheme = defaultTheme;

                loadThemes(info, extension);
                loadScripts(info, extension);

                // init look and feel properties if necessary
                if( info.initScript != null )
                {
                    try
                    {
                        if( lookAndFeelClass == null )
                        {
                            lookAndFeelClass = cl.loadClass(info.getClassName());
                        }
                        Context cx = Context.enter();
                        Scriptable scope = cx.initStandardObjects(null);
                        scope.put("lookAndFeelClass", scope, Context.toObject(lookAndFeelClass, scope));
                        cx.evaluateString(scope, info.initScript, "init script", 1, null);
                    }
                    catch( Exception e )
                    {
                        log.log(Level.SEVERE, "JavaScript error: " + e + "\nScript: \n" + info.initScript);
                    }
                    finally
                    {
                        Context.exit();
                    }
                }
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "Can not load Look and Feel, extension=" + extension.getName() + ", error: " + t + ".");
            }
        }
    }

    protected static void loadThemes(LookAndFeelInfo info, IConfigurationElement extension)
    {
        IConfigurationElement[] themes = extension.getChildren(THEME_ELEMENT);
        for( IConfigurationElement theme : themes )
        {
            String pluginId = theme.getNamespaceIdentifier();

            String name = theme.getAttribute(NAME_ATTR);
            if( name == null )
            {
                log.log(Level.SEVERE, "Theme name is missing, extension=" + extension.getName());
                continue;
            }

            String className = theme.getAttribute(CLASS_ATTR);
            if( className == null )
            {
                log.log(Level.SEVERE, "Theme class is missing, extension=" + extension.getName());
                continue;
            }

            try
            {
                Platform.getBundle(pluginId).loadClass(className);
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Coul not load theme class " + className + ", extension=" + extension.getName(), e);
                continue;
            }

            if( info.themesMap == null )
                info.themesMap = new TreeMap<>();

            info.themesMap.put(name, className);
        }
    }

    protected static void loadScripts(LookAndFeelInfo info, IConfigurationElement extension)
    {
        IConfigurationElement[] scripts = extension.getChildren(SCRIPT_ELEMENT);

        for( IConfigurationElement script : scripts )
        {
            String name = script.getAttribute(NAME_ATTR);
            if( name == null )
            {
                log.log(Level.SEVERE, "Script name is missing, extension=" + extension.getName());
                continue;
            }

            if( "init".equals(name) )
                info.initScript = script.getValue();
            else if( "setDefaultTheme".equals(name) )
                info.setDefaultThemeScript = script.getValue();
            else if( "getTheme".equals(name) )
                info.getThemeScript = script.getValue();
            else if( "setTheme".equals(name) )
                info.setThemeScript = script.getValue();
            else
                log.log(Level.SEVERE, "Unknown script '" + name + "', extension=" + extension.getName());
        }
    }
}
