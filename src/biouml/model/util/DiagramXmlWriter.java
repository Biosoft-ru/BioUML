package biouml.model.util;

import java.awt.Dimension;
import java.awt.Point;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementStyle;
import biouml.model.DiagramFilter;
import biouml.model.DiagramType;
import biouml.model.Edge;
import biouml.model.EquivalentNodeGroup;
import biouml.model.ModelDefinition;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.SubDiagram;
import biouml.model.dynamics.Connection;
import biouml.model.dynamics.DirectedConnection;
import biouml.model.dynamics.MultipleConnection;
import biouml.model.dynamics.plot.PlotsInfo;
import biouml.model.dynamics.plot.Curve;
import biouml.model.dynamics.plot.Experiment;
import biouml.model.dynamics.plot.PlotInfo;
import biouml.model.dynamics.UndirectedConnection;
import biouml.model.xml.XmlDiagramType;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.state.StateXmlSerializer;
import biouml.standard.type.Base;
import biouml.standard.type.DatabaseReference;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.DiagramInfo.AuthorInfo;
import biouml.standard.type.ImageDescriptor;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.exception.Assert;
import ru.biosoft.graph.Layouter;
import ru.biosoft.graph.Path;
import ru.biosoft.util.ColorUtils;
import ru.biosoft.util.DPSUtils;
import ru.biosoft.util.TextUtil2;

public class DiagramXmlWriter extends DiagramXmlSupport implements DiagramWriter
{
    protected static final Logger log = Logger.getLogger( DiagramXmlWriter.class.getName() );

    /**
     * Version of diagram format ATTENTION - do not change this parameter
     * without necessity (because of some old diagrams cannot be read). Change
     * only after additional testing of old diagrams.
     *
     * @see DiagramXmlReader.readAndApplyEdges
     */
    public static final String VERSION = "0.7.7";

    /**
     * Application version
     */
    public static final String APPVERSION = "0.7.7";


    protected Document doc;

    // //////////////////////////////////////////////////////////////////////////
    // Static functions that can be used by other applications
    // to write information about diagram nodes location and compartment info
    
    /**
     * Technical constructor used when DiagramType returns appropriate writer. Stream need to be set later!
     */
    public DiagramXmlWriter()
    {
        
    }

    public static void writeDiagram(Diagram diagram, OutputStream stream) throws Exception
    {
        DiagramType type = diagram.getType();
        DiagramXmlWriter writer = type.getDiagramWriter();
        writer.stream = stream;
        writer.write(diagram);
    }

    
    @Override
    public void writeNode(Element parent, Node node)
    {
        Element element;
        if( node instanceof Compartment )
            element = createCompartmentNode(DiagramXmlConstants.COMPARTMENT_ELEMENT, (Compartment)node);
        else
            element = createNode(node);
        if( element != null )
            parent.appendChild(element);
    }

    @Override
    public void writeEdge(Element parent, Edge edge)
    {
        Compartment compartment = getDiagram();
        if( edge.getOrigin() instanceof Compartment && !(edge.getOrigin() instanceof Diagram) )
            compartment = (Compartment)edge.getOrigin();

        Element element = createEdge(edge, compartment);
        if( element != null )
            parent.appendChild(element);
    }

    public static void writeCompartmentInfo(Element element, Compartment compartment, Document doc)
    {
        setTitle(element, compartment);
        setLocation(element, compartment);
        Dimension size = compartment.getShapeSize();
        if( size == null )
            size = new Dimension(10, 10);
        element.setAttribute(WIDTH_ATTR, Integer.toString((int)size.getWidth()));
        element.setAttribute(HEIGHT_ATTR, Integer.toString((int)size.getHeight()));
        element.setAttribute(SHAPE_ATTR, Integer.toString(compartment.getShapeType()));
        element.setAttribute( IS_TITLE_HIDDEN_ATTR, String.valueOf(!compartment.isShowTitle())  );
        
        if( compartment.isUseCustomImage() )
        {
            Element imageEl = createImage(compartment.getImage(), doc);
            if( imageEl != null )
                element.appendChild(imageEl);
        }
        writeStyle(element, compartment);
        if( !compartment.isVisible() )
            element.setAttribute(IS_HIDDEN_ATTR, "true");
    }

