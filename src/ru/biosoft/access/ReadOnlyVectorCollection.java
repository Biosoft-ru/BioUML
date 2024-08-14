package ru.biosoft.access;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nonnull;

import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementDescriptor;
import ru.biosoft.access.core.DataElementReadException;
import ru.biosoft.access.core.RepositoryAccessDeniedException;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.access.security.SecurityManager;

/**
 * Class for read-only elements all of which are initialized in doInit method during the first request
 * @author lan
 */
public abstract class ReadOnlyVectorCollection<T extends DataElement> extends VectorDataCollection<T>
{
    private volatile boolean init = false;

    protected final void init()
    {
        if(!init)
        {
            synchronized(this)
            {
                if(!init)
                {
                    doInit();
                    init = true;
                }
            }
        }
    }
    
    protected void reset() {
        synchronized(this) {
            init = false;
            super.clear();
        }
    }

    /**
     * Actual elements creation occurs here. Use doPut() method to put elements
     */
    protected abstract void doInit();

    public ReadOnlyVectorCollection(DataCollection<?> parent, Properties properties)
    {
        super(parent, properties);
        setNotificationEnabled(false);
    }

    public ReadOnlyVectorCollection(String name, DataCollection<?> parent, Properties properties)
    {
        super(name, parent, properties);
        setNotificationEnabled(false);
    }

    public ReadOnlyVectorCollection(String name, Class<? extends T> elementClass, DataCollection<?> parent)
    {
        super(name, elementClass, parent);
        setNotificationEnabled(false);
    }

    @Override
    public int getSize()
    {
        try
        {
            init();
        }
        catch( Exception e )
        {
            throw new DataElementReadException(e, this, "size");
        }
        return super.getSize();
    }

    @Override
    public @Nonnull Iterator<T> iterator()
    {
        try
        {
            init();
        }
        catch( Exception e )
        {
            throw new DataElementReadException(e, this, "elements");
        }
        return super.iterator();
    }

    @Override
    public @Nonnull T[] toArray(T[] a)
    {
        init();
        return super.toArray(a);
    }

    @Override
    public @Nonnull List<String> getNameList()
    {
        try
        {
            init();
        }
        catch( Exception e )
        {
            throw new DataElementReadException(e, this, "names");
        }
        return super.getNameList();
    }

    @Override
    public StreamEx<T> stream()
    {
        try
        {
            init();
        }
        catch( Exception e )
        {
            throw new DataElementReadException(e, this, "elements");
        }
        return StreamEx.of( super.stream() );
    }

    @Override
    public boolean contains(String name)
    {
        init();
        return super.contains( name );
    }

    @Override
    protected T doGet(String name)
    {
        init();
        return super.doGet(name);
    }

    @Override
    protected void clear()
    {
        if(init)
        {
            throw new RepositoryAccessDeniedException( getCompletePath(), SecurityManager.getSessionUser(), "Write" );
        }
        super.clear();
    }

    @Override
    protected void doPut(T dataElement, boolean isNew) throws IllegalArgumentException, RepositoryAccessDeniedException
    {
        if(init)
        {
            throw new RepositoryAccessDeniedException( getCompletePath(), SecurityManager.getSessionUser(), "Write" );
        }
        super.doPut(dataElement, isNew);
    }

    @Override
    protected void doRemove(String name) throws Exception
    {
        if(init)
        {
            throw new RepositoryAccessDeniedException( getCompletePath(), SecurityManager.getSessionUser(), "Write" );
        }
        super.doRemove(name);
    }

    @Override
    public DataElementDescriptor getDescriptor(String name)
    {
        init();
        return super.getDescriptor(name);
    }

    @Override
    public boolean isMutable()
    {
        return !init;
    }
}
