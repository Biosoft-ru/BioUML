package biouml.plugins.cytoscape;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.DiagramImporter;
import biouml.model.Edge;
import biouml.model.Module;
import biouml.model.Node;
import biouml.plugins.cytoscape.cx.CXEdge;
import biouml.plugins.cytoscape.cx.CXElement;
import biouml.plugins.cytoscape.cx.CXNode;
import biouml.plugins.cytoscape.cx.CXReactionNode;
import biouml.plugins.sbgn.SBGNPropertyConstants;
import biouml.plugins.sbgn.SbgnDiagramViewOptions;
import biouml.plugins.sbgn.extension.SbgnExDiagramType;
import biouml.plugins.sbgn.extension.SbgnExSemanticController;
import biouml.standard.diagram.Util;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Reaction;
import biouml.standard.type.SemanticRelation;
import biouml.standard.type.Specie;
import biouml.standard.type.SpecieReference;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.graphics.font.ColorFont;
import ru.biosoft.jobcontrol.FunctionJobControl;

public class CytoscapeDiagramImporter extends DiagramImporter
{
    public static final String CX_EDGE_PREFIX = "CXEdge_";
    public static final String CX_NODE_PREFIX = "CXNode_";
    protected static final String CX_FORMAT = "cx";

    protected static final Logger log = Logger.getLogger( CytoscapeDiagramImporter.class.getName() );
    @Override
    public int accept(File file)
    {
        if( !file.canRead() )
            return ACCEPT_UNSUPPORTED;

        //TODO: think about more correct condition
        if( !CX_FORMAT.equals( file.getName().substring( file.getName().lastIndexOf( "." ) + 1 ).toLowerCase() ) )
            return ACCEPT_UNSUPPORTED;
        try
        {
            //file is too big
            if( Files.size( file.toPath() ) > (int)2e+9 )
                return ACCEPT_UNSUPPORTED;
        }
        catch(IOException e)
        {
            return ACCEPT_UNSUPPORTED;
        }


        try( FileInputStream is = new FileInputStream( file );
                InputStreamReader reader = new InputStreamReader( is, StandardCharsets.UTF_8 ) )
        {
            String fileSource = ApplicationUtils.readAsString( is );
            new JSONArray( fileSource );
            return ACCEPT_HIGH_PRIORITY;
        }
        catch( IOException | JSONException e )
        {
            return ACCEPT_UNSUPPORTED;
        }
    }

    @Override
    public int accept(DataCollection parent, File file)
    {
        if( parent instanceof FolderCollection && parent.isAcceptable( Diagram.class ) )
            return file == null ? ACCEPT_HIGH_PRIORITY : accept( file );
        return super.accept( parent, file );
    }