    public static void writeNodeInfo(Element element, Node node, Document doc)
    {
        setTitle( element, node );
        setLocation( element, node );

        Dimension size = node.getShapeSize();
        if( size != null )
        {
            element.setAttribute( WIDTH_ATTR, Integer.toString( (int)size.getWidth() ) );
            element.setAttribute( HEIGHT_ATTR, Integer.toString( (int)size.getHeight() ) );
        }
        if(!node.isVisible())
            element.setAttribute( IS_HIDDEN_ATTR, "true" );

        element.setAttribute( IS_TITLE_HIDDEN_ATTR, String.valueOf(!node.isShowTitle())  );

        // write note background
        if( node.getKernel() instanceof Stub.Note )
        {
            if( !((Stub.Note)node.getKernel()).isBackgroundVisible() )
                element.setAttribute( BACKGROUND_VISIBLE_ATTR, "false" );
        }       

        ImageDescriptor imageDesc = node.getImage();
        Element imageEl = createImage(imageDesc, doc);
        if( imageEl != null )
            element.appendChild(imageEl);
        
        writeStyle(element, node);

        if( node.isFixed() )
            element.setAttribute( FIXED_ATTR, String.valueOf( node.isFixed() ) );

        String comment = node.getComment();
        if (comment != null && !comment.isEmpty())
            element.setAttribute(COMMENT_ATTR, comment);
        
        DynamicPropertySet properties = Diagram.getDiagram( node ).getType().getProperties();
        if( properties != null )
            serializeDPS( doc, element, node.getAttributes(), properties );
    }

    public static boolean isEdgeInfoInformative(Edge edge)
    {
        Path path = edge.getPath();
        if( path != null && path.npoints > 1 )
            return true;

        Point inPort = edge.getInPort();
        if( inPort != null && inPort.getX() != 0 && inPort.getY() != 0 )
            return true;

        Point outPort = edge.getOutPort();
        if( outPort != null && outPort.getX() != 0 && outPort.getY() != 0 )
            return true;

        if( edge.getKernel() == null )
            return true;

        if( edge.getTitle() == null && edge.getKernel().getTitle() == null )
            return false;

        return ( edge.getTitle() == null && edge.getKernel().getTitle() != null ) || !edge.getTitle().equals( edge.getKernel().getTitle() );
    }

    public static void writeEdgeInfo(Element element, Edge edge, Document doc)
    {
        if( isEdgeInfoInformative( edge ) )
        {
            Path path = edge.getPath();
            if( path != null && path.npoints > 2)
            {
                Element pathElem = doc.createElement( "path" );
                for( int i = 0; i < path.npoints; i++ )
                {
                    Element segment = doc.createElement( "segment" );
                    segment.setAttribute( "x0", String.valueOf( path.xpoints[i] ) );
                    segment.setAttribute( "y0", String.valueOf( path.ypoints[i] ) );
                    if( i == 0 )
                    {
                        segment.setAttribute( "segmentType", LINE_MOVETO );
                    }
                    else if( path.pointTypes[i] == 1 )
                    {
                        segment.setAttribute( "segmentType", LINE_QUADRIC );
                    }
                    else if( path.pointTypes[i] == 2 )
                    {
                        segment.setAttribute( "segmentType", LINE_CUBIC );
                    }
                    else
                    {
                        segment.setAttribute( "segmentType", LINE_LINETO );
                    }
                    pathElem.appendChild( segment );
                }
                element.appendChild( pathElem );
            }
        }
        if( edge != null && edge.getName() != null && !edge.getName().isEmpty() )
            element.setAttribute( EDGE_ID_ATTR, edge.getName() );
        setTitle( element, edge );
        writeStyle(element, edge);
        if (edge.isFixed())
            element.setAttribute(FIXED_ATTR, String.valueOf(edge.isFixed()));

        if( edge.isFixedInOut() )
            element.setAttribute( FIXED_IN_OUT_ATTR, String.valueOf( edge.isFixedInOut() ) );

        Point inPort = edge.getInPort();
        if( inPort != null && inPort.getX() != 0 && inPort.getY() != 0 )
            element.setAttribute(INPORT_ATTR, XmlSerializationUtils.getPointString(inPort));

        Point outPort = edge.getOutPort();
        if( outPort != null && outPort.getX() != 0 && outPort.getY() != 0 )
            element.setAttribute(OUTPORT_ATTR, XmlSerializationUtils.getPointString(outPort));

        String comment = edge.getComment();
        if (comment != null && !comment.isEmpty())
            element.setAttribute(COMMENT_ATTR, comment);
        
        DiagramType diagramType = Diagram.getDiagram( edge ).getType();
        DynamicPropertySet properties = diagramType.getProperties();
        if( properties != null )
            serializeDPS( doc, element, edge.getAttributes(), properties );
    }

    public static void writeEdgeRole(Element element, Edge edge, Document doc)
    {
        Role role = edge.getRole();
        if( role instanceof Connection )
            writeConnection( doc, edge, (Connection)role, element );
    }

