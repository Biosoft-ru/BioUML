package ru.biosoft.tasks.process;

import ru.biosoft.access.ClassLoading;
import com.developmentontheedge.beans.Preferences;
import com.developmentontheedge.application.Application;

/**
 * @author lan
 *
 */
public class LauncherFactory
{
    private static final String LAUNCHERS_PROPERTY = "Launchers";

    public static ProcessLauncher getDefaultLauncher()
    {
        return new LocalProcessLauncher();
    }

    /**
     * 
     * @param name
     * @return ProcessLauncher or null if no launcher with given name is configured
     * @throws Exception if launcher class is not found or cannot be instantiated
     */
    public static ProcessLauncher getLauncher(String name) throws Exception
    {
        Preferences config = getLauncherConfig( name );
        if(config == null)
            return null;
        String className = config.getStringValue("class", null);
        String plugins = config.getStringValue("plugins", "ru.biosoft.workbench");
        Class<? extends ProcessLauncher> clazz = ClassLoading.loadSubClass( className, plugins, ProcessLauncher.class );
        return clazz.getConstructor(Preferences.class).newInstance(config);
    }
    
    public static Preferences getLauncherConfig(String name)
    {
        Preferences launcherPreferences = Application.getPreferences().getPreferencesValue(LAUNCHERS_PROPERTY);
        if(launcherPreferences == null)
            return null;
        return launcherPreferences.getPreferencesValue(name);
    }
}
