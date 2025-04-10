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
import biouml.model.DiagramType;
import biouml.model.DiagramViewBuilder;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.SubDiagram.PortOrientation;
import biouml.model.xml.JSUtility;
import biouml.model.xml.XmlDiagramSemanticController;
import biouml.model.xml.XmlDiagramType;
import biouml.model.xml.XmlDiagramTypeConstants;
import biouml.plugins.sbgn.SBGNPropertyConstants;
import biouml.plugins.sbgn.SbgnUtil;
import biouml.plugins.sbml.SbmlConstants;
import biouml.plugins.sbml.extensions.RdfExtensionReader;
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
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.graph.Path;
import ru.biosoft.util.TextUtil2;

/**
 * SBML/SBGN converter
 * converts to and from obsolete xml diagram type "sbml_sbgn". Should not be used now.
 */
@Deprecated
public class SBGNConverter extends SBGNConverterSupport
{
    public static final String SBGN_NOTATION_NAME = "sbml_sbgn.xml";

    public static final String EMPTY_SET = "EmptySet";
    public static final String COMPLEX_ATTR = "ComplexElements";
    public static final String MODIFICATION_ATTR = "ModificationElements";
    public static final String BODY_COLOR_ATTR = "BodyColor";
    public static final String ANGLE_ATTR = "Angle";
    public static final String ALIAS_ATTR = "Alias";
    public static final String ALIASES_ATTR = "NodeAliases";
    public static final String SPECIE_NAME_ATTR = "SpecieName"; //species name for edge with possible aliases


    protected String getNotationName()
    {
        return SBGN_NOTATION_NAME;
    }

    @Override
    protected Object getType()
    {
        return getNotationName();
    }

    @Override
    protected Diagram createDiagram(DiagramType type, Diagram oldDiagram) throws Exception
    {
        if( type instanceof XmlDiagramType )
        {
            XmlDiagramType xmlDiagramType = (XmlDiagramType)type;
            ( (XmlDiagramSemanticController)xmlDiagramType.getSemanticController() ).setPrototype( oldDiagram.getType()
                    .getSemanticController() );
            DiagramViewBuilder viewBuilder = xmlDiagramType.getDiagramViewBuilder();
            viewBuilder.setBaseViewBuilder( oldDiagram.getType().getDiagramViewBuilder() );
            viewBuilder.setBaseViewOptions( oldDiagram.getViewOptions() );
            return xmlDiagramType.createDiagram( oldDiagram.getOrigin(), oldDiagram.getName(), null );
        }
        return super.createDiagram( type, oldDiagram );
    }