    @Override
    public DataElement doImport(Module module, File file, String diagramName) throws Exception
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataElement doImport(@Nonnull DataCollection parent, @Nonnull File file, String diagramName, FunctionJobControl jobControl,
            Logger log) throws Exception
    {
        if( jobControl != null )
            jobControl.functionStarted();

        JSONArray nodesJson = null;
        JSONArray edgesJson = null;
        JSONArray networkAttrsJson = null;
        JSONArray layoutJson = null;
        JSONArray verificationJson = null;
        JSONArray nodeAttrsJson = null;
        JSONArray edgeAttrsJson = null;
        try( FileInputStream is = new FileInputStream( file );
                InputStreamReader reader = new InputStreamReader( is, StandardCharsets.UTF_8 ) )
        {
            String fileSource = ApplicationUtils.readAsString( is );
            JSONArray cxJson = new JSONArray( fileSource );
            for( int i = 0; i < cxJson.length(); i++ )
            {
                JSONObject innerObj = cxJson.getJSONObject( i );
                Set<String> names = Stream.of( JSONObject.getNames( innerObj ) ).collect( Collectors.toSet() );
                if( names.contains( CytoscapeConstants.NODES ) )
                    nodesJson = innerObj.getJSONArray( CytoscapeConstants.NODES );
                else if( names.contains( CytoscapeConstants.EDGES ) )
                    edgesJson = innerObj.getJSONArray( CytoscapeConstants.EDGES );
                else if( names.contains( CytoscapeConstants.NETWORK_ATTRIBUTES ) )
                    networkAttrsJson = innerObj.getJSONArray( CytoscapeConstants.NETWORK_ATTRIBUTES );
                else if( names.contains( CytoscapeConstants.NODE_ATTRIBUTES ) )
                    nodeAttrsJson = innerObj.getJSONArray( CytoscapeConstants.NODE_ATTRIBUTES );
                else if( names.contains( CytoscapeConstants.EDGE_ATTRIBUTES ) )
                    edgeAttrsJson = innerObj.getJSONArray( CytoscapeConstants.EDGE_ATTRIBUTES );
                else if( names.contains( CytoscapeConstants.CARTESIAN_LAYOUT ) )
                    layoutJson = innerObj.getJSONArray( CytoscapeConstants.CARTESIAN_LAYOUT );
                else if( names.contains( CytoscapeConstants.NUMBER_VERIFICATION ) )
                    verificationJson = innerObj.getJSONArray( CytoscapeConstants.NUMBER_VERIFICATION );
            }
        }
        catch( IOException | JSONException e )
        {
            log.log( Level.SEVERE, "Import error.", e );
            if( jobControl != null )
                jobControl.functionTerminatedByError( e );
            return null;
        }

        if( !verify( verificationJson ) )
            return null;

        if( networkAttrsJson == null )
        {
            log.log( Level.SEVERE, getMissedOrEmptyMessage( CytoscapeConstants.NETWORK_ATTRIBUTES ) );
            return null;
        }
        if( jobControl != null )
            jobControl.setPreparedness( 25 );

        if( nodesJson == null )
        {
            log.log( Level.SEVERE, getMissedOrEmptyMessage( CytoscapeConstants.NODES ) );
            return null;
        }
        TLongObjectMap<CXNode> cxNodes = processNodes( nodesJson );
        processElementAttributes( cxNodes, nodeAttrsJson, CytoscapeConstants.NODES );
        processLayout( cxNodes, layoutJson );
        if( jobControl != null )
            jobControl.setPreparedness( 50 );

        TLongObjectMap<CXEdge> cxEdges = processEdges( edgesJson );
        processElementAttributes( cxEdges, edgeAttrsJson, CytoscapeConstants.EDGES );
        if( jobControl != null )
            jobControl.setPreparedness( 60 );

        DynamicPropertySet diagramDPS = processNetworkAttributes( networkAttrsJson );
        Diagram result = createDiagram( parent, diagramName, diagramDPS );
        CollectionFactoryUtils.save( result );
        try
        {
            result = fillDiagram( result, cxNodes, cxEdges );
        }
        catch( Exception e )
        {
            parent.remove( diagramName );
            throw e;
        }
        if( jobControl != null )
            jobControl.setPreparedness( 90 );
        CollectionFactoryUtils.save( result );
        if( jobControl != null )
        {
            jobControl.setPreparedness( 100 );
            jobControl.functionFinished();
        }
        return result;
    }

    private @Nonnull Diagram createDiagram(@Nonnull DataCollection<?> parent, String diagramName, DynamicPropertySet diagramDPS)
            throws Exception
    {
        Diagram diagram = new SbgnExDiagramType().createDiagram( parent, diagramName, new DiagramInfo( null, diagramName ) );

        String title = diagramDPS.getValueAsString( "name" );
        if( title != null )
            diagram.setTitle( title );
        String description = diagramDPS.getValueAsString( "description" );
        if( description != null )
            diagram.setDescription( description );

        for( DynamicProperty dp : diagramDPS )
            diagram.getAttributes().add( dp );

        diagram.getViewOptions().setNodeTitleLimit( 100 );
        diagram.getViewOptions().setNodeTitleFont( new ColorFont( "Arial", Font.PLAIN, 12, Color.black ) );
        ( (SbgnDiagramViewOptions)diagram.getViewOptions() ).setShrinkNodeTitleSize( true );

        return diagram;
    }

