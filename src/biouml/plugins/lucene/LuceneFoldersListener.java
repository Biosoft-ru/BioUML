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
        // getDataElement fetches the element from its collection (owner.get): once, for the check and the indexing
        DataElement element = e.getDataElement();
        if( cannotBeIndexed( element, e.getOwner() ) )
            return;
        indexAdded( e, element );
    }

    /** Adds the element of the event to the index of its module (an IndexWriter opened and committed). */
    protected void indexAdded(DataCollectionEvent e, DataElement element) throws Exception
    {
        Module module = Module.optModule( element );
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
        // not skipped like an addition: whether a document of the element exists depends on the lucene-indexes the
        // parent had when the element was indexed, not on the ones it has now
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
     * Whether adding the element creates no document in the index, so that the addition needs no index writer (each one
     * opens a Lucene IndexWriter and commits: per row of a table, per node of a diagram).
     *
     * <p>Additions are indexed by LuceneQuerySystemImpl.addToIndex(String,...): an element of a folder (the primary
     * collection type, as checked there) gets a document with its name; any other element goes to replaceToIndex(de,
     * origin), which adds a document only if the origin lists lucene-indexes (non-blank); a collection added may index
     * its contents. So an element that is not a collection, in a parent that is not a folder and lists no lucene-indexes,
     * gets none. (replaceToIndex also deleted a previous document of the element's path first; one could exist only if
     * the parent listed lucene-indexes when it was indexed, and the element added now replaces it in no way.)</p>
     */
    protected static boolean cannotBeIndexed(DataElement element, DataCollection<?> parent)
    {
        if( element == null || parent == null || element instanceof DataCollection )
            return false;
        if( DataCollectionUtils.checkPrimaryElementType( parent, FolderCollection.class ) )
            return false;
        String indexes = parent.getInfo().getProperty( LuceneQuerySystem.LUCENE_INDEXES );
        return indexes == null || indexes.trim().isEmpty();
    }
}
