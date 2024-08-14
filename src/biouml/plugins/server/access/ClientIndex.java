package biouml.plugins.server.access;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.Index;
import ru.biosoft.access.core.Key;

/**
 * Client index
 */
public class ClientIndex implements Index
{

    Logger log = Logger.getLogger(ClientIndex.class.getName());

    protected String indexName = DEFAULT_INDEX_NAME;

    protected DataCollection dc;

    protected ClientQuerySystem query;

    protected Map data = new HashMap();

    private boolean isInit = false;
    protected void internalIndexInit() throws IOException, ClassNotFoundException
    {
        if( !isInit )
        {
            if( ! ( dc.getInfo().getQuerySystem() instanceof ClientQuerySystem ) )
                throw new IllegalArgumentException("ClientIndex can be applied only for ClientQuerySystem nad cannot be applied to type "
                        + dc.getInfo().getQuerySystem().getClass().getName());
            isInit = true;
            query = (ClientQuerySystem)dc.getInfo().getQuerySystem();
            Map index = query.indexGet(indexName);
            data.putAll(index);
        }
    }

    public ClientIndex(DataCollection dc, String name)
    {
        if( name != null )
            indexName = name;
        this.dc = dc;
    }

    @Override
    public String getName()
    {
        return indexName;
    }

    @Override
    public Iterator nodeIterator(Key key)
    {
        throw new java.lang.UnsupportedOperationException("Method nodeIterator() not yet implemented.");
    }

    @Override
    public void close() throws Exception
    {
        internalIndexInit();
        query.closeIndex(indexName);
    }

    @Override
    public boolean isValid()
    {
        try
        {
            internalIndexInit();
            return query.checkValidIndex(indexName);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Cannot check index valid: " + indexName, e);
        }
        return false;
    }

    @Override
    public File getIndexFile()
    {
        return null;
    }

    @Override
    public int size()
    {
        try
        {
            internalIndexInit();
            int size = data.size();
            if( size <= 0 )
                size = query.getIndexSize(indexName);
            return size;
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Cannot get index size: " + indexName, e);
        }
        return 0;
    }

    @Override
    public boolean isEmpty()
    {
        try
        {
            internalIndexInit();
            return size() == 0;
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Cannot test index empty: " + indexName, e);
        }
        return true;
    }

    @Override
    public boolean containsKey(Object arg0)
    {
        try
        {
            internalIndexInit();
            return data.containsKey(arg0);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Cannot test index contains key: " + indexName, e);
        }
        return false;
    }

    @Override
    public boolean containsValue(Object arg0)
    {
        try
        {
            internalIndexInit();
            return data.containsValue(arg0);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Cannot test index contains value: " + indexName, e);
        }
        return false;
    }

    @Override
    public Object get(Object arg0)
    {
        try
        {
            internalIndexInit();
            return data.get(arg0);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Cannot get value: " + indexName, e);
        }
        return null;
    }

    @Override
    public Object put(Object arg0, Object arg1)
    {
        Object retVal = null;
        try
        {
            internalIndexInit();
            retVal = query.indexPut(indexName, arg0, arg1);
            data.put(arg0, arg1);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Cannot put to index: " + indexName, e);
        }
        return retVal;
    }

    @Override
    public Object remove(Object arg0)
    {
        Object retVal = null;
        try
        {
            internalIndexInit();
            retVal = query.indexRemove(indexName, arg0);
            data.remove(arg0);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Cannot remove from index: " + indexName, e);
        }
        return retVal;
    }

    @Override
    public void putAll(Map arg0)
    {
        try
        {
            internalIndexInit();
            query.indexPutAll(indexName, arg0);
            data.putAll(arg0);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Cannot put to index: " + indexName, e);
        }
    }

    @Override
    public void clear()
    {
        try
        {
            if( isInit )
            {
                query.indexClear(indexName);
            }
            data.clear();
        }
        catch( IOException e )
        {
            log.log(Level.SEVERE, "Cannot clear index: " + indexName, e);
        }
    }

    @Override
    public Set keySet()
    {
        try
        {
            internalIndexInit();
            return Collections.unmodifiableSet(data.keySet());
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Cannot get key set: " + indexName, e);
        }
        return new HashSet<>();
    }

    @Override
    public Collection values()
    {
        try
        {
            internalIndexInit();
            return Collections.unmodifiableCollection(data.values());
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Cannot get values: " + indexName, e);
        }
        return new ArrayList<>();
    }

    @Override
    public Set entrySet()
    {
        try
        {
            internalIndexInit();
            return Collections.unmodifiableSet(data.entrySet());
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Cannot get entry set: " + indexName, e);
        }
        return new HashSet();
    }

}
