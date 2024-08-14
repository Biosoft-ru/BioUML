package ru.biosoft.bsa.transformer;

import java.io.File;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.BTreeIndex;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionVetoException;
import ru.biosoft.access.core.Index;
import ru.biosoft.access.core.QuerySystem;

/**
 * SequenceQuerySystem is used to accelerate initialising of long sequences.
 * Now SequenceQuerySystem contains only one index SEQUENCE_INDEX.
 *
 * This index stores:
 * <ul>
 *   <li> sequence begining offset (line number from the entry begining).</li>
 *   <li> sequence length </li>
 * </ul>
 *
 * SequenceQuerySystem does not create index values  if the index absents or is not valid.
 * This is a responsibility of {@link SequenceTransformer} to add index value
 * for the requested sequence.
 * @see SequenceTransformer#transformInput
 * @see SequenceTransformer
 * @pending {@link DataCollectionEvent}s are not processed.
 * It is suggested that sequences can not be added or removed.
 */
public class SequenceQuerySystem implements QuerySystem
{
    protected static final Logger cat = Logger.getLogger(SequenceQuerySystem.class.getName());

    /** String constant to get sequence index */
    final public static String SEQUENCE_INDEX = "sequence";

    /** File extention for sequence index */
    final private static String INDEX_EXT = "seq.idx";

    ////////////////////////////////////////////////////////////////////////////
    // Constructors
    //

    public SequenceQuerySystem(DataCollection dc)
    {
        Properties properties = dc.getInfo().getProperties();
        String indexPath = properties.getProperty(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY) + "/" + dc.getName();

        try
        {
            index = new BTreeIndex(new File(indexPath), INDEX_EXT, indexPath, 1024);
            if(!index.isValid())
                index.clear();
        }
        catch(Throwable t)
        {
            cat.log(Level.SEVERE, "Can not initialize SequenceQuerySystem for " + dc.getCompletePath(), t);

            try
            {
                index.close();
                File indexFile = new File(indexPath + "." + INDEX_EXT);
                indexFile.delete();
            }
            catch(Throwable t2)
            {
                cat.log(Level.SEVERE, t2.getMessage(), t2);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // QuerySystem interface implementation
    //

    private Index index = null;

    @Override
    public Index getIndex(String name)
    {
        if(name.equals(SEQUENCE_INDEX))
            return index;
        return null;
    }

    @Override
    public Index[] getIndexes()
    {
        Index[] indexes = {index};
        return indexes;
    }

    /** Close sequence index. */
    @Override
    public void close()
    {
        try
        {
            if(index != null)
                index.close();
            index = null;
        }
        catch(Throwable t)
        {
            cat.log(Level.SEVERE, t.getMessage(), t);
        }
    }

    @Override
    protected void finalize() throws Throwable
    {
        close();
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
    public void elementWillChange(DataCollectionEvent e)
        throws DataCollectionVetoException, Exception
        {
    }

    @Override
    public void elementRemoved(DataCollectionEvent e) throws Exception
    {
    }

    @Override
    public void elementWillRemove(DataCollectionEvent e)throws DataCollectionVetoException, Exception
    {
    }
}




