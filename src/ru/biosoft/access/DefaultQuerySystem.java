package ru.biosoft.access;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionInfo;
import ru.biosoft.access.core.DataCollectionVetoException;
import ru.biosoft.access.core.Index;
import ru.biosoft.access.core.QuerySystem;

/**
 * DefaultQuerySystem loads indexes based on information from {@link DataCollectionInfo}.
 *
 * Generally this information is stored in config file in following form:
 * <pre>
 * querySystem = ru.biosoft.access.DefaultQuerySystem
 * querySystem.indexes = title;other
 * index.title = biouml.standard.type.access.TitleSqlIndex
 * index.title.table = substances
 * index.other = com.xxx.Index
 * </pre>
 *
 * @pending indexes update when data collection is changed.
 */
public class DefaultQuerySystem implements QuerySystem
{
    protected static final Logger cat = Logger.getLogger(DefaultQuerySystem.class.getName());
    protected HashMap<String, Index> indexes = new HashMap<>();

    public DefaultQuerySystem(DataCollection dc)
    {
        Properties properties = dc.getInfo().getProperties();

        // load indexes from properties
        String str = properties.getProperty(INDEX_LIST);
        if( str != null )
        {
            StringTokenizer tokens = new StringTokenizer(str, ",;");
            while( tokens.hasMoreTokens() )
            {
                String name = tokens.nextToken().trim();
                try
                {
                    String indexClassName = properties.getProperty("index." + name);
                    if( indexClassName == null )
                    {
                        cat.log(Level.SEVERE, "Index class missing, index=" + name + ", dc=" + dc.getCompletePath());
                        continue;
                    }
                    
                    DataCollection<?> source = CollectionFactory.getDataCollection( indexClassName );
                    if(source != null)
                    {
                        // Inherit index from another collection
                        if(source.getInfo().getQuerySystem() == null)
                            throw new IllegalArgumentException("No query system for "+indexClassName);
                        Index index = source.getInfo().getQuerySystem().getIndex(name);
                        if(index == null)
                            throw new IllegalArgumentException("No index '"+name+"' for "+indexClassName);
                        indexes.put(name, index);
                    } else
                    {
                        String plugins = properties.getProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY, ClassLoading.getPluginForClass( indexClassName ));
                        Class<? extends Index> indexClass = ClassLoading.loadSubClass( indexClassName, plugins, Index.class );
                        Constructor<? extends Index> constructor = indexClass.getConstructor(DataCollection.class, String.class);
                        Index index = constructor.newInstance(dc, name);
                        indexes.put(name, index);
                    }
                }
                catch( Throwable t )
                {
                    cat.log(Level.SEVERE, "Can not load index '" + name + "', dc=" + dc.getCompletePath() + ",  error: " + t, t);
                }
            }
        }
    }

    @Override
    public Index[] getIndexes()
    {
        Index[] ind = new Index[indexes.size()];
        return indexes.values().toArray(ind);
    }

    @Override
    public Index getIndex(String name)
    {
        return indexes.get(name);
    }

    @Override
    protected void finalize() throws Throwable
    {
        close();
    }

    /**
     * Closes all indexes and releases any system resources associated with them stream.
     * A closed QuerySystem cannot be reopened.
     */
    @Override
    public void close()
    {
        for( Entry<String, Index> indexesEntry : indexes.entrySet() )
        {
            try
            {
                indexesEntry.getValue().close();
            }
            catch( Throwable t )
            {
                cat.log(Level.SEVERE, "Can not close index: " + indexesEntry.getKey() + ", error: " + t);
            }
        }
    }


    ////////////////////////////////////////////////////////////////////////////
    // Interface ru.biosoft.access.core.DataCollectionListener
    //

    @Override
    public void elementAdded(DataCollectionEvent e) throws Exception
    {
    }

    @Override
    public void elementWillAdd(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
    }

    @Override
    public void elementChanged(DataCollectionEvent e) throws Exception
    {
    }

    @Override
    public void elementWillChange(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
    }

    @Override
    public void elementRemoved(DataCollectionEvent e) throws Exception
    {
    }

    @Override
    public void elementWillRemove(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
    }
}
