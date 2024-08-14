package ru.biosoft.plugins;

import java.util.Properties;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;

import com.developmentontheedge.application.Application;

import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPutException;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.util.LazyValue;

/**
 * This class is used to show 'visible' plugins in plugins tab of repository pane.
 *
 * <p>To be shown in this tab plugin.xml should provide visible plugin extension point(s).
 *
 * @see VisiblePlugin
 */
public class Plugins extends VectorDataCollection<DataCollection<?>>
{
    /** Name of root data collection for analyses. */
    public static final String ANALYSES = "analyses";

    /** Property name for base repository collection. */
    public static final String REPOSITORY_DC = "repository collection";

    private static Class<? extends Plugins> pluginsClass = Plugins.class;
    /**
     * Set another {@link Plugins} implementation. Call it before first getPlugins() call.
     * @param pluginSpecificClass subclass of {@link Plugins}
     */
    public static void setPluginClass(Class<? extends Plugins> pluginSpecificClass)
    {
        java.lang.SecurityManager sm = System.getSecurityManager();
        if( sm != null )
        {
            sm.checkPermission(new RuntimePermission("modifyRepository"));
        }
        pluginsClass = pluginSpecificClass;
    }

    private static final LazyValue<Plugins> plugins = new LazyValue<Plugins>("plugins")
    {
        @Override
        protected Plugins doGet() throws Exception
        {
            return (Plugins)SecurityManager.runPrivileged( () -> {
                Plugins plugins = pluginsClass.newInstance();
                plugins.init();
                CollectionFactory.registerRoot( plugins );
                return plugins;
            } );
        }
    };
    public static DataCollection getPlugins()
    {
        return plugins.get();
    }

    ////////////////////////////////////////////////////////////////////////////

    /**
     * Default constructor get all extensions using Eclipse platform API
     *
     * @pending whether we should activate plugin?
     */
    protected Plugins()
    {
        super(ANALYSES);
    }

    public void init()
    {
        try
        {
            IExtensionRegistry registry = Application.getExtensionRegistry();
            if(registry == null) return;
            IExtensionPoint point = registry.getExtensionPoint( "ru.biosoft.plugins.visiblePlugin" );
            IExtension[] extensions = point.getExtensions();

            for( IExtension extension : extensions )
            {
                IConfigurationElement element = extension.getConfigurationElements()[0];
                String[] attributes = element.getAttributeNames();
                Properties properties = new Properties();
                properties.setProperty( DataCollectionConfigConstants.PLUGINS_PROPERTY, extension.getNamespaceIdentifier() );
                for( String attribute : attributes )
                    properties.setProperty(attribute, element.getAttribute(attribute));

                try
                {
                    DataCollection<?> dc = createCollection(properties);
                    doPut(dc, true);
                }
                catch( Throwable t )
                {
                    log.log(Level.SEVERE, "Plugins loading error, extention=" + extension.getLabel(), t);
                }
            }
            
            //load folders from file repository
            DataCollection<DataCollection<?>> fileBasedFolders = CollectionFactory.getDataCollection(ANALYSES);
            if( fileBasedFolders != null )
            {
                getInfo().getProperties().put(REPOSITORY_DC, fileBasedFolders);
                fileBasedFolders.getInfo().getProperties().forEach( (key, value) -> {
                    if(value instanceof String && key instanceof String && getInfo().getProperty( (String)key ) == null)
                        getInfo().getProperties().put( key, value );
                });
                for( String name : fileBasedFolders.getNameList() )
                {
                    try
                    {
                        doPut(fileBasedFolders.get(name), true);
                    }
                    catch( Exception e )
                    {
                        log.log( Level.SEVERE, "Repository folder loading error, name=" + name, e );
                    }
                }
            }
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Plugins loading error: ", t);
        }
    }
    
    /**
     * This method can be redefined by child implementations and should be used for creating collections in {@link Plugins}
     */
    protected DataCollection createCollection(Properties properties)
    {
        return CollectionFactory.createCollection(this, properties);
    }

    @Override
    public @Nonnull Class<DataCollection> getDataElementType()
    {
        return ru.biosoft.access.core.DataCollection.class;
    }

    @Override
    public void close() throws Exception
    {
        for( String name: super.getNameList() )
            super.get(name).close();

        super.close();
    }
    
    private DataCollection getRepositoryDC()
    {
        Object object = getInfo().getProperties().get(REPOSITORY_DC);
        if(object instanceof DataCollection)
            return (DataCollection)object;
        return null;
    }

    @Override
    protected void doPut(DataCollection dataElement, boolean isNew)
    {
        super.doPut(dataElement, isNew);
        DataCollection dc = getRepositoryDC();
        try
        {
            if(dc != null) dc.put(dataElement);
        }
        catch( Exception e )
        {
            throw new DataElementPutException( e, getCompletePath().getChildPath( dataElement.getName() ) );
        }
    }

    @Override
    protected void doRemove(String name) throws Exception
    {
        super.doRemove(name);
        DataCollection dc = getRepositoryDC();
        if(dc != null) dc.remove(name);
    }
}
