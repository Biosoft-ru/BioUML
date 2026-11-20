package biouml.plugins.lucene._test;

import biouml.model.Diagram;
import biouml.model.Node;
import biouml.plugins.lucene.LuceneFoldersListener;
import biouml.plugins.lucene.LuceneQuerySystem;
import biouml.standard.diagram.PathwaySimulationDiagramType;
import biouml.standard.type.Stub;
import ru.biosoft.access.FolderVectorCollection;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.access.core.VectorDataCollection;

/**
 * Which elements need no index writer when they are added or removed ({@link LuceneFoldersListener#cannotBeIndexed}):
 * those the index has no document for, which used to cost a Lucene commit each (rows, diagram nodes).
 */
public class LuceneFoldersListenerTest extends AbstractBioUMLTest
{
    private static class Element extends DataElementSupport
    {
        Element(String name, ru.biosoft.access.core.DataCollection<?> origin)
        {
            super( name, origin );
        }
    }

    /** A row of a table, an element of any collection that lists no lucene-indexes: no document. */
    public void testElementOfAnUnindexedCollection()
    {
        VectorDataCollection<Element> table = new VectorDataCollection<>( "table" );
        assertTrue( LuceneFoldersListener.cannotBeIndexed( new Element( "row1", table ), table ) );
        table.getInfo().getProperties().setProperty( LuceneQuerySystem.LUCENE_INDEXES, "  " );
        assertTrue( "blank lucene-indexes index nothing", LuceneFoldersListener.cannotBeIndexed( new Element( "row1", table ), table ) );
    }

    /** A node of a diagram: no document (a diagram lists no lucene-indexes). */
    public void testDiagramNode() throws Exception
    {
        Diagram diagram = new Diagram( null, new Stub( null, "d" ), new PathwaySimulationDiagramType() );
        Node node = new Node( diagram, new Stub( null, "A" ) );
        assertTrue( LuceneFoldersListener.cannotBeIndexed( node, diagram ) );
    }

    /** An element of a collection that lists lucene-indexes is indexed: the writer still runs. */
    public void testElementOfAnIndexedCollection()
    {
        VectorDataCollection<Element> indexed = new VectorDataCollection<>( "indexed" );
        indexed.getInfo().getProperties().setProperty( LuceneQuerySystem.LUCENE_INDEXES, "name;title" );
        assertFalse( LuceneFoldersListener.cannotBeIndexed( new Element( "e1", indexed ), indexed ) );
    }

    /** A collection added may have indexed contents: the writer still runs. */
    public void testCollection()
    {
        VectorDataCollection<VectorDataCollection<?>> parent = new VectorDataCollection<>( "parent" );
        assertFalse( LuceneFoldersListener.cannotBeIndexed( new VectorDataCollection<>( "child", parent, null ), parent ) );
    }

    /** An element of a folder gets a document (its name): the writer still runs. */
    public void testFolderElement()
    {
        FolderVectorCollection folder = new FolderVectorCollection( "folder", null );
        assertFalse( LuceneFoldersListener.cannotBeIndexed( new Element( "result", folder ), folder ) );
    }

    /** Nothing known: the writer still runs, as before. */
    public void testUnknown()
    {
        VectorDataCollection<Element> table = new VectorDataCollection<>( "table" );
        assertFalse( LuceneFoldersListener.cannotBeIndexed( null, table ) );
        assertFalse( LuceneFoldersListener.cannotBeIndexed( new Element( "row1", table ), null ) );
    }
}
