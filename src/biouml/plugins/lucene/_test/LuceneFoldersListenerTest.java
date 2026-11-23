package biouml.plugins.lucene._test;

import biouml.model.Diagram;
import biouml.model.Node;
import biouml.model.Module;
import biouml.model._test.DynamicModelTest.TestModuleType;
import biouml.plugins.lucene.LuceneQuerySystemImpl;
import com.developmentontheedge.application.ApplicationUtils;
import biouml.plugins.lucene.LuceneFoldersListener;
import biouml.plugins.lucene.LuceneQuerySystem;
import biouml.standard.diagram.PathwaySimulationDiagramType;
import biouml.standard.type.Stub;
import ru.biosoft.access.FolderVectorCollection;
import ru.biosoft.access._test.AbstractBioUMLTest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.FSDirectory;

import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollectionConfigConstants;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.access.core.VectorDataCollection;

/**
 * Which added elements need no index writer ({@link LuceneFoldersListener#cannotBeIndexed}): those that get no document,
 * which used to cost a Lucene commit each (rows, diagram nodes). Through the listener: a skipped addition does not reach
 * the indexing; an indexed one does; a removal always does (a document may exist from an earlier lucene-indexes).
 */
public class LuceneFoldersListenerTest extends AbstractBioUMLTest
{
    /** The listener, recording the additions that reach the indexing instead of indexing them. */
    private static class RecordingListener extends LuceneFoldersListener
    {
        final List<String> indexed = new ArrayList<>();

        RecordingListener()
        {
            super( null );
        }

        @Override
        protected void indexAdded(DataCollectionEvent e, DataElement element)
        {
            indexed.add( element.getName() ); // the element fetched by the listener, passed on
        }

        static boolean rule(DataElement element, DataCollection<?> parent)
        {
            return LuceneFoldersListener.cannotBeIndexed( element, parent );
        }
    }

    /** The element put into the collection (the event fetches it from there) and the event of its addition. */
    @SuppressWarnings ( {"unchecked", "rawtypes"} )
    private static DataCollectionEvent added(DataCollection owner, DataElement element) throws Exception
    {
        owner.put( element );
        return new DataCollectionEvent( owner, DataCollectionEvent.ELEMENT_ADDED, owner, element.getName(), element, null );
    }

    /** Through the listener: a row and a diagram node are not indexed; folder and indexed-collection elements are. */
    public void testListenerSkipsOnlyUnindexableAdditions() throws Exception
    {
        RecordingListener listener = new RecordingListener();
        VectorDataCollection<Element> table = new VectorDataCollection<>( "table" );
        listener.elementAdded( added( table, new Element( "row1", table ) ) );
        Diagram diagram = new Diagram( null, new Stub( null, "d" ), new PathwaySimulationDiagramType() );
        listener.elementAdded( added( diagram, new Node( diagram, new Stub( null, "A" ) ) ) );
        assertEquals( "no indexing for a row or a node", 0, listener.indexed.size() );

        FolderVectorCollection folder = new FolderVectorCollection( "folder", null );
        listener.elementAdded( added( folder, new Element( "result", folder ) ) );
        VectorDataCollection<Element> indexedCollection = new VectorDataCollection<>( "indexed" );
        indexedCollection.getInfo().getProperties().setProperty( LuceneQuerySystem.LUCENE_INDEXES, "name" );
        listener.elementAdded( added( indexedCollection, new Element( "e1", indexedCollection ) ) );
        VectorDataCollection<VectorDataCollection<?>> parent = new VectorDataCollection<>( "parent" );
        listener.elementAdded( added( parent, new VectorDataCollection<>( "child", parent, null ) ) );
        assertEquals( Arrays.asList( "result", "e1", "child" ), listener.indexed );
    }

    /**
     * The primary event of a change event (a row added to a table of a folder, seen by the folder) is handled the same
     * way: the row is not indexed.
     */
    public void testPrimaryEventOfAChange() throws Exception
    {
        RecordingListener listener = new RecordingListener();
        FolderVectorCollection folder = new FolderVectorCollection( "folder", null );
        VectorDataCollection<Element> table = new VectorDataCollection<>( "table", folder, null );
        DataCollectionEvent rowAdded = added( table, new Element( "row1", table ) );
        listener.elementChanged( new DataCollectionEvent( folder, DataCollectionEvent.ELEMENT_CHANGED, folder, "table", rowAdded ) );
        assertEquals( 0, listener.indexed.size() );
    }

