package biouml.plugins.psimi.access;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionVetoException;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.Index;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.access.core.DataElementPutException;
import ru.biosoft.util.HashMapSoftValues;

public class IndexedDataCollection<T extends DataElement> extends VectorDataCollection<T>
{
    protected Index index = null;
    protected String currentFileName = null;
    protected Class<T> type = null;

    public IndexedDataCollection(String name, Class<T> type, DataCollection<?> parent, Properties properties, File indexFile)
    {
        super(name, parent, properties);
        createIndex(indexFile);
        if( v_cache == null )
        {
            v_cache = new HashMapSoftValues();
        }
        this.type = type;
    }

    public void setCurrentFileName(String fileName)
    {
        this.currentFileName = fileName;
    }

    @Override
    public boolean contains(String name)
    {
        return index.containsKey(name);
    }

    public boolean containsInCache(String name)
    {
        return v_cache.containsKey(name);
    }

    @Override
    public int getSize()
    {
        return index.size();
    }

    @Override
    public void close() throws Exception
    {
        super.close();

        if( index != null )
        {
            index.close();
            index = null;
        }
    }

    @Override
    public @Nonnull List<String> getNameList()
    {
        List<String> nameList = null;
        Set<String> keys = index.keySet();
        if( keys != null && keys.size() > 0 )
        {
            nameList = new ArrayList<>(keys);
        }
        else
        {
            nameList = new ArrayList<>();
        }
        return Collections.unmodifiableList(nameList);
    }

    @Override
    protected T doGet(String name)
    {
        T de = getFromCache(name);
        if( de == null )
        {
            String filename = (String)index.get(name);
            loadDataModule(filename);
            de = getFromCache(name);
        }
        return de;
    }

    private synchronized void loadDataModule(String filename)
    {
        try
        {
            ( (PsimiDataCollection)getOrigin().getOrigin() ).processFile(new File(filename));
        }
        catch( Throwable t )
        {
        }
    }

    @Override
    public @Nonnull Class<T> getDataElementType()
    {
        return type;
    }

    @Override
    public T put(T element)
    {
        T prev = null;
        String dataElementName = element.getName();
        if( checkMutable() )
        {
            if( log.isLoggable( Level.FINE ) )
                log.log(Level.FINE, "put <" + element.getName() + ">, start");

            try
            {
                if( !v_cache.containsKey(dataElementName) )
                {
                    doAddPreNotify(dataElementName, true);

                    doPut(element, true);
                    cachePut(element);

                    doAddPostNotify(dataElementName, true, null);
                }
            }
            catch( DataCollectionVetoException ex )
            {
                if( log.isLoggable( Level.FINE ) )
                    log.log(Level.FINE, "Veto exception for <" + dataElementName + ">, is caught.");
            }
            catch(Exception ex)
            {
                throw new DataElementPutException( ex, getCompletePath().getChildPath( element.getName() ) );
            }
        }
        return prev;
    }

    @Override
    protected void doPut(DataElement element, boolean isNew)
    {
        if( !index.containsKey(element.getName()) )
        {
            index.put(element.getName(), currentFileName);
        }
    }
    @Override
    protected void doRemove(String name)
    {
        release(name);
        index.remove(name);
    }

    private void createIndex(File indexFile)
    {
        final String indexName = "id";
        try
        {
            index = new FileIndex(indexFile, indexName);
        }
        catch( Throwable t )
        {
        }
    }

    public void saveIndexes() throws Exception
    {
        if( index != null )
        {
            index.close();
        }
    }

    public boolean hasIndex()
    {
        return index.getIndexFile().exists();
    }

    @Override
    protected void doAddPreNotify(String dataElementName, boolean bNew) throws Exception, DataCollectionVetoException
    {

    }
}