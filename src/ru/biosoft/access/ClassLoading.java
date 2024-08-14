package ru.biosoft.access;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import com.developmentontheedge.application.Application;

import one.util.streamex.StreamEx;
import ru.biosoft.access.exception.Assert;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.LoggedClassCastException;
import ru.biosoft.exception.LoggedClassNotFoundException;
import ru.biosoft.util.TextUtil;

public class ClassLoading
{
    protected static final Logger log = Logger.getLogger(ClassLoading.class.getName());

    private static final Map<String, String[]> movedClasses = new HashMap<>();

    private static final Map<String, Bundle> packageToBundle = new ConcurrentHashMap<>();

    static {
        initBundles();
        initMovedClasses();
    }
    private static final ClassLoader classLoader = new ClassLoader(CollectionFactoryUtils.class.getClassLoader())
    {
        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException
        {
            try
            {
                return ClassLoading.loadClass( name );
            }
            catch( LoggedClassNotFoundException e )
            {
                throw new ClassNotFoundException(e.getProperty("class").toString());
            }
        }
    };

    private static void initBundles()
    {
        Bundle bundle = Platform.getBundle( "ru.biosoft.access" );
        if(bundle == null) // no bundles: likely launched without OSGi (as unit test?)
            return;
        Bundle[] bundles = bundle.getBundleContext().getBundles();
        String bundleInfo = StreamEx.of( bundles )
                .map( b -> "|- " + b.getSymbolicName() ).mapLast( name -> "\\" + name.substring( 1 ) )
                .joining( "\n", "Loaded bundles:\n", "" );
        log.config( bundleInfo );
        for(Bundle b : bundles)
        {
            URL resource = b.getResource( "META-INF/MANIFEST.MF" );
            if(resource == null)
                continue;
            try(InputStream is = resource.openStream())
            {
                Manifest m = new Manifest(is);
                String value = m.getMainAttributes().getValue( "Export-Package" );
                if(value != null)
                {
                    for(String packageName : OsgiManifestParser.getStrings( value ))
                    {
                        Bundle oldBundle = packageToBundle.put( packageName, b );
                        if(oldBundle != null && oldBundle != b && !packageName.startsWith( "org.eclipse.core" ))
                        {
                            log.warning( "Package "+packageName+" exported both from "+oldBundle.getSymbolicName()+" and "+b.getSymbolicName() );
                        }
                    }
                }
            }
            catch(Exception ex)
            {
                log.log(Level.SEVERE,  "Exception parsing manifest for bundle "+b.getSymbolicName(), ex );
            }
        }
        log.config( "Found "+packageToBundle.size()+" exported packages" );
    }

    private static void initMovedClasses()
    {
        IExtensionRegistry registry = Application.getExtensionRegistry();
        if( registry == null )
            return;
        IConfigurationElement[] extensions = registry.getConfigurationElementsFor("ru.biosoft.access.movedClass");
        if( extensions == null )
            return;
        for( IConfigurationElement extension : extensions )
        {
            String pluginId = extension.getNamespaceIdentifier();
            String name = extension.getAttribute("name").trim();
            String[] descriptor = new String[] {pluginId, name};
            for(String oldName: TextUtil.split(extension.getAttribute("oldNames"), ','))
                movedClasses.put(oldName.trim(), descriptor);
        }
    }
    @SuppressWarnings ( "unchecked" )
    static public @Nonnull <T> Class<? extends T> loadSubClass(@Nonnull String className, @Nonnull Class<T> superClass)
            throws LoggedClassNotFoundException, LoggedClassCastException
    {
        Class<?> clazz = loadClass(className);
        if (superClass.isAssignableFrom(clazz))
            return (Class<? extends T>) clazz;
        throw new LoggedClassCastException( className, superClass.getName() );
    }

    @SuppressWarnings ( "unchecked" )
    static public @Nonnull <T> Class<? extends T> loadSubClass(@Nonnull String className, String pluginNames, @Nonnull Class<T> superClass)
            throws LoggedClassNotFoundException, LoggedClassCastException
    {
        Class<?> clazz = loadClass(className, pluginNames);
        if (superClass.isAssignableFrom(clazz))
            return (Class<? extends T>) clazz;
        throw new LoggedClassCastException( className, superClass.getName() );
    }

