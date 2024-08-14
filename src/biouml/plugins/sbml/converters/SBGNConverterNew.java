package biouml.plugins.sbml.converters;

import java.awt.Dimension;
import java.awt.Point;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Stream;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementStyle;
import biouml.model.DiagramType;
import biouml.model.Edge;
import biouml.model.ModelDefinition;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.SemanticController;
import biouml.model.SubDiagram;
import biouml.model.SubDiagram.PortOrientation;
import biouml.model.dynamics.VariableRole;
import biouml.model.xml.JSUtility;
import biouml.plugins.sbgn.SBGNPropertyConstants;
import biouml.plugins.sbgn.SbgnCompositeDiagramType;
import biouml.plugins.sbgn.SbgnSemanticController;
import biouml.plugins.sbgn.SbgnUtil;
import biouml.plugins.sbml.SbmlConstants;
import biouml.plugins.sbml.SbmlDiagramType_L3v2;
import biouml.plugins.sbml.SbmlSemanticController;
import biouml.plugins.sbml.composite.SbmlCompositeDiagramType;
import biouml.plugins.sbml.extensions.RdfExtensionReader;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.type.Base;
import biouml.standard.type.BaseSupport;
import biouml.standard.type.DatabaseReference;
import biouml.standard.type.Reaction;
import biouml.standard.type.Referrer;
import biouml.standard.type.Specie;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import biouml.standard.type.Stub.ConnectionPort;
import biouml.standard.type.Stub.InputConnectionPort;
import biouml.standard.type.Stub.OutputConnectionPort;
import biouml.standard.type.Type;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.exception.InternalException;
import ru.biosoft.graph.Path;
import ru.biosoft.util.TextUtil;

/**
 * SBML/SBGN converter
 */
public class SBGNConverterNew extends SBGNConverterSupport
{

    public static final String EMPTY_SET = "EmptySet";
    public static final String COMPLEX_ATTR = "ComplexElements";
    public static final String MODIFICATION_ATTR = "ModificationElements";
    public static final String BODY_COLOR_ATTR = "BodyColor";
    public static final String ANGLE_ATTR = "Angle";
    public static final String ALIAS_ATTR = "Alias";
    public static final String ALIASES_ATTR = "NodeAliases";
    public static final String SPECIE_NAME_ATTR = "SpecieName"; //species name for edge with possible aliases

    public static Diagram convert(Diagram diagram) throws Exception
    {
        return DiagramUtility.isComposite( diagram ) ? new SBGNCompositeConverter().convert( diagram, null ) : new SBGNConverterNew()
                .convert( diagram, null );
    }

    public static Diagram restore(Diagram diagram) throws Exception
    {
        if( diagram.getType() instanceof SbgnCompositeDiagramType || DiagramUtility.containPorts( diagram ))
        {
            Diagram sbmlDiagram = new SBGNCompositeConverter().restoreSBML( diagram, new SbmlCompositeDiagramType() );
            SbmlSemanticController.addPackage( sbmlDiagram, "comp" );
            return sbmlDiagram;
        }

        String type = (String)diagram.getAttributes().getValue( "baseDiagramType" );
        if( type == null )
            type = SbmlDiagramType_L3v2.class.getName();

        return new SBGNConverterNew().restoreSBML( diagram, ClassLoading.loadSubClass( type, DiagramType.class ).newInstance() );
    }

    @Override
    protected void createElements(Compartment baseCompartment, Compartment compartment) throws Exception
    {
        List<String> elementNames = baseCompartment.getNameList();
        for( String name : elementNames )
        {
            DiagramElement de = baseCompartment.get( name );
            createElement( de, compartment, name );
            if( ( de instanceof Node ) && ( de.getAttributes().getValue( ALIASES_ATTR ) instanceof Node[] ) )
            {
                //process aliases
                for( Node n : (Node[])de.getAttributes().getValue( ALIASES_ATTR ) )
                    createElement( n, compartment, n.getName() );
            }
        }
    }

    @Override
    protected void postProcess(Diagram oldDiagram, Diagram diagram)
    {
        super.postProcess( oldDiagram, diagram );
        SemanticController controller = diagram.getType().getSemanticController();

        for( Node n : diagram.recursiveStream().select( Node.class ).filter( node -> node.getKernel() instanceof Reaction ) )
        {
            try
            {
                controller.validate( n.getCompartment(), n ).save(); //TODO: take into account that validate contract implies that node can be totally recreated (though not in this case)
            }
            catch( Exception ex )
            {
                log.log(Level.SEVERE,  "Error during node " + n.getName() + " post processing. " + ex.getMessage() );
            }
        }
    }

