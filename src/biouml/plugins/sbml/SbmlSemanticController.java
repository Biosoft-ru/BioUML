package biouml.plugins.sbml;

import java.awt.Dimension;
import java.awt.Point;
import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nonnull;
import javax.swing.JFrame;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.math.model.Utils;
import ru.biosoft.util.DPSUtils;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.DiagramViewOptions;
import biouml.model.Edge;
import biouml.model.ModelDefinition;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.SubDiagram;
import biouml.model.SubDiagram.PortOrientation;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.Function;
import biouml.model.dynamics.VariableRole;
import biouml.model.xml.XmlDiagramType;
import biouml.model.xml.XmlDiagramTypeConstants;
import biouml.model.xml.XmlDiagramViewOptions;
import biouml.standard.diagram.CreatorElementWithName;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.PathwaySimulationSemanticController;
import biouml.standard.diagram.PortProperties;
import biouml.standard.diagram.Util;
import biouml.standard.type.Base;
import biouml.standard.type.Gene;
import biouml.standard.type.Reaction;
import biouml.standard.type.Specie;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import biouml.standard.type.Stub.ConnectionPort;
import biouml.standard.type.Stub.UndirectedConnection;
import biouml.standard.type.Type;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.application.Application;

/**
 *
 */
public class SbmlSemanticController extends PathwaySimulationSemanticController implements CreatorElementWithName
{
    /** Array of reserves key words. */
    static final String[] RESERVED_KEY_WORDS = {"abs", "acos", "and", "asin", "atan", "ceil", "cos", "exp", "floor", "hilli", "hillmmr",
            "hillmr", "hillr", "isouur", "log", "log10", "massi", "massr", "not", "or", "ordbbr", "ordbur", "ordubr", "pow", "ppbrr", "sin",
            "sqr", "sqrt", "substance", "time", "tan", "umai", "umar", "uai", "ualii", "uar", "ucii", "ucir", "ucti", "uctr", "uhmi",
            "uhmr", "umar", "umi", "unii", "unir", "uuhr", "umr", "usii", "usir", "uuci", "uucr", "uui", "uur", "volume", "xor"};

