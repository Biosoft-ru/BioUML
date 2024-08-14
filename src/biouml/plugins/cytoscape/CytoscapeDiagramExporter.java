package biouml.plugins.cytoscape;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;

import org.json.JSONArray;
import org.json.JSONObject;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.model.Property;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramExporter;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.sbgn.SBGNPropertyConstants;
import biouml.standard.type.Base;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Type;
import ru.biosoft.util.BeanAsMapUtil;

/**
 * Exports diagram to .cx format
 *
 * Aspects supported:
 * numberVerification (mandatory)
 * networkAttributes
 * nodes
 * edges
 * nodeAttributes
 * edgeAttributes
 * cartesianLayout
 */

public class CytoscapeDiagramExporter extends DiagramExporter
{
    private Map<String, Integer> node2id = new HashMap<>();

    @Override
    public boolean accept(Diagram diagram)
    {
        return diagram != null;
    }

    @Override
    public boolean init(String format, String suffix)
    {
        return true;
    }

    @Override
    public void doExport(@Nonnull Diagram diagram, @Nonnull File file) throws Exception
    {
        JSONArray cxDiagram = generateCXDiagram( diagram );
        try (FileWriter fw = new FileWriter( file, false ))
        {
            fw.write( cxDiagram.toString( 1 ) );
            fw.close();
        }
    }

    public JSONArray generateCXDiagram(Diagram diagram)
    {
        JSONArray cxDiagram = new JSONArray();
        cxDiagram.put( getNumberVerification() );
        processDiagramProperties( diagram, cxDiagram );
        processNodes( diagram, cxDiagram );
        processEdges( diagram, cxDiagram );
        processLayout( diagram, cxDiagram );
        return cxDiagram;
    }

    private JSONObject getNumberVerification()
    {
        return new JSONObject().put( CytoscapeConstants.NUMBER_VERIFICATION,
                new JSONArray().put( new JSONObject().put( CytoscapeConstants.LONG_NUMBER_KEY, CytoscapeConstants.LONG_NUMBER_VALUE ) ) );

    }

    private void processDiagramProperties(Diagram diagram, JSONArray cxDiagram)
    {
        JSONArray networkAttr = new JSONArray();

        networkAttr.put( new JSONObject().put( CytoscapeConstants.NAME_KEY, "name" ).put( CytoscapeConstants.ATTRIBUTE_VALUE_KEY,
                diagram.getTitle() ) );
        if( diagram.getDescription() != null )
            networkAttr.put( new JSONObject().put( CytoscapeConstants.NAME_KEY, "description" ).put( CytoscapeConstants.ATTRIBUTE_VALUE_KEY,
                    diagram.getDescription() ) );

        cxDiagram.put( new JSONObject().put( CytoscapeConstants.NETWORK_ATTRIBUTES, networkAttr ) );
    }

    private void processNodes(Diagram diagram, JSONArray cxDiagram)
    {
        JSONArray nodes = new JSONArray();
        JSONArray nodeAttributes = new JSONArray();
        diagram.stream( Node.class ).forEach( n -> processNode( n, nodes, nodeAttributes ) );
        cxDiagram.put( new JSONObject().put( CytoscapeConstants.NODES, nodes ) );
        cxDiagram.put( new JSONObject().put( CytoscapeConstants.NODE_ATTRIBUTES, nodeAttributes ) );
    }

