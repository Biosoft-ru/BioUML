package biouml.plugins.lucene;

import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionListener;
import ru.biosoft.access.core.DataCollectionVetoException;
import ru.biosoft.access.core.QuerySystem;

public class LuceneInitListener implements DataCollectionListener
{
    
    LuceneQuerySystem luceneFacade;
    LuceneDataCollectionListener listener;

    public LuceneInitListener ( QuerySystem luceneFacade )
    {
       if ( ! ( luceneFacade instanceof LuceneQuerySystem ) )
           throw new IllegalArgumentException ( "Invalid query system type: expected " + LuceneQuerySystem.class.getName ( ) + " but was " + (luceneFacade == null?null:luceneFacade.getClass ( ).getName ( )) );
       this.luceneFacade = ( LuceneQuerySystem ) luceneFacade;
    }
    
    @Override
    public void elementAdded ( DataCollectionEvent e ) throws Exception
    {
        init ( );
    }

    @Override
    public void elementChanged ( DataCollectionEvent e ) throws Exception
    {
        init ( );
    }

    @Override
    public void elementRemoved ( DataCollectionEvent e ) throws Exception
    {
        init ( );
    }

    @Override
    public void elementWillAdd ( DataCollectionEvent e )
            throws DataCollectionVetoException, Exception
    {
        init ( );
    }

    @Override
    public void elementWillChange ( DataCollectionEvent e )
            throws DataCollectionVetoException, Exception
    {
        init ( );
    }

    @Override
    public void elementWillRemove ( DataCollectionEvent e )
            throws DataCollectionVetoException, Exception
    {
        init ( );
    }

    private void init ( ) throws Exception
    {
        if ( listener == null )
        {
            listener = new LuceneDataCollectionListener ( luceneFacade );
            listener.setEnable ( true );
        }
    }
    
}
