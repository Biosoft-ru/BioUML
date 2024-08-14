package biouml.plugins.lucene;

import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.DataCollectionListenerSupport;
import ru.biosoft.access.core.QuerySystem;
import biouml.model.Module;

public class LuceneFoldersListener extends DataCollectionListenerSupport
{
    /**
     * Necessary constructor as it's called via reflection
     * @param luceneFacade unused
     */
    public LuceneFoldersListener ( QuerySystem luceneFacade )
    {
    }

    @Override
    public void elementAdded(DataCollectionEvent e) throws Exception
    {
        Module module = Module.optModule( e.getDataElement() );
        if(module == null)
        {
            return;
        }
        LuceneQuerySystem luceneFacade = LuceneUtils.getLuceneFacade( module );
        if(luceneFacade == null)
        {
            return;
        }
        luceneFacade.addToIndex(e.getDataElementPath().getPathDifference( module.getCompletePath() ), false, null, null);
    }

    @Override
    public void elementRemoved(DataCollectionEvent e) throws Exception
    {
        Module module = Module.optModule( e.getOldElement() );
        if(module == null)
        {
            return;
        }
        LuceneQuerySystem luceneFacade = LuceneUtils.getLuceneFacade( module );
        if(luceneFacade == null)
        {
            return;
        }
        if( luceneFacade.testHaveLuceneDir() )
        {
            luceneFacade.deleteFromIndex( e.getDataElementPath().getPathDifference( module.getCompletePath() ), null );
        }
    }

    @Override
    public void elementChanged(DataCollectionEvent e) throws Exception
    {
        if( e.getPrimaryEvent() != null )
        {
            switch(e.getPrimaryEvent().getType())
            {
            case DataCollectionEvent.ELEMENT_ADDED:
                elementAdded(e.getPrimaryEvent());
                break;
            case DataCollectionEvent.ELEMENT_REMOVED:
                elementRemoved(e.getPrimaryEvent());
                break;
            case DataCollectionEvent.ELEMENT_CHANGED:
                elementChanged(e.getPrimaryEvent());
                break;
            default:
                break;
            }
        }
    }
}
