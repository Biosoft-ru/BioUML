package biouml.plugins.cytoscape._test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Preferences;

import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.bionetgen.diagram.BionetgenImporter;
import biouml.plugins.cytoscape.CytoscapeConstants;
import biouml.plugins.cytoscape.CytoscapeDiagramImporter;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.core.DataElementPath;

public class CytoscapeImporterTest extends AbstractBioUMLTest
{
    private static final String DIAGRAM_NAME = "DiagramName";
    protected static final DataElementPath COLLECTION_NAME = DataElementPath.create( "databases/test/Diagrams" );
    protected @Nonnull File file = new File( "biouml/plugins/cytoscape/_test/resources/test_network.cx" );

    public CytoscapeImporterTest(String name)
    {
        super( name );
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite( CytoscapeImporterTest.class.getName() );

        suite.addTest( new CytoscapeImporterTest( "testImport" ) );

        return suite;
    }

    @Override
    public void tearDown() throws Exception
    {
        try
        {
            DataCollection<?> collection = COLLECTION_NAME.getDataCollection();
            collection.remove( DIAGRAM_NAME );
        }
        catch( Exception e )
        {
        }
        super.tearDown();
    }

    public void testImport() throws Exception
    {
        CollectionFactory.createRepository( "../data" );
        Application.setPreferences( new Preferences() );

        CytoscapeDiagramImporter importer = new CytoscapeDiagramImporter();
        assertTrue( importer.accept( file ) == DataElementImporter.ACCEPT_HIGH_PRIORITY );

        DataCollection<?> collection = COLLECTION_NAME.getDataCollection();

        Diagram diagram = (Diagram)importer.doImport( collection, file, DIAGRAM_NAME, null,
                Logger.getLogger( BionetgenImporter.class.getName() ) );
        assertNotNull( diagram );
        assertEquals( "Incorrect size", 5, diagram.getSize() );
        assertEquals( "Incorrect diagram name", DIAGRAM_NAME, diagram.getName() );
        assertEquals( "Incorrect diagram title", "Network", diagram.getTitle() );
        DynamicProperty dp = diagram.getAttributes().getProperty( "selected" );
        checkProperty( dp, "selected", Boolean.class, true );

        String nodeName = CytoscapeDiagramImporter.CX_NODE_PREFIX + 64;
        Node node = diagram.findNode( nodeName );
        List<DynamicProperty> props = new ArrayList<>();
        props.add( new DynamicProperty( "name", String.class, "Node 1" ) );
        props.add( new DynamicProperty( "test", Double[].class, new Double[] {1.3, 2.5} ) );
        props.add( new DynamicProperty( "selected", Boolean.class, false ) );
        checkNode( node, nodeName, node.getTitle(), props );

        nodeName = CytoscapeDiagramImporter.CX_NODE_PREFIX + 66;
        node = diagram.findNode( nodeName );
        props = new ArrayList<>();
        props.add( new DynamicProperty( "name", String.class, "Node 2" ) );
        props.add( new DynamicProperty( "selected", Boolean.class, false ) );
        assertNotNull( diagram.findNode( CytoscapeDiagramImporter.CX_NODE_PREFIX + 66 ) );

        nodeName = CytoscapeDiagramImporter.CX_NODE_PREFIX + 68;
        node = diagram.findNode( nodeName );
        props = new ArrayList<>();
        props.add( new DynamicProperty( "name", String.class, "Node 3" ) );
        props.add( new DynamicProperty( "selected", Boolean.class, false ) );
        assertNotNull( diagram.findNode( CytoscapeDiagramImporter.CX_NODE_PREFIX + 68 ) );

        String edgeName = CytoscapeDiagramImporter.CX_EDGE_PREFIX + 83;
        Edge edge = (Edge)diagram.findDiagramElement( edgeName );
        props = new ArrayList<>();
        props.add( new DynamicProperty( "selected", Boolean.class, false ) );
        props.add( new DynamicProperty( "name", String.class, "Node 1 (interacts with) Node 2" ) );
        props.add( new DynamicProperty( "shared name", String.class, "Node 1 (interacts with) Node 2" ) );
        props.add( new DynamicProperty( "interaction", String.class, "interacts with" ) );
        props.add( new DynamicProperty( CytoscapeConstants.INTERACTION_ATTRIBUTE, String.class, "interacts with" ) );
        checkEdge( edge, edgeName, CytoscapeDiagramImporter.CX_NODE_PREFIX + 64, CytoscapeDiagramImporter.CX_NODE_PREFIX + 66, props );

        edgeName = CytoscapeDiagramImporter.CX_EDGE_PREFIX + 85;
        edge = (Edge)diagram.findDiagramElement( edgeName );
        props = new ArrayList<>();
        props.add( new DynamicProperty( "selected", Boolean.class, false ) );
        props.add( new DynamicProperty( "name", String.class, "Node 2 (interacts with) Node 3" ) );
        props.add( new DynamicProperty( "shared name", String.class, "Node 2 (interacts with) Node 3" ) );
        props.add( new DynamicProperty( "interaction", String.class, "interacts with" ) );
        props.add( new DynamicProperty( CytoscapeConstants.INTERACTION_ATTRIBUTE, String.class, "interacts with" ) );
        checkEdge( edge, edgeName, CytoscapeDiagramImporter.CX_NODE_PREFIX + 66, CytoscapeDiagramImporter.CX_NODE_PREFIX + 68, props );
    }

    private void checkNode(Node node, String name, String title, List<DynamicProperty> props)
    {
        assertNotNull( "Missed node " + name, node );
        assertEquals( "Incorrect name for " + name, name, node.getName() );
        assertEquals( "Incorrect title for " + name, title, node.getTitle() );
        //Structure, sbgn:multimer and sboTerm were added
        assertEquals( "Incorrect property number", props.size() + 3, node.getAttributes().size() );
        for( DynamicProperty prop : props )
            checkProperty( node.getAttributes().getProperty( prop.getName() ), prop.getName(), prop.getType(), prop.getValue() );
    }

    private void checkEdge(Edge edge, String name, String inputName, String outputName, List<DynamicProperty> props)
    {
        assertNotNull( "Missed edge " + name, edge );
        assertEquals( "Incorrect input for edge " + edge, inputName, edge.getInput().getName() );
        assertEquals( "Incorrect output for edge " + edge, outputName, edge.getOutput().getName() );
        assertEquals( "Incorrect property number", props.size(), edge.getAttributes().size() );
        for( DynamicProperty prop : props )
            checkProperty( edge.getAttributes().getProperty( prop.getName() ), prop.getName(), prop.getType(), prop.getValue() );
    }

    private void checkProperty(DynamicProperty dp, String name, Class<?> type, Object value)
    {
        assertNotNull( "Missed property " + name, dp );
        assertEquals( name, dp.getName() );
        assertEquals( "Incorrect property type", type, dp.getType() );
        if( type.isArray() )
            assertArrayEquals( "Incorrect property value", (Object[])value, (Object[])dp.getValue() );
        else
            assertEquals( "Incorrect property value", value, dp.getValue() );
    }
}