    @Override
    protected void postProcessRestore(Diagram oldDiagram, Diagram diagram)
    {
        String notationPath = DataElementPath.create( XmlDiagramType.getTypeObject( SBGN_NOTATION_NAME ) ).toString();
        diagram.getAttributes().add( new DynamicProperty( notationPath, Diagram.class, oldDiagram ) );
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
                {
                    createElement( n, compartment, n.getName() );
                }
            }
        }
    }

    protected void createElement(DiagramElement de, Compartment compartment, String name) throws Exception
    {
        if( de instanceof Compartment )
        {
            Compartment newCompartment = new Compartment( compartment, name, de.getKernel() );
            setXmlType( newCompartment, "compartment" );
            newCompartment.setLocation( ( (Compartment)de ).getLocation() );
            newCompartment.setShapeSize( ( (Compartment)de ).getShapeSize() );
            newCompartment.setShapeType( ( (Compartment)de ).getShapeType() );
            newCompartment.setRole( de.getRole() );
            newCompartment.setTitle( de.getTitle() );
            newCompartment.setComment( de.getComment() );
//            copyAttribute( de, newCompartment, SBGNPropertyConstants.LINE_PEN_ATTR );
//            copyAttribute( de, newCompartment, SBGNPropertyConstants.LINE_IN_PEN_ATTR );
//            copyAttribute( de, newCompartment, SBGNPropertyConstants.LINE_OUT_PEN_ATTR );
            copyAttribute( de, newCompartment, SBGNPropertyConstants.NAME_POINT_ATTR );
            copyAttribute( de, newCompartment, SBGNPropertyConstants.CLOSEUP_ATTR );
            copyAttribute( de, newCompartment, SBGNPropertyConstants.TYPE_ATTR );
            copyAttribute( de, newCompartment, SbmlConstants.SBO_TERM_ATTR );
            compartment.put( newCompartment );
            createElements( (Compartment)de, newCompartment );
            copyAttribute( de, newCompartment, "metaid" );
        }
        else if( de instanceof Node )
        {
            Base kernel = de.getKernel();
            if( kernel.getType().equals( Type.MATH_EVENT ) )
            {
                Node newNode = createNodeClone( compartment, (Node)de, name, "event" );
                copyAttribute( de, newNode, "metaid" );
                compartment.put( newNode );
            }
            else if( kernel.getType().equals( Type.MATH_FUNCTION ) )
            {
                Node newNode = createNodeClone( compartment, (Node)de, name, "function" );
                copyAttribute( de, newNode, "metaid" );
                compartment.put( newNode );
            }
            else if( kernel.getType().equals( Type.MATH_EQUATION ) )
            {
                Node newNode = createNodeClone( compartment, (Node)de, name, "equation" );
                copyAttribute( de, newNode, "metaid" );
                compartment.put( newNode );
            }
            else if( kernel instanceof Reaction )
            {
                Reaction reaction = (Reaction)kernel;
                int reactants = 0;
                int products = 0;
                int modifiers = 0;
                for( SpecieReference sr : reaction.getSpecieReferences() )
                {
                    if( sr.getRole().equals( SpecieReference.REACTANT ) )
                    {
                        reactants++;
                    }
                    else if( sr.getRole().equals( SpecieReference.PRODUCT ) )
                    {
                        products++;
                    }
                    else if( sr.getRole().equals( SpecieReference.MODIFIER ) )
                    {
                        modifiers++;
                    }
                }

                String reactionType;
                Object rtObject = de.getAttributes().getValue( REACTION_TYPE_ATTR );
                if( rtObject != null )
                {
                    String s = rtObject.toString().toLowerCase();
                    if( s.endsWith( "association" ) )
                    {
                        reactionType = "association";
                    }
                    else if( s.endsWith( "dissociation" ) )
                    {
                        reactionType = "dissociation";
                    }
                    else
                    {
                        reactionType = "process";
                    }
                }
                else
                {
                    if( reactants > 1 && products == 1 )
                    {
                        reactionType = "association";
                    }
                    else if( reactants == 1 && products > 1 )
                    {
                        reactionType = "dissociation";
                    }
                    else
                    {
                        reactionType = "process";
                    }
                }

                Node reactionNode = createNodeClone( compartment, (Node)de, name, "reaction" );
                reactionNode.getAttributes().add(
                        new DynamicProperty( SBGNPropertyConstants.SBGN_REACTION_TYPE_PD, String.class, reactionType ) );
                copyAttribute( de, reactionNode, SbmlConstants.SBO_TERM_ATTR );
                if( reactants == 0  || products == 0)
                    SbgnUtil.generateSourceSink(reactionNode, true);
            }
            else
            {

                Compartment newNode = new Compartment( compartment, name, kernel );
                newNode.setLocation( ( (Node)de ).getLocation() );
                newNode.setVisible( ( (Node)de ).isVisible() );
                if( ( (Node)de ).getRole() != null )
                {
                    newNode.setRole( ( (Node)de ).getRole().clone( newNode ) );
                }
                newNode.setTitle( de.getTitle() );
                newNode.setComment( de.getComment() );
                compartment.put( newNode );

                Object type = de.getAttributes().getValue( XmlDiagramTypeConstants.XML_TYPE );

                if( type != null ) //TODO: check this.
                                   //very strange, why this type would exist in newly created SBML diagrams??
                {
                    if( type.equals( "source-sink" ) )
                    {
                        kernel = new Stub( null, kernel.getName(), "source-sink" );
                    }

                    if( type.equals( "complex" ) )
                    {
                        setXmlType( newNode, "complex" );
                        Dimension shapeSize = ( (Node)de ).getShapeSize();
                        if( shapeSize != null )
                            newNode.setShapeSize( shapeSize );
                    }
                }
                else if( checkComplex( de ) )
                {
                    //try to build complex by database reference info
                    setXmlType( newNode, "complex" );
                    int childCount = 0;
                    if( de.getKernel() instanceof Referrer )
                    {
                        //try to
                        DatabaseReference[] dbRefs = ( (Referrer)de.getKernel() ).getDatabaseReferences();
                        if( dbRefs != null )
                        {
                            for( DatabaseReference ref : dbRefs )
                            {
                                if( ref.getRelationshipType() != null
                                        && ref.getRelationshipType().equals( RdfExtensionReader.HASPART_ELEMENT ) )
                                {
                                    childCount++;
                                    String refName = ref.getId();
                                    DataElement childDE = CollectionFactory
                                            .getDataElement( "databases/UniProt/Data/protein/" + ref.getId() );
                                    if( childDE instanceof BaseSupport )
                                    {
                                        refName = ( (BaseSupport)childDE ).getTitle();
                                    }
                                    Node child = new Compartment( newNode, new Stub( null, refName, "entity" ) );
                                    child.getAttributes()
                                            .add( new DynamicProperty( SBGNPropertyConstants.SBGN_ENTITY_TYPE_PD, String.class,
                                                    "macromolecule" ) );
                                    child.setShapeSize( new Dimension( 60, 30 ) );
                                    child.setLocation( newNode.getLocation().x + 10, newNode.getLocation().y
                                            + ( 30 * ( childCount - 1 ) + 10 ) );
                                    newNode.put( child );
                                }
                            }
                        }
                    }
                    newNode.setShapeSize( new Dimension( 80, 30 + childCount * 30 ) );
                }
                else
                {
                    type = "entity";
                    setXmlType( newNode, (String)type );
                    newNode.getAttributes().add(
                            new DynamicProperty( SBGNPropertyConstants.SBGN_ENTITY_TYPE_PD, String.class,
                                    getElementTypeAttribute( (Node)de ) ) );
                    //type of specie is not impotant in SBGN annotation
                    copyAttribute( de, newNode, SbmlConstants.SBO_TERM_ATTR );
                    copyAttribute( de, newNode, SBGNPropertyConstants.SBGN_MULTIMER );
                    Dimension d = ( (Node)de ).getShapeSize();
                    if( d == null )
                        d = new Dimension( 60, 40 );
                    newNode.setShapeSize( d );

                    //try to create states (for example, CellDesignerExtension set this property)
                    Object states = de.getAttributes().getValue( "states" );
                    if( states != null )
                    {
                        createStates( newNode, (String)states );
                    }
                }

                //add sub elements to complexes
                addComplexElements( de, newNode );

                //add modifications to node
                Object modifications = de.getAttributes().getValue( MODIFICATION_ATTR );
                if( ( modifications != null ) && ( modifications instanceof Node[] ) )
                {
                    for( Node subElement : (Node[])modifications )
                    {
                        DiagramElement newSubElement = subElement.clone( newNode, subElement.getName() );
                        newNode.put( newSubElement );
                    }
                    locateModifications( newNode );
                }
                copyAttribute( de, newNode, "metaid" );
                copyAttribute( de, newNode, SbmlConstants.SBO_TERM_ATTR );
            }

        }
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
            String[] stateArray = TextUtil2.split( states, ';' );
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
                        {
                            completeName = (String)edge.getAttributes().getValue( SPECIE_NAME_ATTR );
                        }
                        else
                        {
                            if( edge.getInput() == de )
                            {
                                completeName = edge.getOutput().getCompleteNameInDiagram();
                            }
                            else
                            {
                                completeName = edge.getInput().getCompleteNameInDiagram();
                            }
                        }

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
                                if( ( node1.getAttributes().getValue( XmlDiagramTypeConstants.XML_TYPE ) != null && node1.getAttributes()
                                        .getValue( XmlDiagramTypeConstants.XML_TYPE ).equals( "source-sink" ) )
                                        || ( node2.getAttributes().getValue( XmlDiagramTypeConstants.XML_TYPE ) != null && node2
                                                .getAttributes().getValue( XmlDiagramTypeConstants.XML_TYPE ).equals( "source-sink" ) ) )
                                {
                                    newEdge = new Edge( newEdge.getOrigin(), edge.getName(), new Stub( sr.getOrigin(), edge.getName(),
                                            "production" ), newEdge.getInput(), newEdge.getOutput() );
                                }
                                newEdge.setRole( edge.getRole() );
                                Path path = edge.getPath();
                                if( path != null )
                                {
                                    newEdge.setPath( path );
                                }
                                DynamicProperty dp = new DynamicProperty( XmlDiagramTypeConstants.XML_TYPE_PD, String.class, "reaction" );
                                newEdge.getAttributes().add( dp );
                                newEdge.getAttributes().add(
                                        new DynamicProperty( XmlDiagramTypeConstants.KERNEL_ROLE_ATTR_PD, String.class, sr.getRole() ) );

//                                copyAttribute( edge, newEdge, SBGNPropertyConstants.LINE_PEN_ATTR );
//                                copyAttribute( edge, newEdge, SBGNPropertyConstants.LINE_IN_PEN_ATTR );
//                                copyAttribute( edge, newEdge, SBGNPropertyConstants.LINE_OUT_PEN_ATTR );
                                copyAttribute( edge, newEdge, "text" );

                                newEdge.save();
                            }
                        }
                    }
                }
            }
        }
    }

    protected Node createNodeClone(Compartment compartment, Node base, String name, String type) throws Exception
    {
        Node newNode = createNodeClone( compartment, base, name );
        setXmlType( newNode, type );
        return newNode;
    }


    protected boolean checkComplex(DiagramElement de)
    {
        if( de.getKernel() instanceof Referrer )
        {
            DatabaseReference[] dbRefs = ( (Referrer)de.getKernel() ).getDatabaseReferences();
            if( dbRefs != null )
            {
                return Stream.of( dbRefs ).map( DatabaseReference::getRelationshipType )
                        .anyMatch( RdfExtensionReader.HASPART_ELEMENT::equals );
            }
        }
        return false;
    }

    protected String getElementTypeAttribute(Node baseNode)
    {
        Object entityType = baseNode.getAttributes().getValue( SBGNPropertyConstants.SBGN_ENTITY_TYPE );
        if( entityType != null )
        {
            return (String)entityType;
        }
        if( baseNode.getKernel().getType().equals( Type.TYPE_PROTEIN ) )
            return "macromolecule";
        if( baseNode.getKernel() instanceof Referrer )
        {
            DatabaseReference[] dbRefs = ( (Referrer)baseNode.getKernel() ).getDatabaseReferences();
            if( dbRefs != null )
            {
                for( DatabaseReference dbRef : dbRefs )
                {
                    if( dbRef.getRelationshipType().equals( RdfExtensionReader.ISVERSIONOF_ELEMENT ) )
                    {
                        if( dbRef.getDatabaseName().equals( "MIR:00000011" ) //InterPro
                                || dbRef.getDatabaseName().equals( "MIR:00000005" ) ) //Uniprot
                        {
                            return "macromolecule";
                        }
                        if( dbRef.getDatabaseName().equals( "MIR:00000002" ) ) // Chebi
                        {
                            return "simple chemical";
                        }
                    }
                }
            }
        }
        return "unspecified";
    }

    protected void setXmlType(DiagramElement de, String type) throws Exception
    {
        DynamicProperty dp = new DynamicProperty( XmlDiagramTypeConstants.XML_TYPE_PD, String.class, type );
        de.getAttributes().add( dp );
    }

    @Override
    protected void restoreElements(Compartment sbgnCompartment, Compartment compartment) throws Exception
    {
        Iterator<DiagramElement> iter = sbgnCompartment.iterator();
        while( iter.hasNext() )
        {
            DiagramElement de = iter.next();
            if( de instanceof Node )
            {
                String xmlType = (String)de.getAttributes().getValue( XmlDiagramTypeConstants.XML_TYPE );
                if( xmlType != null && !xmlType.equals( "source-sink" ) && !xmlType.equals( "note" ) )
                {
                    if( xmlType.equals( "complex" ) || xmlType.equals( "entity" ) || xmlType.equals( "event" )
                            || xmlType.equals( "function" ) || xmlType.equals( "equation" ) || xmlType.equals( "phenotype" ) )
                    {
                        Base kernel = de.getKernel();
                        convertSbgnTypeToKernelType( (Node)de, kernel );
                        if( xmlType.equals( "equation" ) )
                        {
                            kernel = new Stub( compartment, de.getName(), Type.MATH_EQUATION );
                        }
                        else if( xmlType.equals( "event" ) )
                        {
                            kernel = new Stub( compartment, de.getName(), Type.MATH_EVENT );
                        }
                        else if( xmlType.equals( "function" ) )
                        {
                            kernel = new Stub( compartment, de.getName(), Type.MATH_FUNCTION );
                        }
                        else if( xmlType.equals( "complex" ) )
                        {
                            //TODO: check if kernel recreation here is really neccesary
                            Base newKernel = new Specie( compartment, de.getName() );
                            for( DynamicProperty dp : kernel.getAttributes() )
                                newKernel.getAttributes().add( dp );
                            kernel = newKernel;
                        }

                        DynamicProperty cloneMarker = de.getAttributes().getProperty( SBGNPropertyConstants.SBGN_CLONE_MARKER );
                        if( cloneMarker != null && cloneMarker.getValue() != null )
                        {
                            String cloneRef = (String)cloneMarker.getValue();
                            if( !cloneRef.isEmpty() && !cloneRef.equals( de.getCompleteNameInDiagram() ) )
                            {
                                continue;
                            }
                        }

                        Node node = new Node( compartment, kernel );
                        node.setRole( de.getRole( Role.class ).clone( node ) );
                        node.setTitle( de.getTitle() );
                        node.setLocation( ( (Node)de ).getLocation() );
                        node.setShapeSize( ( (Node)de ).getShapeSize() );
                        node.setVisible( ( (Node)de ).isVisible() );
                        for( DynamicProperty dp : de.getAttributes() )
                        {
                            if( isSbmlProperty( dp ) )
                                copyAttribute( de, node, dp.getName() );
                        }
                        compartment.put( node );
                    }
                    else if( de.getClass().equals( Compartment.class ) )
                    {
                        Compartment newCompartment = new Compartment( compartment, de.getName(), de.getKernel() );
                        newCompartment.setTitle( de.getTitle() );
                        newCompartment.setRole( de.getRole() );
                        newCompartment.setLocation( ( (Compartment)de ).getLocation() );
                        newCompartment.setShapeSize( ( (Compartment)de ).getShapeSize() );
                        compartment.put( newCompartment );
                        copyAttribute( de, newCompartment, "metaid" );
                        restoreElements( (Compartment)de, newCompartment );
                    }
                }
            }
        }
    }

    @Override
    protected void restoreEdges(Compartment sbgnCompartment, Compartment compartment, Diagram sbmlDiagram) throws Exception
    {
        for( DiagramElement de : sbgnCompartment )
        {
            if( de instanceof Node )
            {
                String xmlType = (String)de.getAttributes().getValue( XmlDiagramTypeConstants.XML_TYPE );
                if( ( de.getKernel() instanceof Reaction )
                        || ( ( xmlType != null ) && ( xmlType.equals( "association" ) || xmlType.equals( "dissociation" ) || xmlType
                                .equals( "process" ) ) ) )
                {
                    Node reactionNode = new Node( compartment, de.getKernel() );
                    reactionNode.setTitle( de.getTitle() );
                    reactionNode.setRole( de.getRole().clone( reactionNode ) );
                    reactionNode.setLocation( ( (Node)de ).getLocation() );
                    for( DynamicProperty dp : de.getAttributes() )
                    {
                        if( isSbmlProperty( dp ) )
                            copyAttribute( de, reactionNode, dp.getName() );
                    }
                    compartment.put( reactionNode );

                    if( de.getKernel() instanceof Reaction )
                    {
                        for( SpecieReference sr : ( (Reaction)de.getKernel() ).getSpecieReferences() )
                        {
                            Edge originalEdge = findEdge( (Node)de, sr );
                            if( SpecieReference.REACTANT.equals( sr.getRole() ) )
                            {
                                Node rNode = sbmlDiagram.findNode( sr.getSpecie() );

                                if( rNode == null )
                                {
                                    Node sbgnNode = Diagram.getDiagram( sbgnCompartment ).findNode( sr.getSpecie() );
                                    Node masterNode = (Node)sbgnNode.getRole( Role.class ).getDiagramElement();
                                    rNode = sbmlDiagram.findNode( masterNode.getName() );
                                }
                                if( rNode != null )
                                    restoreEdge( sr, rNode, reactionNode, originalEdge );
                            }
                            else if( SpecieReference.PRODUCT.equals( sr.getRole() ) )
                            {

                                Node rNode = sbmlDiagram.findNode( sr.getSpecie() );

                                if( rNode == null )
                                {
                                    Node sbgnNode = Diagram.getDiagram( sbgnCompartment ).findNode( sr.getSpecie() );
                                    Node masterNode = (Node)sbgnNode.getRole( Role.class ).getDiagramElement();
                                    rNode = sbmlDiagram.findNode( masterNode.getName() );
                                }
                                if( rNode != null )
                                    restoreEdge( sr, reactionNode, rNode, originalEdge );
                            }
                            else
                            {
                                Node rNode = sbmlDiagram.findNode( sr.getSpecie() );
                                if( rNode == null )
                                {
                                    Node sbgnNode = Diagram.getDiagram( sbgnCompartment ).findNode( sr.getSpecie() );
                                    Node masterNode = (Node)sbgnNode.getRole( Role.class ).getDiagramElement();
                                    rNode = sbmlDiagram.findNode( masterNode.getName() );
                                }
                                if( rNode != null )
                                    restoreEdge( sr, rNode, reactionNode, originalEdge );
                            }
                        }
                    }
                }
                else if( de instanceof Compartment )
                {
                    DiagramElement comp = compartment.get( de.getName() );
                    if( comp instanceof Compartment )
                    {
                        restoreEdges( (Compartment)de, (Compartment)comp, sbmlDiagram );
                    }
                }
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
                            new DynamicProperty( "variableName", String.class, basePort.getAttributes()
                                    .getValue( ConnectionPort.VARIABLE_NAME_ATTR ).toString() ) );
                    setXmlType( newNode, "port" );
                    newNode.setLocation( basePort.getLocation() );
                    sbgnDiagram.put( newNode );

                    //create edge
                    Node targetNode = sbgnDiagram.findNode( elName );
                    if( targetNode != null )
                    {
                        Edge edge = new Edge( sbgnDiagram, new Stub( null, newNode.getName() + "_link", "portlink" ), newNode, targetNode );
                        setXmlType( edge, "portlink" );
                        sbgnDiagram.put( edge );
                    }
                }
            }
        }
    }
}
