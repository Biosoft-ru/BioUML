package ru.biosoft.tasks;

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
import ru.biosoft.util.TransformedIterator;

/**
 * @author lan
 *
 */
public class TaskWrapperCollection extends TransformedDataCollection<TaskInfo, TaskInfoWrapper> implements SortableDataCollection<TaskInfoWrapper>
{
    private static Properties createProperties(DataCollection parent)
    {
        Properties properties = new Properties();
        properties.put(DataCollectionConfigConstants.NAME_PROPERTY, "");
        properties.put(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY, TaskInfoWrapper.class.getName());
        properties.put(DataCollectionConfigConstants.TRANSFORMER_CLASS, TaskWrapperTransformer.class.getName());
        properties.put(DataCollectionConfigConstants.PRIMARY_COLLECTION, parent);
        return properties;
    }

    public TaskWrapperCollection(DataCollection parent) throws Exception
    {
        super(parent, createProperties(parent));
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
        return new String[] {"type","source","startTime","endTime","status","logInfo"};
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
    public Iterator<TaskInfoWrapper> getSortedIterator(String field, boolean direction, int from, int to)
    {
        if(!(primaryCollection instanceof SortableDataCollection))
        {
            List<String> nameList = getNameList();
            return AbstractDataCollection.createDataCollectionIterator( this, nameList.subList( from, to ).iterator() );
        }
        return new TransformedIterator<TaskInfo, TaskInfoWrapper>(((SortableDataCollection<TaskInfo>)primaryCollection).getSortedIterator(field, direction, from, to))
        {
            @Override
            protected TaskInfoWrapper transform(TaskInfo value)
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
}
