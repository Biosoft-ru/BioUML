package biouml.model.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Paint;
import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;
import com.developmentontheedge.beans.DynamicPropertySetSupport;
import com.developmentontheedge.beans.editors.StringTagEditor;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramContainer;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementStyle;
import biouml.model.DiagramElementStyleDeclaration;
import biouml.model.DiagramFilter;
import biouml.model.DiagramType;
import biouml.model.DiagramViewOptions;
import biouml.model.Edge;
import biouml.model.EquivalentNodeGroup;
import biouml.model.ModelDefinition;
import biouml.model.Module;
import biouml.model.Node;
import biouml.model.SubDiagram;
import biouml.model.SubDiagram.PortOrientation;
import biouml.model.dynamics.Connection;
import biouml.model.dynamics.DirectedConnection;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.MultipleConnection;
import biouml.model.dynamics.MultipleDirectedConnection;
import biouml.model.dynamics.MultipleUndirectedConnection;
import biouml.model.dynamics.plot.PlotsInfo;
import biouml.model.dynamics.plot.Curve;
import biouml.model.dynamics.plot.Experiment;
import biouml.model.dynamics.plot.PlotInfo;
import biouml.model.dynamics.plot.PlotVariable;
import biouml.model.dynamics.UndirectedConnection;
import biouml.model.dynamics.UndirectedConnection.MainVariableType;
import biouml.model.dynamics.VariableRole;
import biouml.model.xml.XmlDiagramType;
import biouml.model.xml.XmlDiagramTypeConstants;
import biouml.standard.state.StateXmlSerializer;
import biouml.standard.type.Base;
import biouml.standard.type.BaseSupport;
import biouml.standard.type.DatabaseReference;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.ImageDescriptor;
import biouml.standard.type.Reaction;
import biouml.standard.type.SemanticRelation;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import biouml.standard.type.Stub.PlotElement;
import biouml.standard.type.Type;
import biouml.standard.type.DiagramInfo.AuthorInfo;
import one.util.streamex.StreamEx;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementDescriptor;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.security.CodePrivilege;
import ru.biosoft.access.security.CodePrivilegeType;
import ru.biosoft.exception.LoggedClassNotFoundException;
import ru.biosoft.graph.Layouter;
import ru.biosoft.graph.Path;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.editor.GridOptions;
import ru.biosoft.graphics.font.ColorFont;
import ru.biosoft.util.ColorUtils;
import ru.biosoft.util.DPSUtils;
import ru.biosoft.util.TextUtil;
import ru.biosoft.util.XmlStream;
import ru.biosoft.util.XmlUtil;

@CodePrivilege ( CodePrivilegeType.REFLECTION )
// Allow private fields access for XML deserialization
public class DiagramXmlReader extends DiagramXmlSupport implements DiagramReader
{
    protected static final Logger log = Logger.getLogger( DiagramXmlReader.class.getName() );

    private Module module = null;

    protected Map<String, String> newPaths = new HashMap<>();
    
    public void setNewPaths(Map<String, String> newPaths)
    {
        this.newPaths = newPaths;
    }    
    
    /**
     * Static functions that can be used by other applications
     * to write information about diagram nodes location and compartment info
     */
    public static Layouter readLayouterInfo(Element element)
    {
        if( element == null )
            return null;

        Layouter layouter = (Layouter)readElement( element, Layouter.class );
        return layouter == null ? (Layouter)XmlSerializationUtils.deserialize( getElement( element, XmlSerializationUtils.OBJECT_ELEMENT ) )
                : layouter;
    }

    public static void readFilters(Element element, Diagram diagram)
    {
        if( element == null )
            return;

        String selectedFilter = element.getAttribute( SELECTED_FILTER_ATTR );
        DiagramFilter[] filters = new DiagramFilter[0];
        if( filters.getClass().getName().equals( element.getAttribute( TYPE_ATTR ) ) )
        {
            filters = XmlStream.elements( element, ITEM_ELEMENT ).map( item -> {
                DiagramFilter filter = (DiagramFilter)readElement( item, DiagramFilter.class );
                if( filter != null )
                    filter.setLoading( false );
                return filter;
            } ).nonNull().toArray( DiagramFilter[]::new );
        }
        else
        {
            Object filtersObj = XmlSerializationUtils.deserialize( getElement( element, XmlSerializationUtils.ARRAY_ELEMENT ) );
            if( filtersObj != null )
                filters = (DiagramFilter[])filtersObj;
        }
        for( DiagramFilter f : filters )
            initFilter( f, diagram, selectedFilter );
        diagram.setFilterList( filters );
    }

    private static void initFilter(DiagramFilter filter, Diagram diagram, String selectedFilter)
    {
        filter.setDiagram( diagram );
        if( selectedFilter.equals( filter.getName() ) )
            diagram.setDiagramFilter( filter );
    }

    private static Object readElement(Element element, Class<?> expectedType)
    {
        if( element == null )
            return null;
        String type = element.getAttribute( TYPE_ATTR );
        Class<?> clazz;
        try
        {
            clazz = Class.forName( type );
        }
        catch( ClassNotFoundException | NoClassDefFoundError e )
        {
            try
            {
                clazz = ClassLoading.loadClass( type );
            }
            catch( LoggedClassNotFoundException e1 )
            {
                log.warning( e1.getMessage() );
                return null;
            }
        }
        if( !expectedType.isAssignableFrom( clazz ) )
            return null;
        try
        {
            Object result = clazz.newInstance();
            DynamicPropertySet defaults = new DynamicPropertySetAsMap();
            DPSUtils.writeBeanToDPS( result, defaults, "" );
            DynamicPropertySet dps = readDPS( element, defaults );
            DPSUtils.readBeanFromDPS( result, dps, "" );
            return result;
        }
        catch( Throwable t )
        {
            return null;
        }
    }

    public static void readViewOptions(Element element, Diagram diagram)
    {
        GridOptions gridOptions = new GridOptions();
        gridOptions.setUseDefault( false );
        String showGrid = element.getAttribute( SHOW_GRID_ATTR );
        if( !showGrid.isEmpty() )
            gridOptions.setShowGrid( Boolean.parseBoolean( showGrid ) );
        String gridStyle = element.getAttribute( GRID_STYLE_ATTR );
        if( !gridStyle.isEmpty() )
            gridOptions.setGridStyle( gridOptions.getStyleFromString( gridStyle ) );
        String cellSize = element.getAttribute( GRID_CELL_SIZE_ATTR );
        if( !cellSize.isEmpty() )
            gridOptions.setCellSize( Integer.parseInt( cellSize ) );
        String stepSize = element.getAttribute( GRID_STEP_SIZE_ATTR );
        if( !stepSize.isEmpty() )
            gridOptions.setStepSize( Integer.parseInt( stepSize ) );
        diagram.getViewOptions().setGridOptions( gridOptions );
        DynamicPropertySet defaults = new DynamicPropertySetAsMap();
        DPSUtils.writeBeanToDPS( diagram.getType().getDiagramViewBuilder().createDefaultDiagramViewOptions(), defaults, "" );
        DynamicPropertySet dps = readDPS( element, defaults );
        DPSUtils.readBeanFromDPS( diagram.getViewOptions(), dps, "" );
    }

