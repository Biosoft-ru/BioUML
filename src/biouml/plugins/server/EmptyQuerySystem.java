package biouml.plugins.server;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionVetoException;
import ru.biosoft.access.core.Index;
import ru.biosoft.access.core.QuerySystem;

/**
 * Utility class for ClientModule
 */
public class EmptyQuerySystem implements QuerySystem
{

    public EmptyQuerySystem ( DataCollection dc )
    {
    }
    
    @Override
    public void close ( )
    {
    }

    @Override
    public Index getIndex ( String name )
    {
        return null;
    }

    @Override
    public Index[] getIndexes ( )
    {
        return new Index[0];
    }

    @Override
    public void elementAdded ( DataCollectionEvent e ) throws Exception
    {
    }

    @Override
    public void elementChanged ( DataCollectionEvent e ) throws Exception
    {
    }

    @Override
    public void elementRemoved ( DataCollectionEvent e ) throws Exception
    {
    }

    @Override
    public void elementWillAdd ( DataCollectionEvent e )
            throws DataCollectionVetoException, Exception
    {
    }

    @Override
    public void elementWillChange ( DataCollectionEvent e )
            throws DataCollectionVetoException, Exception
    {
    }

    @Override
    public void elementWillRemove ( DataCollectionEvent e )
            throws DataCollectionVetoException, Exception
    {
    }

}