    public static void writeConnection(Document doc, Edge edge, Connection connection, Element parentElement)
    {
        Element connectionElement = doc.createElement( CONNECTION_ROLE_ELEMENT );
        connectionElement.setAttribute( TYPE_ATTR, edge.getRole(Role.class).getClass().getName() );

        Element inPort = doc.createElement( CONNECTION_INPUT_ELEMENT );
        inPort.setAttribute( ID_ATTR, connection.getInputPort().getVariableName() );
        inPort.setAttribute( TITLE_ATTR, connection.getInputPort().getVariableTitle() );
        connectionElement.appendChild( inPort );

        Element outPort = doc.createElement( CONNECTION_OUTPUT_ELEMENT );
        outPort.setAttribute( ID_ATTR, connection.getOutputPort().getVariableName() );
        outPort.setAttribute( TITLE_ATTR, connection.getOutputPort().getVariableTitle() );
        connectionElement.appendChild( outPort );

        if( connection instanceof DirectedConnection )
            setOptionalAttribute(connectionElement, FORMULA_ATTR, ( (DirectedConnection)connection ).getFunction());
        else if( connection instanceof UndirectedConnection )
        {
            String mainVariable = ( (UndirectedConnection)connection ).getMainVariableType().toString();
            connectionElement.setAttribute( MAIN_VARIABLE_ATTR, mainVariable );
        }
        if( connection instanceof MultipleConnection )
        {
            Connection[] connectionList = ( (MultipleConnection)connection ).getConnections();
            Element connectionListElement = doc.createElement( CONNECTION_LIST_ELEMENT );
            for( Connection con : connectionList )
                writeConnection( doc, (Edge)con.getParent(), con, connectionListElement );
            connectionElement.appendChild( connectionListElement );
        }
        parentElement.appendChild( connectionElement );
    }

    protected static void setTitle(Element element, DiagramElement diagramElement)
    {
        if( diagramElement == null || diagramElement.getTitle() == null ) //title can be empty
            return;
        element.setAttribute(TITLE_ATTR, validate(diagramElement.getTitle()));
    }

    protected static void setComment(Element element, DiagramElement diagramElement)
    {
        if( diagramElement != null )
            setComment(element, diagramElement.getComment());
    }

   

    protected static void setLocation(Element element, Node node)
    {
        if( node != null )
        {
            Point location = node.getLocation();
            element.setAttribute( X_ATTR, Integer.toString( location.x ) );
            element.setAttribute( Y_ATTR, Integer.toString( location.y ) );
        }
    }

    protected static Element createImage(ImageDescriptor imageDesc, Document doc)
    {
       Element element = null;
        if( imageDesc != null )
        {
            if (imageDesc.getPath() != null)
            {
                element = doc.createElement( IMAGE_ELEMENT );
                element.setAttribute(PATH_ATTR, imageDesc.getPath().toString() );
            }
            String imageSource = imageDesc.getSource();
            if( imageSource != null )
            {
                element = doc.createElement( IMAGE_ELEMENT );
                element.setAttribute( SRC_ATTR, imageSource );
                Dimension size = imageDesc.getSize();
                element.setAttribute( WIDTH_ATTR, Integer.toString( (int)size.getWidth() ) );
                element.setAttribute( HEIGHT_ATTR, Integer.toString( (int)size.getHeight() ) );
                /* TODO: Add 'hide_title' attribute */
            }
        }
        return element;
    }

    /** Stream to store the XML . */
    protected OutputStream stream;

    /**
     * This constructor is used to store the diagram XML description into the
     * specified output stream and can be used to store the diagram description
     * into relational database.
     */
    public DiagramXmlWriter(OutputStream stream)
    {
        this.stream = stream;
    }
    
    public void setStream(OutputStream stream)
    {
        this.stream = stream;
    }

    /**
     * This constructor is used to store Diagram object to XML Element
     */
    public DiagramXmlWriter(Document doc, Diagram diagram)
    {
        this.doc = doc;
        this.diagram = diagram;
    }

    public void write(@Nonnull Diagram sourceDiagram) throws Exception
    {
        write( sourceDiagram, TransformerFactory.newInstance().newTransformer() );
    }

    public void write(@Nonnull Diagram sourceDiagram, Transformer transformer) throws Exception
    {
        Assert.notNull("sourceDiagram", sourceDiagram);

        diagram = sourceDiagram;

        boolean notificationEnabled = false;
        if( diagram.isNotificationEnabled() )
        {
            notificationEnabled = true;
            diagram.setNotificationEnabled( false );
        }
        buildDocument();

        if( notificationEnabled )
            diagram.setNotificationEnabled( true );

        DOMSource source = new DOMSource( doc );
        StreamResult result = new StreamResult( stream );
        transformer.setOutputProperty( javax.xml.transform.OutputKeys.MEDIA_TYPE, "text/xml" );
        transformer.setOutputProperty( javax.xml.transform.OutputKeys.INDENT, "yes" );
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2"); // set because default indent amount is zero
        transformer.transform( source, result );
        // TODO: add validation (diagram may contain unresolved symbols)
    }