    /**
     * Load {@link Class} with the specified name trying to guess class plugin automatically.
     */
    static public @Nonnull Class<?> loadClass(@Nonnull String className) throws LoggedClassNotFoundException
    {
        Assert.notNull("className", className);
        String[] newDescriptor = movedClasses.get(className);
        if(newDescriptor != null)
            return loadClass(newDescriptor[1], newDescriptor[0]);

        String plugin = getPluginForClass( className );
        if(plugin == null)
        {
            Class<?> result = tryLoad(className, null);
            if(result != null)
                return result;
            throw new LoggedClassNotFoundException( className, null );
        }
        try
        {
            Bundle bundle = Platform.getBundle(plugin);
            Class<?> clazz = bundle.loadClass(className);
            clazz.getConstructors();
            return clazz;
        }
        catch( Throwable t )
        {
            throw new LoggedClassNotFoundException( t, className, plugin );
        }
    }

    /**
     * Load {@link Class} with the specified name and necessary plugins.
     */
    static public @Nonnull Class<?> loadClass(String className, @CheckForNull String pluginNames) throws LoggedClassNotFoundException
    {
        String[] newDescriptor = movedClasses.get(className);
        if(newDescriptor != null)
        {
            className = newDescriptor[1];
            pluginNames = newDescriptor[0];
        }
        Class<?> c = tryLoad(className, null);
        if( c == null )
        {
            Set<String> plugins = getPluginList(pluginNames);
            for( String pluginId : plugins )
            {
                c = loadClassFromPlugin( className, pluginId );
                if(c != null)
                    break;
            }
            if(c == null)
            {
                String pluginForClass = getPluginForClass( className );
                if(pluginForClass != null)
                    c = loadClassFromPlugin( className, pluginForClass );
                if(c != null)
                    log.warning( "Class "+className+" was requested from plugins "+pluginNames+"; but actually it's located in "+pluginForClass );
            }
        }

        if( c == null )
            throw new LoggedClassNotFoundException(className, pluginNames);

        try
        {
            c.getConstructors(); // trigger class initialization
        }
        catch( Throwable t )
        {
            throw new LoggedClassNotFoundException(ExceptionRegistry.translateException(t), className, pluginNames);
        }

        return c;
    }

    private static Class<?> loadClassFromPlugin(String className, String pluginId)
    {
        try
        {
            Bundle bundle = Platform.getBundle(pluginId);
            if( bundle != null )
            {
                return bundle.loadClass(className);
            }
            else
            {
                if(!SecurityManager.isTestMode())
                    log.log(Level.SEVERE, "Plugin '" + pluginId + "' is necessary for class '" + className + "'");
            }
        }
        catch( ClassNotFoundException | NoClassDefFoundError e )
        {
            // just try another plugin
        }
        catch( Throwable t )
        {
            throw new LoggedClassNotFoundException(t, className, pluginId);
        }
        return null;
    }

    private static Class<?> tryLoad(String className, String pluginId)
    {
        if( pluginId == null )
        {
            try
            {
                return Class.forName( className );
            }
            catch( ClassNotFoundException e )
            {
                //log.log( Level.SEVERE, "Class '" + className + "' not found (no plugin)", e );
            }
        }
        else
        {
            Bundle bundle = Platform.getBundle( pluginId );
            if( bundle != null )
            {
                try
                {
                    packageToBundle.putIfAbsent( pluginId, bundle );
                    return bundle.loadClass( className );
                }
                catch( ClassNotFoundException e )
                {
                    log.log(Level.SEVERE, "Class '" + className + "' not found in " + pluginId + ", reason = " + e.getMessage(), e );
                }
            }
        }
        return null;
    }

    /**
     *  Get ClassLoader for class
     */
    static public ClassLoader getClassLoader(String className, String pluginNames)
    {
        Class<?> c = null;
        for( String pluginId : getPluginList(pluginNames) )
        {
            try
            {
                Bundle bundle = Platform.getBundle(pluginId);
                if( bundle != null )
                {
                    ClassLoader cl = new BundleDelegatingClassLoader(bundle, null);
                    c = cl.loadClass(className);
                    if( c != null )
                    {
                        return cl;
                    }
                }
            }
            catch( Throwable t )
            {
                //just try another plugin
            }
        }
        return null;
    }

    static public ClassLoader getClassLoader(Class<?> clazz)
    {
        String className = clazz.getName();
        Class<?> c = null;
        try
        {
            Bundle bundle = Platform.getBundle(getPluginForClass(className));
            if( bundle != null )
            {
                ClassLoader cl = new BundleDelegatingClassLoader(bundle, null);
                c = cl.loadClass(className);
                if( c != null )
                {
                    return cl;
                }
            }
        }
        catch( Throwable t )
        {
        }
        return getClassLoader();
    }

    /**
     * @return the classLoader suitable to load any class from any plugin (via loadClass(String clazz))
     */
    public static ClassLoader getClassLoader()
    {
        return classLoader;
    }
    /**
     * Get available plugin list
     */
    static private Set<String> getPluginList(String pluginNames)
    {
        Set<String> pluginList = new HashSet<>();
        pluginList.add("biouml.workbench");
        pluginList.add("ru.biosoft.access");
        if( pluginNames != null )
        {
            StreamEx.split(pluginNames, ';').map( String::trim ).filter( TextUtil::nonEmpty ).forEach( pluginList::add );
        }
        return pluginList;
    }