    protected void createElement(DiagramElement de, Compartment compartment, String name) throws Exception
    {
        Base kernel = de.getKernel();
        String type = kernel.getType();
        Node newNode = null;
        if( kernel instanceof biouml.standard.type.Compartment )
        {
            newNode = createCompartmentClone( compartment, (Compartment)de, name );
            createElements( (Compartment)de, (Compartment)newNode );
        }
        else if( de instanceof Node )
        {
            Node node = (Node)de;
            if( type.equals(Type.MATH_EVENT) || type.equals(Type.MATH_FUNCTION) || type.equalsIgnoreCase(Type.MATH_EQUATION)
                    || type.equalsIgnoreCase(Type.MATH_CONSTRAINT) )
            {
                newNode = createNodeClone( compartment, node, name );
            }
            else if( kernel instanceof Reaction )
            {
                newNode = createNodeClone( compartment, node, name );
                newNode.getAttributes().add(
                        new DynamicProperty( SBGNPropertyConstants.SBGN_REACTION_TYPE_PD, String.class, guessReactionType( node ) ) );
            }
            else
            {
                newNode = new Compartment( compartment, name, kernel );
                newNode.setLocation( node.getLocation() );
                newNode.setVisible( node.isVisible() );
                Role role = node.getRole();
                if( role != null )
                    newNode.setRole( role.clone( newNode ) );
                newNode.setTitle( node.getTitle() );
                newNode.setComment( node.getComment() );
                newNode.setPredefinedStyle(node.getPredefinedStyle());
                newNode.setUseCustomImage(node.isUseCustomImage());
                if( newNode.isUseCustomImage() )
                    newNode.setImage(node.getImage().clone());

                if( newNode.getPredefinedStyle().equals(DiagramElementStyle.STYLE_NOT_SELECTED) )
                    newNode.setCustomStyle(node.getCustomStyle().clone());
                compartment.put( newNode );

                if( kernel instanceof Specie )
                {
                    String sbgnType = guessSpecieType( node );
                    SbgnUtil.setSBGNTypes( (Specie)kernel );
                    ( (Specie)kernel ).setType( sbgnType );
                }

                if( checkComplex( node ) )
                {
                    buildComplex( (Compartment)newNode, node );
                }
                else
                {
                    Dimension d = node.getShapeSize();
                    newNode.setShapeSize( d != null ? d : new Dimension( 60, 40 ) );

                    //try to create states (for example, CellDesignerExtension set this property)
                    Object states = de.getAttributes().getValue( "states" );
                    if( states != null )
                        createStates( newNode, (String)states );
                }

                //add sub elements to complexes
                addComplexElements( node, (Compartment)newNode );

                //add modifications to node
                Object modifications = node.getAttributes().getValue( MODIFICATION_ATTR );
                if( ( modifications != null ) && ( modifications instanceof Node[] ) )
                {
                    Compartment c = (Compartment)newNode;
                    for( Node subElement : (Node[])modifications )
                        c.put( subElement.clone( c, subElement.getName() ) );
                    locateModifications( c );
                }
            }
        }

        if( newNode != null )
        {
            copyAttributes( de, newNode );
            SbgnSemanticController.setNeccessaryAttributes( newNode );
        }
    }


    public void buildComplex(Compartment newNode, Node oldNode)
    {
        //try to build complex by database reference info
        int childCount = 0;
        if( oldNode.getKernel() instanceof Referrer )
        {
            //try to
            DatabaseReference[] dbRefs = ( (Referrer)oldNode.getKernel() ).getDatabaseReferences();
            if( dbRefs != null )
            {
                for( DatabaseReference ref : dbRefs )
                {
                    if( ref.getRelationshipType() != null && ref.getRelationshipType().equals( RdfExtensionReader.HASPART_ELEMENT ) )
                    {
                        childCount++;
                        String refName = ref.getId();
                        DataElement childDE = CollectionFactory.getDataElement( "databases/UniProt/Data/protein/" + ref.getId() );
                        if( childDE instanceof BaseSupport )
                            refName = ( (BaseSupport)childDE ).getTitle();

                        Node child = new Compartment( newNode, new Specie( null, refName, "macromolecule" ) );
                        child.setShapeSize( new Dimension( 60, 30 ) );
                        child.setLocation( newNode.getLocation().x + 10, newNode.getLocation().y + ( 30 * ( childCount - 1 ) + 10 ) );
                        SbgnSemanticController.setNeccessaryAttributes( child );
                        newNode.put( child );
                    }
                }
            }
        }
        newNode.setShapeSize( new Dimension( 80, 30 + childCount * 30 ) );
    }