    private @Nonnull Diagram fillDiagram(@Nonnull Diagram diagram, TLongObjectMap<CXNode> cxNodes, TLongObjectMap<CXEdge> cxEdges)
            throws Exception
    {
        SbgnExSemanticController controller = (SbgnExSemanticController) ( (SbgnExDiagramType)diagram.getType() ).getSemanticController();
        TLongObjectMap<CXReactionNode> reactionNodes = new TLongObjectHashMap<>();
        for( CXNode cxNode : cxNodes.valueCollection() )
        {
            if( "BiochemicalReaction".equals( cxNode.getBioPAXType() ) )
                reactionNodes.put( cxNode.getID(), new CXReactionNode( cxNode ) );
            else
                createAndPutSpecieNode( diagram, controller, cxNode );
        }

        //Since there can be semantic edges between reactions we should add all nodes before adding edges
        for( CXEdge cxEdge : cxEdges.valueCollection().stream().collect( Collectors.toSet() ) )
        {
            //filter reaction edges and add them to CXReaction
            if( checkAndAddReactionEdge( cxEdge, reactionNodes ) )
                cxEdges.remove( cxEdge.getID() );
        }
        for( CXReactionNode cxRNode : reactionNodes.valueCollection() )
            createAndPutReactionNode( diagram, controller, cxRNode );

        for( CXEdge cxEdge : cxEdges.valueCollection() )
            createAndPutRelationEdge( diagram, cxEdge );

        return diagram;
    }

    private void createAndPutReactionNode(@Nonnull Diagram diagram, SbgnExSemanticController sc, CXReactionNode cxRNode)
    {
        String reactionName = CX_NODE_PREFIX + cxRNode.getID();
        Reaction template = new Reaction( null, reactionName );
        for( CXEdge cxEdge : cxRNode.getCXEdges() )
        {
            String role = "";
            String type = "";
            String biopaxType = cxEdge.getBioPAXType();
            switch( biopaxType )
            {
                case CytoscapeConstants.CX_EDGE_PRODUCT:
                    role = SpecieReference.PRODUCT;
                    break;
                case CytoscapeConstants.CX_EDGE_REACTANT:
                    role = SpecieReference.REACTANT;
                    break;
                case CytoscapeConstants.CX_EDGE_MODIFIER_INHIBITION:
                    role = SpecieReference.MODIFIER;
                    type = biouml.plugins.sbgn.Type.TYPE_INHIBITION;
                    break;
                case CytoscapeConstants.CX_EDGE_MODIFIER_ACTIVATION:
                    role = SpecieReference.MODIFIER;
                    type = biouml.plugins.sbgn.Type.TYPE_CATALYSIS;
                    break;
                default:
                    break;
            }
            if( role.isEmpty() )
            {
                log.log( Level.WARNING, "Incorrect reaction edge type for edge '" + cxEdge.getID() );
                continue;
            }
            String specieName = CX_NODE_PREFIX +  (cxEdge.getSource() == cxRNode.getID() ? cxEdge.getTarget() : cxEdge.getSource() );
            SpecieReference sr = new SpecieReference(template, reactionName, specieName, role);
            if( !type.isEmpty() )
                sr.getAttributes().add( new DynamicProperty( SBGNPropertyConstants.SBGN_EDGE_TYPE, String.class, type ) );
            sr.setSpecie( specieName );
            template.put( sr );
        }

        DiagramElementGroup reactionElements = sc.createInstance( diagram, Reaction.class, reactionName, cxRNode.getLocation(), template );
        Node reactionNode = (Node)reactionElements.getElement( Util::isReaction );
        for( Node n : reactionElements.nodesStream() )
            diagram.put( n );
        for( Edge e : reactionElements.edgesStream() )
            diagram.put( e );
        reactionNode.setTitle( cxRNode.getName() );
        for( DynamicProperty dp : cxRNode.getAttributes() )
            reactionNode.getAttributes().add( dp );
    }

    /**
     * Check if edge is reaction edge and add it to correspondent reaction.
     * Return <code>true</code> if edge was added.
     */
    private boolean checkAndAddReactionEdge(CXEdge cxEdge, TLongObjectMap<CXReactionNode> reactionNodes)
    {
        String biopaxType = cxEdge.getBioPAXType();
        long cxEdgeSource = cxEdge.getSource();
        long cxEdgeTarget = cxEdge.getTarget();
        if( CytoscapeConstants.isCXReactionEdge( biopaxType ) )
        {
            CXReactionNode reactionS = reactionNodes.get( cxEdgeSource );
            CXReactionNode reactionT = reactionNodes.get( cxEdgeTarget );
            //add edge to reaction only if just one end is reaction
            if( ( reactionS == null ) == ( reactionT == null ) )
                return false;
            if( reactionS != null )
                reactionS.addEdge( cxEdge );
            else if( reactionT != null )
                reactionT.addEdge( cxEdge );
            return true;
        }
        return false;
    }

