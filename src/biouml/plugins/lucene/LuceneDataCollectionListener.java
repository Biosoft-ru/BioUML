package biouml.plugins.lucene;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import biouml.model.Module;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionListener;
import ru.biosoft.access.core.DataCollectionVetoException;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.QuerySystem;

/**
 * 
 */
public class LuceneDataCollectionListener implements DataCollectionListener
{

    protected Logger log = Logger.getLogger(LuceneDataCollectionListener.class.getName());

    protected LuceneQuerySystemImpl luceneFacade = null;

    protected DataCollection<?> module = null;

    protected boolean enable = false;

    public boolean isEnable()
    {
        return enable;
    }

    public void setEnable(boolean enable) throws Exception
    {
        this.enable = enable;
        DataElement data = module.get(Module.DATA);
        if( data != null )
        {
            if( enable )
                addToAllCollections( (DataCollection<?>)data );
            else
                removeFromAllCollections( (DataCollection<?>)data );
        }
    }

    public LuceneDataCollectionListener(QuerySystem luceneFacade)
    {
        this.luceneFacade = (LuceneQuerySystemImpl)luceneFacade;
        module = this.luceneFacade.getCollection();
    }

    @Override
    public void elementAdded(DataCollectionEvent e) throws Exception
    {
        if( e.getType() == DataCollectionEvent.ELEMENT_ADDED )
        {
            if( enable )
            {
                String name = CollectionFactory.getRelativeName(e.getDataElement(), module);
                //System.out.println ( "Add to index data element " + name );
                if( luceneFacade.testHaveLuceneDir() )
                {
                    if( luceneFacade.testHaveIndex() )
                    {
                        luceneFacade.deleteFromIndex(name, null);
                    }
                    luceneFacade.addToIndex(name, true, null, null);
                }
            }
            if( e.getPrimaryEvent() == null )
                if( e.getDataElement() instanceof DataCollection )
                    addToAllCollections( (DataCollection<?>)e.getDataElement() );
        }
    }

    @Override
    public void elementWillChange(DataCollectionEvent e) throws Exception
    {
        if( e.getPrimaryEvent() == null )
        {
            if( e.getType() == DataCollectionEvent.ELEMENT_WILL_CHANGE )
            {
                if( enable )
                {
                    String name = CollectionFactory.getRelativeName(e.getDataElement(), module);
                    //System.out.println ( "Will change in index data element " + name );
                    if( luceneFacade.testHaveLuceneDir() )
                    {
                        if( luceneFacade.testHaveIndex() )
                        {
                            luceneFacade.deleteFromIndex(name, null);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void elementChanged(DataCollectionEvent e) throws Exception
    {
        if( e.getPrimaryEvent() == null )
            if( e.getType() == DataCollectionEvent.ELEMENT_CHANGED )
            {
                if( enable )
                {
                    String name = CollectionFactory.getRelativeName(e.getDataElement(), module);
                    //System.out.println ( "Change in index data element " + name );
                    if( luceneFacade.testHaveLuceneDir() )
                    {
                        if( luceneFacade.testHaveIndex() )
                        {
                            luceneFacade.addToIndex(name, true, null, null);
                        }
                    }
                }
            }
    }

    @Override
    public void elementWillRemove(DataCollectionEvent e) throws Exception
    {
        if( e.getType() == DataCollectionEvent.ELEMENT_WILL_REMOVE )
        {
            if( e.getPrimaryEvent() == null )
                if( e.getDataElement() instanceof DataCollection )
                    removeFromAllCollections( (DataCollection<?>)e.getDataElement() );
            if( enable )
            {
                String name = CollectionFactory.getRelativeName(e.getDataElement(), module);
                //System.out.println ( "Remove from index data element " + name );
                if( luceneFacade.testHaveLuceneDir() )
                {
                    luceneFacade.deleteFromIndex(name, null);
                }
            }
        }
    }

    private void addToAllCollections(DataCollection<?> dc)
    {
        dc.addDataCollectionListener(this);
        if( LuceneUtils.checkIfChildIndexPossible(dc) )
        {
            Iterator<?> iter = dc.iterator();
            while( iter.hasNext() )
            {
                try
                {
                    Object o = iter.next();
                    addToAllCollections( (DataCollection<?>)o );
                }
                catch( Throwable t )
                {
                    log.log(Level.SEVERE, t.getMessage(), t);
                }
            }
        }
    }

    private void removeFromAllCollections(DataCollection<?> dc)
    {
        dc.removeDataCollectionListener(this);
        if( LuceneUtils.checkIfChildIndexPossible(dc) )
        {
            Iterator<?> iter = dc.iterator();
            while( iter.hasNext() )
            {
                try
                {
                    Object o = iter.next();
                    removeFromAllCollections( (DataCollection<?>)o );
                }
                catch( Throwable t )
                {
                    log.log(Level.SEVERE, t.getMessage(), t);
                }
            }
        }
    }

    @Override
    public void elementWillAdd(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
    }

    @Override
    public void elementRemoved(DataCollectionEvent e) throws Exception
    {
    }
}