    protected Document buildDocument() throws Exception
    {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        doc = builder.newDocument();
        ModelXmlWriter modelWriter = getModelWriter();
        modelWriter.setNewPaths( replacements );
        doc.appendChild( createDml( createDiagram(), modelWriter.createModel( diagram, doc ) , createStates() ) );
        return doc;
    }

    protected ModelXmlWriter getModelWriter()
    {
        return new ModelXmlWriter();
    }

    protected Element createDml(Element diag, Element model, Element states)
    {
        Element element = doc.createElement( DML_ELEMENT );
        element.setAttribute( VERSION_ATTR, VERSION );
        element.setAttribute( APPVERSION_ATTR, APPVERSION );
        element.appendChild( diag );
        if( model != null )
            element.appendChild( model );
        if( states != null )
            element.appendChild( states );
        return element;
    }

    // //////////////////////////////////////////////////////////////////////////
    // Create structural model (diagram)
    //

    protected Element createDiagram()
    {
        Assert.notNull("diagram", diagram);
        Element element = doc.createElement( DIAGRAM_ELEMENT );
        element.setAttribute( DIAGRAM_TYPE_ATTR, diagram.getType().getClass().getName() );
        element.setAttribute( TITLE_ATTR, diagram.getTitle() );

        if( diagram.getType() instanceof XmlDiagramType )
            element.setAttribute( DIAGRAM_XML_NOTATION, DataElementPath.create( (XmlDiagramType)diagram.getType() ).toString() );

        element.appendChild( createDiagramInfo( diagram ) );
        Element viewOptions = createDiagramViewOptions( doc, diagram, VIEW_OPTIONS_ELEMENT );
        if( viewOptions != null )
            element.appendChild( viewOptions );

        Element simulationOptions = writeSimulationOptions(doc, SIMULATION_OPTIONS, diagram);
        if( simulationOptions != null )
            element.appendChild( simulationOptions );

        Element plots = writePlotsInfo(doc, PLOTS_ELEMENT, diagram, replacements);
        if( plots != null )
            element.appendChild( plots );
                
        Element layouterInfo = createLayouterInfo( doc, diagram.getPathLayouter(), LAYOUTER_INFO_ELEMENT );
        if( layouterInfo != null )
            element.appendChild( layouterInfo );

        Element labelLayouterInfo = createLayouterInfo( doc, diagram.getLabelLayouter(), LABEL_LAYOUTER_INFO_ELEMENT );
        if( labelLayouterInfo != null )
            element.appendChild( labelLayouterInfo );

        element.appendChild( createNodes( diagram ) );
        element.appendChild( createEdges( diagram ) );
        element.appendChild( createFilters( diagram ) );
        return element;
    }
    
    public void createAttributes(Base base, Element element)
    {
        DynamicPropertySet dps = base.getAttributes();
        if( dps != null && dps.size() > 0 )
        {
            Element dpsElement = doc.createElement( ATTRIBUTES_ELEMENT );
            serializeDPS( doc, dpsElement, dps, null );
            element.appendChild( dpsElement );
        }
    }

    protected Element createDiagramInfo(Diagram diagram)
    {
        Element element = doc.createElement( DIAGRAM_INFO_ELEMENT );

        Base kernel = diagram.getKernel();
        if( kernel instanceof DiagramInfo )
        {
            setOptionalAttribute(element, TITLE_ATTR, kernel.getTitle());
            setComment(element, ( (DiagramInfo)kernel ).getComment());

            String value = ( (DiagramInfo)kernel ).getDescription();
            if( value != null )
                element.appendChild( doc.createCDATASection( validate( value ) ) );

            writeCreationInfo((DiagramInfo)kernel, element);
            createAttributes(kernel, element);

            DatabaseReference[] dr = ( (DiagramInfo)kernel ).getDatabaseReferences();
            if( dr != null )
            {
                for( DatabaseReference element2 : dr )
                {
                    Element itemElement = doc.createElement( DATABASE_REFERENCE_ELEMENT );
                    itemElement.setAttribute( VALUE_ATTR, element2.getAsText() );
                    element.appendChild( itemElement );
                }
            }
            String[] lr = ( (DiagramInfo)kernel ).getLiteratureReferences();
            if( lr != null  )
            {
                for( String element2 : lr )
                {
                    Element itemElement = doc.createElement( LITERATURE_REFERENCE_ELEMENT );
                    itemElement.setAttribute( VALUE_ATTR, element2 );
                    element.appendChild( itemElement );
                }
            }
        }
        DynamicPropertySet properties = diagram.getType().getProperties();
        if( properties != null )
            serializeDPS( doc, element, diagram.getAttributes(), properties );

        return element;
    }
    