    private void createAndPutSpecieNode(@Nonnull Diagram diagram, SbgnExSemanticController sc, CXNode cxNode)
    {
        Node node = (Node)sc.createInstance( diagram, Specie.class, CX_NODE_PREFIX + cxNode.getID(), cxNode.getLocation(), null )
                .getElement();
        if( !cxNode.getName().isEmpty() )
            node.setTitle( cxNode.getName() );
        String biopaxType = cxNode.getBioPAXType();
        String type = getSpecieTypeByBioPAX( biopaxType );
        if( type != null && !type.isEmpty() )
            ( (Specie)node.getKernel() ).setType( type );

        //is necessary, because SbgnSemanticController ignores location from createInstance
        node.setLocation( cxNode.getLocation() );

        DiagramElement prevNode = diagram.put( node );
        if( prevNode != null )
            log.log( Level.WARNING, "Network contains nodes with duplicated ID: '" + prevNode.getName().replace( CX_NODE_PREFIX, "" ) );

        //add attributes after put, because some of them are processed by listeners
        for( DynamicProperty dp : cxNode.getAttributes() )
            node.getAttributes().add( dp );
    }
    private String getSpecieTypeByBioPAX(String biopaxType)
    {
        if( biopaxType == null )
            return null;
        switch( biopaxType )
        {
            case "RNA":
                return biouml.plugins.sbgn.Type.TYPE_NUCLEIC_ACID_FEATURE;
            case "Complex":
                return biouml.plugins.sbgn.Type.TYPE_COMPLEX;
            case "SmallMolecule":
                return biouml.plugins.sbgn.Type.TYPE_SIMPLE_CHEMICAL;
            case "Protein":
                return biouml.plugins.sbgn.Type.TYPE_MACROMOLECULE;
            case "PhysicalEntity":
                return biouml.plugins.sbgn.Type.TYPE_PERTURBING_AGENT;
            default:
                return null;
        }
    }

    private void createAndPutRelationEdge(@Nonnull Diagram diagram, CXEdge cxEdge)
    {
        long cxID = cxEdge.getID();
        long cxEdgeSource = cxEdge.getSource();
        long cxEdgeTarget = cxEdge.getTarget();

        Node input = diagram.findNode( CX_NODE_PREFIX + cxEdgeSource );
        if( input == null )
        {
            log.log( Level.WARNING, "Edge '" + cxID + "' has incorrect input '" + cxEdgeSource + "'. It will be skipped." );
            return;
        }
        Node output = diagram.findNode( CX_NODE_PREFIX + cxEdgeTarget );
        if( output == null )
        {
            log.log( Level.WARNING, "Edge '" + cxID + "' has incorrect output '" + cxEdgeTarget + "'. It will be skipped." );
            return;
        }

        Edge edge = new Edge( CX_EDGE_PREFIX + cxID, new SemanticRelation( null, CX_EDGE_PREFIX + cxID ), input, output );
        for( DynamicProperty dp : cxEdge.getAttributes() )
            edge.getAttributes().add( dp );
        edge.setTitle( "" );
        edge.getAttributes().add( new DynamicProperty( CytoscapeConstants.INTERACTION_ATTRIBUTE, String.class, cxEdge.getInteraction() ) );

        DiagramElement prevEdge = diagram.put( edge );
        if( prevEdge != null )
            log.log( Level.WARNING, "Network contains edges with duplicated ID: '" + prevEdge.getName().replace( CX_EDGE_PREFIX, "" ) );
    }

    private static String getMissedOrEmptyMessage(String aspect)
    {
        return "Invalid input file: missed or empty '" + aspect + "' aspect.";
    }

    private boolean verify(JSONArray numberVerification)
    {
        if( numberVerification == null )
        {
            log.log( Level.SEVERE, getMissedOrEmptyMessage( CytoscapeConstants.NUMBER_VERIFICATION ) );
            return false;
        }
        for( int i = 0; i < numberVerification.length(); i++ )
        {
            JSONObject obj = numberVerification.optJSONObject( i );
            if( obj == null )
                continue;
            long verificationValue = obj.optLong( CytoscapeConstants.LONG_NUMBER_KEY, -1 );
            if( verificationValue == -1 )
                continue;
            boolean verificationSuccess = verificationValue == CytoscapeConstants.LONG_NUMBER_VALUE;
            if( !verificationSuccess )
            {
                log.log( Level.SEVERE, "Number verification failed: " + verificationValue + " not equals expected "
                        + CytoscapeConstants.LONG_NUMBER_VALUE );
            }
            return verificationSuccess;
        }
        log.log( Level.SEVERE, "Invalid input file: incorrect 'numberVerification' aspect." );
        return false;
    }