    /** Returns true if the specified string is reserved SBML key word. */
    public static boolean isReservedKeyWord(String str)
    {
        for( String element : RESERVED_KEY_WORDS )
        {
            if( element.equals( str ) )
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Search SBML model element with the specified name.
     * Generally this method is used to check whether sname is unique.
     *
     * <i>Note:</i> this the rather slow method while it parse all model and put the elements
     * into hash map: sname - object.
     *
     * @return Object with the specified name or null of no object was found.
     *
     * @pending process rules and units
     */
    public static Object resolveName(Diagram diagram, String sname)
    {
        Object result = null;
        HashMap<String, DiagramElement> map = new HashMap<>();

        // put all compartments, species and reaction names
        fillNameMap( diagram, map );
        result = map.get( sname );
        if( result != null )
        {
            return result;
        }

        // put all parameter names
        if( diagram.getRole() instanceof EModel )
        {
            try
            {
                EModel model = diagram.getRole( EModel.class );
                result = model.getVariable( sname );
            }
            catch( Throwable t )
            {
                log.log( Level.SEVERE, "Error during resolving name " + sname + " in diagram " + diagram + ", error: " + t.getMessage(),
                        t );
            }
        }

        return result;
    }

    protected static void fillNameMap(Compartment compartment, HashMap<String, DiagramElement> map)
    {
        map.put( compartment.getName(), compartment );

        Iterator<DiagramElement> i = compartment.iterator();
        while( i.hasNext() )
        {
            DiagramElement de = i.next();

            if( de instanceof Compartment )
            {
                fillNameMap( (Compartment)de, map );
            }
            else
            {
                map.put( de.getName(), de );
            }
        }
    }

    /**
     * Checks whether the name is valid SBML SName and replaces all invalid chaacters by '_'.
     *
     * @return valid SBML SName.
     */
    public static String validateSName(String name)
    {
        if( !Character.isLetter( name.charAt( 0 ) ) && name.charAt( 0 ) != '_' )
        {
            name = "_" + name;

        }
        StringBuffer buf = new StringBuffer();
        for( int i = 0; i < name.length(); i++ )
        {
            char ch = name.charAt( i );
            if( Character.isLetter( ch ) || Character.isDigit( ch ) || ch == '_' )
            {
                buf.append( ch );
            }
            else
            {
                buf.append( '_' );
            }
        }

        return buf.toString();
    }

    /**
     * Generates unique reaction name for the specified diagram.
     *
     * @pending verify that nhere is no parameter, rule or unit with the same name.
     */
    public String generateReactionName(Diagram diagram) throws Exception
    {
        // put all compartments, species and reaction names
        HashMap<String, DiagramElement> map = new HashMap<>();
        fillNameMap( diagram, map );

        int n = 1;
        String name = null;
        DecimalFormat formatter = Reaction.NAME_FORMAT;
        while( true )
        {
            name = formatter.format( n++ );
            if( !map.containsKey( name ) )
            {
                break;
            }
        }

        return name;
    }

    /**
     * Creates diagram element for the specified kernel type, parent compartment and location.
     * The created diagram element can be compartment, specie node or reaction node.
     *
     * @param parent compartment where new diagram element should be located
     * @param type kernel type
     * @param point diagram element location
     * @param viewEditor diagram view editor.
     */
    @Override
    public DiagramElementGroup createInstance(@Nonnull Compartment parent, Object type, Point point, ViewEditorPane viewEditor)
    {
        Class<?> classType = (Class<?>)type;
        DiagramElement de = null;
        Diagram diagram = (Diagram)viewEditor.getView().getModel();

        if( classType == Reaction.class || classType == Stub.Note.class || classType == Stub.NoteLink.class || classType == Equation.class
                || classType == Function.class || classType == Event.class || Stub.ConnectionPort.class.isAssignableFrom( classType )
                || classType == SubDiagram.class || classType == ModelDefinition.class || classType == UndirectedConnection.class )
        {
            return super.createInstance( parent, type, point, viewEditor );
        }
        else
        {
            JFrame frame = Application.getApplicationFrame();
            CreateDiagramElementDialog dialog = new CreateDiagramElementDialog( frame, diagram, parent, classType );
            if( dialog.doModal() )
            {
                de = dialog.getNode();
            }
        }
        return new DiagramElementGroup( de );
    }

    @Override
    public DiagramElementGroup createInstance(@Nonnull Compartment compartment, Object type, Point point, Object properties)
    {
        return this.createInstance( compartment, type, null, point, properties );
    }

    @Override
    public DiagramElementGroup createInstance(@Nonnull Compartment compartment, Object type, String name, Point point, Object properties)
    {
        try
        {
            for( TypeDescriptor descriptor : TYPE_DESCRIPTORS )
            {
                if( descriptor.getTypeClass().equals( type ) || descriptor.getXMLType().equals( type ) )
                {
                    String id = name;
                    if( id == null )
                    {
                        id = generateUniqueNodeName( compartment, descriptor.getType().replace( "-", "_" ) );
                    }

                    if( type.equals( Specie.class ) || type.equals( descriptor.getXMLType() ) )
                    {
                        return new DiagramElementGroup(
                                createVariableRoleOwner( descriptor.getXMLType(), compartment, point, new Specie( null, id ) ) );
                    }
                    else if( type.equals( Gene.class ) || type.equals( descriptor.getXMLType() ) )
                    {
                        return new DiagramElementGroup(
                                createVariableRoleOwner( descriptor.getXMLType(), compartment, point, new Gene( null, id ) ) );
                    }
                    else if( type.equals( biouml.standard.type.Compartment.class ) || type.equals( descriptor.getXMLType() ) )
                    {
                        return new DiagramElementGroup( createVariableRoleOwner( descriptor.getXMLType(), compartment, point,
                                new biouml.standard.type.Compartment( null, id ) ) );
                    }
                    else if( type.equals( Reaction.class ) )
                    {
                        return new DiagramElementGroup( createReaction( descriptor.getXMLType(), compartment, point, properties, id ) );
                    }
                    else
                    {
                        Stub stub = new Stub( null, id, descriptor.getType() );
                        Node node = new Node( compartment, stub );
                        Role role = (Role) ( ! ( properties instanceof Role )
                                ? descriptor.getTypeClass().getConstructor( DiagramElement.class ).newInstance( node )
                                : (Role) ( (Role)properties ).clone( node ) );
                        setXmlType( node, descriptor.getXMLType() );
                        node.setRole( role.clone( node ) );
                        if( point != null )
                            node.setLocation( point );
                        return new DiagramElementGroup( node );
                    }
                }
            }
        }
        catch( Exception e )
        {
            log.log( Level.SEVERE, "While creating instance of type " + type.toString(), e );
        }
        return DiagramElementGroup.EMPTY_EG;
    }

    /**
     * Gets the default properties of <code>DiagramElement</code> by its type for the correct element creation
     * by createInstance method.
     */
    @Override
    public Object getPropertiesByType(Compartment compartment, Object type, Point point)
    {
        Class<?> cl = null;
        if( type instanceof Class )
        {
            cl = (Class<?>)type;
            if( Reaction.class == cl || ! ( Role.class.isAssignableFrom( cl ) || Base.class.isAssignableFrom( cl ) ) )
            {
                return null;
            }
        }
        else
        {
            for( TypeDescriptor descriptor : TYPE_DESCRIPTORS )
            {
                if( descriptor.getXMLType().equals( type ) )
                {
                    cl = descriptor.getTypeClass();
                    break;
                }
            }
        }
        if( cl != null && Stub.ConnectionPort.class.isAssignableFrom( cl ) )
        {
            return new PortProperties( Diagram.getDiagram( compartment ), cl.asSubclass( Stub.ConnectionPort.class ) );
        }
        if( cl != null )
        {
            try
            {
                Object[] values = new Object[cl.getConstructors()[0].getParameterTypes().length];
                return cl.getConstructors()[0].newInstance( values );
            }
            catch( Exception e )
            {
                log.log( Level.SEVERE, "SbmlSemanticComtroller: can not get properties by type " + type );
                return null;
            }
        }
        return null;
    }

    protected DiagramElement createVariableRoleOwner(String xmlType, Compartment compartment, Point point, Base kernel)
    {
        Diagram diagram = Diagram.getDiagram( compartment );

        boolean isSbgnModel = false;
        if( diagram.getType() instanceof XmlDiagramType )
        {
            String name = ( (XmlDiagramType)diagram.getType() ).getName();
            if( "sbml_sbgn.xml".equals( name ) || "sbml_sbgn_composite.xml".equals( name ) )
                isSbgnModel = true;
        }

        Node node = null;
        if( ( kernel instanceof biouml.standard.type.Compartment ) || isSbgnModel )
        {
            node = new Compartment( compartment, kernel );
        }
        else
        {
            node = new Node( compartment, kernel );
        }

        boolean notificationEnabled = node.isNotificationEnabled();
        if( notificationEnabled )
            node.setNotificationEnabled( false );

        node.setShapeSize( new Dimension( 0, 0 ) );
        setXmlType( node, xmlType );
        VariableRole var = new VariableRole( node, 0.0 );
        node.setRole( var );

        if( notificationEnabled )
            node.setNotificationEnabled( true );
        return node;
    }

    protected DiagramElement createReaction(String xmlType, Compartment compartment, Point point, Object properties, String nodeName)
            throws Exception
    {
        Diagram diagram = Diagram.getDiagram( compartment );

        Reaction oldReaction = (Reaction)properties;
        List<SpecieReference> components = Arrays.asList( oldReaction.getSpecieReferences() );
        Node reactionNode = DiagramUtility.createReactionNode( diagram, compartment, null, components, oldReaction.getFormula(), point,
                xmlType, nodeName );
        setXmlType( reactionNode, xmlType );
        for( Edge edge : reactionNode.getEdges() )
        {
            setXmlType( edge, xmlType );
        }
        return reactionNode;
    }


    /**
     * Validate {@link DiagramElement}.
     * Is used by {@link XmlDiagramType} to create SBML compatibility kernels
     */
    @Override
    public synchronized DiagramElement validate(Compartment compartment, @Nonnull DiagramElement de, boolean newElement) throws Exception
    {
        if( de instanceof Diagram )
        {
            if( ( (Diagram)de ).getRole() == null )
                ( (Diagram)de ).setRole( new SbmlEModel( de ) );
            addSBOAttriubute( de );
            return de;
        }

        addSBOAttriubute( de );

        String parentStubType = getXmlType( compartment );
        if( ( de.getOrigin() != compartment ) && "complex".equals( parentStubType ) )
        {
            //remove specie and role when moving into complex
            Base kernel = de.getKernel();
            if( ! ( kernel instanceof Stub ) )
            {
                Node node = new Node( de.getOrigin(), new Stub( null, kernel.getName(), "entity" ) );
                boolean notificationEnabled = node.isNotificationEnabled();
                if( notificationEnabled )
                    node.setNotificationEnabled( false );
                node.setShapeSize( new Dimension( 0, 0 ) );
                setXmlType( node, "entity" );
                copyAttributes( de, node );
                if( notificationEnabled )
                    node.setNotificationEnabled( true );
                return node;
            }
        }

        if( de.getKernel() instanceof Specie || de.getKernel() instanceof Compartment )
        {
            if( de.getRole() == null )
                de.setRole( new VariableRole( de ) );
        }

        if( ( de.getKernel() instanceof Stub ) && ( de.getRole() == null ) )
        {
            Diagram diagram = Diagram.getDiagram( compartment );

            //new element added
            String stubType = de.getKernel().getType();
            if( "compartment".equals( stubType ) )
            {
                Base kernel = new biouml.standard.type.Compartment( null, de.getName() );
                DiagramElement node = createVariableRoleOwner( stubType, compartment, ( (Node)de ).getLocation(), kernel );
                copyAttributes( de, node );
                return node;
            }
            else if( "entity".equals( stubType ) && !"complex".equals( parentStubType ) )
            {
                Node node = (Node)createVariableRoleOwner( stubType, compartment, ( (Node)de ).getLocation(),
                        new Specie( null, de.getName() ) );
                copyAttributes( de, node );
                return node;
            }
            else if( "complex".equals( stubType ) )
            {
                Base kernel = new biouml.standard.type.Compartment( null, de.getName() );
                Node node = (Node)createVariableRoleOwner( stubType, compartment, ( (Node)de ).getLocation(), kernel );
                copyAttributes( de, node );
                return node;
            }
            else if( "event".equals( stubType ) )
            {
                DiagramElement node = createInstance( de.getCompartment(), Event.class, de.getName(), ( (Node)de ).getLocation(),
                        de.getRole() ).getElement();

                Event event = node.getRole( Event.class );
                String message = checkMath( diagram, event.getTrigger() );
                if( message != null )
                    throw new Exception( "Incorrect value of trigger: " + event.getTrigger() + ". " + message );

                for( int i = 0; i < event.getEventAssignment().length; ++i )
                {
                    message = checkMath( diagram, event.getEventAssignment( i ).getMath() );
                    if( message != null )
                        throw new Exception( "Incorrect value of trigger: " + event.getTrigger() + ". " + message );

                }
                return node;
            }
            else if( "function".equals( stubType ) )
            {
                return createInstance( de.getCompartment(), Function.class, de.getName(), ( (Node)de ).getLocation(), de.getRole() )
                        .getElement();
            }
            else if( "equation".equals( stubType ) )
            {
                DiagramElement node = createInstance( de.getCompartment(), Equation.class, de.getName(), ( (Node)de ).getLocation(),
                        de.getRole() ).getElement();

                Equation eq = node.getRole( Equation.class );
                String message = checkMath( diagram, eq.getFormula() );
                if( message != null )
                {
                    throw new Exception( "Can not accept the equation to " + compartment.getCompleteNameInDiagram() + ". " + message );
                }

                if( !eq.getType().equals( Equation.TYPE_SCALAR ) && !eq.getType().equals( Equation.TYPE_ALGEBRAIC )
                        && !eq.getType().equals( Equation.TYPE_RATE ) && !eq.getType().equals( Equation.TYPE_INITIAL_ASSIGNMENT ) )
                {
                    throw new Exception(
                            "Unknown type of the equation:" + eq.getType() + ". Acceptable types are: " + Equation.TYPE_ALGEBRAIC + ", "
                                    + Equation.TYPE_RATE + ", " + Equation.TYPE_SCALAR + ", " + Equation.TYPE_INITIAL_ASSIGNMENT );
                }

                return node;
            }
            else if( "association".equals( stubType ) || "dissociation".equals( stubType ) || "process".equals( stubType ) )
            {
                Reaction reaction = new Reaction( null, de.getName() );
                Node node = new Node( de.getOrigin(), de.getName(), reaction );
                setXmlType( node, stubType );
                return node;
            }
            //means it is newly created port, here we reset its type to avoid calling of this code later
            //TODO probably we should handle this better...
            //e.g. create method validateOnCreation and use it instead of validate()...
            else if( de instanceof Node && Util.isPort( de ) )
            {
                this.setXmlType( de, "port" );
                DynamicProperty property = de.getAttributes().getProperty( ConnectionPort.PORT_TYPE );
                if( property != null )
                {
                    property.setHidden( true );
                }
                property = de.getAttributes().getProperty( ConnectionPort.ACCESS_TYPE );
                if( property != null )
                {
                    property.setHidden( true );
                }
                else
                {
                    property = new DynamicProperty( ConnectionPort.ACCESS_TYPE, String.class, ConnectionPort.PUBLIC );
                    property.setHidden( true );
                    de.getAttributes().add( property );
                }
                property = de.getAttributes().getProperty( ConnectionPort.VARIABLE_NAME_ATTR );
                if( property != null )
                {
                    property.setReadOnly( true );
                }


                DynamicProperty dp = de.getAttributes().getProperty( "direction" );
                if( dp != null )
                {
                    de.getAttributes().add( new DynamicProperty( XmlDiagramTypeConstants.XML_TYPE, String.class, "port" ) );
                    String direction = dp.getValue().toString();
                    DynamicProperty newDp = new DynamicProperty( "orientation", PortOrientation.class,
                            PortOrientation.getOrientation( newStyleOrientation.get( direction ) ) );

                    if( compartment instanceof SubDiagram )
                    {
                        newDp.setReadOnly( true );
                        newDp.setHidden( true );
                    }
                    de.getAttributes().add( newDp );
                    de.getAttributes().remove( "direction" );


                }
                if( de.getParent() instanceof SubDiagram )
                {
                    dp = de.getAttributes().getProperty( ConnectionPort.PORT_ORIENTATION );
                    if( dp != null )
                        dp.setHidden( true );
                }
                //                addPackage(diagram, "comp");
                return de;
            }
            else if( "consumption".equals( stubType ) )
            {
                if( ( de instanceof Edge ) && ( ( (Edge)de ).getOutput().getKernel() instanceof Reaction ) )
                {
                    Reaction reaction = (Reaction) ( (Edge)de ).getOutput().getKernel();
                    String id = ( (Edge)de ).getOutput().getName() + ": " + reaction.getName() + " as " + SpecieReference.REACTANT;
                    SpecieReference ref = new SpecieReference( reaction, id, SpecieReference.REACTANT );
                    try
                    {
                        reaction.put( ref );
                    }
                    catch( Exception e )
                    {
                        log.log( Level.SEVERE, "Can not add specie reference to reaction", e );
                    }
                }
            }
            else if( "production".equals( stubType ) )
            {
                if( ( de instanceof Edge ) && ( ( (Edge)de ).getInput().getKernel() instanceof Reaction ) )
                {
                    Reaction reaction = (Reaction) ( (Edge)de ).getInput().getKernel();
                    String id = reaction.getName() + ": " + ( (Edge)de ).getOutput().getName() + " as " + SpecieReference.PRODUCT;
                    SpecieReference ref = new SpecieReference( reaction, id, SpecieReference.PRODUCT );
                    try
                    {
                        reaction.put( ref );
                    }
                    catch( Exception e )
                    {
                        log.log( Level.SEVERE, "Can not add specie reference to reaction", e );
                    }
                }
            }
            else if( "regulation".equals( stubType ) )
            {
                if( ( de instanceof Edge ) && ( ( (Edge)de ).getOutput().getKernel() instanceof Reaction ) )
                {
                    Reaction reaction = (Reaction) ( (Edge)de ).getOutput().getKernel();
                    String id = ( (Edge)de ).getOutput().getName() + ": " + reaction.getName() + " as " + SpecieReference.MODIFIER;
                    SpecieReference ref = new SpecieReference( reaction, id, SpecieReference.MODIFIER );
                    try
                    {
                        reaction.put( ref );
                    }
                    catch( Exception e )
                    {
                        log.log( Level.SEVERE, "Can not add specie reference to reaction", e );
                    }
                }
            }
            else if( "submap".equals( stubType ) )
            {
                throw new Exception( stubType + " is not able for this diagram" );
            }
        }
        else if( de.getKernel() instanceof Reaction )
        {
            //add source sink if necessary
            if( ( (Node)de ).getEdges().length > 0 ) //HACK: skip if edges are not initialized yet
            {
                int reactantCount = 0;
                int productCount = 0;
                for( Edge edge : ( (Node)de ).getEdges() )
                {
                    if( edge.getKernel() instanceof SpecieReference )
                    {
                        String type = ( (SpecieReference)edge.getKernel() ).getRole();
                        if( type.equals( SpecieReference.REACTANT ) )
                            reactantCount++;
                        else if( type.equals( SpecieReference.PRODUCT ) )
                            productCount++;
                    }
                    else if( edge.getKernel() instanceof Stub )
                    {
                        String type = edge.getKernel().getType();
                        if( type.equals( "consumption" ) )
                            reactantCount++;
                        else if( type.equals( "production" ) )
                            productCount++;
                        if( type.isEmpty() )//HACK: this is not correct diagram, is is not necessary to add source sink.
                        {
                            reactantCount++;
                            productCount++;
                        }
                    }
                }

                boolean addSourceSink = false;
                DiagramViewOptions options = Diagram.getDiagram( de ).getViewOptions();
                if( options instanceof XmlDiagramViewOptions )
                {
                    DynamicProperty property = ( (XmlDiagramViewOptions)options ).getOptions().getProperty( "addSourceSink" );
                    if( property != null && property.getValue() instanceof Boolean )
                    {
                        addSourceSink = (Boolean)property.getValue();
                    }
                }

                if( addSourceSink )
                {
                    //TODO: use here default DiagramUtility method
                    if( reactantCount == 0 )
                    {
                        Node sourceSink = new Node( de.getOrigin(),
                                new Stub( null, de.getKernel().getName() + "_empty_reactant", "source-sink" ) );
                        sourceSink.setShapeSize( new Dimension( 30, 30 ) );
                        sourceSink.setLocation( ( (Node)de ).getLocation().x - 60, ( (Node)de ).getLocation().y );
                        de.getOrigin().put( sourceSink );

                        String edgeName = sourceSink.getName() + "_egde";
                        Edge sourceSinkEdge = new Edge( de.getOrigin(), edgeName,
                                new Stub( (Reaction)de.getKernel(), edgeName, "consumption" ), sourceSink, (Node)de );
                        setXmlType( sourceSinkEdge, "reaction" );
                        de.getOrigin().put( sourceSinkEdge );
                    }
                    if( productCount == 0 )
                    {
                        Node sourceSink = new Node( de.getOrigin(),
                                new Stub( null, de.getKernel().getName() + "_empty_product", "source-sink" ) );
                        sourceSink.setShapeSize( new Dimension( 30, 30 ) );
                        sourceSink.setLocation( ( (Node)de ).getLocation().x + 30, ( (Node)de ).getLocation().y );
                        de.getOrigin().put( sourceSink );

                        String edgeName = sourceSink.getName() + "_egde";
                        Edge sourceSinkEdge = new Edge( de.getOrigin(), edgeName,
                                new Stub( (Reaction)de.getKernel(), edgeName, "production" ), (Node)de, sourceSink );
                        setXmlType( sourceSinkEdge, "reaction" );
                        de.getOrigin().put( sourceSinkEdge );
                    }
                }
            }
        }
        else if( ( de.getKernel().getType().equals( "unit of information" ) || de.getKernel().getType().equals( "variable" ) )
                && ! ( compartment.getKernel() instanceof Specie ) )
        {
            throw new Exception( "Can not accept " + de.getKernel().getType() + " to " + compartment.getCompleteNameInDiagram() );
        }
        return de;
    }

    protected void copyAttributes(DiagramElement oldElement, DiagramElement newElement)
    {
        oldElement.getAttributes().forEach( dp -> newElement.getAttributes().add( dp ) );
    }

    protected void setXmlType(DiagramElement de, String type)
    {
        DynamicProperty dp = new DynamicProperty( XmlDiagramTypeConstants.XML_TYPE, String.class, type );
        dp.setHidden( true );
        de.getAttributes().add( dp );
    }

    protected void addSBOAttriubute(DiagramElement de)
    {
        if( de.getAttributes().getProperty( SbmlConstants.SBO_TERM_ATTR ) == null )
            de.getAttributes().add( new DynamicProperty( SbmlConstants.SBO_TERM_ATTR, String.class, "" ) );
    }

    protected String getXmlType(DiagramElement de)
    {
        Object xmlType = de.getAttributes().getValue( XmlDiagramTypeConstants.XML_TYPE );
        return xmlType != null ? xmlType.toString() : "";
    }

    private String checkMath(Diagram diagram, String math)
    {
        Role role = diagram.getRole();
        if( ! ( role instanceof EModel ) )
            return "Invalid executable model of the diagram " + diagram.getName();

        EModel emodel = (EModel)role;
        int status = emodel.getParser().parse( math );
        try
        {
            if( status > ru.biosoft.math.model.Parser.STATUS_OK )
            {
                return "There were errors or warnings during the math " + math + " parsing\n" + " errors: \n"
                        + Utils.formatErrors( emodel.getParser() );
            }
        }
        catch( Throwable t )
        {
            return "There were errors or warnings during the math " + math + " parsing\n" + " errors: \n" + t.toString();
        }
        return null;
    }

    private final static HashMap<String, String> newStyleOrientation = new HashMap<String, String>()
    {
        {
            put( "north", "up" );
            put( "east", "right" );
            put( "west", "left" );
            put( "south", "bottom" );
        }
    };

    public static void addPackage(Diagram diagram, String packageName) throws Exception
    {
        DynamicProperty dp = diagram.getAttributes().getProperty( "Packages" );
        if( dp == null )
        {
            dp = new DynamicProperty( "Packages", String[].class, new String[] {"comp"} );
            dp.setHidden( true );
            DPSUtils.makeTransient( dp );
            diagram.getAttributes().add( dp );
        }
        String[] val = (String[])dp.getValue();
        boolean alreadyHasPackage = false;
        for( String v : val )
        {
            if( packageName.equals( v ) )
            {
                alreadyHasPackage = true;
                break;
            }
        }

        if( !alreadyHasPackage )
        {
            String[] newVal = new String[val.length + 1];
            System.arraycopy( val, 0, newVal, 0, val.length );
            newVal[val.length] = packageName;
        }
    }

    private static class TypeDescriptor
    {
        private final String xmlType;
        private final String type;
        private final Class<?> typeClass;

        public TypeDescriptor(String xmlType, String type, Class<?> typeClass)
        {
            super();
            this.xmlType = xmlType;
            this.type = type;
            this.typeClass = typeClass;
        }

        public String getXMLType()
        {
            return xmlType;
        }
        public String getType()
        {
            return type;
        }
        public Class<?> getTypeClass()
        {
            return typeClass;
        }
    }

    /** Removes the diagram element and all related edges. *///TODO: remove copypaste code
    @Override
    public boolean remove(DiagramElement de) throws Exception
    {
        if( de instanceof Diagram )
            return false;

        if( de.getKernel() instanceof Stub.Note || de.getKernel() instanceof Stub.NoteLink )
            return super.remove( de );

        if( de instanceof Edge )
        {
            // we can remove only semantic relations or regulatory events
            Edge edge = (Edge)de;
            if( edge.getKernel().getType().equals( Type.TYPE_SEMANTIC_RELATION ) )
            {
                edge.getOrigin().remove( edge.getName() );
                return true;
            }

            // reaction edges can not be removed. We should remove the whole
            // reaction
            return false;
        }

        // to remove compartment we should remove all its edges
        if( de instanceof Compartment )
            doRemove( ( (Compartment)de ).stream().toList() );

        // remove node
        Node node = (Node)de;
        Base kernel = node.getKernel();
        DataCollection<?> parent = node.getOrigin();

        if( ( kernel.getType().equals( Type.TYPE_REACTION ) ) )
        {
            // now we are remove usual edges
            for( Edge edge : node.edges().toList() )
            {
                edge.getCompartment().remove( edge.getName() );
            }
        }
        parent.remove( node.getName() );

        return true;
    }

    @Override
    public boolean canAccept(Compartment compartment, DiagramElement de)
    {
        if( de instanceof Edge && de.getKernel() instanceof SpecieReference )
            return true;
        return super.canAccept( compartment, de );
    }

    private static final TypeDescriptor[] TYPE_DESCRIPTORS = {new TypeDescriptor( "equation", Type.MATH_EQUATION, Equation.class ),
            new TypeDescriptor( "function", Type.MATH_FUNCTION, Function.class ),
            new TypeDescriptor( "event", Type.MATH_EVENT, Event.class ), new TypeDescriptor( "entity", Type.TYPE_SUBSTANCE, Specie.class ),
            new TypeDescriptor( "entity", Type.TYPE_GENE, Gene.class ),
            new TypeDescriptor( "compartment", Type.TYPE_COMPARTMENT, biouml.standard.type.Compartment.class ),
            new TypeDescriptor( "reaction", Type.TYPE_REACTION, Reaction.class )};
}
