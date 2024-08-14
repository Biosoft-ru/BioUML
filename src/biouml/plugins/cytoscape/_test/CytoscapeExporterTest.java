package biouml.plugins.cytoscape._test;

import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.cytoscape.CytoscapeConstants;
import biouml.plugins.cytoscape.CytoscapeDiagramExporter;
import biouml.plugins.sbgn.extension.SbgnExDiagramType;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.SemanticRelation;
import biouml.standard.type.Stub;
import biouml.workbench.graph.DiagramToGraphTransformer;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.graph.ForceDirectedLayouter;

public class CytoscapeExporterTest extends AbstractBioUMLTest
{
    public void testCytoscapeExport() throws Exception
    {
        Diagram diagram = generateTestDiagram();
        CytoscapeDiagramExporter exporter = new CytoscapeDiagramExporter();
        JSONArray cxDiagram = exporter.generateCXDiagram( diagram );
        assertEquals( "Incorrect CX stream size", 7, cxDiagram.length() );
        String[] sections = {CytoscapeConstants.NUMBER_VERIFICATION, CytoscapeConstants.NETWORK_ATTRIBUTES, CytoscapeConstants.NODES,
                CytoscapeConstants.NODE_ATTRIBUTES, CytoscapeConstants.EDGES, CytoscapeConstants.EDGE_ATTRIBUTES,
                CytoscapeConstants.CARTESIAN_LAYOUT};
        int i = 0;
        for( String sectionName : sections )
        {
            assertTrue( sectionName + " section missed", cxDiagram.getJSONObject( i++ ).has( sectionName ) );
        }

        JSONArray nodes = cxDiagram.getJSONObject( 2 ).getJSONArray( CytoscapeConstants.NODES );
        assertEquals( "Incorrect number of nodes", diagram.stream( Node.class ).count(), nodes.length() );

        JSONArray edges = cxDiagram.getJSONObject( 4 ).getJSONArray( CytoscapeConstants.EDGES );
        assertEquals( "Incorrect number of edges", diagram.stream( Edge.class ).count(), edges.length() );

        Set<Integer> nodeIds = new HashSet<>();
        for( i = 0; i < nodes.length(); i++ )
        {
            JSONObject node = nodes.getJSONObject( i );
            assertTrue( CytoscapeConstants.ELEMENT_ID_KEY + " is missing", node.has( CytoscapeConstants.ELEMENT_ID_KEY ) );
            assertTrue( CytoscapeConstants.NAME_KEY + " is missing", node.has( CytoscapeConstants.NAME_KEY ) );

            int id = node.getInt( CytoscapeConstants.ELEMENT_ID_KEY );
            assertFalse( "Duplicate node id found", nodeIds.contains( id ) );
            nodeIds.add( id );
            String name = node.getString( CytoscapeConstants.NAME_KEY );
            assertTrue( "Node not found in diagram", diagram.contains( name ) );
        }

        Set<Integer> edgeIds = new HashSet<>();
        for( i = 0; i < edges.length(); i++ )
        {
            JSONObject edge = edges.getJSONObject( i );
            int id = edge.getInt( CytoscapeConstants.ELEMENT_ID_KEY );
            assertFalse( "Duplicate edge id found", nodeIds.contains( id ) );
            edgeIds.add( id );
            assertTrue( "Source node id not found", nodeIds.contains( edge.getInt( CytoscapeConstants.EDGE_SOURCE_KEY ) ) );
            assertTrue( "Target node id not found", nodeIds.contains( edge.getInt( CytoscapeConstants.EDGE_TARGET_KEY ) ) );
        }

        //TODO: check attributes

        JSONArray layout = cxDiagram.getJSONObject( 6 ).getJSONArray( CytoscapeConstants.CARTESIAN_LAYOUT );
        assertEquals( "Incorrect number of layouted nodes", nodes.length(), layout.length() );
        for( i = 0; i < layout.length(); i++ )
        {
            JSONObject layoutElement = layout.getJSONObject( i );
            assertTrue( "Layouted node id not found", nodeIds.contains( layoutElement.getInt( CytoscapeConstants.LAYOUT_NODE_KEY ) ) );
        }
    }

    private Diagram generateTestDiagram() throws Exception
    {
        Diagram diagram = new Diagram( null, new DiagramInfo( "test" ), new SbgnExDiagramType() );

        Stub gene = new Stub( null, "gene", "gene_type" );
        Node node1 = new Node( diagram, gene );
        diagram.put( node1 );

        Stub protein = new Stub( null, "protein", "protein_type" );
        Node node2 = new Node( diagram, protein );
        diagram.put( node2 );

        Stub substance = new Stub( null, "substance", "substance_type" );
        Node node3 = new Node( diagram, substance );
        diagram.put( node3 );

        Edge edge1 = new Edge( new SemanticRelation( null, "edge1" ), node1, node2 );
        diagram.put( edge1 );

        Edge edge2 = new Edge( new SemanticRelation( null, "edge2" ), node2, node3 );
        diagram.put( edge2 );

        ForceDirectedLayouter layouter = new ForceDirectedLayouter();
        DiagramToGraphTransformer.layout( diagram, layouter );

        return diagram;
    }
}