    private @Nonnull TLongObjectMap<CXNode> processNodes(@Nonnull JSONArray nodes)
    {
        TLongObjectMap<CXNode> result = new TLongObjectHashMap<>();
        for( int i = 0; i < nodes.length(); i++ )
        {
            JSONObject nodeObj = nodes.getJSONObject( i );
            try
            {
                CXNode cxNode = CXNode.fromJSON( nodeObj );
                result.put( cxNode.getID(), cxNode );
            }
            catch( IllegalArgumentException e )
            {
                log.log( Level.WARNING, "Incorrect node element", e );
            }
        }
        return result;
    }

    private @Nonnull TLongObjectMap<CXEdge> processEdges(JSONArray edges)
    {
        TLongObjectMap<CXEdge> result = new TLongObjectHashMap<>();
        if( edges == null )
            return result;
        for( int i = 0; i < edges.length(); i++ )
        {
            JSONObject edgeObj = edges.getJSONObject( i );
            try
            {
                CXEdge cxEdge = CXEdge.fromJSON( edgeObj );
                result.put( cxEdge.getID(), cxEdge );
            }
            catch( IllegalArgumentException e )
            {
                log.log( Level.WARNING, "Incorrect edge element", e );
            }
        }
        return result;
    }

    private void processElementAttributes(@Nonnull TLongObjectMap<? extends CXElement> elements, JSONArray attrsArray, String elementType)
    {
        if( attrsArray == null )
            return;
        for( int i = 0; i < attrsArray.length(); i++ )
        {
            JSONObject attrObj = attrsArray.getJSONObject( i );

            long elementID;
            DynamicProperty dp;
            try
            {
                elementID = CXElement.getLongMandatoryValue( CytoscapeConstants.ATTRIBUTE_ELEMENT_ID_KEY, attrObj,
                        CXElement.ATTRIBUTE_TYPE );
                dp = CXElement.readAttributeFromJSON( attrObj );
            }
            catch( IllegalArgumentException e )
            {
                log.log( Level.WARNING, "Incorrect attribute of " + elementType, e );
                continue;
            }

            CXElement cxElement = elements.get( elementID );
            if( cxElement == null )
            {
                log.log( Level.WARNING, "Attribute to missed element of type '" + elementType + "'" );
                continue;
            }
            cxElement.addAttribute( dp );
        }
    }

    private DynamicPropertySet processNetworkAttributes(@Nonnull JSONArray networkAttrs)
    {
        DynamicPropertySet dps = new DynamicPropertySetAsMap();
        for( int i = 0; i < networkAttrs.length(); i++ )
        {
            try
            {
                JSONObject attrObj = networkAttrs.getJSONObject( i );
                DynamicProperty dp = CXElement.readAttributeFromJSON( attrObj );
                dps.add( dp );
            }
            catch( IllegalArgumentException e )
            {
                log.log( Level.WARNING, "Incorrect network attribute", e );
            }
        }
        return dps;
    }

    private void processLayout(@Nonnull TLongObjectMap<CXNode> nodes, JSONArray layout)
    {
        if( layout == null )
            return;
        for( int i = 0; i < layout.length(); i++ )
        {
            JSONObject object = layout.getJSONObject( i );
            long elementID;
            int x;
            int y;
            try
            {
                elementID = CXElement.getLongMandatoryValue( CytoscapeConstants.LAYOUT_NODE_KEY, object, CXElement.LAYOUT_TYPE );
                x = (int)CXElement.getDoubleMandatoryValue( CytoscapeConstants.LAYOUT_X_KEY, object, CXElement.LAYOUT_TYPE );
                y = (int)CXElement.getDoubleMandatoryValue( CytoscapeConstants.LAYOUT_Y_KEY, object, CXElement.LAYOUT_TYPE );
            }
            catch( IllegalArgumentException e )
            {
                log.log( Level.WARNING, "Incorrect layout element", e );
                continue;
            }

            CXNode node = nodes.get( elementID );
            if( node == null )
            {
                log.log( Level.WARNING, "Position of non existing element " + elementID );
                continue;
            }
            node.setLocation( x, y );
        }
    }

}
