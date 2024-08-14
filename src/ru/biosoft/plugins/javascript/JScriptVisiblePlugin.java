package ru.biosoft.plugins.javascript;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nonnull;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.plugins.VisiblePlugin;

/**
 * Utility class used as root tree for JavaScript functions and host objects.
 *
 * This class (CollectionFactory classloader) duplicates some functionality of
 * JScriptPlugin (Eclipse classloader). This is due to different classloaders
 * that are used to initialise them.
 */
public class JScriptVisiblePlugin extends VisiblePlugin<DataCollection>
{
    static JScriptVisiblePlugin instance;
    public static JScriptVisiblePlugin getInstance()
    {
        return instance;
    }
    
    /**
     * Constructor to be used by {@link CollectionFactoryUtils} to create a Plugin.
     */
    public JScriptVisiblePlugin(DataCollection parent, Properties properties)
    {
        super(parent, properties);
        instance = this;
    }

    @Override
    public void startup()
    {
        functions   = new VectorDataCollection("Functions",    this, null);
        hostObjects = new VectorDataCollection("Host objects", VectorDataCollection.class, this);
        JScriptContext.loadExtensions(this);
    }

    @Override
    public @Nonnull Class<? extends DataCollection> getDataElementType()
    {
        return ru.biosoft.access.core.DataCollection.class;
    }

    private DataCollection functions;
    public DataCollection getFunctions()
    {
        return functions;
    }

    private DataCollection hostObjects;
    public DataCollection getHostObjects()
    {
        return hostObjects;
    }

    @Override
    protected DataCollection doGet(String name) throws Exception
    {
        if(name.equals("Functions"))
            return getFunctions();
        if(name.equals("Host objects"))
            return getHostObjects();
        return null;
    }

    private List<String> nameList = Collections.unmodifiableList(Arrays.asList("Functions", "Host objects"));

    @Override
    public @Nonnull List<String> getNameList()
    {
        return nameList;
    }
}
