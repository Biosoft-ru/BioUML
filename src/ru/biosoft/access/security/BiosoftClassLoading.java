package ru.biosoft.access.security;



import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.ClassLoading;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.PluginEntry;
import ru.biosoft.exception.LoggedClassCastException;
import ru.biosoft.exception.LoggedClassNotFoundException;
import ru.biosoft.util.ApplicationUtils;

/**
 * Temporary interlace between ru.biosoft.access.core.ClassLoading used in ru.biosot.access.core.Environment 
 * and existing ru.biosoft.access.ClassLoading
 * @author anna
 *
 */

//TODO: refactor code, remove duplications 
public class BiosoftClassLoading implements ClassLoading
{
    public BiosoftClassLoading()
    {

    }
    @Override
    public Class<?> loadClass(String className) throws LoggedClassNotFoundException
    {
        return ru.biosoft.access.ClassLoading.loadClass( className );
    }

    @Override
    public Class<?> loadClass(String className, String pluginNames) throws LoggedClassNotFoundException
    {
        return ru.biosoft.access.ClassLoading.loadClass( className, pluginNames );
    }

    @Override
    public <T> Class<? extends T> loadClass(String className, Class<T> superClass)
            throws LoggedClassNotFoundException, LoggedClassCastException
    {
        return ru.biosoft.access.ClassLoading.loadSubClass( className, superClass );
    }

    @Override
    public <T> Class<? extends T> loadClass(String className, String pluginNames, Class<T> superClass)
            throws LoggedClassNotFoundException, LoggedClassCastException
    {
        return ru.biosoft.access.ClassLoading.loadSubClass( className, pluginNames, superClass );
    }

    @Override
    public String getResourceLocation(Class<?> clazz, String resource)
    {
        return ru.biosoft.access.ClassLoading.getResourceLocation( clazz, resource );
    }

    @Override
    public String getClassTitle(Class<?> clazz)
    {
        if( DataElement.class.isAssignableFrom( clazz ) )
            return DataCollectionUtils.getClassTitle( (Class<? extends DataElement>)clazz );
        else
            return clazz.getSimpleName();
    }

    @Override
    public String getPluginForClass(Class<?> clazz)
    {
        return ru.biosoft.access.ClassLoading.getPluginForClass( clazz );
    }

    @Override
    public String getPluginForClass(String className)
    {
        return ru.biosoft.access.ClassLoading.getPluginForClass( className );
    }
    @Override
    public PluginEntry resolvePluginPath(String pluginPath, String parentPath)
    {
        return ApplicationUtils.resolvePluginPath( pluginPath, parentPath );
    }

}
