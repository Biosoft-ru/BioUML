package biouml.plugins.lucene;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.DataCollectionListenerSupport;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.FolderCollection;
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
        if( cannotBeIndexed( e.getDataElement(), e.getOwner() ) )
            return;
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
        if( cannotBeIndexed( e.getOldElement(), e.getOwner() ) )
            return;
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

    /**
     * Whether the element has, and gets, no document of its own in the index, so that adding or removing it needs no
     * index writer (each one opens a Lucene IndexWriter and commits: per row of a table, per node of a diagram).
     *
     * <p>The index has a document for an element of a folder (its name), for an element of a collection that lists
     * lucene-indexes, and for the contents of a collection added. So an element that is not a collection, in a parent
     * that is not a folder and has no lucene-indexes, has none. (The old path also deleted a document of that element,
     * which could exist only if the parent had lucene-indexes when the element was indexed.)</p>
     */
    public static boolean cannotBeIndexed(DataElement element, DataCollection<?> parent) // public for tests
    {
        if( element == null || parent == null || element instanceof DataCollection )
            return false;
        if( DataCollectionUtils.checkPrimaryElementType( parent, FolderCollection.class ) )
            return false;
        String indexes = parent.getInfo().getProperty( LuceneQuerySystem.LUCENE_INDEXES );
        return indexes == null || indexes.trim().isEmpty();
    }
}