    public void testFolderBatchCommitsOnceAndSkipsRemovedEntries() throws Exception
    {
        Path directory = Files.createTempDirectory("lucene-folder-batch");
        String moduleName = "luceneFolderBatch";
        Properties properties = new Properties();
        properties.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, moduleName);
        properties.setProperty(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, directory.toString());
        properties.setProperty(Module.TYPE_PROPERTY, TestModuleType.class.getName());
        properties.setProperty(LuceneQuerySystem.LUCENE_INDEX_DIRECTORY, "index");
        properties.put(DataCollectionConfigConstants.PRIMARY_COLLECTION, new VectorDataCollection<>(moduleName));
        Module module = new Module(null, properties);
        CollectionFactory.registerRoot(module);
        try
        {
            LuceneQuerySystemImpl index = new LuceneQuerySystemImpl(module);
            module.getInfo().setQuerySystem(index);
            FolderVectorCollection results = new FolderVectorCollection("results", module);
            module.put(results);
            LuceneFoldersListener listener = new LuceneFoldersListener(index);

            listener.elementAdded(added(results, new Element("before", results)));
            try( var luceneDirectory = FSDirectory.open(directory.resolve("index"));
                    var initial = DirectoryReader.open(luceneDirectory) )
            {
                long generation;
                try( var batch = LuceneFoldersListener.batchFolderAdditions(results.getCompletePath().getChildPath("run")) )
                {
                    FolderVectorCollection run = new FolderVectorCollection("run", results);
                    listener.elementAdded(added(results, run));
                    listener.elementAdded(added(run, new Element("table", run)));
                    listener.elementAdded(added(run, new Element("removed", run)));
                    run.remove("removed");

                    // An addition outside the scoped result tree is still indexed immediately.
                    listener.elementAdded(added(results, new Element("outside", results)));
                    try( var current = DirectoryReader.open(luceneDirectory) )
                    {
                        generation = current.getIndexCommit().getGeneration();
                        assertEquals(initial.getIndexCommit().getGeneration() + 1, generation);
                        assertEquals(2, current.numDocs());
                    }
                }
                try( var current = DirectoryReader.open(luceneDirectory) )
                {
                    assertEquals(generation + 1, current.getIndexCommit().getGeneration());
                    assertEquals(4, current.numDocs());
                    IndexSearcher searcher = new IndexSearcher(current);
                    for( String path : List.of("results/run", "results/run/table", "results/outside") )
                    {
                        assertEquals(1, searcher.count(new TermQuery(new Term(
                                LuceneQuerySystem.DATA_ELEMENT_RELATIVE_NAME, moduleName + "/" + path))));
                    }
                    assertEquals(0, searcher.count(new TermQuery(new Term(
                            LuceneQuerySystem.DATA_ELEMENT_RELATIVE_NAME, moduleName + "/results/run/removed"))));
                }
            }
        }
        finally
        {
            CollectionFactory.unregisterRoot(module);
            ApplicationUtils.removeDir(directory.toFile());
        }
    }

    private static class Element extends DataElementSupport
    {
        Element(String name, DataCollection<?> origin)
        {
            super( name, origin );
        }
    }

    /** A row of a table, an element of any collection that lists no lucene-indexes: no document. */
    public void testElementOfAnUnindexedCollection()
    {
        VectorDataCollection<Element> table = new VectorDataCollection<>( "table" );
        assertTrue( RecordingListener.rule( new Element( "row1", table ), table ) );
        table.getInfo().getProperties().setProperty( LuceneQuerySystem.LUCENE_INDEXES, "  " );
        assertTrue( "blank lucene-indexes index nothing", RecordingListener.rule( new Element( "row1", table ), table ) );
    }

    /** A node of a diagram: no document (a diagram lists no lucene-indexes). */
    public void testDiagramNode() throws Exception
    {
        Diagram diagram = new Diagram( null, new Stub( null, "d" ), new PathwaySimulationDiagramType() );
        Node node = new Node( diagram, new Stub( null, "A" ) );
        assertTrue( RecordingListener.rule( node, diagram ) );
    }

    /** An element of a collection that lists lucene-indexes is indexed: the writer still runs. */
    public void testElementOfAnIndexedCollection()
    {
        VectorDataCollection<Element> indexed = new VectorDataCollection<>( "indexed" );
        indexed.getInfo().getProperties().setProperty( LuceneQuerySystem.LUCENE_INDEXES, "name;title" );
        assertFalse( RecordingListener.rule( new Element( "e1", indexed ), indexed ) );
    }

    /** A collection added may have indexed contents: the writer still runs. */
    public void testCollection()
    {
        VectorDataCollection<VectorDataCollection<?>> parent = new VectorDataCollection<>( "parent" );
        assertFalse( RecordingListener.rule( new VectorDataCollection<>( "child", parent, null ), parent ) );
    }

    /** An element of a folder gets a document (its name): the writer still runs. */
    public void testFolderElement()
    {
        FolderVectorCollection folder = new FolderVectorCollection( "folder", null );
        assertFalse( RecordingListener.rule( new Element( "result", folder ), folder ) );
    }

    /** Nothing known: the writer still runs, as before. */
    public void testUnknown()
    {
        VectorDataCollection<Element> table = new VectorDataCollection<>( "table" );
        assertFalse( RecordingListener.rule( null, table ) );
        assertFalse( RecordingListener.rule( new Element( "row1", table ), null ) );
    }
}