    /**
     * Process complex elements recursively
     * @param de
     * @param newNode
     * @throws Exception
     */
    protected void addComplexElements(DiagramElement de, Compartment newNode) throws Exception
    {
        Object complexes = de.getAttributes().getValue( COMPLEX_ATTR );
        if( ( complexes != null ) && ( complexes instanceof Node[] ) )
        {
            for( Node subElement : (Node[])complexes )
            {
                DiagramElement newSubElement = subElement.clone( newNode, subElement.getName() );
                newNode.put( newSubElement );
                if( newSubElement instanceof Compartment )
                {
                    locateModifications( (Compartment)newSubElement );
                    addComplexElements( subElement, (Compartment)newSubElement );
                }
                Object aliases = subElement.getAttributes().getValue( SBGNConverter.ALIASES_ATTR );
                if( aliases instanceof Node[] )
                {
                    for( Node n : (Node[])aliases )
                    {
                        newSubElement = n.clone( newNode, n.getName() );
                        newNode.put( newSubElement );
                        if( newSubElement instanceof Compartment )
                        {
                            locateModifications( (Compartment)newSubElement );
                            addComplexElements( n, (Compartment)newSubElement );
                        }
                    }
                }
            }
        }
    }

    /**
     * Locate inner modifications
     */
    protected void locateModifications(Compartment node)
    {
        for( DiagramElement de : node )
        {
            if( de instanceof Node )
            {
                Object angleObj = ( (Node)de ).getAttributes().getValue( ANGLE_ATTR );
                if( angleObj != null )
                {
                    try
                    {
                        double angle = Math.PI - Double.parseDouble( angleObj.toString() );
                        Point location = new Point();
                        Dimension nodeSize = node.getShapeSize();
                        JSUtility.fillLocationByAngle( location, new Dimension( nodeSize.width, nodeSize.width ), new Dimension( 20, 20 ),
                                angle, false );
                        Point nodeLocation = node.getLocation();
                        location.x += nodeLocation.x;
                        location.y = (int) ( location.y * ( (double)nodeSize.height / (double)nodeSize.width ) ) + nodeLocation.y;
                        ( (Node)de ).setLocation( location );
                    }
                    catch( Exception e )
                    {
                        log.log(Level.SEVERE,  "Can not locate modification", e );
                    }
                }
            }
        }
    }

    protected void createStates(Node node, String states)
    {
        if( node instanceof Compartment )
        {
            String[] stateArray = TextUtil.split( states, ';' );
            for( String state : stateArray )
            {
                Stub stateKenel = new Stub( null, node.getName() + "_" + state, "variable" );
                Node stateNode = new Node( (Compartment)node, stateKenel );
                stateNode.setLocation( node.getLocation() );
                stateNode.getAttributes().add( new DynamicProperty( "value", String.class, state ) );
                ( (Compartment)node ).put( stateNode );
            }
        }
    }

