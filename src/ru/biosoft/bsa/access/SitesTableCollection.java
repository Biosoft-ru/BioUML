package ru.biosoft.bsa.access;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.SortableDataCollection;
import ru.biosoft.access.core.TransformedDataCollection;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.Track;
import ru.biosoft.util.TransformedIterator;

/**
 * DataCollection which represents Site collection in nice tabular manner
 * @author lan
 */
public class SitesTableCollection extends TransformedDataCollection<Site, TransformedSite> implements SortableDataCollection<TransformedSite>
{
    private final Track track;

    private static Properties createProperties(DataCollection<Site> dc)
    {
        Properties properties = new Properties();
        properties.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, "Sites");
        properties.setProperty(DataCollectionConfigConstants.TRANSFORMER_CLASS, SitesToTableTransformer.class.getName());
        properties.put(DataCollectionConfigConstants.PRIMARY_COLLECTION, dc);
        return properties;
    }

    public SitesTableCollection(Track track, DataCollection<Site> dc) throws Exception
    {
        super(null, createProperties(dc));
        this.track = track;
    }

    @Override
    public boolean isSortingSupported()
    {
        return (primaryCollection instanceof SortableDataCollection) && ((SortableDataCollection<?>)primaryCollection).isSortingSupported();
    }

    @Override
    public String[] getSortableFields()
    {
        if(!isSortingSupported()) return null;
        return ((SortableDataCollection<?>)primaryCollection).getSortableFields();
    }

    @Override
    public List<String> getSortedNameList(String field, boolean direction)
    {
        if(!isSortingSupported()) return getNameList();
        return ((SortableDataCollection<?>)primaryCollection).getSortedNameList(field, direction);
    }

    @Override
    public @Nonnull List<String> getNameList()
    {
        return getPrimaryCollection().getNameList();
    }

    @Override
    public Iterator<TransformedSite> getSortedIterator(String field, boolean direction, int from, int to)
    {
        if( ! ( primaryCollection instanceof SortableDataCollection ) )
        {
            List<String> nameList = getNameList();
            return AbstractDataCollection.createDataCollectionIterator( this, nameList.subList( from, to ).iterator() );
        }
        return new TransformedIterator<Site, TransformedSite>(((SortableDataCollection<Site>)primaryCollection).getSortedIterator(field, direction, from, to))
        {
            @Override
            protected TransformedSite transform(Site value)
            {
                try
                {
                    return getTransformer().transformInput(value);
                }
                catch( Exception e )
                {
                    throw ExceptionRegistry.translateException(e);
                }
            }
        };
    }

    public Track getTrack()
    {
        return track;
    }
}