    protected void writeCreationInfo(DiagramInfo info, Element element)
    {
        Element authorsElement = doc.createElement( AUTHORS_ELEMENT );
        AuthorInfo[] authors = info.getAuthors();
        for (int i=0; i<authors.length; i++)
        {
            Element author = doc.createElement( AUTHOR_ELEMENT );
            author.setAttribute( FAMILY_NAME_ATTR, authors[i].getFamilyName() );
            author.setAttribute( GIVEN_NAME_ATTR, authors[i].getGivenName() );
            author.setAttribute( EMAIL_ATTR, authors[i].getEmail());
            author.setAttribute( ORGANISATION_ATTR, authors[i].getOrgName() );            
            authorsElement.appendChild( author );
        }
        if( authorsElement.hasChildNodes() )
            element.appendChild( authorsElement );

        Element historyElement = doc.createElement( HISTORY_ELEMENT );
        
        String created = info.getCreated();       
        if( created != null && !created.isEmpty() )
        {
            Element createdElement = doc.createElement( CREATED_ELEMENT );
            createdElement.setAttribute( DATE_ATTR, info.getCreated() );
            historyElement.appendChild( createdElement );
        }
          
        String[] modified = info.getModified();
        for (int i=0; i<modified.length; i++)
        {
            Element modifiedElement = doc.createElement( MODIFIED_ELEMENT );
            modifiedElement.setAttribute( DATE_ATTR, modified[i] );
            historyElement.appendChild( modifiedElement );
        }

        if( historyElement.hasChildNodes() )
            element.appendChild( historyElement );
    }

    public static Element createDiagramViewOptions(Document doc, Diagram diagram, String elementName)
    {
        return createDiagramViewOptions( doc, diagram, elementName, false );
    }

    public static Element createDiagramViewOptions(Document doc, Diagram diagram, String elementName, boolean writeIfDefault)
    {
        Element element = doc.createElement( elementName );
        DynamicPropertySet dps = new DynamicPropertySetAsMap();
        DPSUtils.writeBeanToDPS( diagram.getViewOptions(), dps, "" );
        if(!dps.isEmpty())
        {
            DynamicPropertySet defaults = new DynamicPropertySetAsMap();
            DPSUtils.writeBeanToDPS( diagram.getType().getDiagramViewBuilder().createDefaultDiagramViewOptions(), defaults, "" );
            serializeDPS( doc, element, dps, defaults, writeIfDefault );
        }
        return element;
    }

    public static Element createLayouterInfo(Document doc, Layouter layouter, String elementName)
    {
        if( layouter != null )
        {
            Element element = doc.createElement( elementName );
            element.setAttribute( TYPE_ATTR, layouter.getClass().getName() );
            DynamicPropertySet dps = new DynamicPropertySetAsMap();
            DPSUtils.writeBeanToDPS( layouter, dps, "" );
            serializeDPS( doc, element, dps, new DynamicPropertySetAsMap(), false );
            return element;
        }
        return null;
    }

    private Element createCompartmentInfo(Compartment compartment)
    {
        Element element = doc.createElement( COMPARTMENT_INFO_ELEMENT );
        setKernelRef( element, compartment );
        writeCompartmentInfo( element, compartment, doc );
        setComment( element, compartment );
        return element;
    }

    private Element createNodes(Compartment compartment)
    {
        Element element = doc.createElement(NODES_ELEMENT);
        Collection<SubDiagram> subDiagrams = new HashSet<>();
        for( DiagramElement de : compartment )
        {
            Element child = null;
            if( de instanceof SubDiagram )
            {
                subDiagrams.add((SubDiagram)de);
            }
            else if( de instanceof ModelDefinition )
            {
                child = createModelDefinitionNode((ModelDefinition)de);
            }
            else if( de instanceof biouml.model.EquivalentNodeGroup )
            {
                child = createCompartmentNode(EQUIVALENTNODEGROUP_ELEMENT, (EquivalentNodeGroup)de);
            }
            else if( de instanceof biouml.model.Compartment )
            {
                child = createCompartmentNode(COMPARTMENT_ELEMENT, (Compartment)de);
            }
            else if( de instanceof biouml.model.Node )
            {
                child = createNode((Node)de);
            }
            if( child != null )
                element.appendChild(child);
        }
        //SubDiagram must be added in Diagram after ModelDefinitions
        for( SubDiagram sd : subDiagrams )
            element.appendChild(createSubDiagramNode(sd));
        return element;
    }

    private Element createEdges(Compartment compartment)
    {
        Element element = doc.createElement( EDGES_ELEMENT );
        compartment.stream().select(Edge.class).forEach(e-> element.appendChild( createEdge( e, compartment ) ));
        return element;
    }

