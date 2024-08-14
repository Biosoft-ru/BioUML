package biouml.plugins.research;

import java.util.Iterator;

import javax.annotation.Nonnull;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.journal.Journal;
import ru.biosoft.tasks.TaskInfo;

/**
 * Research journal based on {@link ru.biosoft.access.core.DataCollection}
 */
public class DataCollectionJournal extends Journal
{
    protected static final Logger log = Logger.getLogger(DataCollectionJournal.class.getName());

    protected DataCollection collection;

    public DataCollectionJournal(DataCollection collection)
    {
        this.collection = collection;
    }

    @Override
    public void addAction(@Nonnull TaskInfo action)
    {
        try
        {
            collection.put(action);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not add element to journal");
        }
    }

    @Override
    public void removeAction(@Nonnull TaskInfo action)
    {
        try
        {
            collection.remove(action.getName());
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not remove element from journal");
        }
    }

    @Override
    public @Nonnull TaskInfo getEmptyAction()
    {
        TaskInfo result = new TaskInfo(collection, null, null, null, null);
        result.setJournal(this);
        return result;
    }

    @Override
    public @Nonnull Iterator<TaskInfo> iterator()
    {
        return collection.iterator();
    }

    @Override
    public DataElementPath getJournalPath()
    {
        return DataElementPath.create(collection);
    }
}
