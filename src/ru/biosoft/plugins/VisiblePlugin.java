package ru.biosoft.plugins;

import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

/**
 * Superclass for all 'visible' plugins.
 *
 * It defines {@link ru.biosoft.access.core.DataCollection} that will be shown in plugins tab of repository pane.
 * It is subclass responsibility what will be the data collection content and what actions
 * can be associated with it.
 *
 * See <code>ru.biosoft.plugins.visiblePlugins</code> extension point description for more details.
 */
public class VisiblePlugin<T extends DataElement> extends AbstractDataCollection<T>
{
    private static final String PROBLEMS_STARTUP  = "Problems starting plug-in ";
    private static final String PROBLEMS_SHUTDOWN = "Problems shutting down plug-in ";

    ////////////////////////////////////////////////////////////////////////////
    // Constructors
    //

    /**
     * Constructor to be used by {@link CollectionFactoryUtils} to create a Plugin.
     */
    public VisiblePlugin(DataCollection parent, Properties properties)
    {
        super(parent, properties);

        try
        {
            startup();
        }
        catch(Throwable t)
        {
            log.log(Level.SEVERE, PROBLEMS_STARTUP + getName() + ".", t);
            shutdown();
        }
    }

    @Override
    public void close() throws Exception
    {
        try
        {
            shutdown();
        }
        catch(Throwable t)
        {
            log.log(Level.SEVERE, PROBLEMS_SHUTDOWN + getName() + ".", t);
        }

        super.close();
    }

    /**
     * Starts up this plug-in.
     *
     * <p><b>Clients must never explicitly call this method.</b>
     */
    public void startup() throws Exception
    {}

    /**
     * Shuts down this plug-in and discards all plug-in state.
     *
     * <p><b>Clients must never explicitly call this method.</b>
     */
    public void shutdown()
    {}

    @Override
    @Nonnull
    public List<String> getNameList()
    {
        return Collections.<String>emptyList();
    }
}