    public Element createCompartmentNode(String compartmentName, Compartment compartment)
    {
        Element element = doc.createElement( compartmentName );
        if( compartmentName.equals( EQUIVALENTNODEGROUP_ELEMENT ) )
        {
            /** TODO: Set correct representative */
            element.setAttribute( REPRESENTATIVE_ATTR, UNKNOWN_VALUE );
        }
        element.appendChild( createCompartmentInfo( compartment ) );
        element.appendChild( createNodes( compartment ) );
        element.appendChild( createEdges( compartment ) );

        DiagramType diagramType = Diagram.getDiagram( compartment ).getType();
        DynamicPropertySet properties = diagramType.getProperties();
        if( properties != null )
            serializeDPS( doc, element, compartment.getAttributes(), properties );

        return element;
    }

    private Map<String, String> replacements = new HashMap<>();
    public void setReplacements(Map<String, String> replacements)
    {
        this.replacements = replacements;
    }
    
    private Element createSubDiagramNode(SubDiagram subDiagram)
    {
        Element element = createCompartmentNode( SUBDIAGRAM_ELEMENT, subDiagram );
        element.setAttribute( ID_ATTR, subDiagram.getName() );
        Diagram innerDiagram = subDiagram.getDiagram();
        if(ModelDefinition.isDefindInModelDefinition(innerDiagram))
            element.setAttribute( MODEL_DEFINITION_REF_ATTR, ModelDefinition.getModelDefinition(innerDiagram).getName() );
        else
        {
            String path = subDiagram.getDiagramPath();
            if (replacements != null && replacements.containsKey( path ))
                path = replacements.get( path );
            element.setAttribute( DIAGRAM_REF_ATTR, path );
        }
        element.setAttribute( DIAGRAM_STATE_ATTR, subDiagram.getStateName() );

        try
        {
            biouml.standard.state.State state = innerDiagram.getState(SubDiagram.SUBDIAGRAM_STATE_NAME);
            if( state != null )
                element.appendChild(StateXmlSerializer.getStateXmlElement(state, doc, new DiagramXmlWriter(doc, innerDiagram)));
        }
        catch( Throwable t )
        {
            error( "ERROR_SUBDIAGRAM_STATES_WRITING", new String[] {diagram.getName(), subDiagram.getName(), t.getMessage()} );
        }
        return element;
    }

    private Element createModelDefinitionNode(ModelDefinition modelDefinition)
    {
        Element element = createCompartmentNode( MODEL_DEFINITION_ELEMENT, modelDefinition );
        element.setAttribute( ID_ATTR, modelDefinition.getName() );
        try
        {
            Diagram innerDiagram = modelDefinition.getDiagram();
            DiagramXmlWriter writer = new DiagramXmlWriter(doc, innerDiagram);
            boolean notificationEnabled = innerDiagram.isNotificationEnabled();
            innerDiagram.setNotificationEnabled( false );
            biouml.standard.state.State currentState = innerDiagram.getCurrentState();
            if( currentState != null )
                innerDiagram.restore();

            element.appendChild(writer.createDml(writer.createDiagram(), new ModelXmlWriter().createModel(diagram, doc), writer.createStates()));

            if( currentState != null )
                innerDiagram.setStateEditingMode( currentState );

            innerDiagram.setNotificationEnabled( notificationEnabled );
        }
        catch( Throwable t )
        {
            error( "ERROR_MODEL_DEFINITION_WRITING", new String[] {diagram.getName(), modelDefinition.getName(), t.getMessage()} );
        }
        return element;
    }

    public Element createNode(Node node)
    {
        Element element = doc.createElement( NODE_ELEMENT );
        setKernelRef( element, node );
        writeNodeInfo( element, node, doc );
        setComment( element, node );
        return element;
    }

    public Element createEdge(Edge edge, @Nonnull Compartment root)
    {
        Element element = doc.createElement( EDGE_ELEMENT );
        setKernelRef( element, edge );
        String inRefStr = CollectionFactory.getRelativeName( edge.getInput(), root );
        String outRefStr = CollectionFactory.getRelativeName( edge.getOutput(), root );
        element.setAttribute( IN_REF_ATTR, inRefStr );
        element.setAttribute( OUT_REF_ATTR, outRefStr );
        writeEdgeInfo( element, edge, doc );
        writeEdgeRole( element, edge, doc );
        setComment( element, edge );
        return element;
    }

    protected Element createFilters(Diagram diagram)
    {
        return createFilters( doc, diagram );
    }

