package ru.biosoft.access.generic;

import java.io.File;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementDescriptor;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.Index;
import ru.biosoft.access.core.Key;
import ru.biosoft.util.TransformedIterator;

/**
 * @author lan
 * Index for GenericDataCollection
 */
public class GenericTitleIndex extends AbstractMap<String, String> implements Index<String>
{
    protected DataCollection<?> dc;
    protected String indexName;

    public GenericTitleIndex(DataCollection<?> dc, String indexName) throws Exception
    {
        this.dc = dc;
        this.indexName = indexName;
    }

    @Override
    public String getName()
    {
        return indexName;
    }

    @Override
    public void close() throws Exception
    {
    }

    @Override
    public boolean isValid()
    {
        return true;
    }

    @Override
    public File getIndexFile()
    {
        return null;
    }

    @Override
    public Iterator nodeIterator(Key key)
    {
        throw new java.lang.UnsupportedOperationException( "Method nodeIterator() not yet implemented." );
    }

    @Override
    public Set<Entry<String, String>> entrySet()
    {
        return new AbstractSet<Entry<String, String>>()
        {
            @Override
            public Iterator<Entry<String, String>> iterator()
            {
                return new TransformedIterator<String, Entry<String, String>>(dc.getNameList().iterator())
                {
                    @Override
                    protected Entry<String, String> transform(final String name)
                    {
                        return new SimpleImmutableEntry<>(name, get(name));
                    }
                };
            }

            @Override
            public int size()
            {
                return dc.getSize();
            }
        };
    }

    @Override
    public boolean containsKey(Object key)
    {
        return dc.contains(key.toString());
    }

    @Override
    public String get(Object key)
    {
        String displayName = null;
        if( dc instanceof FolderCollection )
        {
            try
            {
                DataElementDescriptor descriptor = dc.getDescriptor( key.toString() );
                if( descriptor != null )
                    displayName = descriptor.getValue( ru.biosoft.access.core.DataCollectionConfigConstants.DISPLAY_NAME_PROPERTY );
            }
            catch( Exception e1 )
            {
            }
        }
        else
        {
            try
            {
                DataElement de = dc.get( key.toString() );
                if( de instanceof DataCollection )
                {
                    displayName = ( (DataCollection)de ).getInfo().getDisplayName();
                }
                else
                {
                    displayName = (String)de.getClass().getMethod( "getDisplayName" ).invoke( de );
                }
            }
            catch( Exception e )
            {
            }
        }
        return displayName == null ? key.toString() : displayName;
    }
}