    private void processNode(Node node, JSONArray nodes, JSONArray nodeAttributes)
    {
        JSONObject cxNode = new JSONObject();
        int cxNodeId = generateId();
        cxNode.put( CytoscapeConstants.ELEMENT_ID_KEY, cxNodeId );
        cxNode.put( CytoscapeConstants.NAME_KEY, node.getName() );
        nodes.put( cxNode );
        node2id.put( node.getName(), cxNodeId );

        //attributes
        JSONObject cxNodeAttr = new JSONObject();
        //name attribute will be displayed in Cytoscape visualisation
        cxNodeAttr.put( CytoscapeConstants.ATTRIBUTE_ELEMENT_ID_KEY, cxNodeId );
        cxNodeAttr.put( CytoscapeConstants.NAME_KEY, "name" );
        //TODO: add something else instead of title for Reaction node?
        cxNodeAttr.put( CytoscapeConstants.ATTRIBUTE_VALUE_KEY, node.getTitle() );
        nodeAttributes.put( cxNodeAttr );

        Base kernel = node.getKernel();
        if( kernel != null )
        {
            cxNodeAttr = new JSONObject();
            cxNodeAttr.put( CytoscapeConstants.ATTRIBUTE_ELEMENT_ID_KEY, cxNodeId );
            cxNodeAttr.put( CytoscapeConstants.NAME_KEY, "kernel" );
            cxNodeAttr.put( CytoscapeConstants.ATTRIBUTE_VALUE_KEY, kernel.getTitle() );
            nodeAttributes.put( cxNodeAttr );

            cxNodeAttr = new JSONObject();
            cxNodeAttr.put( CytoscapeConstants.ATTRIBUTE_ELEMENT_ID_KEY, cxNodeId );
            cxNodeAttr.put( CytoscapeConstants.NAME_KEY, "kernel type" );
            String kernelType = kernel.getType();
            cxNodeAttr.put( CytoscapeConstants.ATTRIBUTE_VALUE_KEY, kernelType );
            nodeAttributes.put( cxNodeAttr );
        }

        //TODO: decide what fields we should export
        //serialize node fields
        Map<String, Object> properties = BeanAsMapUtil.convertBeanToMap( node, p -> p.isVisible( Property.SHOW_USUAL ) );
        properties.forEach( (name, value) -> {
            if( value == null )
                return;
            String cxType = CytoscapeConstants.getCXTypeForClass( value.getClass() );
            if( cxType == null )
                return;

            JSONObject nodeAttr = new JSONObject();
            nodeAttr.put( CytoscapeConstants.ATTRIBUTE_ELEMENT_ID_KEY, cxNodeId );
            nodeAttr.put( CytoscapeConstants.NAME_KEY, name );
            nodeAttr.put( CytoscapeConstants.ATTRIBUTE_DATATYPE_KEY, cxType );
            nodeAttr.put( CytoscapeConstants.ATTRIBUTE_VALUE_KEY, value );
            nodeAttributes.put( nodeAttr );
        } );

        //serialize attributes
        Iterator<DynamicProperty> it = node.getAttributes().propertyIterator();
        while( it.hasNext() )
        {
            DynamicProperty p = it.next();
            Object value = p.getValue();
            if( value == null )
                continue;
            String cxType = CytoscapeConstants.getCXTypeForClass( p.getType() );
            if( cxType == null )
                continue;

            JSONObject nodeAttr = new JSONObject();
            nodeAttr.put( CytoscapeConstants.ATTRIBUTE_ELEMENT_ID_KEY, cxNodeId );
            nodeAttr.put( CytoscapeConstants.NAME_KEY, p.getDisplayName() );
            nodeAttr.put( CytoscapeConstants.ATTRIBUTE_DATATYPE_KEY, cxType );
            nodeAttr.put( CytoscapeConstants.ATTRIBUTE_VALUE_KEY, value );
            nodeAttributes.put( nodeAttr );
        }
        JSONObject nodeAttr = new JSONObject();
        nodeAttr.put( CytoscapeConstants.ATTRIBUTE_ELEMENT_ID_KEY, cxNodeId );
        nodeAttr.put( CytoscapeConstants.NAME_KEY, CytoscapeConstants.BIOPAX_TYPE );
        nodeAttr.put( CytoscapeConstants.ATTRIBUTE_VALUE_KEY, getBioPAXType( node ) );
        nodeAttributes.put( nodeAttr );
    }