    @Override
    protected void createEdges(Compartment baseCompartment, Compartment compartment, Diagram sbgnDiagram) throws Exception
    {
        List<String> elementNames = baseCompartment.getNameList();
        for( String name : elementNames )
        {
            DiagramElement de = baseCompartment.get( name );
            if( de instanceof Compartment )
            {
                createEdges( (Compartment)de, (Compartment)compartment.get( name ), sbgnDiagram );
            }
            else if( ( de instanceof Node ) && ( de.getKernel() instanceof Reaction ) )
            {
                for( Edge edge : ( (Node)de ).getEdges() )
                {
                    if( edge.getKernel() instanceof SpecieReference )
                    {
                        SpecieReference sr = (SpecieReference)edge.getKernel();
                        Node node1 = (Node)compartment.get( name );

                        String completeName = null;
                        if( edge.getAttributes().getValue( SPECIE_NAME_ATTR ) instanceof String )
                            completeName = (String)edge.getAttributes().getValue( SPECIE_NAME_ATTR );
                        else
                            completeName = edge.getInput() == de ? edge.getOutput().getCompleteNameInDiagram() : edge.getInput()
                                    .getCompleteNameInDiagram();

                        Node node2 = sbgnDiagram.findNode( completeName );
                        if( node1 != null && node2 != null )
                        {
                            Edge newEdge = null;
                            if( sr.getRole().equals( SpecieReference.REACTANT ) )
                            {
                                newEdge = new Edge( edge.getName(), sr, node2, node1 );
                            }
                            else if( sr.getRole().equals( SpecieReference.PRODUCT ) )
                            {
                                newEdge = new Edge( edge.getName(), sr, node1, node2 );
                            }
                            else if( sr.getRole().equals( SpecieReference.MODIFIER ) )
                            {
                                newEdge = new Edge( edge.getName(), sr, node2, node1 );
                                Object oldEdgeType = edge.getAttributes().getValue( SBGNPropertyConstants.SBGN_EDGE_TYPE );
                                newEdge.getAttributes().add(
                                        new DynamicProperty( SBGNPropertyConstants.SBGN_EDGE_TYPE_PD, String.class, oldEdgeType == null
                                                ? "catalysis" : oldEdgeType.toString() ) );
                            }
                            if( newEdge != null )
                            {
                                newEdge.setRole( edge.getRole() );
                                Path path = edge.getPath();
                                if( path != null )
                                    newEdge.setPath( path );

                                copyAttributes( edge, newEdge );
                                newEdge.save();
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void updateEdgeConnections(final Diagram diagram, Compartment compartment)
    {
        compartment.visitEdges( edge -> diagram.getType().getSemanticController().recalculateEdgePath( edge ) );
    }

    protected boolean checkComplex(DiagramElement de)
    {
        if( de.getKernel() instanceof Referrer )
        {
            DatabaseReference[] dbRefs = ( (Referrer)de.getKernel() ).getDatabaseReferences();
            if( dbRefs != null )
                return Stream.of( dbRefs ).map( DatabaseReference::getRelationshipType )
                        .anyMatch( RdfExtensionReader.HASPART_ELEMENT::equals );
        }
        return false;
    }

    private static int parseSBO(String sboTerm)
    {
        if( sboTerm == null || !sboTerm.startsWith( "SBO:" ) )
            return -1;
        try
        {
            return Integer.parseInt( sboTerm.substring( "SBO:".length() ) );
        }
        catch( NumberFormatException e )
        {
            return -1;
        }
    }

    protected String guessSpecieType(Node baseNode)
    {
        Object entityType = baseNode.getAttributes().getValue( SBGNPropertyConstants.SBGN_ENTITY_TYPE );
        if( entityType != null )
            return (String)entityType;

        String type = baseNode.getKernel().getType();
        if( Type.TYPE_PROTEIN.equals( type ) )
            return "macromolecule";
        else if( Type.TYPE_GENE.equals( type ) || Type.TYPE_RNA.equals( type ) )
            return "nucleic acid feature";

        int sboTerm = parseSBO( baseNode.getAttributes().getValueAsString( SbmlConstants.SBO_TERM_ATTR ) );
        switch( sboTerm )
        {
            case 296:
            case 297:
                return "complex";
            case 247:
            case 280:
            case 299:
                return "simple chemical";
            case 14:
            case 245:
            case 246:
            case 248:
            case 249:
            case 251:
            case 252:
                return "macromolecule";
            case 317:
            case 250:
            case 404:
            case 243:
            case 278:
            case 334:
                return "nucleic acid feature";
            case 405:
                return "perturbing agent";
            default:
                break;
        }

        if( checkComplex( baseNode ) )
            return "complex";

        if( baseNode.getKernel() instanceof Referrer )
        {
            DatabaseReference[] dbRefs = ( (Referrer)baseNode.getKernel() ).getDatabaseReferences();
            if( dbRefs != null )
            {
                for( DatabaseReference dbRef : dbRefs )
                {
                    switch( dbRef.getRelationshipType() )
                    {
                        case RdfExtensionReader.ISENCODEDBY_ELEMENT:
                            return "macromolecule";
                        case RdfExtensionReader.ISVERSIONOF_ELEMENT:
                        case RdfExtensionReader.IS_ELEMENT:
                            switch( dbRef.getDatabaseName() )
                            {
                                case "MIR:00000011": // InterPro
                                case "MIR:00000005": // UniProt
                                    return "macromolecule";
                                case "MIR:00000002": // Chebi
                                case "MIR:00000033": // PubChem
                                    return "simple chemical";
                                case "MIR:00000051": // HMDB
                                    if( dbRef.getId().startsWith( "HMDBP" ) )
                                        return "macromolecule";
                                    if( dbRef.getId().matches( "HMDB\\d+" ) )
                                        return "simple chemical";
                                    break;
                                default:
                                    break;
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
        }
        return "unspecified";
    }

    @Override
    protected void restoreElements(Compartment sbgnCompartment, Compartment compartment) throws Exception
    {
        for( Node de : sbgnCompartment.getNodes() )
        {
            restoreElement(de, sbgnCompartment, compartment); 
        }
    }

    protected Node restoreElement(Node de, Compartment sbgnCompartment, Compartment compartment) throws Exception
    {
        Base kernel = de.getKernel();
        String type = kernel.getType();
        if( biouml.plugins.sbgn.Type.TYPE_LOGICAL.equals( type ) || biouml.plugins.sbgn.Type.TYPE_PHENOTYPE.equals( type )
                || biouml.plugins.sbgn.Type.TYPE_SOURCE_SINK.equals( type ) || biouml.plugins.sbgn.Type.TYPE_NOTE.equals( type )
                || biouml.plugins.sbgn.Type.TYPE_VARIABLE.equals( type ) || biouml.plugins.sbgn.Type.TYPE_UNIT_OF_INFORMATION.equals( type )
                || de instanceof ModelDefinition || de instanceof SubDiagram || Type.TYPE_REACTION.equals(  type ))
            return null; //these elements are not in SBML model

        if( kernel instanceof biouml.standard.type.Compartment )
        {
            Compartment newCompartment;

            if( !de.getRole( VariableRole.class ).getDiagramElement().equals( de ) )
            {
                Compartment sbgnMain = (Compartment)de.getRole( VariableRole.class ).getDiagramElement();
                Compartment sbmlMain = (Compartment)compartment.get( sbgnMain.getName() );
                if( sbmlMain == null ) // not convereted yet
                    sbmlMain = (Compartment)restoreElement( sbgnMain, sbgnCompartment, compartment );
                restoreElements( (Compartment)de, sbmlMain );
                return null;
            }
            else
                newCompartment = createCompartmentClone( compartment, (Compartment)de, de.getName() );
            restoreElements( (Compartment)de, newCompartment );
            return newCompartment;
        }
        else
        {
            if( type.equals( Type.MATH_EQUATION ) || type.equals( Type.MATH_EVENT ) || type.equals( Type.MATH_FUNCTION )
                    || type.equals( Type.MATH_CONSTRAINT ) )
                kernel = new Stub( compartment, de.getName(), type );

            if( de.getRole() instanceof VariableRole && !de.getRole( VariableRole.class ).getDiagramElement().equals( de ) )
                return null;
            
            Node node = new Node( compartment, kernel );

            if( de.getRole() != null )
                node.setRole( de.getRole( Role.class ).clone( node ) );
            node.setTitle( de.getTitle() );
            node.setLocation( de.getLocation() );
            node.setShapeSize( de.getShapeSize() );
            node.setVisible( de.isVisible() );
            node.setComment( de.getComment() );
            node.setUseCustomImage( de.isUseCustomImage() );
            if( node.isUseCustomImage() )
                node.setImage( de.getImage().clone() );
            for( DynamicProperty dp : de.getAttributes() )
            {
                if( isSbmlProperty( dp ) )
                    copyAttribute( de, node, dp.getName() );
            }
            node.save();
            return node;
        }
    }


    @Override
    protected void restoreEdges(Compartment sbgnCompartment, Compartment compartment, Diagram sbmlDiagram) throws Exception
    {
        for( DiagramElement de : sbgnCompartment )
        {
            if( de instanceof Node && de.getKernel() instanceof Reaction )
            {
                Reaction oldReaction = (Reaction)de.getKernel();
                Reaction newReaction = oldReaction.clone(null, oldReaction.getName());

                Node reactionNode = new Node( compartment, newReaction );
                newReaction.setParent(reactionNode);
                reactionNode.setTitle( de.getTitle() );
                reactionNode.setRole( de.getRole().clone( reactionNode ) );
                reactionNode.setLocation( ( (Node)de ).getLocation() );
                reactionNode.setVisible( ( (Node)de ).isVisible() );
                reactionNode.setComment(de.getComment());
                compartment.put( reactionNode );

                for( DynamicProperty dp : de.getAttributes() )
                {
                    if( isSbmlProperty( dp ) )
                        copyAttribute( de, reactionNode, dp.getName() );
                }

                for( SpecieReference sr : (Reaction)de.getKernel() )
                {
                    try
                    {
                        Node node = (Node)de;
                        Edge originalEdge = findEdge((Node)de, sr);

                        if( originalEdge == null )
                            originalEdge = node.edges().filter(edge -> SbgnUtil.isLogical(edge.getOtherEnd(node)))
                                    .flatMap(edge -> edge.getOtherEnd(node).edges()).filter(e -> e.getKernel().equals(sr)).findAny()
                                    .orElseThrow(() -> new InternalException(
                                            "Unable to find original edge for de=" + de.getCompleteNameInDiagram() + " and sr = " + sr));

                        SpecieReference newSr = sr.clone((Reaction)de.getKernel(), sr.getName());

                        Node sbmlNode = sbmlDiagram.findNode(sr.getSpecie());
                        if( sbmlNode == null ) //some sbgn nodes correspond to more then one sbml nodes
                        {
                            Node sbgnNode = Diagram.getDiagram(sbgnCompartment).findNode(sr.getSpecie());
                            Node masterNode = (Node)sbgnNode.getRole(Role.class).getDiagramElement();
                            sbmlNode = sbmlDiagram.findNode(masterNode.getName());
                        }

                        if( sbmlNode == null )
                            throw new Exception("Can not convert specie reference " + sr.getName() + ".");

                        newSr.setSpecie(sbmlNode.getCompleteNameInDiagram());
                        newReaction.put(newSr);
                        if( newSr.getRole().equals(SpecieReference.PRODUCT) )
                            restoreEdge(newSr, reactionNode, sbmlNode, originalEdge);
                        else
                            restoreEdge(newSr, sbmlNode, reactionNode, originalEdge);
                    }
                    catch( Exception ex )
                    {
                        log.log(Level.SEVERE, "Error during restoring specie reference " + sr.getName(), ex);
                    }
                }
            }
            else if( de instanceof Compartment )
            {
                DiagramElement comp = compartment.get( de.getName() );
                if( comp instanceof Compartment )
                    restoreEdges( (Compartment)de, (Compartment)comp, sbmlDiagram );
            }
        }
    }

    /**
     * Append ports from base diagram to current SBGN diagram
     */
    public void appendPorts(Diagram baseDiagram, Diagram sbgnDiagram) throws Exception
    {
        Iterator<DiagramElement> elements = baseDiagram.iterator();
        while( elements.hasNext() )
        {
            Object obj = elements.next();
            if( obj instanceof Node )
            {
                Base kernel = ( (Node)obj ).getKernel();
                if( ( kernel != null ) && ( kernel instanceof Stub.ConnectionPort ) )
                {
                    Node basePort = (Node)obj;
                    baseDiagram.findNode( basePort.getAttributes().getValueAsString( ConnectionPort.VARIABLE_NAME_ATTR ) );
                    String orientationStr = PortOrientation.getOrientation(
                            basePort.getAttributes().getValueAsString( PortOrientation.ORIENTATION_ATTR ) ).toDirection();

                    String portType = "contact";
                    if( kernel instanceof InputConnectionPort )
                    {
                        portType = "input";
                    }
                    else if( kernel instanceof OutputConnectionPort )
                    {
                        portType = "output";
                    }

                    String elName = basePort.getAttributes().getValue( ConnectionPort.VARIABLE_NAME_ATTR ).toString();
                    if( elName.startsWith( "$\"" ) && elName.endsWith( "\"" ) )
                    {
                        elName = elName.substring( 2, elName.length() - 1 );
                    }

                    String portName = elName.replaceAll( "\\.", "_" ) + Stub.ConnectionPort.SUFFIX;
                    Node newNode = new Node( sbgnDiagram, new Stub.ConnectionPort( portName, null, portType ) );
                    newNode.setTitle( basePort.getTitle() );
                    newNode.getAttributes().add( new DynamicProperty( "direction", String.class, orientationStr ) );
                    newNode.getAttributes().add( new DynamicProperty( "portType", String.class, portType ) );
                    newNode.getAttributes().add(
                            new DynamicProperty( ConnectionPort.VARIABLE_NAME_ATTR, String.class, basePort.getAttributes()
                                    .getValue( ConnectionPort.VARIABLE_NAME_ATTR ).toString() ) );
                    newNode.setLocation( basePort.getLocation() );
                    sbgnDiagram.put( newNode );

                    //create edge
                    Node targetNode = sbgnDiagram.findNode( elName );
                    if( targetNode != null )
                    {
                        Edge edge = new Edge( sbgnDiagram, new Stub( null, newNode.getName() + "_link", "portlink" ), newNode, targetNode );
                        sbgnDiagram.put( edge );
                    }
                }
            }
        }
    }
}