    public static Element createFilters(Document doc, Diagram diagram)
    {
        DiagramFilter[] filters = diagram.getFilterList();
        Element element = doc.createElement( FILTERS_ELEMENT );
        element.setAttribute( TYPE_ATTR, filters.getClass().getName() );
        element.setAttribute( ARRAY_ELEM_TYPE_ATTR, DiagramFilter.class.getName() );
        DynamicPropertySet dps = new DynamicPropertySetAsMap();
        for( DiagramFilter filter : filters )
        {
            Element item = doc.createElement( ITEM_ELEMENT );
            item.setAttribute( TYPE_ATTR, filter.getClass().getName() );
            if( filter.getName() != null )
                serializeDynamicProperty( doc, item, new DynamicProperty( "name", String.class, filter.getName() ), dps );
            if( filter.isEnabled() )
            {
                serializeDynamicProperty( doc, item, new DynamicProperty( "enabled", boolean.class, true ), dps );
                element.setAttribute( SELECTED_FILTER_ATTR, filter.getName() );
            }

            Object properties = filter.getProperties();
            if( properties != null )
            {
                Element property = doc.createElement( PROPERTY_ELEMENT );
                property.setAttribute( NAME_ATTR, "properties" );
                property.setAttribute( TYPE_ATTR, properties.getClass().getName() );
                DPSUtils.writeBeanToDPS( properties, dps, "" );
                serializeDPS( doc, property, dps, new DynamicPropertySetAsMap() );
                item.appendChild( property );
            }
            else
            //TODO: rework serialization for other filters
            {
                DPSUtils.writeBeanToDPS( filter, dps, "" );
                serializeDPS( doc, item, dps, new DynamicPropertySetAsMap() );
            }

            element.appendChild( item );
        }
        return element;
    }

    protected Element createStates()
    {
        Element[] states = StateXmlSerializer.getXmlElements( diagram, doc, this );
        if( states == null )
            return null;
        Element element = doc.createElement( STATES_ELEMENT );
        for( Element state : states )
            element.appendChild( state );
        return element;
    }

    private void setKernelRef(Element element, DiagramElement diagramElement)
    {
        Base kernel = diagramElement.getKernel();
        if( kernel != null && kernel.getOrigin() != null && ! ( kernel instanceof Stub ) )
        {
            String ref = DataElementPath.create( kernel ).toString();
            if( !TextUtil2.isFullPath( ref ) || !kernel.getOrigin().contains( kernel.getName() ) )
                ref = STUB + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ref;

            if( ref != null && ref.length() > 0 )
            {
                element.setAttribute( KERNEL_REF_ATTR, ref );
                element.setAttribute( ID_ATTR, diagramElement.getName() );
                if( ref.startsWith( STUB ) )
                    element.setAttribute( KERNEL_TYPE_ATTR, kernel.getType() );
                return;
            }
        }

        // write dummy
        DataElementPath kernelPath = DiagramUtility.toRepositoryPath(diagramElement.getCompleteNameInDiagram());
        if(kernel != null && !kernel.getName().equals(diagramElement.getName()))
        {
            kernelPath = kernelPath.getSiblingPath(kernel.getName());
            element.setAttribute(ID_ATTR, diagramElement.getName());
        }
        element.setAttribute(KERNEL_REF_ATTR, STUB + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + kernelPath);
        element.setAttribute( KERNEL_TYPE_ATTR, kernel != null ? kernel.getType() : Type.TYPE_UNKNOWN );
        
        if( ( kernel instanceof Reaction ) && ( kernel.getOrigin() == null ) )
        {
            //reaction with NULL parent should be saved into diagram
            Element reaction = doc.createElement( REACTION_ELEMENT );
            Reaction reactionKernel = (Reaction)kernel;
            if( reactionKernel.getKineticLaw() != null && reactionKernel.getKineticLaw().getFormula() != null )
                reaction.setAttribute( REACTION_FORMULA_ATTR, reactionKernel.getKineticLaw().getFormula() );

            for( SpecieReference sr : reactionKernel )
            {
                Element srElement = doc.createElement( SPECIE_REFERENCE_ELEMENT );
                srElement.setAttribute( NAME_ATTR, sr.getName() );
                String role = sr.getRole();
                srElement.setAttribute( ROLE_ATTR, role );
                if( role != null && role.equals( SpecieReference.MODIFIER ) )
                    srElement.setAttribute( MODIFIER_ACTION_ATTR, sr.getModifierAction() );

                srElement.setAttribute( SPECIE_ATTR, sr.getSpecie() );
                srElement.setAttribute( STOICHIOMETRY_ATTR, sr.getStoichiometry() );
                srElement.setAttribute( PARTICIPATION_ATTR, sr.getParticipation() );
                setComment(srElement , sr.getComment());
                reaction.appendChild( srElement );
            }
            element.appendChild( reaction );
            return;
        }
        
        //kernel with null parent should save its attributes to diagram
        if( kernel != null && kernel.getOrigin() == null )
            createAttributes( kernel, element );
    }

   

    public static Element serializeDiagram(Document doc, Diagram diagram)
    {
        return new DiagramXmlWriter( doc, diagram ).createDiagram();
    }