    private @Nonnull String getBioPAXType(DiagramElement element)
    {
        if( element instanceof Edge )
        {
            String role = ( element.getKernel() instanceof SpecieReference ) ? ( (SpecieReference)element.getKernel() ).getRole()
                    : element.getKernel().getType();
            String edgeType = element.getAttributes().getValueAsString( SBGNPropertyConstants.SBGN_EDGE_TYPE );

            if( SpecieReference.PRODUCT.equals( role ) || biouml.plugins.sbgn.Type.TYPE_PRODUCTION.equals( role ) )
                return CytoscapeConstants.CX_EDGE_PRODUCT;
            else if( SpecieReference.REACTANT.equals( role ) || biouml.plugins.sbgn.Type.TYPE_CONSUMPTION.equals( role ) )
                return CytoscapeConstants.CX_EDGE_REACTANT;
            else if( SpecieReference.MODIFIER.equals( role ) || biouml.plugins.sbgn.Type.TYPE_REGULATION.equals( role ) )
            {
                if( biouml.plugins.sbgn.Type.TYPE_INHIBITION.equals( edgeType ) )
                    return CytoscapeConstants.CX_EDGE_MODIFIER_INHIBITION;
                else
                    return CytoscapeConstants.CX_EDGE_MODIFIER_ACTIVATION;
            }
            else
            {
                String interaction = element.getAttributes().getValueAsString( CytoscapeConstants.INTERACTION_ATTRIBUTE );
                return interaction != null ? interaction : "";
            }
        }
        Base kernel = element.getKernel();
        if( kernel instanceof Reaction )
            return "BiochemicalReaction";

        String type = kernel.getType();
        switch( type )
        {
            case biouml.plugins.sbgn.Type.TYPE_NUCLEIC_ACID_FEATURE:
                return "RNA";
            case biouml.plugins.sbgn.Type.TYPE_COMPLEX:
                return "Complex";
            case biouml.plugins.sbgn.Type.TYPE_SIMPLE_CHEMICAL:
                return "SmallMolecule";
            case biouml.plugins.sbgn.Type.TYPE_MACROMOLECULE:
                return "Protein";
            case biouml.plugins.sbgn.Type.TYPE_PERTURBING_AGENT:
            default:
                return "PhysicalEntity";
        }
    }

    private void processEdges(Diagram diagram, JSONArray cxDiagram)
    {
        JSONArray edges = new JSONArray();
        JSONArray edgeAttributes = new JSONArray();
        diagram.stream( Edge.class ).forEach( e -> processEdge( e, edges, edgeAttributes ) );
        cxDiagram.put( new JSONObject().put( CytoscapeConstants.EDGES, edges ) );
        cxDiagram.put( new JSONObject().put( CytoscapeConstants.EDGE_ATTRIBUTES, edgeAttributes ) );
    }

    private void processEdge(Edge edge, JSONArray edges, JSONArray edgeAttributes)
    {
        JSONObject cxEdge = new JSONObject();
        int cxEdgeId = generateId();
        cxEdge.put( CytoscapeConstants.ELEMENT_ID_KEY, cxEdgeId );
        cxEdge.put( CytoscapeConstants.EDGE_SOURCE_KEY, node2id.get( edge.getInput().getName() ) );
        cxEdge.put( CytoscapeConstants.EDGE_TARGET_KEY, node2id.get( edge.getOutput().getName() ) );
        cxEdge.put( CytoscapeConstants.EDGE_TYPE_KEY, getBioPAXType( edge ) );

        edges.put( cxEdge );

        JSONObject cxEdgeAttr = new JSONObject();
        cxEdgeAttr.put( CytoscapeConstants.ATTRIBUTE_ELEMENT_ID_KEY, cxEdgeId );
        cxEdgeAttr.put( CytoscapeConstants.NAME_KEY, "kernel type" );
        Base kernel = edge.getKernel();
        String kernelType = kernel != null ? kernel instanceof SpecieReference ? ( (SpecieReference)kernel ).getRole() : kernel.getType()
                : Type.TYPE_REACTION;
        cxEdgeAttr.put( CytoscapeConstants.ATTRIBUTE_VALUE_KEY, kernelType );
        edgeAttributes.put( cxEdgeAttr );

    }

    private void processLayout(Diagram diagram, JSONArray cxDiagram)
    {
        JSONArray layout = new JSONArray();
        int viewId = generateId();
        diagram.stream( Node.class ).forEach( n -> {
            layout.put( new JSONObject().put( CytoscapeConstants.LAYOUT_NODE_KEY, node2id.get( n.getName() ) )
                    .put( CytoscapeConstants.LAYOUT_VIEW_KEY, viewId ).put( CytoscapeConstants.LAYOUT_X_KEY, n.getLocation().getX() )
                    .put( CytoscapeConstants.LAYOUT_Y_KEY, n.getLocation().getY() ) );
        } );
        cxDiagram.put( new JSONObject().put( CytoscapeConstants.CARTESIAN_LAYOUT, layout ) );
    }

    private AtomicInteger cxElementId = new AtomicInteger( 0 );

    private int generateId()
    {
        return cxElementId.incrementAndGet();
    }

}