    /**
     * Get plugin ID for loaded class
     */
    public static String getPluginForClass(Class<?> clazz)
    {
        return getPluginForClass(clazz.getName());
    }

    /**
     * Get plugin ID for class by given class name
     */
    public static String getPluginForClass(String className)
    {
        // First try plugins which names are package name substring
        Assert.notNull("className", className);
        int pos = className.lastIndexOf( '.' );
        if(pos == -1)
        {
            // HACK, TODO: remove this
            // It was added because rhino tries to load all top packages
            Exception e = new Exception();
            for( StackTraceElement element : e.getStackTrace() )
            {
                if( ( "ru.biosoft.plugins.javascript.JScriptContext".equals( element.getClassName() )
                        && "evaluateString".equals( element.getMethodName() ) )
                        || ( "org.mozilla.javascript.NativeJavaTopPackage".equals( element.getClassName() )
                                && "init".equals( element.getMethodName() ) ) )
                    return null;
            }

            log.warning( "Tried to load class without package: "+className );
            return null;
        }
        String packageName = className.substring( 0, pos );
        Bundle b = packageToBundle.get( packageName );
        if(b != null)
            return b.getSymbolicName();
        if(SecurityManager.isTestMode()) // no bundles in tests
            return null;
        //TODO: think about better solution.
        // We do not log warning, since java.lang classes are loaded automatically
        if( !packageName.startsWith( "java.lang" ) )
        {   
            //log.log( Level.WARNING, "Unable to find cached bundle for class " + className + " using old path", new Exception() );
            log.log( Level.WARNING, "Unable to find cached bundle for class " + className + " using old path" );
        }
        String pluginId = className;
        int pluginNameEnd;
        while( ( pluginNameEnd = pluginId.lastIndexOf(".") ) != -1 )
        {
            pluginId = pluginId.substring(0, pluginNameEnd);
            if(tryLoad(className, pluginId) != null)
                return pluginId;
        }
        if(className.startsWith("ru.biosoft") && tryLoad(className, "ru.biosoft.workbench") != null)
        {
            return "ru.biosoft.workbench";
        }
        //Then try biouml.workbench plugin
        if(tryLoad(className, "biouml.workbench") != null)
        {
            return "biouml.workbench";
        }
        return null;
    }

    /**
     * Returns absolute resource location by class and location relative to class
     *
     */
    public static @Nonnull String getResourceLocation(Class<?> clazz, String resource)
    {
        String plugin = getPluginForClass(clazz);
        return (plugin == null ? "default" : plugin) + ":" + clazz.getPackage().getName().replace(".", "/") + "/" + resource;
    }

    /**
     * Returns URL pointing to the resource. Warning: this URL shouldn't be saved between launches, as it may contain temporary bundle ID
     */
    public static URL getResourceURL(Class<?> baseClass, String resource)
    {
        URL url = null;
        try
        {
            long id = Platform.getBundle(getPluginForClass(baseClass.getName())).getBundleId();
            url = new URL("bundleresource", String.valueOf(id), 0, resource.replaceFirst("\\/[^\\/]+$", "/"));
        }
        catch( Exception e )
        {
        }
        if( url == null )
            url = getClassLoader(baseClass).getResource(resource.replaceFirst("\\/[^\\/]+$", "/"));
        return url;
    }

    public static void addJavaLibraryPath(String path)
    {
        if( path == null || path.isEmpty() )
            return;
        try
        {
            // This enables the java.library.path to be modified at runtime
            // From a Sun engineer at http://forums.sun.com/thread.jspa?threadID=707176
            // 
            Field field = ClassLoader.class.getDeclaredField( "usr_paths" );
            field.setAccessible( true );
            String[] paths = (String[])field.get( null );
            for( int i = 0; i < paths.length; i++ )
            {
                if( path.equals( paths[i] ) )
                {
                    return;
                }
            }
            String[] tmp = new String[paths.length + 1];
            System.arraycopy( paths, 0, tmp, 0, paths.length );
            tmp[paths.length] = path;
            field.set( null, tmp );
            System.setProperty( "java.library.path", System.getProperty( "java.library.path" ) + File.pathSeparator + path );
        }
        catch( IllegalAccessException e )
        {
            log.log( Level.SEVERE, "Failed to get permissions to set library path" );
        }
        catch( NoSuchFieldException e )
        {
            log.log( Level.SEVERE, "Failed to get field handle to set library path" );
        }
    }
}