    public static void writeStyle(Element element, DiagramElement de)
    {
        String predefinedStyle = de.getPredefinedStyle();
        if (predefinedStyle.equals(DiagramElementStyle.STYLE_DEFAULT))
            return;
        if( predefinedStyle.equals(DiagramElementStyle.STYLE_NOT_SELECTED) )
        {
            DiagramElementStyle style = de.getCustomStyle();
            element.setAttribute(PEN_ATTR, XmlSerializationUtils.getPenString(style.getPen()));
            element.setAttribute(COLOR_ATTR, ColorUtils.paintToString(style.getBrush().getPaint()));
            element.setAttribute(FONT_ATTR, XmlSerializationUtils.getFontString(style.getFont()));
        }
        else
            element.setAttribute(STYLE_ATTR, predefinedStyle);
    }

    public static Element writeSimulationOptions(Document doc, String elementName, Diagram diagram)
    {
        DynamicProperty dp = diagram.getAttributes().getProperty(SIMULATION_OPTIONS);
        if( dp == null || dp.getValue() == null)
            return null;
        Object value = dp.getValue();
        Element element = doc.createElement(elementName);
        element.setAttribute("type", value.getClass().getName());
        DynamicPropertySet dps = new DynamicPropertySetAsMap();
        DPSUtils.writeBeanToDPS(value, dps, "");
        if( dps.isEmpty() )
            return null;

        serializeDPS(doc, element, dps, null, false);
        return element;
    }

    public Element writePlotsInfo(Document doc, String elementName, Diagram diagram, Map<String, String> newPaths)
    {
        DynamicProperty dp = diagram.getAttributes().getProperty("Plots");
        if( dp == null || dp.getValue() == null )
            return null;
        Object value = dp.getValue();
        PlotsInfo info = (PlotsInfo)value;

        Element element = doc.createElement(elementName);

        for( PlotInfo plotInfo : info.getPlots() )
        {
            Element plotElement = doc.createElement("plot");
            plotElement.setAttribute("active", String.valueOf(plotInfo.isActive()));
            plotElement.setAttribute("title", plotInfo.getTitle());
            plotElement.setAttribute( "autoColorNumber", String.valueOf( plotInfo.getAutoColorNumber() ) );
            //X axis
            plotElement.setAttribute( "xAxisType", plotInfo.getXAxisType() );
            if( plotInfo.getXFrom() != plotInfo.getXTo() )
            {
                plotElement.setAttribute( "xFrom", String.valueOf( plotInfo.getXFrom() ) );
                plotElement.setAttribute( "xTo", String.valueOf( plotInfo.getXTo() ) );
            }
            plotElement.setAttribute( "xAutoRange", String.valueOf( plotInfo.isXAutoRange() ) );

            //Y axis
            plotElement.setAttribute( "yAxisType", plotInfo.getYAxisType() );
            if( plotInfo.getXFrom() != plotInfo.getXTo() )
            {
                plotElement.setAttribute( "yFrom", String.valueOf( plotInfo.getYFrom() ) );
                plotElement.setAttribute( "yTo", String.valueOf( plotInfo.getYTo() ) );
            }
            plotElement.setAttribute( "yAutoRange", String.valueOf( plotInfo.isYAutoRange() ) );

            Element xElement = doc.createElement("xVariable");
            xElement.setAttribute("name", plotInfo.getXVariable().getName());
            xElement.setAttribute("title", plotInfo.getXVariable().getTitle());
            xElement.setAttribute("path", plotInfo.getXVariable().getPath());
            plotElement.appendChild(xElement);

            for( Curve curve : plotInfo.getYVariables() )
            {
                Element curveElement = doc.createElement("yVariable");
                curveElement.setAttribute("name", curve.getName());
                curveElement.setAttribute("title", curve.getTitle());
                curveElement.setAttribute("path", curve.getPath());
                curveElement.setAttribute("pen", XmlSerializationUtils.getPenString(curve.getPen()));
                curveElement.setAttribute("type", curve.getType());
                plotElement.appendChild(curveElement);
            }
            
            if( plotInfo.getExperiments() != null )
            {
                for( Experiment experiment : plotInfo.getExperiments() )
                {
                    Element experimentElement = doc.createElement( "experiment" );
                    
                    String path = experiment.getPath().toString();
                    if (newPaths != null && newPaths.containsKey( path ))
                        path = newPaths.get( path );
                    
                    experimentElement.setAttribute( "path", path );
                    experimentElement.setAttribute( "nameX", experiment.getNameX() );
                    experimentElement.setAttribute( "nameY", experiment.getNameY() );
                    experimentElement.setAttribute( "title", experiment.getTitle() );
                    experimentElement.setAttribute( "pen", XmlSerializationUtils.getPenString( experiment.getPen() ) );
                    plotElement.appendChild(experimentElement);
                }
            }
            element.appendChild(plotElement);
        }
        return element.hasChildNodes() ? element : null;
    }
}