    public static void readSimulationOptions(Element element, Diagram diagram)
    {
        try
        {
            String className = element.getAttribute(TYPE_ATTR);
            if( !className.isEmpty() )
            {
                Class<?> clazz = ClassLoading.loadClass(className);
                Object object = clazz.newInstance();
                DynamicPropertySet dps = readDPS(element, null);
                DPSUtils.readBeanFromDPS(object, dps, "");
                diagram.getAttributes().add(DPSUtils.createHiddenReadOnlyTransient(SIMULATION_OPTIONS, object.getClass(), object));
            }
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "Error reading simulation engine", ex);
        }
    }

    public static void readPlotsInfo(Element element, Diagram diagram, Map<String, String> newPaths)
    {
        try
        {
            if( ! ( diagram.getRole() instanceof EModel ) )
                return;

            EModel emodel = diagram.getRole(EModel.class);
            PlotsInfo plotsInfo = new PlotsInfo(emodel);

            List<PlotInfo> plotInfos = new ArrayList<>();
            for( Element plotElement : XmlUtil.elements(element, "plot") )
            {
                PlotInfo plotInfo = new PlotInfo();
                plotInfo.setEModel(emodel);
                plotInfo.setActive(Boolean.parseBoolean(plotElement.getAttribute("active")));
                plotInfo.setTitle(plotElement.getAttribute("title"));
                //X axis
                String xType = plotElement.getAttribute( "xAxisType" );
                if( !xType.isEmpty() )
                    plotInfo.setXAxisType( xType );
                String from = plotElement.getAttribute( "xFrom" );
                String to = plotElement.getAttribute( "xTo" );
                plotInfo.setXFrom( from.isEmpty() ? 0.0 : Double.parseDouble( from ) );
                plotInfo.setXTo( to.isEmpty() ? 0.0 : Double.parseDouble( to ) );
                if( !plotElement.getAttribute( "xAutoRange" ).isEmpty() )
                    plotInfo.setXAutoRange( Boolean.parseBoolean( plotElement.getAttribute( "xAutoRange" ) ) );

                ///X axis
                String yType = plotElement.getAttribute( "yAxisType" );
                if( !yType.isEmpty() )
                    plotInfo.setYAxisType( yType );
                from = plotElement.getAttribute( "yFrom" );
                to = plotElement.getAttribute( "yTo" );
                plotInfo.setYFrom( from.isEmpty() ? 0.0 : Double.parseDouble( from ) );
                plotInfo.setYTo( to.isEmpty() ? 0.0 : Double.parseDouble( to ) );
                if( !plotElement.getAttribute( "yAutoRange" ).isEmpty() )
                    plotInfo.setYAutoRange( Boolean.parseBoolean( plotElement.getAttribute( "yAutoRange" ) ) );


                plotInfos.add(plotInfo);

                Element xElement = XmlUtil.findElementByTagName(plotElement, "xVariable");
                plotInfo.setXVariable(new PlotVariable(xElement.getAttribute("path"), xElement.getAttribute("name"),
                        xElement.getAttribute("title"), emodel));

                List<Curve> curves = new ArrayList<>();
                for( Element cElement : XmlUtil.elements(plotElement, "yVariable") )
                {
                    Curve curve = new Curve(cElement.getAttribute("path"), cElement.getAttribute("name"), cElement.getAttribute("title"),
                            emodel);
                    curve.setPen(XmlSerializationUtils.readPen(cElement.getAttribute("pen")));
                    curve.setType(cElement.getAttribute("type"));
                    curves.add(curve);
                }
                
                List<Experiment> experiments = new ArrayList<>();
                for( Element cElement : XmlUtil.elements( plotElement, "experiment" ) )
                {
                    String path = cElement.getAttribute( "path" );
                    if( newPaths != null && newPaths.containsKey( path ) )
                        path = newPaths.get( path );
                    experiments.add( new Experiment( DataElementPath.create( path ), cElement.getAttribute( "nameX" ),
                            cElement.getAttribute( "nameY" ), cElement.getAttribute( "title" ),
                            XmlSerializationUtils.readPen( cElement.getAttribute( "pen" ) ) ) );
                }
                if( !experiments.isEmpty() )
                    plotInfo.setExperiments( StreamEx.of( experiments ).toArray( Experiment[]::new ) );

                String autoColorNumberStr = plotElement.getAttribute( "autoColorNumber" );
                try
                {
                    int autoColorNumber = autoColorNumberStr.isEmpty() ? curves.size() : Integer.parseInt( autoColorNumberStr );
                    plotInfo.setAutoColorNumber( autoColorNumber );
                }
                catch( NumberFormatException e )
                {
                    log.log( Level.WARNING, "Invalid 'autoColorNumber' attribute value in plots infos: " + autoColorNumberStr );
                }
                plotInfo.setYVariables(curves.stream().toArray(Curve[]::new));

            }

            plotsInfo.setPlots(StreamEx.of(plotInfos).toArray(PlotInfo[]::new));
            diagram.getAttributes().add(DPSUtils.createHiddenReadOnlyTransient("Plots", PlotsInfo.class, plotsInfo));
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "Error reading plot infos", ex);
        }
    }

    /**
     * @param node 'compartmentInfo' element.
     * @param compartment Compartment to which information will be applied.
     * TODO: error processing
     */
    public static void readCompartmentInfo(Element element, Compartment compartment, String diagramName)
    {
        boolean notificationEnabled = compartment.isNotificationEnabled();
        if( notificationEnabled )
            compartment.setNotificationEnabled( false );

        getTitle( element, compartment );

        compartment.setLocation( readLocation( element, compartment.getName(), diagramName ) );
        compartment.setShapeSize( readDimension( element, diagramName, compartment.getName() ) );
        compartment.setVisible( !Boolean.parseBoolean( element.getAttribute( IS_HIDDEN_ATTR ) ) );

        if( element.hasAttribute(IS_TITLE_HIDDEN_ATTR) )
            compartment.setShowTitle(!Boolean.parseBoolean( element.getAttribute( IS_TITLE_HIDDEN_ATTR ) ) );

        readStyle(element, compartment);

        XmlStream.elements( element, IMAGE_ELEMENT ).findFirst().ifPresent( img -> {
            ImageDescriptor imageDesc = readImage( img, diagramName, compartment.getName() );
            if( imageDesc != null )
            {
                compartment.setUseCustomImage(true);
                compartment.setImage( imageDesc );
            }
        } );

        if( element.hasAttribute( SHAPE_ATTR ) )
            compartment.setShapeType( Integer.parseInt( element.getAttribute( SHAPE_ATTR ) ) );

        if( notificationEnabled )
            compartment.setNotificationEnabled( true );
    }

    public static void readNodeInfo(Element element, Node node, String diagramName)
    {
        node.setNotificationEnabled( false );

        if( element.hasAttribute( TITLE_ATTR ) )
            node.setTitle( element.getAttribute( TITLE_ATTR ) );

        if( element.hasAttribute( COMMENT_ATTR ) )
            node.setComment( element.getAttribute( COMMENT_ATTR ) );

        node.setLocation( readLocation( element, node.getName(), diagramName ) );
        node.setShapeSize( readDimension( element, diagramName, node.getName() ) );
        node.setVisible( !Boolean.parseBoolean( element.getAttribute( IS_HIDDEN_ATTR ) ) );

        if( element.hasAttribute(IS_TITLE_HIDDEN_ATTR) )
            node.setShowTitle(!Boolean.parseBoolean( element.getAttribute( IS_TITLE_HIDDEN_ATTR ) ) );
        else if (node.getKernel().getType().equals(Type.MATH_EQUATION) || node.getKernel().getType().equals(Type.MATH_FUNCTION)) //support for old diagrams
            node.setShowTitle(false);

        readStyle(element, node);

        if( element.hasAttribute( FIXED_ATTR ) )
            node.setFixed( true );

        processHighlightAttr( element, node ); //support for old diagrams

        // read note background info
        if( node.getKernel() instanceof Stub.Note )
        {
            if( "false".equals( element.getAttribute( BACKGROUND_VISIBLE_ATTR ) ) )
                ( (Stub.Note)node.getKernel() ).setBackgroundVisible( false );

            //old style
            if( element.hasAttribute(BACKGROUND_COLOR_ATTR) )
            {
                node.setPredefinedStyle(DiagramElementStyle.STYLE_NOT_SELECTED);
                node.getCustomStyle()
                        .setBrush(new Brush(stringToColor(element.getAttribute(BACKGROUND_COLOR_ATTR), diagramName, node.getName())));
            }
        }

        XmlStream.elements( element, IMAGE_ELEMENT ).findFirst().ifPresent( img -> {
            ImageDescriptor imageDesc = readImage( img, diagramName, node.getName() );
            if( imageDesc != null )
            {
                node.setUseCustomImage(true);
                node.setImage( imageDesc );
            }
        } );

        node.setNotificationEnabled( true );
        DiagramType diagramType = Diagram.getDiagram( node ).getType();
        DynamicPropertySet properties = diagramType.getProperties();
        if( properties != null )
        {
            fillProperties( element, node.getAttributes(), properties );
            if( diagramType instanceof XmlDiagramType )
                ( (XmlDiagramType)diagramType ).fillPropertyEditorParameters( node.getAttributes() );
        }
    }

    /**
     * Processes highlight attribute (in old diagrams) and saves it to the style property.
     * Should be removed in the future.
     */
    private static void processHighlightAttr(Element element, Node node)
    {
        for( Element child : XmlUtil.elements( element ) )
        {
            String name = child.getAttribute( NAME_ATTR );
            if( !name.equals( "highlight" ) )
                continue;
            String highlight = child.getAttribute( VALUE_ATTR );
            element.removeChild( child );
            if( highlight.equals( "none" ) )
                break;

            DiagramViewOptions opts = Diagram.getDiagram( node ).getViewOptions();
            if( opts.getStyle( highlight ) == null )
            {
                String highlightColor = highlight.equals( "highlight3" ) ? "#FFCEFF"
                        : highlight.equals( "highlight2" ) ? "#FFCECE" : "#CCF";
                DiagramElementStyleDeclaration newStyle = new DiagramElementStyleDeclaration(highlight);
                newStyle.getStyle().setBrush( new Brush( ColorUtils.parseColor( highlightColor ) ) );
                opts.addStyleIfAbsent( newStyle );
            }
            node.setPredefinedStyle( highlight );
            return;
        }
    }

    public static String readEdgeID(Element element)
    {
        return element.hasAttribute( EDGE_ID_ATTR ) ? element.getAttribute( EDGE_ID_ATTR ) : "";
    }

    public static void readEdgeInfo(Element element, Edge edge, String diagramName)
    {
        edge.setNotificationEnabled( false );

        NodeList pathNodes = element.getElementsByTagName( "path" );
        if( pathNodes.getLength() > 0 )
        {
            NodeList segments = ( (Element)pathNodes.item( 0 ) ).getElementsByTagName( "segment" );
            Path path = new Path();
            for( Element segment : XmlUtil.elements( segments ) )
            {
                String strType = segment.getAttribute( "segmentType" );
                int type = 0;
                if( strType.equals( LINE_LINETO ) )
                    type = 0;
                else if( strType.equals( LINE_QUADRIC ) )
                    type = 1;
                else if( strType.equals( LINE_CUBIC ) )
                    type = 2;
                int x = Integer.parseInt( segment.getAttribute( "x0" ) );
                int y = Integer.parseInt( segment.getAttribute( "y0" ) );
                path.addPoint( x, y, type );
            }
            edge.setPath( path );
        }
        else
        {
            if( element.hasAttribute( INPORT_ATTR ) )
                edge.setInPort( XmlSerializationUtils.readPoint( element.getAttribute( INPORT_ATTR ) ) );
            if( element.hasAttribute( OUTPORT_ATTR ) )
                edge.setOutPort( XmlSerializationUtils.readPoint( element.getAttribute( OUTPORT_ATTR ) ) );
        }
        if( element.hasAttribute( TITLE_ATTR ) )
            edge.setTitle( element.getAttribute( TITLE_ATTR ) );

        if( element.hasAttribute( COMMENT_ATTR ) )
            edge.setComment( element.getAttribute( COMMENT_ATTR ) );

        if (element.hasAttribute(FIXED_ATTR))
            edge.setFixed(true);

        if( element.hasAttribute( FIXED_IN_OUT_ATTR ) )
            edge.setFixedInOut( true );

        readStyle(element, edge);
        edge.setNotificationEnabled( true );
        DiagramType diagramType = Diagram.getDiagram( edge ).getType();
        DynamicPropertySet properties = diagramType.getProperties();
        if( properties != null )
        {
            fillProperties( element, edge.getAttributes(), properties );
            if( diagramType instanceof XmlDiagramType )
            {
                ( (XmlDiagramType)diagramType ).fillPropertyEditorParameters( edge.getAttributes() );
                if( edge.getKernel() instanceof SpecieReference )
                {
                    Object role = edge.getAttributes().getValue( XmlDiagramTypeConstants.KERNEL_ROLE_ATTR );
                    if( role != null )
                        ( (SpecieReference)edge.getKernel() ).setRole( role.toString() );
                }
            }
        }
    }

    protected static void getTitle(Element element, DiagramElement de)
    {
        if( element.hasAttribute( TITLE_ATTR ) )
            de.setTitle( element.getAttribute( TITLE_ATTR ) );
    }

    protected static Point readLocation(Element element, String nodeName, String diagramName)
    {
        String x = getRequiredAttribute( element, X_ATTR, diagramName );
        String y = getRequiredAttribute( element, Y_ATTR, diagramName );

        if( x == null || y == null )
            return new Point( 0, 0 );
        try
        {
            return new Point( (int)Double.parseDouble( x ), (int)Double.parseDouble( y ) );
        }
        catch( Throwable t )
        {
            error( "ERROR_LOCATION_PARSING", new String[] {diagramName, nodeName, x, y, t.getMessage()} );
        }
        return new Point( 0, 0 );
    }

    protected static @Nonnull Dimension readDimension(Element element, String diagramName, String elementName)
    {
        Dimension dim = new Dimension( 50, 50 ); //default

        String width = element.getAttribute( WIDTH_ATTR );
        String height = element.getAttribute( HEIGHT_ATTR );
        if( !width.isEmpty() && !height.isEmpty() )
        {
            try
            {
                dim = new Dimension( (int)Double.parseDouble( width ), (int)Double.parseDouble( height ) );
            }
            catch( Throwable t )
            {
                error( "ERROR_SIZE_PARSING", new String[] {diagramName, elementName, width, height, t.getMessage()} );
            }
        }

        return dim;
    }

    public static Paint stringToColor(String colorStr, String diagramName, String name)
    {
        if( !colorStr.contains( "," ) || colorStr.contains( "rgb" ) )
            return ColorUtils.parsePaint( colorStr );
        // Old diagrams go here
        Color color = Color.gray;
        try
        {
            String[] vals = TextUtil.split( colorStr, ',' );
            if( vals.length <= 4 )
            {
                int alpha = 255;
                int r = Integer.parseInt( vals[0].trim() );
                int g = Integer.parseInt( vals[1].trim() );
                int b = Integer.parseInt( vals[2].trim() );
                if( vals.length > 3 )
                {
                    alpha = Integer.parseInt( vals[3].trim() );
                }
                color = new Color( r, g, b, alpha );
            }
            else
            {
                int i = 3;
                int alpha = 255;
                if( vals.length > 7 )
                {
                    i++;
                    alpha = Integer.parseInt( vals[7].trim() );
                }
                int r = Integer.parseInt( vals[i++].trim() );
                int g = Integer.parseInt( vals[i++].trim() );
                int b = Integer.parseInt( vals[i++].trim() );
                Color color2 = new Color( r, g, b, alpha );
                return new GradientPaint( 0, 0, color, 100, 100, color2 );
            }
        }
        catch( Throwable t )
        {
            error( "ERROR_COLOR_PARSING", new String[] {diagramName, name, colorStr, t.getMessage()} );
        }

        return color;
    }

    protected static ImageDescriptor readImage(Element element, String diagramName, String nodeName)
    {

        String path = element.getAttribute(PATH_ATTR);
        if (path!= null)
              return new ImageDescriptor(DataElementPath.create(path));

        String src = getRequiredAttribute( element, SRC_ATTR, diagramName );
        if( src == null )
            return null;
        try
        {
            return new ImageDescriptor( src, readDimension( element, diagramName, nodeName ) ); // to load
        }
        catch( Throwable t )
        {
            warn( "ERROR_IMAGE_PROCESSING", new String[] {diagramName, nodeName, src, t.getMessage()} );
        }
        return null;
    }

    public void readEdgeRole(Element element, Edge edge, String diagramName)
    {
        Element connectionElement = getElement( element, CONNECTION_ROLE_ELEMENT );
        if( connectionElement == null )
            return;
        String type = getRequiredAttribute( connectionElement, TYPE_ATTR, diagramName );
        if( type == null )
            return;
        Connection role;
        try
        {
            Class<? extends Connection> connectionClass = ClassLoading.loadSubClass( type, null, Connection.class );
            role = connectionClass.getConstructor( Edge.class ).newInstance( edge );
        }
        catch( Throwable t )
        {
            error( "ERROR_READ_CONNECTION", new String[] {diagramName, edge.getName(), t.getMessage()} );
            return;
        }

        //dirty hack for correct visualization for connections wihtout titles
        if( !element.hasAttribute( TITLE_ATTR ) )
            edge.setTitle( "" );

        Element inPort = getElement( connectionElement, CONNECTION_INPUT_ELEMENT );
        if( inPort != null )
            role.setInputPort( new Connection.Port( inPort.getAttribute( ID_ATTR ), inPort.getAttribute( TITLE_ATTR ) ) );

        Element outPort = getElement( connectionElement, CONNECTION_OUTPUT_ELEMENT );
        if( outPort != null )
            role.setOutputPort( new Connection.Port( outPort.getAttribute( ID_ATTR ), outPort.getAttribute( TITLE_ATTR ) ) );

        if( role instanceof DirectedConnection )
        {
            String function = connectionElement.getAttribute( FORMULA_ATTR );
            if( !function.isEmpty() )
                ( (DirectedConnection)role ).setFunction( function );
        }
        else if( role instanceof UndirectedConnection )
        {
            String mainVariable = connectionElement.getAttribute( MAIN_VARIABLE_ATTR );
            if( !mainVariable.isEmpty() )
                ( (UndirectedConnection)role ).setMainVariableType(MainVariableType.valueOf(mainVariable));
        }
        else if( role instanceof MultipleConnection )
        {
            Element connectionListElement = getElement( connectionElement, CONNECTION_LIST_ELEMENT );

            NodeList list = connectionListElement.getChildNodes();
            for( Element connectionListItemElement : XmlUtil.elements( list ) )
            {
                if( connectionListItemElement.getNodeName().equals( CONNECTION_ROLE_ELEMENT ) )
                {
                    Element innerInPort = getElement( connectionListItemElement, CONNECTION_INPUT_ELEMENT );
                    Element innerOutPort = getElement( connectionListItemElement, CONNECTION_OUTPUT_ELEMENT );

                    if( role instanceof MultipleDirectedConnection )
                    {
                        DirectedConnection c = new DirectedConnection( edge );
                        c.setInputPort( new Connection.Port( innerInPort.getAttribute( ID_ATTR ), innerInPort.getAttribute( TITLE_ATTR ) ) );
                        c.setOutputPort( new Connection.Port( innerOutPort.getAttribute( ID_ATTR ), innerOutPort.getAttribute( TITLE_ATTR ) ) );
                        c.setFunction( connectionListItemElement.getAttribute( FORMULA_ATTR ) );
                        ( (MultipleConnection)role ).addConnection( c );
                    }
                    else if( role instanceof MultipleUndirectedConnection )
                    {
                        UndirectedConnection c = new UndirectedConnection( edge );
                        c.setInputPort( new Connection.Port( innerInPort.getAttribute( ID_ATTR ), innerInPort.getAttribute( TITLE_ATTR ) ) );
                        c.setOutputPort( new Connection.Port( innerOutPort.getAttribute( ID_ATTR ), innerOutPort.getAttribute( TITLE_ATTR ) ) );
                        String initialValue = connectionListItemElement.getAttribute( INITIAL_VALUE_ATTR );
                        if( !initialValue.isEmpty() )
                            c.setInitialValue( Double.parseDouble( initialValue ) );
                        ( (MultipleConnection)role ).addConnection( c );
                    }
                }
            }
        }

        edge.setRole( role );
    }

    // //////////////////////////////////////////////////////////////////////////
    // Constructors and public methods
    //

    protected InputStream stream;

    protected DiagramInfo diagramInfo;

    protected String name;

    protected boolean getKernels = true;

    protected DiagramType defaultDiagramType = new XmlDiagramType();

    /**
     * This constructor is used to read diagram from stream and can be used to
     * read diagram from relational database (TEXT or BLOB).
     *
     * @param name
     *            diagram name
     * @param stream
     *            stream that contains diagram XML
     * @param info
     *            DiagramInfo can be stored as separated record in relational
     *            databases, so if info is specified, it will be used instead of
     *            DiagramInfo stored in XML.
     */
    public DiagramXmlReader(String name, InputStream stream, DiagramInfo diagramInfo)
    {
        this.name = name;
        this.stream = stream;
        this.diagramInfo = diagramInfo;
    }

    public void setDefaultDiagramType(DiagramType defaultDiagramType)
    {
        this.defaultDiagramType = defaultDiagramType;
    }

    public DiagramXmlReader(String name)
    {
        this.name = name;
    }

    public DiagramXmlReader()
    {

    }

    public Diagram read(DataCollection origin, Module module) throws Exception
    {
        Document document = createDocument(name, stream, diagramInfo);
        this.module = module;
        Element root = document.getDocumentElement();
        Element diagramElement = getElement( root, DIAGRAM_ELEMENT );
        if( diagramElement == null )
            throw new Exception("Diagram element not found while reading diagram "+name);
        DiagramType type = readDiagramType(diagramElement, origin, new XmlDiagramType());
        return read(origin, type, document);
    }

    /**
     * Method will automatically detect diagram type and try to use DiagramXmlReader returned by DiagramType object
     */
    public static Diagram readDiagram(String name, InputStream stream, DiagramInfo diagramInfo, DataCollection origin, Module module) throws Exception
    {
        return readDiagram(name, stream, diagramInfo, origin, module, null, null);
    }

    public static Diagram readDiagram(String name, InputStream stream, DiagramInfo diagramInfo, DataCollection origin, Module module,
            List<String> requestedKernels, Map<String, String> newPaths) throws Exception
    {
        Document document = createDocument(name, stream, diagramInfo);

        Element root = document.getDocumentElement();
        Element diagramElement = getElement( root, DIAGRAM_ELEMENT );
        if( diagramElement == null )
            throw new Exception("Diagram element not found while reading diagram "+name);

        DiagramType type = readDiagramType(diagramElement, origin, new XmlDiagramType());
        DiagramXmlReader reader = type.getDiagramReader();
        reader.setNewPaths( newPaths );
        reader.name = name;
        reader.module = module;
        reader.diagramInfo = diagramInfo;
        reader.getKernels = requestedKernels == null;
        Diagram diagram = reader.read(origin, type, document);
        getTitle( diagramElement, diagram );

        if( requestedKernels != null )
            requestedKernels.addAll(reader.getRequestedKernelsNameList());
        return diagram;
    }

    String version = DiagramXmlWriter.VERSION;

    private Diagram read(DataCollection origin, DiagramType diagramType, Document doc) throws Exception
    {
        return read( origin, diagramType, doc.getDocumentElement() );
    }

    private Diagram read(DataCollection origin, DiagramType diagramType, Element root) throws Exception
    {
        if( root.hasAttribute( VERSION_ATTR ) )
            version = root.getAttribute( VERSION_ATTR );

        Element diagramElement = getElement( root, DIAGRAM_ELEMENT );
        if( diagramElement == null )
        {
            error( "ERROR_DIAGRAM_ELEMENT_ABSENTS", new String[] {name} );
            // TODO - fix
            return null;
        }

        diagram = readDiagram( diagramElement, diagramType, origin );

        readRole(root, diagram);

        Element plotElement = getElement(diagramElement, PLOTS_ELEMENT);
        if( plotElement != null )
            readPlotsInfo(plotElement, diagram, newPaths);

        Element statesElement = getElement( root, STATES_ELEMENT );

        //Support for old version
        if( statesElement == null )
            statesElement = getElement( root, EXPERIMENTS_ELEMENT );

        if( statesElement != null )
            readStates( statesElement, diagram );

        //Set notification enabled for diagram and all subdiagrams.
        diagram.setNotificationEnabled( true );
        return diagram;
    }

    /**
     * Method to read role of the diagram (usually EModel)
     * It should be overridden in subclasses to read specific to given diagram type roles
     */
    protected void readRole(Element root, Diagram diagram)
    {
        Element modelElement = getElement( root, EXECUTABLE_MODEL_ELEMENT );
        if( modelElement != null )
        {
            ModelXmlReader reader = createModelReader( diagram );
            reader.setNewPaths( newPaths );
            diagram.setRole(reader.readModel( modelElement ));
        }
    }

    protected ModelXmlReader createModelReader(Diagram diagram)
    {
        return new ModelXmlReader( diagram );
    }

    // //////////////////////////////////////////////////////////////////////////
    // Read structural model (diagram)
    //

    protected Diagram readDiagram(Element diagramElement, DiagramType diagramType, DataCollection origin) throws Exception
    {
        diagram = null;

        if( diagramInfo == null )
        {
            diagramInfo = new DiagramInfo( name );

            Element diagramInfoElement = getElement( diagramElement, DIAGRAM_INFO_ELEMENT );
            if( diagramInfoElement != null )
            {
                // v. 0.7.2
                if( diagramInfoElement.hasAttribute( DIAGRAM_INFO_ATTR ) )
                    diagramInfo.setDescription( diagramInfoElement.getAttribute( DIAGRAM_INFO_ATTR ) );

                // v. 0.7.3
                else
                {
                    XmlStream.nodes( diagramInfoElement ).findFirst( child -> child.getNodeType() == org.w3c.dom.Node.CDATA_SECTION_NODE )
                            .ifPresent( child -> diagramInfo.setDescription( child.getNodeValue() ) );
                }
                if( diagramInfoElement.hasAttribute( TITLE_ATTR ) )
                    diagramInfo.setTitle( diagramInfoElement.getAttribute( TITLE_ATTR ) );
                if( diagramInfoElement.hasAttribute( COMMENT_ATTR ) )
                    diagramInfo.setComment( diagramInfoElement.getAttribute( COMMENT_ATTR ) );
                Element attrElement = getElement( diagramInfoElement, ATTRIBUTES_ELEMENT );
                if( attrElement != null )
                    fillProperties( attrElement, diagramInfo.getAttributes(), null );
                List<DatabaseReference> dbRefs = new ArrayList<>();
                for( Element dbRef : XmlUtil.elements( diagramInfoElement, DATABASE_REFERENCE_ELEMENT ) )
                    dbRefs.add( new DatabaseReference( dbRef.getAttribute( VALUE_ATTR ) ) );

                if( !dbRefs.isEmpty() )
                    diagramInfo.setDatabaseReferences( dbRefs.toArray( new DatabaseReference[dbRefs.size()] ) );
                List<String> litRefs = new ArrayList<>();
                for( Element litRef : XmlUtil.elements( diagramInfoElement, LITERATURE_REFERENCE_ELEMENT ) )
                    litRefs.add( litRef.getAttribute( VALUE_ATTR ) );

                if( !litRefs.isEmpty() )
                    diagramInfo.setLiteratureReferences( litRefs.toArray( new String[litRefs.size()] ) );
                
                readCreationInfo(diagramInfo, diagramInfoElement);
            }
        }

        try
        {
            if( diagramType == null )
                diagramType = readDiagramType(diagramElement, origin, defaultDiagramType);
            diagram = diagramType.createDiagram( origin, name, diagramInfo );

            //read diagram attributes
            Element diagramInfoElement = getElement( diagramElement, DIAGRAM_INFO_ELEMENT );
            if( diagramInfoElement != null )
            {
                DynamicPropertySet properties = diagramType.getProperties();
                if( properties != null )
                    fillProperties( diagramInfoElement, diagram.getAttributes(), properties );
            }
        }
        catch( Throwable t )
        {
            error( "ERROR_DIAGRAM_TYPE", new String[] {name, diagramElement.getAttribute( DIAGRAM_TYPE_ATTR ), t.getMessage()} );
        }

        diagram.setNotificationEnabled( false );

        Element viewOptionsElement = getElement( diagramElement, VIEW_OPTIONS_ELEMENT );
        if( viewOptionsElement != null )
            readViewOptions( viewOptionsElement, diagram );


        Element layouterInfoElement = getElement( diagramElement, LAYOUTER_INFO_ELEMENT );
        if( layouterInfoElement != null )
        {
            Layouter layouter = readLayouterInfo( layouterInfoElement );
            if( layouter != null )
                diagram.setPathLayouter( layouter );
        }

        Element labelLayouterInfoElement = getElement( diagramElement, LABEL_LAYOUTER_INFO_ELEMENT );
        if( labelLayouterInfoElement != null )
        {
            Layouter layouter = readLayouterInfo( labelLayouterInfoElement );
            if( layouter != null )
                diagram.setLabelLayouter( layouter );
        }

        Element nodesElement = getElement( diagramElement, NODES_ELEMENT );
        if( nodesElement == null )
            error( "ERROR_NODES_ABSENT", new String[] {diagram.getName(), diagram.getName()} );
        else
            readNodes( nodesElement, diagram );

        //Set notification false for diagram and all subdiagrams.
        diagram.setNotificationEnabled( false );

        Element edgesElement = getElement( diagramElement, EDGES_ELEMENT );
        if( edgesElement == null )
            error( "ERROR_EDGES_ABSENT", new String[] {diagram.getName(), diagram.getName()} );
        else
            readEdges( edgesElement, diagram );

        // fix bug open old diagrams
        readAndApplyWrongEdges();

        //read filters after all nodes and edges
        diagram.setNotificationEnabled( true );
        Element filtersElement = getElement( diagramElement, FILTERS_ELEMENT );
        if( filtersElement != null )
            readFilters( filtersElement, diagram );
        diagram.setNotificationEnabled( false );

        Element simulationElement = getElement(diagramElement, SIMULATION_OPTIONS);
        if( simulationElement != null )
            readSimulationOptions(simulationElement, diagram);

        // set view builder for new diagram elements
        diagram.setNodeViewBuilders();
        return diagram;
    }
    
    protected void readCreationInfo(DiagramInfo info, Element element)
    {
        List<AuthorInfo> authors = new ArrayList<>();
        Element authorsElement = getElement( element, AUTHORS_ELEMENT );
        if( authorsElement != null )
        {
            for( Element authorElement : XmlUtil.elements( authorsElement ) )
            {
                AuthorInfo authorInfo = new AuthorInfo();
                authorInfo.setFamilyName( authorElement.getAttribute( FAMILY_NAME_ATTR ) );
                authorInfo.setGivenName( authorElement.getAttribute( GIVEN_NAME_ATTR ) );
                authorInfo.setEmail( authorElement.getAttribute( EMAIL_ATTR ) );
                authorInfo.setOrgName( authorElement.getAttribute( ORGANISATION_ATTR ) );
                authors.add( authorInfo );
            }
        }
        info.setAuthors( authors.toArray(new AuthorInfo[authors.size()]));

        Element historyElement = getElement( element, HISTORY_ELEMENT );
        if( historyElement != null )
        {
            Element createdElement = getElement( historyElement, CREATED_ELEMENT );
            if( createdElement != null )
                info.setCreated( createdElement.getAttribute( DATE_ATTR ) );

            List<String> modified = new ArrayList<>();
            for( Element modifiedElement : XmlUtil.elements( historyElement, MODIFIED_ELEMENT ) )
                modified.add( modifiedElement.getAttribute( DATE_ATTR ) );
            info.setModified( modified.toArray(new String[modified.size()]));
        }
    }

    public static DiagramType readDiagramType(Element diagramElement, DataCollection origin, DiagramType defaultDiagramType) throws Exception
    {
        String type = diagramElement.getAttribute(DIAGRAM_TYPE_ATTR);

        // for compatibility with v.0.7.2
        if( type.trim().length() == 0 )
            type = diagramElement.getAttribute("diagram_type");
        if( type.equals("biouml.standard.diagram.OntologyDiagramType") )
            type = "biouml.standard.diagram.SemanticNetworkDiagramType";
        if( type.equals("biouml.standard.diagram.pathway.PathwayDiagramType") )
            type = "biouml.standard.diagram.PathwayDiagramType";
        if( type.equals("biouml.standard.diagram.pathway.PathwaySimulationDiagramType") )
            type = "biouml.standard.diagram.PathwaySimulationDiagramType";

        //        if( type.equals( "biouml.standard.diagram.MathDiagramType" ) )
        //            type = "biouml.plugins.sbgn.SbgnDiagramType";

        if( type.equals(XmlDiagramType.class.getName()) )
        {
            String notation = diagramElement.getAttribute(DIAGRAM_XML_NOTATION);
            // for compatibility with v.0.9.0
            if( notation.startsWith("data/graphic notations/") )
                notation = notation.replaceFirst("data/graphic notations", XmlDiagramType.DEFAULT_NOTATION_PATH);

            DataElement xdt = CollectionFactory.getDataElement(notation);
            return xdt != null ? ( (XmlDiagramType)xdt ).clone() : defaultDiagramType;
        }
        else
        {
            String pluginNames = null;
            if( origin != null )
                pluginNames = origin.getInfo().getProperties().getProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY);

            String pluginForClass = ClassLoading.getPluginForClass(type);
            return ClassLoading.loadSubClass(type, pluginForClass == null ? pluginNames : pluginForClass, DiagramType.class).newInstance();
        }
    }

    /**Creates appropriate compartment from element<br>
     * Override this method to create compartments of type/role specific to your particular diagram type
     */
    public Compartment createCompartment(Element element, String id, Base kernel, Compartment origin)
    {
        if( element.getNodeName().equals( EQUIVALENTNODEGROUP_ELEMENT ) )
            return new EquivalentNodeGroup( origin, id );
        else
            return new Compartment( origin, id, kernel );
    }

    /**Creates appropriate node from element<br>
     * Override this method to create compartments of type/role specific to your particular diagram type
     */
    public Node createNode(Element element, String id, Base kernel, Compartment origin)
    {
        return new Node(origin, id, kernel);
    }

    public Compartment readCompartment(Element compartmentElement, Compartment origin)
    {
        Element compartmentInfoElement = getElement( compartmentElement, COMPARTMENT_INFO_ELEMENT );
        if( compartmentInfoElement == null )
        {
            error( "ERROR_COMPARTMENT_INFO_ABSENTS", new String[] {diagram.getName(), "parent=" + origin.getName()} );
            return null;
        }

        Base kernel = getKernel( compartmentInfoElement, origin );

        String id = kernel.getName();
        if( compartmentElement.hasAttribute( ID_ATTR ) )
            id = compartmentElement.getAttribute( ID_ATTR );
        if( compartmentInfoElement.hasAttribute( ID_ATTR ) )
            id = compartmentInfoElement.getAttribute( ID_ATTR );

        Compartment compartment = createCompartment(compartmentElement, id, kernel, origin);
        //fill attributes before validating, they may be used during validation
        DiagramType diagramType = Diagram.getDiagram( compartment ).getType();
        DynamicPropertySet properties = diagramType.getProperties();
        if( properties != null )
        {
            fillProperties( compartmentElement, compartment.getAttributes(), properties );
            if( diagramType instanceof XmlDiagramType )
                ( (XmlDiagramType)diagramType ).fillPropertyEditorParameters( compartment.getAttributes() );
        }

        compartment = (Compartment)validate( origin, compartment );
        compartment.setNotificationEnabled( false );

        readCompartmentInfo( compartmentInfoElement, compartment, diagram.getName() );
        readComment(compartment, compartmentInfoElement);

        Element nodesElement = getElement( compartmentElement, NODES_ELEMENT );
        if( nodesElement == null )
            error( "ERROR_NODES_ABSENT", new String[] {diagram.getName(), compartment.getName()} );
        else
            readNodes( nodesElement, compartment );

        Element edgesElement = getElement( compartmentElement, EDGES_ELEMENT );
        if( edgesElement == null )
            error( "ERROR_EDGES_ABSENT", new String[] {diagram.getName(), compartment.getName()} );
        else
            readEdges( edgesElement, compartment );

        compartment.setNotificationEnabled( true );
        return compartment;
    }

    @Override
    public void readNodes(Element nodesElement, Compartment compartment)
    {
        NodeList list = nodesElement.getChildNodes();
        for( Element element : XmlUtil.elements( list ) )
        {
            try
            {
                boolean notificationEnabled = compartment.isNotificationEnabled();
                compartment.setNotificationEnabled(false);

                Node node = null;

                String nodeName = element.getNodeName();
                if( nodeName.equals(COMPARTMENT_ELEMENT) )
                    node = readCompartment(element, compartment);
                else if( nodeName.equals(EQUIVALENTNODEGROUP_ELEMENT) )
                    node = readCompartment(element, compartment);
                else if( nodeName.equals(NODE_ELEMENT) )
                    node = readNode(element, compartment);
                else if( nodeName.equals(SUBDIAGRAM_ELEMENT) )
                    node = readSubDiagram(element, compartment);
                else if( nodeName.equals(MODEL_DEFINITION_ELEMENT) )
                    node = readModelDefinition(element, compartment);
                compartment.setNotificationEnabled(notificationEnabled);

                if( node != null )
                    compartment.put( node );
            }
            catch( Throwable t )
            {
                error( "ERROR_READ_NODES", new String[] {diagram.getName(), compartment.getName(), t.getMessage()}, t );
            }
        }
    }

    private Node readModelDefinition(Element element, Compartment compartment)
    {
        try
        {
            String id = element.getAttribute( ID_ATTR );

            DiagramXmlReader reader = new DiagramXmlReader( id );
            Diagram innerDiagram = reader.read( null, null, getElement( element, DML_ELEMENT ) );
            ModelDefinition modelDefinition = new ModelDefinition( compartment, innerDiagram, id );
            DiagramType diagramType = readInfo( element, modelDefinition );
            return (Compartment)diagramType.getSemanticController().validate( compartment, modelDefinition );
        }
        catch( Throwable t )
        {
            error( "ERROR_READ_MODEL_DEFINITION", new String[] {diagram.getName(), compartment.getName(), t.getMessage()}, t );
            return null;
        }

    }

    private DiagramType readInfo(Element element, DiagramContainer compartment)
    {
        Element compartmentInfoElement = getElement( element, COMPARTMENT_INFO_ELEMENT );
        if( compartmentInfoElement == null )
        {
            compartment.setLocation( readLocation( element, compartment.getName(), diagram.getName() ) );
            compartment.setShapeSize( readDimension( element, diagram.getName(), compartment.getName() ) );
        }
        else
        {
            readCompartmentInfo( compartmentInfoElement, compartment, diagram.getName() );
            readComment( compartment, compartmentInfoElement );
        }

        DiagramType diagramType = Diagram.getDiagram( compartment ).getType();
        DynamicPropertySet properties = diagramType.getProperties();
        if( properties != null )
        {
            fillProperties( element, compartment.getAttributes(), properties );
            if( diagramType instanceof XmlDiagramType )
                ( (XmlDiagramType)diagramType ).fillPropertyEditorParameters( compartment.getAttributes() );
        }
        return diagramType;
    }

    protected Compartment readSubDiagram(Element subdiagramElement, Compartment compartment)
    {
        try
        {
            String name = subdiagramElement.getAttribute( ID_ATTR );
            String stateName = subdiagramElement.getAttribute( DIAGRAM_STATE_ATTR );

            Diagram associatedDiagram;
            String modelDefName = subdiagramElement.getAttribute( MODEL_DEFINITION_REF_ATTR );
            if( modelDefName.isEmpty() )
            {
                String diagramPath = subdiagramElement.getAttribute( DIAGRAM_REF_ATTR );
                if( newPaths != null && newPaths.containsKey( diagramPath ) )
                    diagramPath = newPaths.get( diagramPath );

                DataElementPath elementPath = DataElementPath.create( diagramPath );
                associatedDiagram = elementPath.getDataElement( Diagram.class );
            }
            else
            {
                Node node = Diagram.getDiagram( compartment ).findNode( modelDefName );
                if( node instanceof ModelDefinition )
                    associatedDiagram = ( (ModelDefinition)node ).getDiagram();
                else
                    return null;
            }

            SubDiagram subDiagram = new SubDiagram( compartment, associatedDiagram, name );
            readSubDiagramState( getElement( subdiagramElement, DiagramXmlConstants.STATE_ELEMENT ), subDiagram );

            biouml.standard.state.State state = subDiagram.getDiagram().getState( stateName );
            if( state != null )
                subDiagram.setStateName( state.getName() );

            Element nodesElement = getElement( subdiagramElement, NODES_ELEMENT );
            if( nodesElement != null )
                readNodes( nodesElement, subDiagram );

            DiagramType diagramType = readInfo( subdiagramElement, subDiagram );

            //old style port reading should be removed later
            readPortInfos( subDiagram, subdiagramElement );
            //serialized version may be not synced with associated diagram
            subDiagram.updatePorts();

            return (Compartment)diagramType.getSemanticController().validate( compartment, subDiagram );
        }
        catch( Throwable t )
        {
            error( "ERROR_READ_SUBDIAGRAM", new String[] {diagram.getName(), compartment.getName(), t.getMessage()}, t );
            return null;
        }
    }


    private void readPortInfos(SubDiagram subDiagram, Element subdiagramElement) throws Exception
    {
        NodeList list = subdiagramElement.getChildNodes();
        Diagram associatedDiagram = subDiagram.getDiagram();
        for( Element element : XmlUtil.elements( list ) )
        {
            if( element.getNodeName().equals( PORT_ELEMENT ) )
            {
                String nodeName = element.getAttribute( NODE_NAME_ATTR );
                if( !nodeName.isEmpty() )
                {
                    DataElement node = associatedDiagram.get( nodeName );
                    if( node instanceof Node )
                    {
                        Point location = readLocation( element, nodeName, diagram.getName() );
                        location.translate( subDiagram.getLocation().x, subDiagram.getLocation().y );
                        Node portNode = SubDiagram.createPort( (Node)node, subDiagram, location );

                        String orientation = element.getAttribute( ORIENTATION_ATTR );
                        if( !orientation.isEmpty() )
                        {
                            portNode.getAttributes().add(
                                    DPSUtils.createHiddenReadOnly( PortOrientation.ORIENTATION_ATTR, PortOrientation.class,
                                            PortOrientation.getOrientation( orientation ) ) );
                        }
                    }
                }
            }
        }
    }

    public Node readNode(Element element, Compartment compartment)
    {
        Base kernel = getKernel( element, compartment );
        String id = kernel.getName();
        if( element.hasAttribute( ID_ATTR ) )
            id = element.getAttribute( ID_ATTR );
        Node node = createNode(element, id, kernel, compartment);
        if(kernel instanceof Reaction)
            ( (Reaction)kernel ).setParent(node);
        readComment( node, element );
        readNodeInfo( element, node, diagram.getName() );

        return (Node)validate( compartment, node );
    }

    @Override
    public void readEdges(Element edgesElement, Compartment compartment)
    {
        NodeList list = edgesElement.getChildNodes();
        for( Element element : XmlUtil.elements( list ) )
        {
            try
            {
                if( element.getNodeName().equals(EDGE_ELEMENT) )
                {
                    boolean notificationEnabled = compartment.isNotificationEnabled();
                    compartment.setNotificationEnabled(false);

                    Edge edge = readEdge(element, compartment);

                    compartment.setNotificationEnabled(notificationEnabled);

                    if( edge != null )
                        compartment.put(edge);
                }
            }
            catch( Throwable t )
            {
                error( "ERROR_READ_EDGES", new String[] {diagram.getName(), compartment.getName(), t.getMessage()}, t );
            }
        }
    }

    public Edge readEdge(Element edgeElement, Compartment compartment)
    {
        String outRef = edgeElement.getAttribute( OUT_REF_ATTR );
        Node outNode = findNestedNode( compartment, outRef );
        if( outNode == null )
        {
            wrongEdges.put( edgeElement, compartment );
            return null;
        }

        String inRef = edgeElement.getAttribute( IN_REF_ATTR );
        Node inNode = findNestedNode( compartment, inRef );
        if( inNode == null )
        {
            wrongEdges.put( edgeElement, compartment );
            return null;
        }

        Base kernel;
        if( outNode.getKernel() instanceof Reaction )
            kernel = getKernel( edgeElement, outNode.getCompartment() );
        else if( inNode.getKernel() instanceof Reaction )
            kernel = getKernel( edgeElement, inNode.getCompartment() );
        else
            kernel = getKernel( edgeElement, compartment );
        Edge edge;
        if( edgeElement.hasAttribute( ID_ATTR ) )
        {
            String id = edgeElement.getAttribute( ID_ATTR );
            edge = new Edge( compartment, id, kernel, inNode, outNode );
        }
        else
        {
            String id = Edge.getUniqEdgeName( compartment, kernel, inNode, outNode );
            if( version.equals( "0.7.0" ) || version.equals( "0.7.1" ) || version.equals( "0.7.2" ) || version.equals( "0.7.3" )
                    || version.equals( "0.7.4" ) || version.equals( "0.7.5" ) || version.equals( "0.7.6" ) )
            {
                // supporting of some old diagrams
                id = Edge.getUniqEdgeName( compartment, kernel, null, null );
            }
            edge = new Edge( compartment, id, kernel, inNode, outNode );
        }

        readComment( edge, edgeElement );
        readEdgeInfo( edgeElement, edge, diagram.getName() );
        readEdgeRole( edgeElement, edge, diagram.getName() );
        return (Edge)validate( compartment, edge );
    }

    HashMap<Element, Compartment> wrongEdges = new HashMap<>();

    private void readAndApplyWrongEdges()
    {
        if( wrongEdges.size() > 0 )
            log.log(Level.SEVERE,  "Diagram contains some wrong edges - if it is possible, please resave this diagram." );
        for( Map.Entry<Element, Compartment> entry : wrongEdges.entrySet() )
        {
            Element edgeElement = entry.getKey();
            Compartment compartment = entry.getValue();

            try
            {
                String kernelRef = edgeElement.getAttribute( KERNEL_REF_ATTR );
                Base kernel = getKernel( edgeElement, compartment );

                String inRef = edgeElement.getAttribute( IN_REF_ATTR );
                Node inNode = findNestedNode( compartment, inRef );
                if( inNode == null && inRef.equals( compartment.getName() ) )
                    inNode = compartment;
                if( inNode == null && inRef.contentEquals( diagram.getName() ) )
                    inRef = inRef.substring( inRef.indexOf( diagram.getName() ) + diagram.getName().length() + 1, inRef.length() );
                inNode = findNestedNode( diagram, inRef );
                if( inNode == null )
                {
                    error( "ERROR_NODE_NOT_FOUND", new String[] {diagram.getName(), kernelRef, inRef} );
                    continue;
                }

                String outRef = edgeElement.getAttribute( OUT_REF_ATTR );
                Node outNode = findNestedNode( compartment, outRef );
                if( outNode == null && outRef.equals( compartment.getName() ) )
                    outNode = compartment;
                if( outNode == null && outRef.contains( diagram.getName() ) )
                {
                    outRef = outRef.substring( outRef.indexOf( diagram.getName() ) + diagram.getName().length() + 1, outRef.length() );
                    outNode = findNestedNode( diagram, outRef );
                }
                if( outNode == null )
                {
                    error( "ERROR_NODE_NOT_FOUND", new String[] {diagram.getName(), kernelRef, outRef} );
                    continue;
                }

                Edge edge = new Edge( kernel, inNode, outNode );
                readComment( edge, edgeElement );
                readEdgeInfo( edgeElement, edge, diagram.getName() );
                edge.save();
            }
            catch( Throwable t )
            {
                error( "ERROR_READ_EDGES", new String[] {diagram.getName(), compartment.getName(), t.getMessage()}, t );
            }
        }
    }

    private DiagramElement validate(Compartment compartment, DiagramElement de)
    {
        try
        {
            return diagram.getType().getSemanticController().validate( compartment, de );
        }
        catch( Exception e )
        {
            log.warning( "Can not validate element: " + de.getName() );
        }
        return de;
    }

    private static Node findNestedNode(Compartment compartment, String name)
    {
        Node node = null;
        try
        {
            DataElement de = CollectionFactory.getDataElement( name, compartment );
            if( de instanceof Node )
                return (Node)de;

            Compartment nestedCompartment = null;
            int oldPos = 0;
            int pos = 0;
            while( nestedCompartment == null )
            {
                pos = name.indexOf( VariableRole.SCOPE, oldPos );
                if( pos > oldPos )
                {
                    String s1 = name.substring( oldPos, pos );
                    if( compartment.getName().equals( s1 ) )
                        return findNestedNode( compartment, name.substring( pos + 1 ) );
                    String str = name.substring( 0, pos );
                    nestedCompartment = (Compartment)compartment.get( str );
                }
                else
                {
                    de = compartment.get( name );
                    break;
                }
                oldPos = pos + 1;
            }
            if( de instanceof Node )
                node = (Node)de;

            if( nestedCompartment != null )
                return findNestedNode( nestedCompartment, name.substring( pos + 1 ) );
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE,  "Error during node search: ", t );
        }

        return node;
    }

    public void readStates(Element element, Diagram diagram)
    {
        if( element == null )
            return;
        NodeList list = element.getChildNodes();
        for( Element child : XmlUtil.elements( list ) )
            diagram.addState( StateXmlSerializer.readXmlElement( child, diagram, this ) );
    }

    private final List<String> kernelNames = new ArrayList<>();

    public @Nonnull List<String> getRequestedKernelsNameList()
    {
        return getKernels ? new ArrayList<>() : kernelNames;
    }

    @SuppressWarnings ( "serial" )
    private static class KernelNotFoundException extends Exception
    {
        public KernelNotFoundException(String kernelPath)
        {
            super( "Kernel not found: " + kernelPath );
        }
    }

    protected Base getKernel(Element element, Compartment compartment)
    {
        String name = element.getAttribute( KERNEL_REF_ATTR );
        Base kernel = null;

        DataElementPath kernelRef = DataElementPath.create( name );
        try
        {
            if( kernelRef.isDescendantOf( diagram.getCompletePath() ) )
                throw new KernelNotFoundException( name );
            if( name.startsWith( STUB ) )
            {
                String simpleName = kernelRef.getName();
                String type = element.getAttribute( KERNEL_TYPE_ATTR );
                if( type.isEmpty() )
                    type = Type.TYPE_UNKNOWN;

                if (type.equals( Type.TYPE_COMPARTMENT ))
                    kernel = new biouml.standard.type.Compartment(null, simpleName);
                else if( type.equals( Type.TYPE_NOTE ) )
                    kernel = new Stub.Note( null, simpleName );
                else if( type.equals( Type.TYPE_OUTPUT_CONNECTION_PORT ) )
                    kernel = new Stub.OutputConnectionPort( null, simpleName );
                else if( type.equals( Type.TYPE_INPUT_CONNECTION_PORT ) )
                    kernel = new Stub.InputConnectionPort( null, simpleName );
                else if( type.equals( Type.TYPE_CONTACT_CONNECTION_PORT ) )
                    kernel = new Stub.ContactConnectionPort( null, simpleName );
                else if( type.equals( Type.TYPE_CONNECTION_BUS ) )
                    kernel = new Stub.Bus( null, simpleName );
                else if( type.equals( Type.TYPE_NOTE_LINK ) )
                    kernel = new Stub.NoteLink( null, simpleName );
                else if( type.equals( Type.TYPE_AVERAGER ) )
                    kernel = new Stub.AveragerElement( null, simpleName );
                else if( type.equals( Type.TYPE_SWITCH ) )
                    kernel = new Stub.SwitchElement( null, simpleName );
                else if( type.equals( Type.TYPE_CONSTANT ) )
                    kernel = new Stub.Constant( null, simpleName );
                else if( type.equals( Type.TYPE_DEPENDENCY ) )
                    kernel = new Stub.Dependency( null, simpleName );
                else if( type.equals( Type.TYPE_DIRECTED_LINK ) )
                    kernel = new Stub.DirectedConnection( null, simpleName );
                else if( type.equals( Type.TYPE_UNDIRECTED_LINK ) )
                    kernel = new Stub.UndirectedConnection( null, simpleName );
                else if( type.equals( Type.TYPE_SEMANTIC_RELATION ) )
                    kernel = new SemanticRelation( null, simpleName );
                else if( type.equals( Type.TYPE_PLOT ) )
                    kernel = new PlotElement( null, simpleName );
                else if( type.equals( Type.TYPE_CHEMICAL_ROLE ) )
                {
                    DataElementPath refName = DataElementPath.create( name.substring( STUB.length() + 1 ) );
                    if( refName.getParentPath().isDescendantOf( diagram.getCompletePath() ) )
                        throw new KernelNotFoundException( name );
                    DataCollection<?> origin = refName.optParentCollection();
                    if( origin == null )
                    {
                        //try to find Reaction in compartment directly
                        DataElement de = CollectionFactory.getDataElement( refName.getParentPath().getName(), compartment );
                        if( ( de instanceof Node ) && ( ( (Node)de ).getKernel() instanceof DataCollection ) )
                        {
                            origin = (DataCollection<?>) ( (Node)de ).getKernel();
                            kernel = (Base)origin.get( refName.getName() );
                        }
                    }
                    if( kernel == null )
                        kernel = new SpecieReference( origin, refName.getName() );
                }
                else if( type.equals( Type.TYPE_REACTION ) )
                {
                    kernel = getReactionKernel( element, simpleName );
                }
                else
                {
                    kernel = new Stub( null, simpleName, type );
                }

                String title = element.getAttribute( TITLE_ATTR );
                if( ( !title.isEmpty() ) && ( kernel instanceof BaseSupport ) )
                {
                    ( (BaseSupport)kernel ).setTitle( title );
                }

                Element attrElement = getElement( element, ATTRIBUTES_ELEMENT );
                if( attrElement != null )
                    fillProperties( attrElement, kernel.getAttributes(), null );
            }
            else
            {
                if( getKernels )
                {
                    //                    name.startsWith(Module.DIAGRAM)
                    //                            || ( TextUtil.isFullPath(name) && name.indexOf(Module.DIAGRAM + "/") != -1 );
                    DataElementPath path = TextUtil.isFullPath( name ) || module == null ? kernelRef : module.getCompletePath()
                            .getRelativePath( "./" + name );
                    if( path.getParentPath().isDescendantOf( diagram.getCompletePath() ) )
                        throw new KernelNotFoundException( name );
                    DataCollection<?> origin = path.getParentCollection();
                    DataElementDescriptor descriptor = origin.getDescriptor( path.getName() );
                    boolean isDiagramName = descriptor != null && Diagram.class.isAssignableFrom( descriptor.getType() );

                    if( !isDiagramName )
                    {
                        if( module != null )
                        {
                            DataElement tmpKernel = module.getKernel( name );
                            if( tmpKernel instanceof Base )
                                kernel = (Base)tmpKernel;
                        }
                        else
                        {
                            kernel = (Base)CollectionFactory.getDataElement( name );
                        }
                        if( kernel == null )
                            throw new KernelNotFoundException( name );
                    }
                }
                else
                {
                    // preload optimization
                    // TODO: support slashes escaping
                    String[] components = kernelRef.getPathComponents();
                    if( components.length > 0 )
                    {
                        DataElementPath tempName = DataElementPath.create( components[0] );
                        for( int i = 1; i < components.length; i++ )
                        {
                            String nextName = components[i];
                            DataElementPath path = tempName.getChildPath( nextName );
                            if( path.equals( kernelRef ) )
                            {
                                kernelNames.add( name );
                                kernel = new Stub( null, nextName );
                                break;
                            }
                            DataElement de;
                            if( module != null )
                            {
                                de = module.getKernel( path.toString() );
                            }
                            else
                            {
                                if( path.isDescendantOf( diagram.getCompletePath() ) )
                                    throw new KernelNotFoundException( name );
                                de = path.optDataElement();
                            }
                            if( de != null && module != null && module.getType().isCategorySupported() )
                            {
                                String reaction = module.getType().getCategory( Reaction.class );
                                if( ( Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName( de.getName() ) )
                                        .equals( reaction ) )
                                {
                                    if( i < components.length - 1 )
                                    {
                                        String reactionName = components[++i];
                                        path = tempName.getChildPath( de.getName(), reactionName );
                                        kernel = new Stub( null, reactionName );
                                        kernelNames.add( path.toString() );
                                    }
                                    break;
                                }
                            }
                            if( de == null )
                            {
                                kernelNames.add( tempName.toString() );
                                break;
                            }
                            tempName = path;
                        }
                        if( kernel == null )
                        {
                            String stubName = name;
                            if( tempName.isAncestorOf( kernelRef ) )
                                stubName = kernelRef.getPathDifference( tempName );
                            kernel = new Stub( null, stubName );
                        }
                    }

                    if( kernel == null )
                        throw new KernelNotFoundException( name );
                }
            }
        }
        catch( KernelNotFoundException e )
        {
            kernel = new Stub( null, kernelRef.getName() );
            error( "ERROR_KERNEL_PROCESSING", new String[] {diagram.getCompletePath().toString(), name, e.getMessage()} );
        }
        catch( Throwable t )
        {
            kernel = new Stub( null, kernelRef.getName() );
            error( "ERROR_KERNEL_PROCESSING", new String[] {diagram.getCompletePath().toString(), name, t.getMessage()}, t );
        }

        return kernel;
    }

    public Base getReactionKernel(Element element, String simpleName)
    {
        Element reactionElement = getElement( element, REACTION_ELEMENT );
        if( reactionElement == null )
        {
            return new Stub( null, simpleName, Type.TYPE_REACTION );
        }
        Reaction reaction = new Reaction( null, simpleName );
        if( reactionElement.hasAttribute( REACTION_FORMULA_ATTR ) )
        {
            reaction.getKineticLaw().setFormula( reactionElement.getAttribute( REACTION_FORMULA_ATTR ) );
        }
        NodeList list = element.getElementsByTagName( SPECIE_REFERENCE_ELEMENT );
        if( list != null )
        {
            for( Element srElement : XmlUtil.elements( list ) )
            {
                try
                {
                    SpecieReference sr = new SpecieReference( reaction, srElement.getAttribute( NAME_ATTR ) );
                    String role = srElement.getAttribute( ROLE_ATTR );
                    if( !role.isEmpty() )
                    {
                        sr.setRole( role );
                        if( role.equals( SpecieReference.MODIFIER ) )
                            sr.setModifierAction( srElement.getAttribute( MODIFIER_ACTION_ATTR ) );
                    }
                    sr.setSpecie( srElement.getAttribute( SPECIE_ATTR ) );
                    sr.setStoichiometry( srElement.getAttribute( STOICHIOMETRY_ATTR ) );
                    sr.setParticipation( srElement.getAttribute( PARTICIPATION_ATTR ) );
                    sr.setComment( srElement.getAttribute( COMMENT_ATTR ) );
                    reaction.put( sr );
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE,  "Cannot read SpecieReference", e );
                }
            }
        }
        return reaction;
    }

    public static void fillProperties(Element element, DynamicPropertySet dps, DynamicPropertySet registry)
    {
        NodeList nodeList = element.getChildNodes();
        for( Element e : XmlUtil.elements( nodeList ) )
        {
            if( PROPERTY_ELEMENT.equals( e.getTagName() ) || PROPERTY_REF_ELEMENT.equals( e.getTagName() ) )
            {
                try
                {
                    DynamicProperty property = getProperty( registry, e );
                    if( property != null )
                        dps.add( property );
                }
                catch( Throwable t )
                {
                    error( "ERROR_PARSING_PROPERTY", new String[] {e.getTagName()}, t );
                }
            }
        }
    }

    private static DynamicProperty getProperty(DynamicPropertySet registry, Element e) throws Exception
    {
        if( PROPERTY_REF_ELEMENT.equals( e.getTagName() ) )
        {
            String name = e.getAttribute( NAME_ATTR );
            String value = e.getAttribute( VALUE_ATTR );

            DynamicProperty property = registry != null ? registry.getProperty( name ) : null;
            if( property == null )
            {
                error( "UNDEFINED_PROPERTY_REF", new String[] {name, value,} );
                return null;
            }
            if( DynamicPropertySet.class.isAssignableFrom( property.getType() ) )
            {
                Object v = readDPS( e, getNestedRegistry( name, registry ) );
                return new DynamicProperty( property.getDescriptor(), property.getType(), v );
            }
            else if( property.getType() != null && property.getType().isArray() )
            {
                Class<?> componentType = property.getType().getComponentType();
                return new DynamicProperty( property.getDescriptor(), property.getType(), readPropertyArray( e, name, componentType,
                        registry ) );
            }
            return new DynamicProperty( property.getDescriptor(), property.getType(), getPropertyValue( e, name, property.getType(), value,
                    registry ) );
        }

        if( PROPERTY_ELEMENT.equals( e.getTagName() ) )
        {
            String name = e.getAttribute( NAME_ATTR );
            String value = e.getAttribute( VALUE_ATTR );
            String type = e.getAttribute( TYPE_ATTR );
            String shortDescr = e.getAttribute( SHORT_DESCR_ATTR );

            Class<?> propertyType = getPropertyType( type );

            if( Composite.class.equals( propertyType ) )
            {
                Object v = readDPS( e, getNestedRegistry( name, registry ) );
                return new DynamicProperty( name, name, DynamicPropertySet.class, v );
            }

            if( Array.class.equals( propertyType ) )
            {
                String elementType = e.getAttribute( ARRAY_ELEM_TYPE_ATTR );
                if( ( elementType == null || "".equals( elementType ) ) && getClosestSiblingElementList( e, ITEM_ELEMENT ).size() > 0 )
                {
                    error( "ERROR_ARRAY_NO_ELEMENT_TYPE", new String[] {name, type} );
                    propertyType = Composite.class;
                }
                else
                {
                    propertyType = getPropertyType( elementType );
                }
                if( propertyType != null )
                {
                    return new DynamicProperty( name, name, java.lang.reflect.Array.newInstance( propertyType, 0 ).getClass(),
                            readPropertyArray( e, name, propertyType, registry ) );
                }
                error( "ERROR_COULD_NOT_RESOLVE_TYPE", new String[] {name, type} );
            }

            if( propertyType == null )
            {
                error( "ERROR_COULD_NOT_RESOLVE_TYPE", new String[] {name, type} );
            }
            else
            {
                DynamicProperty dp = new DynamicProperty( name, name, propertyType, getPropertyValue( e, name, propertyType, value,
                        registry ) );
                dp.setShortDescription( shortDescr );

                readPropertyAttributes( e, dp );

                //read tags
                String[] tagsArray = XmlStream.elements( e, TAGS_ELEMENT )
                    .flatMap( tags -> XmlStream.elements( tags, TAG_ELEMENT ) )
                    .map( tag -> tag.getAttribute( VALUE_ATTR ) )
                    .toArray(String[]::new);
                if( tagsArray.length > 0 )
                {
                    dp.getDescriptor().setValue( StringTagEditor.TAGS_KEY, tagsArray );
                }

                return dp;
            }
        }
        return null;
    }

    private static void readPropertyAttributes(Element e, DynamicProperty dp)
    {
        String isReadOnly = e.getAttribute( IS_READONLY_ATTR );
        if( isReadOnly != null && !isReadOnly.isEmpty() )
            dp.setReadOnly( Boolean.parseBoolean( isReadOnly ) );

        String isHidden = e.getAttribute( IS_HIDDEN_ATTR );
        if( isHidden != null && !isHidden.isEmpty() )
            dp.setHidden( Boolean.parseBoolean( isHidden ) );
    }

    private static Object readPropertyArray(Element element, String name, Class<?> type, DynamicPropertySet registry) throws Exception
    {
        if( String.class.equals( type ) || type.isPrimitive() || XmlSerializationUtils.isPrimitiveWrapperElement( type )
                || ( !Composite.class.isAssignableFrom( type ) && !DynamicPropertySet.class.isAssignableFrom( type ) ) )
        {
            List<Element> items = getClosestSiblingElementList( element, ITEM_ELEMENT );
            int j = 0;
            Object[] array = (Object[])java.lang.reflect.Array.newInstance( type, items.size() );
            if( String.class.equals( type ) )
            {
                for( Element e : items )
                    array[j++] = e.getFirstChild().getNodeValue();
            }
            else if( type.isPrimitive() || XmlSerializationUtils.isPrimitiveWrapperElement( type ) )
            {
                for( Element e : items )
                    array[j++] = XmlSerializationUtils.getPrimitiveValue( type, e.getFirstChild().getNodeValue() );
            }
            else if( !DynamicPropertySet.class.isAssignableFrom( type ) )
            {
                if( isEligibleToTextUtil( type ) )
                {
                    for( Element e : items )
                        array[j++] = TextUtil.fromString( type, e.getFirstChild().getNodeValue() );
                }
                else
                {
                    for( Element e : items )
                    {
                        Object arrayElement = type.newInstance();
                        DynamicPropertySet defaults = new DynamicPropertySetAsMap();
                        DPSUtils.writeBeanToDPS( arrayElement, defaults, "" );
                        DynamicPropertySet dps = readDPS( e, defaults );
                        DPSUtils.readBeanFromDPS( arrayElement, dps, "" );
                        array[j++] = arrayElement;
                    }
                }
            }
            return array;
        }
        else
        {
            if( registry == null )
            {
                List<DynamicProperty> list = new ArrayList<>();
                List<Element> properties = getClosestSiblingElementList( element );
                for( Element p : properties )
                {
                    if( PROPERTY_ELEMENT.equals( p.getTagName() ) || PROPERTY_REF_ELEMENT.equals( p.getTagName() ) )
                        list.add( getProperty( null, p ) );
                }
                return list.toArray( new DynamicProperty[list.size()] );
            }
            else
            {
                List<DynamicPropertySet> list = new ArrayList<>();
                List<Element> properties = getClosestSiblingElementList( element );
                for( Element p : properties )
                {
                    if( PROPERTY_ELEMENT.equals( p.getTagName() ) || PROPERTY_REF_ELEMENT.equals( p.getTagName() ) )
                        list.add( readDPS( p, getNestedRegistry( name, registry ) ) );
                }
                return list.toArray( new DynamicPropertySet[list.size()] );
            }
        }
    }

    public static DynamicPropertySet readDPS(Element element, DynamicPropertySet registry)
    {
        DynamicPropertySet dps = new DynamicPropertySetSupport();
        fillProperties( element, dps, registry );
        return dps;
    }

    private static List<Element> getClosestSiblingElementList(Element e)
    {
        return getClosestSiblingElementList( e, null );
    }

    private static List<Element> getClosestSiblingElementList(Element e, String tagName)
    {
        List<Element> elements = new ArrayList<>();
        NodeList nodeList = e.getChildNodes();
        for( Element elem : XmlUtil.elements( nodeList ) )
        {
            if( tagName == null || tagName.equals( elem.getTagName() ) )
                elements.add( elem );
        }
        return elements;
    }

    private static DynamicPropertySet getNestedRegistry(String name, DynamicPropertySet registry)
    {
        if( registry == null )
            return null;

        DynamicProperty property = registry.getProperty( name );

        if( property == null )
            return null;

        DynamicPropertySet dps;
        if( ! ( property.getValue() instanceof DynamicPropertySet ) )
        {
            dps = new DynamicPropertySetAsMap();
            DPSUtils.writeBeanToDPS( property.getValue(), dps, "" );
        }
        else
        {
            dps = (DynamicPropertySet)property.getValue();
        }
        return dps;
    }

    private static Object getPropertyValue(Element element, String name, Class<?> type, String value, DynamicPropertySet registry)
    {
        List<Element> properties = getClosestSiblingElementList( element, PROPERTY_ELEMENT );
        List<Element> propertyRefs = getClosestSiblingElementList( element, PROPERTY_REF_ELEMENT );

        if( properties.size() > 0 || propertyRefs.size() > 0 )
        {
            DynamicPropertySet dps = readDPS( element, getNestedRegistry( name, registry ) );
            if( type != null )
            {
                Object result = TextUtil.fromString( type, value );
                if( result == null )
                {
                    try
                    {
                        result = type.newInstance();
                    }
                    catch( Exception e )
                    {
                        log.log( Level.SEVERE, "Error while reading diagram: unable to create object of type " + type.getName() );
                        return null;
                    }
                }
                DPSUtils.readBeanFromDPS( result, dps, "" );
                return result;
            }
            return dps;
        }

        if( String.class.equals( type ) )
            return value;

        if( Pen.class.equals( type ) )
            return XmlSerializationUtils.readPen( value );

        if( Brush.class.equals( type ) )
            return XmlSerializationUtils.readBrush( value );

        if( Dimension.class.equals( type ) )
            return XmlSerializationUtils.readDimension( value );

        if( Point.class.equals( type ) )
            return XmlSerializationUtils.readPoint( value );

        if( ColorFont.class.equals( type ) )
            return XmlSerializationUtils.readFont( value );

        if( PortOrientation.class.equals( type ) )
            return PortOrientation.getOrientation( value );

        Object result = TextUtil.fromString( type, value );

        if( result != null )
            return result;

        result = XmlSerializationUtils.getPrimitiveValue( type, value );
        try
        {
            if( result == null && registry.getValue( name ) != null && value.isEmpty() )
                return type.newInstance();
        }
        catch( Exception e )
        {
        }

        try
        {
            return type.newInstance();
        }
        catch( Exception e )
        {
            log.log( Level.SEVERE, "Error while reading diagram: unable to create object of type " + type.getName() );
        }
        return null;
    }

    public static Diagram parseDiagram(String name, Element diagramElement, DataCollection<?> origin, DiagramType diagramType)
    {
        try
        {
            DiagramXmlReader dxr = new DiagramXmlReader( name, null, null );
            dxr.setDefaultDiagramType( diagramType );
            return dxr.readDiagram( diagramElement, null, origin );
        }
        catch( Exception e )
        {
            return null;
        }
    }

    public void setDiagram(Diagram d)
    {
        diagram = d;
    }

    public static void readSubDiagramState(Element stateElement, SubDiagram subDiagram)
    {
        if( stateElement == null )
            return;
        Diagram innerDiagram = subDiagram.getDiagram();
        DiagramXmlReader reader = new DiagramXmlReader( innerDiagram.getName() );
        reader.setDiagram( innerDiagram );
        biouml.standard.state.State oldState = innerDiagram.getState( SubDiagram.SUBDIAGRAM_STATE_NAME );
        if( oldState != null )
            innerDiagram.removeState( oldState );
        innerDiagram.addState( StateXmlSerializer.readXmlElement( stateElement, innerDiagram, reader ) );
    }

    public static void readStyle(Element element, DiagramElement de)
    {
        String style = element.getAttribute(STYLE_ATTR);

        if( !style.isEmpty() )
        {
            de.setPredefinedStyle(style);
            return;
        }

        boolean customStyle = element.hasAttribute(PEN_ATTR) || element.hasAttribute(COLOR_ATTR) || element.hasAttribute(FONT_ATTR);

        if( !customStyle )
            return;

        de.setPredefinedStyle(DiagramElementStyle.STYLE_NOT_SELECTED);

        Pen pen = XmlSerializationUtils.readPen(element.getAttribute(PEN_ATTR));
        de.getCustomStyle().setPen(pen);
        Brush brush = new Brush(stringToColor(element.getAttribute(COLOR_ATTR), Diagram.getDiagram(de).getName(), de.getName()));
        de.getCustomStyle().setBrush( brush );
        ColorFont font = XmlSerializationUtils.readFont(element.getAttribute(FONT_ATTR));
        if( font != null )
            de.getCustomStyle().setFont(font);
    }

    public static Document createDocument(String name, InputStream stream, DiagramInfo diagramInfo) throws Exception
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        String xml = ApplicationUtils.readAsString( stream );
        xml = normalizeXml( xml );
        stream = new ByteArrayInputStream( xml.getBytes( StandardCharsets.UTF_8 ) );

        try
        {
            return builder.parse( stream );
        }
        catch( SAXException e )
        {
            log.log(Level.SEVERE,  "Parse diagram \"" + name + "\" error: " + e.getMessage() );
            // TODO - fix
            return null;
        }
        catch( IOException e )
        {
            log.log(Level.SEVERE,  "Read diagram \"" + name + "\" error: " + e.getMessage() );
            // <![CDATA[ ]]> section is invalid - remove it
            int start = xml.indexOf( "<![CDATA[" );
            if( start >= 0 )
            {
                int finish = xml.indexOf( "]]>" );
                if( finish > start + 9 )
                    xml = xml.substring( 0, start + 9 ) + "Invalid diagram" + xml.substring( finish, xml.length() );
            }
            stream = new ByteArrayInputStream( xml.getBytes( StandardCharsets.UTF_8 ) );

            try
            {
                return builder.parse( stream );
            }
            catch( SAXException | IOException e2 )
            {
                return null;
            }
        }
    }

    private static String normalizeXml(String xmlStr)
    {
        char[] xml = xmlStr.toCharArray();
        for( int i = 0; i < xml.length; i++ )
        {
            switch( xml[i] )
            {
                case '\u0001':
                case '\u0002':
                case '\u0003':
                case '\u0004':
                case '\u0005':
                case '\u0006':
                case '\u000b':
                case '\u000c':
                case '\u000f':
                case '\u0012':
                case '\u0014':
                case '\u0016':
                case '\u0092':
                case '\u001a':
                case '\u001c':
                case '\u001e':
                case '\u00ff':
                    xml[i] = '?';
                    break;
                default:
            }
        }
        return new String( xml );
    }
}
