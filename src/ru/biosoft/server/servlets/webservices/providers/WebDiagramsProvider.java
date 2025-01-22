package ru.biosoft.server.servlets.webservices.providers;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.undo.UndoableEdit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentFactory.Policy;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.undo.TransactionEvent;
import com.developmentontheedge.beans.undo.TransactionUndoManager;
import com.eclipsesource.json.JsonObject;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.DiagramFilter;
import biouml.model.DiagramTypeConverter;
import biouml.model.DiagramViewBuilder;
import biouml.model.Edge;
import biouml.model.InitialElementProperties;
import biouml.model.ModelDefinition;
import biouml.model.Module;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.SemanticController;
import biouml.model.SubDiagram;
import biouml.model.SubDiagram.PortOrientation;
import biouml.model.dynamics.Assignment;
import biouml.model.dynamics.Constraint;
import biouml.model.dynamics.DAEModelUtilities;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.EModelRoleSupport;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.Function;
import biouml.model.dynamics.State;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.model.util.AddElementsUtils;
import biouml.model.util.ImageGenerator;
import biouml.model.xml.XmlDiagramSemanticController;
import biouml.model.xml.XmlDiagramType;
import biouml.standard.diagram.ConnectionEdgePane;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.PathwaySimulationSemanticController;
import biouml.standard.diagram.PortProperties;
import biouml.standard.diagram.ReactionInitialProperties;
import biouml.standard.diagram.Util;
import biouml.standard.state.DiagramStateUtility;
import biouml.standard.type.Base;
import biouml.standard.type.Reaction;
import biouml.standard.type.SemanticRelation;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import biouml.standard.type.Stub.ConnectionPort;
import biouml.standard.type.Type;
import biouml.workbench.diagram.DiagramEditorHelper;
import biouml.workbench.diagram.SetInitialValuesAction;
import biouml.workbench.diagram.ViewEditorPaneStub;
//import biouml.workbench.diagram.viewpart.HighlightFilter;
import biouml.workbench.graph.DiagramToGraphTransformer;
import biouml.workbench.graphsearch.SearchElement;
import one.util.streamex.StreamEx;
import ru.biosoft.access.BeanRegistry;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataCollectionInfo;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementDescriptor;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.undo.DataCollectionUndoListener;
import ru.biosoft.access.exception.BiosoftCustomException;
import ru.biosoft.access.history.HistoryDataCollection;
import ru.biosoft.access.history.HistoryElement;
import ru.biosoft.access.history.HistoryFacade;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.support.IdGenerator;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.LoggedClassNotFoundException;
import ru.biosoft.graph.ForceDirectedLayouter;
import ru.biosoft.graph.Graph;
import ru.biosoft.graph.LayoutJobControlImpl;
import ru.biosoft.graph.Layouter;
import ru.biosoft.graph.Path;
import ru.biosoft.graph.PathwayLayouter;
import ru.biosoft.graphics.ArrowView;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.DummyView;
import ru.biosoft.graphics.PathUtils;
import ru.biosoft.graphics.SimplePath;
import ru.biosoft.graphics.TextView;
import ru.biosoft.graphics.View;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListenerAdapter;
import ru.biosoft.plugins.graph.LayoutBeanProvider;
import ru.biosoft.plugins.graph.LayouterDescriptor;
import ru.biosoft.server.JSONUtils;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.BiosoftWebResponse;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.LayoutContext;
import ru.biosoft.server.servlets.webservices.LayoutThread;
import ru.biosoft.server.servlets.webservices.TileInfo;
import ru.biosoft.server.servlets.webservices.WebException;
import ru.biosoft.server.servlets.webservices.WebJob;
import ru.biosoft.server.servlets.webservices.WebServicesServlet;
import ru.biosoft.server.servlets.webservices.WebSession;
import ru.biosoft.server.servlets.webservices.WebTransactionUndoManager;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.DPSUtils;
import ru.biosoft.util.Pair;
import ru.biosoft.util.TextUtil;

/**
 * Diagram utility functions for web support
 */
public class WebDiagramsProvider extends WebProviderSupport
{
    private static final String WEB_UNDO_MANAGER_ATTRIBUTE = "webUndoManager";
    private static final String WEB_SELECTIONS_ATTRIBUTE = "webSelections";
    private static final String WEB_UNDO_LISTENER_ATTRIBUTE = "webUndoListener";
    public static final String TYPE = "type";
    //    private HighlightFilter highlightFilter = new HighlightFilter( Color.yellow );
    protected static final Logger log = Logger.getLogger(WebDiagramsProvider.class.getName());

    @Override
    public void process(BiosoftWebRequest arguments, BiosoftWebResponse resp) throws Exception
    {
        String diagramAction = arguments.optAction();
        OutputStream out = resp.getOutputStream();
        resp.setContentType("application/json");
        if( diagramAction != null )
        {
            if( diagramAction.equals("get_type") && sendTypeFast(arguments.getDataElementPath(), out) )
                return;
        }
        Diagram diagram = getDiagramChecked(arguments.getDataElementPath());
        int editFrom = arguments.optInt("editFrom", -1);
        if( editFrom >= 0 )
        {
            int editTo = arguments.optInt("editTo", -1);
            diagram = getDiagramWithState(diagram, editFrom, editTo);
        }
        int version = arguments.optInt("version", -2);
        int version2 = arguments.optInt("version2", -2);
        diagram = getDiagramVersion(diagram, version, version2);

        if( arguments.get("get_dimension") != null )
        {
            resp.setContentType("text/html");
            sendDiagramImageDimension(diagram, out);
            return;
        }

        String type = arguments.get(TYPE);
        if( diagramAction != null )
        {
            if( diagramAction.equals("move") )
            {
                List<DiagramElement> elements = moveDiagramElements( diagram, getDiagramElements( diagram, arguments, "e" ),
                        arguments.getPoint(),
                        arguments.optDouble( "sx", 1.0 ), arguments.optDouble( "sy", 1.0 ) );
                sendDiagramChanges( diagram, out, type, elements );
                return;
            }
            else if( diagramAction.equals("resize") )
            {
                String element = arguments.getString("e");
                String control = arguments.getString("control");
                resizeDiagramElement(diagram, getDiagramElement(diagram, element), arguments.getPoint(), control);
                sendDiagramChanges(diagram, out, type);
                return;
            }
            else if( diagramAction.equals("remove") )
            {
                removeDiagramElement(diagram, getDiagramElements(diagram, arguments, "e"));
                sendDiagramChanges(diagram, out, type);
                return;
            }
            else if( diagramAction.equals("update_selections") )
            {
                updateSelection(diagram, getDiagramElements(diagram, arguments, "selections"));
                new JSONResponse(out).sendString("ok");
                return;
            }
            else if( diagramAction.equals( "check_resizable" ) )
            {
                sendElementsResizable( diagram, getDiagramElements( diagram, arguments, "e" ), out );
                return;
            }
            else if( diagramAction.equals("type_structure") )
            {
                if( type != null )
                {
                    JSONArray json = arguments.optJSONArray("json");
                    sendElementStructure(diagram, json, out, arguments.getPoint(), type);
                    return;
                }
            }
            else if( diagramAction.equals("add") )
            {
                DataElementPath dc = DataElementPath.create(arguments.get("dc"));
                String responseType = arguments.get("resptype");
                List<DiagramElement> elements;
                if( dc != null )
                {
                    String element = arguments.get("e");
                    if( element != null )
                    {
                        if( dc.getDataElement() instanceof DiagramElement )
                        {
                            Diagram dcDiagram = Diagram.optDiagram( (DiagramElement)dc.getDataElement() );
                            if( dcDiagram != null && dcDiagram.getCompletePath().equals( diagram.getCompletePath() ) ) //copy elements from the same diagram
                            {


                                DataElementPath dePath = dc.getChildPath( element );
                                DataElement de = dePath.getDataElement();
                                if( de instanceof Node )
                                {
                                    if( ( (Node)de ).getKernel() instanceof Reaction )
                                        throw new Exception( "Reaction copy is not supported" );
                                    else
                                    {
                                        elements = addNodeCopy( de, diagram, arguments.getPoint() );
                                        sendDiagramChanges( diagram, out, responseType, elements );
                                        return;
                                    }
                                }
                            }
                        }
                        if( type == null )
                            elements = addDroppedDiagramElement( diagram, dc.getChildPath( element ), arguments.getPoint() );
                        else
                            elements = addDiagramElement( diagram, element, arguments.getPoint(), type, dc, null );

                        sendDiagramChanges(diagram, out, responseType, elements);
                        return;
                    }
                    JSONArray properties = arguments.optJSONArray("json");
                    if( properties != null && type != null )
                    {
                        elements = addDiagramElement(diagram, getElementNameFromProperties(properties), arguments.getPoint(), type, dc, properties);
                        sendDiagramChanges(diagram, out, responseType, elements);
                        return;
                    }

                    String input = arguments.get("input");
                    String output = arguments.get("output");
                    if( input != null && output != null )
                    {
                        String additional = arguments.get("additional");
                        if( addDiagramEdgeElement(diagram, out, input, output, type, additional) )
                            sendDiagramChanges(diagram, out, responseType);
                        return;
                    }
                }
                //Adding reaction
                JSONArray components = arguments.optJSONArray("components");
                if( components != null )
                {
                    try
                    {
                        String name = arguments.get( "name" );
                        String formula = arguments.get("formula");
                        String title = arguments.get( "title" );
                        elements = addReactionElement( diagram, name, components, formula, title, arguments.getPoint() );
                        sendDiagramChanges(diagram, out, responseType, elements);
                        return;
                    }
                    catch( Exception e )
                    {
                        log.log(Level.SEVERE, "Can not add reaction element");
                    }
                }
            }
            else if( diagramAction.equals("layout_info") )
            {
                sendDiagramLayouterInfo( out, diagram );
                return;
            }
            else if( diagramAction.equals("toolbar_icon") )
            {
                resp.setContentType("image/gif");
                if( type != null )
                    sendToolbarIcon(diagram, type, out);
                return;
            }
            else if( diagramAction.equals("layout") )
            {
                String layouter = arguments.getString("layouter");
                JSONArray options = arguments.getJSONArray("options");
                String jobID = arguments.get("jobID");
                layoutDiagram(diagram, layouter, options, jobID, out);
                return;
            }
            else if( diagramAction.equals("layout_apply") )
            {
                String layouter = arguments.getString("layouter");
                applyLayout(diagram, layouter, arguments.getJSONArray("options"));
                sendDiagramChanges(diagram, out, type);
                return;
            }
            else if( diagramAction.equals("layout_save") )
            {
                saveLayout(diagram);
                new JSONResponse(out).sendString("Layout saved");
                return;
            }
            else if( diagramAction.equals("undo") )
            {
                undoDiagram(diagram, arguments.optInt("undoTo", -1));
                sendDiagramChanges(diagram, out, type);
                return;
            }
            else if( diagramAction.equals("redo") )
            {
                redoDiagram(diagram);
                sendDiagramChanges(diagram, out, type);
                return;
            }
            else if( diagramAction.equals("revert") )
            {
                revertDiagram(diagram, arguments.optInt("revertVersion", -1));
                sendDiagramChanges(diagram, out, type);
                return;
            }
            else if( diagramAction.equals("add_elements") )
            {
                String elements = arguments.getString("elements");
                String responseType = arguments.get("resptype");
                Compartment parent = diagram;
                DataElementPath parentPath = DataElementPath.create( arguments.get( "parent" ) );
                if( parentPath != null )
                {
                    if( parentPath.optDataElement() instanceof Compartment )
                    {
                        parent = parentPath.getDataElement( Compartment.class );
                    }
                }
                addSearchElements( parent, elements );
                sendDiagramChanges(diagram, out, responseType);
                return;
            }
            else if( diagramAction.equals("get_base") )
            {
                sendBase(getDiagramElements(diagram, arguments, "path"), out);
                return;
            }
            else if( diagramAction.equals("get_neighbors") )
            {
                sendNeighbors(diagram, getDiagramElements(diagram, arguments, "path"), out);
                return;
            }
            else if( diagramAction.equals("get_type") )
            {
                sendType(diagram, out);
                return;
            }
            else if( diagramAction.equals("get_auto_layout") )
            {
                JSONObject result = new JSONObject();
                result.put("autoLayout", diagram.getViewOptions().isAutoLayout());
                (new JSONResponse(out)).sendJSON(result);
                return;
            }
            else if( diagramAction.equals("set_auto_layout") )
            {
                diagram.getViewOptions().setAutoLayout(Boolean.parseBoolean(arguments.get("value")));
                JSONObject result = new JSONObject();
                result.put("autoLayout", diagram.getViewOptions().isAutoLayout());
                (new JSONResponse(out)).sendJSON(result);
                return;
            }
            else if( diagramAction.equals("get_port_parameters") )
            {
                sendPortParameters(diagram, out);
                return;
            }
            else if( diagramAction.equals("refresh") )
            {
                storeView( diagram, null );
                sendDiagramChanges(diagram, out, type);
                return;
            }
            else if( diagramAction.equals("check_diagram_element") )
            {
                String nodeName = arguments.getString("node");
                DiagramElement de = diagram.findDiagramElement( DiagramUtility.toDiagramPath( nodeName ) );
                // not only subdiagrams are presented on the composite diagram
                // there are also equations, events, plots, etc.
                if( Util.isSubDiagram( de ) )
                {
                    SubDiagram subDiagram = castDataElement( de, SubDiagram.class );
                    new JSONResponse( out ).sendString( subDiagram.getDiagramPath() );
                }
                else if (de instanceof ModelDefinition)
                {
                    new JSONResponse( out ).sendString( ((ModelDefinition)de).getDiagramPath() );
                }
                return;
            }
            else if( diagramAction.equals("vertex") )
            {
                String edge = arguments.getString("edge");
                String vertexAction = arguments.getString("vertex_action");
                String vertexNum = arguments.get("vertex_number");

                processVertex(diagram, getDiagramElement(diagram, edge), vertexAction, vertexNum == null ? 0 : Integer.parseInt(vertexNum),
                        arguments.getPoint());
                sendDiagramChanges(diagram, out, type);
                return;
            }
            else if( diagramAction.equals("get_changes") )
            {
                synchronized( diagram )
                {
                    if( diagram.getView() != null && diagram.getView() == getStoredView(diagram) )
                    {
                        try
                        {
                            diagram.wait(30000);
                        }
                        catch( Exception e )
                        {
                        }
                    }
                    JSONResponse response = new JSONResponse(out);
                    JSONObject result = getChangedDiagramView(diagram, false);
                    response.sendJSON(result);
                    out.close();
                    return;
                }
            }
            else if( diagramAction.equals("users") )
            {
                JSONResponse response = new JSONResponse(out);
                response.sendJSON(getUsers(diagram));
                return;
            }
            else if( diagramAction.equals( "accept_for_reaction" ) )
            {
                SemanticController semanticController = diagram.getType().getSemanticController();
                String nodeName = arguments.getString( "node" );
                DiagramElement de = diagram.findDiagramElement( DiagramUtility.toDiagramPath( nodeName ) );
                if( de instanceof Node )
                {
                    Object prop = semanticController.getPropertiesByType( diagram, Reaction.class, new Point( 0, 0 ) );
                    if( prop instanceof ReactionInitialProperties )
                        if( ( (ReactionInitialProperties)prop ).acceptForReaction( (Node)de ) )
                        {
                            new JSONResponse( out ).send( new byte[0], 0 );
                            return;
                        }
                }
                new JSONResponse( out ).error( "Component " + de.getName() + " can not be added to reaction" );
                return;
            }
            else if( diagramAction.equals( "highlight_on" ) )
            {
                DiagramUtility.clearHighlight( diagram );
                Set<String> elements = new HashSet<>( Arrays.asList( arguments.getStrings( "jsonrows" ) ) );
                highlightVariables( diagram, elements, arguments.getString( "what" ) );
                sendDiagramChanges( diagram, out, type );
                return;
            }
            else if( diagramAction.equals( "highlight_off" ) )
            {
                DiagramUtility.clearHighlight( diagram );
                sendDiagramChanges( diagram, out, type );
                return;
            }
            else if( diagramAction.equals( "remove_variables" ) )
            {
                DiagramUtility.clearHighlight( diagram );
                Set<String> elements = new HashSet<>( Arrays.asList( arguments.getStrings( "jsonrows" ) ) );
                removeVariables( diagram, elements );
                sendDiagramChanges( diagram, out, type );
                return;
            }
            else if( diagramAction.equals( "add_variable" ) )
            {
                DiagramUtility.clearHighlight( diagram );
                addVariable( diagram );
                new JSONResponse( out ).sendString( "ok" );
                return;
            }
            else if( diagramAction.equals( "detect_variable_types" ) )
            {
                Role model = diagram.getRole();
                if( model != null && ( model instanceof EModel ) )
                {
                    ( (EModel)model ).detectVariableTypes();
                }
                new JSONResponse( out ).sendString( "ok" );
                return;
            }
            else if( diagramAction.equals( "fix" ) )
            {
                boolean fixed = arguments.getBoolean( "fixed" );
                List<DiagramElement> elements = getDiagramElements( diagram, arguments, "e" );
                elements.stream().forEach( de -> de.setFixed( fixed ) );
                sendDiagramChanges( diagram, out, type );
                return;
            }
            else if( diagramAction.equals( "new_reaction_name" ) )
            {
                JSONObject result = new JSONObject();
                String newName = ReactionInitialProperties.generateReactionName( diagram );
                result.put( "name", newName );
                DataCollection<Reaction> module = ReactionInitialProperties.getReactionOrigin( diagram );
                if( module != null )
                {
                    result.put( "readOnly", true );
                }
                new JSONResponse( out ).sendJSON( result );
                return;
            }
            else if( diagramAction.equals( "validate_reaction_name" ) )
            {
                String newName = diagram.getType().getSemanticController().validateName( arguments.get( "name" ) );
                new JSONResponse( out ).sendString( newName );
                return;
            }
            else if( diagramAction.equals( "subdiagrams" ) )
            {
                JSONObject result = new JSONObject();
                JSONArray subdiagrams = new JSONArray();
                if( DiagramUtility.isComposite( diagram ) )
                {
                    List<SubDiagram> subDiagrams = Util.getSubDiagrams( diagram );
                    subDiagrams.stream().forEach( sub -> {
                        JSONObject subObj = new JSONObject().put( "path", Util.getPath( sub ) ).put( "title", sub.getTitle() );
                        subdiagrams.put( subObj );
                    } );
                }
                result.put( "subdiagrams", subdiagrams );
                new JSONResponse( out ).sendJSON( result );
            }
            else if( diagramAction.equals( "get_kernels" ) )
            {
                sendKernels( diagram, out );
                return;
            }
            else if( diagramAction.equals( "get_all_variables" ) )
            {
                sendAllVariables( diagram, out );
                return;
            }
            else if( diagramAction.equals( "set_initial" ))
            {
                setInitial(diagram, arguments, out);
                return;
            }
        }
        else if( type != null && type.equals("json") )
        {
            sendDiagramJSON(diagram, out);
            return;
        }
        else
        {
            resp.setContentType("image/png");
            sendDiagramImage(diagram, out);
            return;
        }
    }

    private void setInitial(Diagram diagram, BiosoftWebRequest arguments, OutputStream out) throws Exception
    {
        JSONResponse response = new JSONResponse( out );
       
        DataElementPath tablePath = arguments.getDataElementPath( "table" );
        TableDataCollection table = tablePath.optDataElement( TableDataCollection.class );
        if( table == null )
        {
            response.error( "The table '" + tablePath.toString() + "' is not found" );
            return;
        }
        
        int ind = table.getColumnModel().optColumnIndex( SetInitialValuesAction.VALUE_COLUMN );
        if( ind == -1 )
        {
            response.error( "The table '" + table.getName() + "' must contain the column '" + SetInitialValuesAction.VALUE_COLUMN
                    + "' including new initial values to be set." );
            return;
        }

        if( !table.getColumnModel().getColumn( ind ).getType().equals( DataType.Float ) )
        {
            response.error( "The column '" + SetInitialValuesAction.VALUE_COLUMN + "' in the table '" + table.getName()
                    + "' must be of the type 'Float'." );
            return;
        }

        EModel emodel = diagram.getRole(EModel.class);
        
        Set<String> errors = new HashSet<>();
        for(Variable p : emodel.getVariables()) {
            if( table.contains( p.getName() ) )
            {
                try
                {
                    double value = (double)table.get( p.getName() ).getValues()[ind];
                    p.setInitialValue( value );
                }
                catch( Exception e )
                {
                    errors.add( p.getName() );
                }
            }
        }
        if( errors.isEmpty() )
            response.sendString( "ok" );
        else
            response.sendString( "Some parameters were not set from table: '" + table.getName() + "' due to errors, the values remain the same for: "
                    + errors.stream().collect( Collectors.joining( ", " ) ) );
    }

    //TODO: rename
    private void sendElementsResizable(Diagram diagram, List<DiagramElement> diagramElements, OutputStream out) throws IOException
    {
        JSONArray resizable = new JSONArray();
        SemanticController controller = diagram.getType().getSemanticController();
        for( DiagramElement de : diagramElements )
        {
            if( controller.isResizable( de ) )
            {
                resizable.put( de.getName() );
            }
        }
        JSONObject result = new JSONObject();
        result.put( "resizable", resizable );
        new JSONResponse( out ).sendJSON( result );
    }

    private List<DiagramElement> addNodeCopy(DataElement de, Diagram diagram, Point location) throws WebException
    {
        final List<DiagramElement> result = new ArrayList<>();
        DiagramEditorHelper helper = new DiagramEditorHelper( diagram );
        Compartment origin = (Compartment)helper.getOrigin( location );
        Compartment parent = origin == null ? diagram : origin;
        SemanticController controller = diagram.getType().getSemanticController();

        performTransaction( diagram, "Add copy of " + de.getName(), () -> {
            try
            {
                String newName = DefaultSemanticController.generateUniqueNodeName( parent, de.getName() );
                Node nodeCopy = controller.copyNode( (Node)de, newName, parent, location );
                if( nodeCopy != null && controller.canAccept( parent, nodeCopy ) )
                    helper.add( nodeCopy, location );
                result.add( nodeCopy );
            }
            catch( Exception e )
            {
                throw ExceptionRegistry.translateException( e );
            }
        } );
        return result;
    }

    private Set<DiagramElement> getNeighbors(List<? extends DiagramElement> diagramElements)
    {
        Set<DiagramElement> result = new HashSet<>();
        for( DiagramElement de : diagramElements )
        {
            List<Edge> newEdges = null;
            if( de instanceof Edge )
            {
                newEdges = Collections.singletonList((Edge)de);
            }
            else if( de instanceof Node )
            {
                Compartment parent = de.getCompartment();
                if( de instanceof Compartment && parent.getKernel() != null
                        && parent.getKernel().getType().equals( "complex" ) )
                {
                    result.add( parent );
                }
                else
                {
                    result.add( de );
                    newEdges = Arrays.asList( ( (Node)de ).getEdges() );
                }
            }
            else
                continue;
            if( newEdges != null )
                for( Edge edge : newEdges )
                {
                    if( result.contains( edge ) )
                        continue;
                    result.add( edge );
                    for( Node node : edge.nodes() )
                    {
                        if( !result.add( node ) )
                            continue;
                        if( node.getKernel() != null && Base.TYPE_REACTION.equals( node.getKernel().getType() ) )
                            result.addAll( getNeighbors( Arrays.asList( node ) ) );
                    }
                }
        }
        return result;
    }

    private void sendNeighbors(Diagram diagram, List<DiagramElement> diagramElements, OutputStream out) throws Exception
    {
        JSONArray nodes = new JSONArray();
        JSONArray edges = new JSONArray();
        for(DiagramElement de : getNeighbors( diagramElements ))
        {
            String relativePath = DataElementPath.create( de ).getPathDifference( diagram.getCompletePath() );
            if(de instanceof Node)
                nodes.put( relativePath );
            else if(de instanceof Edge)
                edges.put( relativePath );
        }
        JSONObject result = new JSONObject();
        result.put( "edges", edges );
        result.put( "nodes", nodes );
        new JSONResponse( out ).sendJSON( result );
    }

    private static class SelectionStorage
    {
        private final Map<String, Map<String, List<DiagramElement>>> selections = new HashMap<>();

        public List<Rectangle> getSelections(Diagram diagram, String user)
        {
            List<Rectangle> rectangles = new ArrayList<>();
            Map<String, List<DiagramElement>> map = selections.get(user);
            if( map == null )
                return Collections.emptyList();
            String currentSessionId = WebSession.getCurrentSession().getSessionId();
            for( String sessionId : map.keySet().toArray(new String[map.size()]) )
            {
                if( sessionId.equals(currentSessionId) )
                    continue;
                if( SecurityManager.checkSessionPosessElement(sessionId, diagram, diagram.getCompletePath().toString()) )
                {
                    for( DiagramElement element : map.get(sessionId) )
                    {
                        try
                        {
                            if( diagram.getDiagramElement(element.getCompleteNameInDiagram()) == element )
                                rectangles.add(element.getView().getBounds());
                        }
                        catch( Exception e )
                        {
                        }
                    }
                }
                else
                {
                    map.remove(sessionId);
                }
            }
            return rectangles;
        }

        public void putSelections(List<DiagramElement> list)
        {
            if( list == null || list.size() == 0 )
                removeSelections();
            else
            {
                WebSession session = WebSession.getCurrentSession();
                String user = session.getUserName();
                selections.computeIfAbsent( user, k -> new HashMap<>() ).put( session.getSessionId(), list );
            }
        }

        public void removeSelections()
        {
            WebSession session = WebSession.getCurrentSession();
            String user = session.getUserName();
            selections.computeIfPresent( user, (u, map) -> {
                map.remove(session.getSessionId());
                return map.isEmpty() ? null : map;
            });
        }
    }

    private void updateSelection(Diagram diagram, List<DiagramElement> list)
    {
        SelectionStorage storage = null;
        synchronized( diagram )
        {
            Object value = diagram.getAttributes().getValue(WEB_SELECTIONS_ATTRIBUTE);
            if( value instanceof SelectionStorage )
            {
                storage = (SelectionStorage)value;
            }
            else
            {
                storage = new SelectionStorage();
                DynamicProperty property = new DynamicProperty(WEB_SELECTIONS_ATTRIBUTE, SelectionStorage.class, storage);
                DPSUtils.makeTransient(property);
                property.setHidden( true );
                diagram.getAttributes().add(property);
            }
            storage.putSelections(list);
            diagram.notifyAll();
        }
    }

    /**
     * Creates JSON like {x:0,y:0,width:100,height:100} from Rectangle object
     */
    private static JSONObject createJSONRectangle(Rectangle rect)
    {
        JSONObject repaintRectJSON = new JSONObject();
        try
        {
            repaintRectJSON.put("x", rect.x);
            repaintRectJSON.put("y", rect.y);
            repaintRectJSON.put("width", rect.width);
            repaintRectJSON.put("height", rect.height);
        }
        catch( JSONException e )
        {
        }
        return repaintRectJSON;
    }

    /**
     * @param diagram
     * @param version
     * @throws Exception
     */
    private void revertDiagram(final Diagram diagram, int version) throws WebException
    {
        if( version < 0 )
            throw new WebException("EX_QUERY_INVALID_VERSION", diagram.getCompletePath(), version);
        DataElement diagramVersion = HistoryFacade.getVersion(diagram, version);
        if( ! ( diagramVersion instanceof Diagram ) )
            throw new WebException("EX_QUERY_INVALID_VERSION", diagram.getCompletePath(), version);
        biouml.standard.state.State state;
        try
        {
            state = DiagramStateUtility.createState(diagram, (Diagram)diagramVersion, "");
        }
        catch( Exception e )
        {
            throw new WebException(e, "EX_INTERNAL_STATE_CREATION", diagram.getCompletePath());
        }
        final biouml.standard.state.State finalState = state;
        performTransaction(diagram, "Revert to version " + version, () -> {
            try
            {
                DiagramStateUtility.redoEdits(diagram, finalState.getStateUndoManager().getEdits());
            }
            catch( Exception e )
            {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Returns older saved version of diagram or diff between two versions
     * @param diagram - current version of diagram
     * @param version - older version (-1 = current)
     * @param version2 - older version (-1 = current, -2 = no diff requested)
     * @return requested version of diagram
     * @throws Exception
     */
    protected Diagram getDiagramVersion(Diagram diagram, int version, int version2) throws WebException
    {
        if( version >= -1 || version2 >= -1 )
        {
            Diagram diagram1 = version == -1 ? diagram : (Diagram)HistoryFacade.getVersion(diagram, version);
            if( diagram1 == null )
                throw new WebException("EX_QUERY_INVALID_VERSION", diagram.getCompletePath(), version);
            if( version2 >= -1 )
            {
                Diagram diagram2 = version2 == -1 ? diagram : (Diagram)HistoryFacade.getVersion(diagram, version2);
                if( diagram2 == null )
                    throw new WebException("EX_QUERY_INVALID_VERSION", diagram.getCompletePath(), version2);

                diagram = (Diagram)HistoryFacade.getDiffElement(diagram1, diagram2);
            }
            else
                diagram = diagram1;
            createView(diagram);
        }
        return diagram;
    }

    /**
     * Get diagram, convert to PNG image and put to output stream
     * @throws Exception
     */
    public static void sendDiagramImage(Diagram diagram, OutputStream out) throws Exception
    {
        BufferedImage image = createImage(null, diagram);
        ImageGenerator.encodeImage(image, "PNG", out);
        WebServicesServlet.getSessionCache().addObject(diagram.getCompletePath().toString(), diagram, true);
        out.close();
    }

    private static void storeView(Diagram diagram, View view)
    {
        WebSession.getCurrentSession().putValue("DiagramView/" + diagram.getCompletePath(), view);
    }

    private static CompositeView getStoredView(Diagram diagram)
    {
        Object value = WebSession.getCurrentSession().getValue("DiagramView/" + diagram.getCompletePath());
        if( value instanceof CompositeView )
            return (CompositeView)value;
        return null;
    }

    private static String getPresentationName(DiagramElement ... elements)
    {
        StringBuilder result = new StringBuilder();
        int maxLength = 50;
        for( DiagramElement element : elements )
        {
            String title = element.getTitle();
            if( title == null || title.isEmpty() )
                title = element.getName();
            if( result.length() > 0 )
                result.append(", ");
            if( result.length() + title.length() > maxLength )
            {
                if( maxLength - result.length() > 5 )
                {
                    result.append(title.substring(0, maxLength - result.length())).append("...");
                }
                else
                    result.append("...");
                break;
            }
            result.append(title);
        }
        return result.toString();
    }

    /**
     * Parses JSON string of diagram element names and returns list of elements
     * @return
     * @throws Exception
     */
    private static List<DiagramElement> getDiagramElements(final Diagram diagram, BiosoftWebRequest arguments, String keyName)
            throws WebException
    {
        String[] elements = arguments.getStrings(keyName);
        final List<DiagramElement> elementsList = new ArrayList<>();
        for( String name : elements )
        {
            DiagramElement de = null;
            try
            {
                de = diagram.findDiagramElement(DiagramUtility.toDiagramPath(name));
            }
            catch( Exception e )
            {
                throw new WebException(e, "EX_QUERY_NO_ELEMENT", DataElementPath.create(diagram).getRelativePath(name));
            }
            if( de == null )
                throw new WebException("EX_QUERY_NO_ELEMENT", DataElementPath.create(diagram).getRelativePath(name));
            elementsList.add(de);
        }
        return elementsList;
    }

    /**
     * Returns diagram element by Diagram and elementName checking possible problems
     * @param diagram
     * @param elementName
     * @return
     * @throws WebException
     */
    private static DiagramElement getDiagramElement(Diagram diagram, String elementName) throws WebException
    {
        DiagramElement de;
        try
        {
            de = diagram.findDiagramElement(DiagramUtility.toDiagramPath(elementName));
        }
        catch( Exception e )
        {
            throw new WebException(e, "EX_QUERY_NO_ELEMENT", DataElementPath.create(diagram).getRelativePath(elementName));
        }
        if( de == null )
            throw new WebException("EX_QUERY_NO_ELEMENT", DataElementPath.create(diagram).getRelativePath(elementName));
        return de;
    }

    private String getElementNameFromProperties(JSONArray elementProperties) throws JSONException
    {
        for( int j = 0; j < elementProperties.length(); j++ )
        {
            JSONObject jsonObject = elementProperties.getJSONObject(j);
            String name = jsonObject.getString("name");
            if( name != null && name.equals("name") )
                return jsonObject.getString("value");
        }
        return null;
    }

    /**
     * Generate image for diagram.
     */
    public static BufferedImage createImage(TileInfo tile, Diagram diagram)
    {
        View view = createView(diagram);
        int x, y, width, height;
        double scale;
        if( tile != null )
        {
            x = tile.getX();
            y = tile.getY();
            width = tile.getWidth();
            height = tile.getHeight();
            scale = tile.getScale();
        }
        else
        {
            Rectangle r = view.getBounds();
            x = 0;
            y = 0;
            width = r.width + r.x;
            height = r.height + r.y;
            scale = 1.0;
        }
        BufferedImage image = new BufferedImage((int) ( width * scale ), (int) ( height * scale ), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.scale(scale, scale);
        graphics.translate( -x, -y);
        graphics.setClip(x, y, width, height);
        graphics.setColor(Color.white);
        graphics.fill(new Rectangle(x, y, width, height));
        view.paint(graphics);

        return image;
    }
    /**
     * Generate diagram view
     */
    public static View createView(Diagram diagram)
    {
        //log.log( Level.INFO, "createView: user = '" + WebSession.getCurrentSession().getUserName() + "', name = '" + diagram.getName() + "', path = '" + diagram.getCompletePath() + "'" );
        View view = diagram.getView();
        if( view == null )
        {   
            synchronized( diagram )
            {
                view = diagram.getView();
                if( view == null )
                {
                    view = ImageGenerator.generateDiagramView(diagram, ApplicationUtils.getGraphics());
                    storeView( diagram, view );
                }
                diagram.notifyAll();
            }
        }
        return view;
    }

    /**
     * Deactivates elements in given view which should not be selectable on the client
     * @param view view to process. CompositeView will be processed recursively
     */
    public static void deactivateElements(View view)
    {
        if( view != null )
        {
            Object obj = view.getModel();

            if( ! ( obj instanceof Node ) || ( obj instanceof Diagram && ! ( ( (Diagram)obj ).getOrigin() instanceof Compartment ) ) )
                view.setSelectable(false);
            if( view instanceof ArrowView && view.getModel() != null )
                view.setSelectable(true);
            if( view instanceof TextView && view.getModel() != null ) //reaction titles
                view.setSelectable( true );
            if( view instanceof CompositeView )
            {
                for( View childView : (CompositeView)view )
                    deactivateElements(childView);
            }
        }
    }

    public static void sendDiagramJSON(Diagram diagram, OutputStream out) throws Exception
    {
        CompositeView diagramView = null;
        synchronized (diagram)
        {
            diagram.setView(null);
            //create new view and store it
            diagramView = (CompositeView) createView(diagram);
        }

        if ( diagramView != null )
        {
            deactivateElements(diagramView);
            JSONObject view = diagramView.toJSON();

            if ( diagram == getDiagram(diagram.getCompletePath().toString(), true) )
                WebServicesServlet.getSessionCache().addObject(diagram.getCompletePath().toString(), diagram, true);
            JSONObject result = new JSONObject();
            result.put("view", view);
            result.put("users", getUsers(diagram));
            result.put("transactions", getTransactions(diagram));
            JSONArray history = getHistory(diagram);
            if ( history != null )
                result.put("history", history);
            //        synchronized( diagram )
            //        {
            //            storeView(diagram, diagramView);
            //            diagram.notifyAll();
            //        }
            new JSONResponse(out).sendJSON(result);
            out.close();
        }
        else
            new JSONResponse(out).error("Can not create view for JSON");
    }

    protected static class WebReferenceGenerator implements ImageGenerator.ReferenceGenerator
    {
        @Override
        public String getReference(Object obj)
        {
            if( ( obj instanceof Node ) && ! ( obj instanceof Diagram ) )
                return ( (Node)obj ).getCompleteNameInDiagram();
            return null;
        }

        @Override
        public String getTarget(Object obj)
        {
            return "_self";
        }

        @Override
        public String getTitle(Object obj)
        {
            return ( obj instanceof Node ) ? ( (Node)obj ).getTitle() : null;
        }
    }


    /**
     * Send diagram size
     * @throws Exception
     */
    public static void sendDiagramImageDimension(Diagram diagram, OutputStream out) throws Exception
    {
        createView(diagram);
        new JSONResponse(out).sendSizeParameters(getDiagramSize(diagram), null);
    }

    /**
     * Get diagram size
     */
    protected static Dimension getDiagramSize(Diagram diagram)
    {
        Rectangle bounds = diagram.getView().getBounds();
        return new Dimension(bounds.x + bounds.width + 10, bounds.y + bounds.height + 10);
    }

    /**
     * Move diagram element
     * @throws WebException
     */
    public static List<DiagramElement> moveDiagramElements(final Diagram diagram, final List<DiagramElement> elementsList,
            final Point offset, final double sx, final double sy)
            throws WebException
    {
        if( offset.x == 0 && offset.y == 0 )
            throw new WebException("EX_QUERY_PARAM_OFFSET_MISSING");
        if( elementsList.size() == 0 )
            throw new WebException("EX_QUERY_PARAM_ELEMENTS_MISSING");
        final DiagramEditorHelper helper = new DiagramEditorHelper(diagram);
        //init stub for view editor pane
        new ViewEditorPaneStub(helper, diagram);
        String presentationName = getPresentationName(elementsList.toArray(new DiagramElement[elementsList.size()]));
        List<DiagramElement> newElements = new ArrayList<>();
        performTransaction(diagram, "Move " + presentationName, () -> {
            for( DiagramElement de : elementsList )
            {
                Rectangle bounds = de.getView().getBounds();
                if( de instanceof Node )
                {
                    //TODO: think about synchronizing of shape size and viewBounds
                    Dimension size = ( (Node)de ).getShapeSize();
                    //TODO: set shape size to newly created node and remove condition check
                    if( size.getWidth() > 0 && size.getHeight() > 0 )
                        bounds = new Rectangle( bounds.x, bounds.y, (int)Math.round( size.getWidth() ),
                                (int)Math.round( size.getHeight() ) );
                }
                Rectangle newBounds = new Rectangle( (int)Math.round( ( bounds.x + offset.x ) * sx ),
                        (int)Math.round( ( bounds.y + offset.y ) * sy ), (int)Math.round( bounds.width * sx ),
                        (int)Math.round( bounds.height * sy ) );

                if( helper.isResizable( de.getView() ) && ( newBounds.width != bounds.width || newBounds.height != bounds.height ) )
                {
                    View view = de.getView();
                    helper.resizeView( view, new Dimension( newBounds.width - bounds.width, newBounds.height - bounds.height ),
                            new Dimension( newBounds.x - bounds.x, newBounds.y - bounds.y ) );
                    de = (DiagramElement)view.getModel();
                    newElements.add( de );
                    diagram.setView( null );
                    ImageGenerator.generateDiagramView( diagram, ApplicationUtils.getGraphics() );
                }
                else if( newBounds.x != bounds.x || newBounds.y != bounds.y )
                {
                    View view = de.getView();
                    helper.moveView( view, new Dimension( newBounds.x - bounds.x, newBounds.y - bounds.y ) );
                    //Obtain a new de, since it could be cloned if it was moved to the other compartment
                    de = (DiagramElement)view.getModel();
                    newElements.add( de );
                    diagram.setView( null );
                    ImageGenerator.generateDiagramView( diagram, ApplicationUtils.getGraphics() );
                }
            }
        });
        return newElements;
    }

    /**
     * Ensure that element has a view (recreate if not)
     * @param de
     */
    protected static void checkView(DiagramElement de)
    {
        if(de.getView() == null)
        {
            Diagram diagram = Diagram.getDiagram(de);
            diagram.setView(null);
            ImageGenerator.generateDiagramView(diagram, ApplicationUtils.getGraphics());
        }
    }

    public static void resizeDiagramElement(final Diagram diagram, final DiagramElement de, final Point offset, final String control)
            throws WebException
    {
        if( offset.x == 0 && offset.y == 0 )
            throw new WebException("EX_QUERY_PARAM_OFFSET_MISSING");
        final DiagramEditorHelper helper = new DiagramEditorHelper(diagram);
        //init stub for view editor pane
        new ViewEditorPaneStub(helper, diagram);
        if( !helper.isResizable(de.getView()) )
            throw new WebException("EX_ACCESS_CANNOT_RESIZE", de.getName());
        performTransaction(diagram, "Resize " + getPresentationName(de), () -> {
            if( control.equals("top_left") )
            {
                helper.moveView(de.getView(), new Dimension(offset.x, offset.y));
                checkView(de);
                helper.resizeView(de.getView(), new Dimension( -offset.x, -offset.y));
            }
            else if( control.equals("top_right") )
            {
                helper.moveView(de.getView(), new Dimension(0, offset.y));
                checkView(de);
                helper.resizeView(de.getView(), new Dimension(offset.x, -offset.y));
            }
            else if( control.equals("bottom_left") )
            {
                helper.moveView(de.getView(), new Dimension(offset.x, 0));
                checkView(de);
                helper.resizeView(de.getView(), new Dimension( -offset.x, offset.y));
            }
            else if( control.equals("bottom_right") )
            {
                helper.resizeView(de.getView(), new Dimension(offset.x, offset.y));
            }
            else if( control.equals("top") )
            {
                helper.moveView(de.getView(), new Dimension(0, offset.y));
                checkView(de);
                helper.resizeView(de.getView(), new Dimension(0, -offset.y));
            }
            else if( control.equals("bottom") )
            {
                helper.resizeView(de.getView(), new Dimension(0, offset.y));
            }
            else if( control.equals("left") )
            {
                helper.moveView(de.getView(), new Dimension(offset.x, 0));
                checkView(de);
                helper.resizeView(de.getView(), new Dimension( -offset.x, 0));
            }
            else if( control.equals("right") )
            {
                helper.resizeView(de.getView(), new Dimension(offset.x, 0));
            }
        });
    }

    public static void removeDiagramElement(final Diagram diagram, final List<DiagramElement> elementsList) throws Exception
    {
        if( elementsList.size() == 0 )
            throw new WebException("EX_QUERY_PARAM_ELEMENTS_MISSING");
        SemanticController sc = diagram.getType().getSemanticController();
        String presentationName = getPresentationName(elementsList.toArray(new DiagramElement[elementsList.size()]));
        performTransaction(diagram, "Remove " + presentationName, () -> {
            for( DiagramElement de : elementsList )
                try
                {
                    sc.remove( de );
                }
                catch( Exception e )
                {
                    log.log( Level.SEVERE, "Error during removing diagram element", e );
                }
        });
    }

    /**
     * Send properties for new element
     * @throws WebException
     */
    public static void sendElementStructure(Diagram diagram, JSONArray jsonParams, OutputStream out, Point location, String typeStr)
            throws WebException
    {
        JSONResponse response = new JSONResponse(out);
        try
        {
            DiagramEditorHelper helper = new DiagramEditorHelper(diagram);

            Compartment parent = (Compartment)helper.getOrigin(location);
            if( parent == null )
                parent = diagram;

            String jsonName = null;
            JSONArray jsonAttributes = null;
            if( jsonParams != null )
            {
                for( int j = 0; j < jsonParams.length(); j++ )
                {
                    JSONObject jsonObject = jsonParams.getJSONObject(j);
                    String name = jsonObject.getString(JSONUtils.NAME_ATTR);
                    if( name == null )
                        continue;
                    if( name.equals("name") )
                    {
                        jsonName = jsonObject.getString(JSONUtils.VALUE_ATTR);
                    }
                    if( name.equals("Attributes") )
                        jsonAttributes = jsonObject.getJSONArray(JSONUtils.VALUE_ATTR);
                }
            }

            JSONArray result = new JSONArray();
            JSONObject p = new JSONObject();
            p.put(JSONUtils.NAME_ATTR, "name");
            p.put(JSONUtils.DISPLAYNAME_ATTR, "Name");
            p.put(JSONUtils.TYPE_ATTR, "code-string");


            boolean nameCreated = false;
            Object properties = diagram.getType().getSemanticController().getPropertiesByType(parent, typeStr, location);
            if(!(properties instanceof InitialElementProperties))
            {
                if( diagram.getType() instanceof XmlDiagramType )
                {
                    properties = null;
                    String idFormat = ( (XmlDiagramType)diagram.getType() ).getIdFormat(typeStr);
                    if( idFormat != null )
                    {
                        String name = ( jsonName != null && !jsonName.isEmpty() ) ? jsonName : IdGenerator.generateUniqueName(parent,
                                new DecimalFormat(idFormat));
                        p.put(JSONUtils.VALUE_ATTR, name);
                        p.put(JSONUtils.READONLY_ATTR, true);
                        result.put(p);
                        nameCreated = true;
                    }

                    DynamicPropertySet dps = ( (XmlDiagramSemanticController)diagram.getType().getSemanticController() )
                            .createAttributes(typeStr);
                    if( jsonAttributes != null )
                        JSONUtils.correctBeanOptions(dps, jsonAttributes);
                    ComponentModel model = ComponentFactory.getModel(dps, Policy.UI, true);
                    JSONArray jsonProperties = JSONUtils.getModelAsJSON(model);
                    if( dps.size() > 0 )
                    {
                        JSONObject at = new JSONObject();
                        at.put(JSONUtils.NAME_ATTR, "Attributes");
                        at.put(JSONUtils.DISPLAYNAME_ATTR, "Attributes");
                        at.put(JSONUtils.TYPE_ATTR, "composite");
                        at.put(JSONUtils.VALUE_ATTR, jsonProperties);
                        result.put(at);
                    }
                }
            }

            if( properties != null )
            {
                if( jsonParams != null )
                {
                    JSONUtils.correctBeanOptions(properties, jsonParams);
                }
                WebBeanProvider.sendBeanStructure(diagram.getCompletePath().toString(), properties, response);
            }
            else
            {
                if( !nameCreated )
                {
                    String name = jsonName != null ? jsonName : "";
                    p.put(JSONUtils.VALUE_ATTR, name);
                    result.put(p);
                }
                response.sendJSONBean(result);
            }
        }
        catch( WebException e )
        {
            throw e;
        }
        catch( Exception e )
        {
            throw new WebException(e, "EX_INTERNAL_CREATE_DIAGRAM_ELEMENT");
        }
    }

    // TODO: rewrite completely!
    public static List<DiagramElement> addDiagramElement(final Diagram diagram, String elementName, final Point location, @Nonnull String typeStr,
            DataElementPath dcPath, JSONArray elementProperties) throws Exception
    {
        final List<DiagramElement> result = new ArrayList<>();
        try
        {
            final DiagramEditorHelper helper = new DiagramEditorHelper(diagram);
            Compartment origin = (Compartment)helper.getOrigin(location);
            final Compartment parent = origin == null ? diagram : origin;
            final SemanticController semanticController = diagram.getType().getSemanticController();
            final ViewEditorPane viewEditor = new ViewEditorPaneStub(helper, diagram);

            final Object bean = semanticController.getPropertiesByType(parent, typeStr, location);
            if( bean instanceof InitialElementProperties )
            {
                if( elementName != null && parent.contains( elementName ) )
                    throw new WebException( "EX_QUERY_ELEMENT_EXIST", elementName, parent.getName() );
                JSONUtils.correctBeanOptions(bean, elementProperties);
                //init stub for view editor pane
                performTransaction(diagram, "Add items (" + typeStr + ")", () -> {
                    try
                    {
                        DiagramElementGroup elements = ( (InitialElementProperties)bean ).createElements( parent, location, viewEditor );
                        //TODO: remove check after all InitialElementProperties will separately add/create elements
                        if( bean instanceof ReactionInitialProperties )
                            elements.putToCompartment( );
                        result.addAll( elements.getElements() );
                    }
                    catch( Exception e )
                    {
                        throw ExceptionRegistry.translateException(e);
                    }
                });
                return result;
            }

            Class<?> typeClass = null;
            try
            {
                typeClass = ClassLoading.loadClass( typeStr );
            }
            catch( LoggedClassNotFoundException e )
            {
            }
            final ru.biosoft.access.core.DataElement kernel = defineKernel(parent, dcPath, elementName, typeStr);
            DiagramElement diagramElement = createDiagramElement(parent, typeStr, kernel);

            //set roles
            if( typeClass != null )
            {
                if( typeClass == Event.class )
                {
                    Event event = new Event(diagramElement);
                    diagramElement.setRole(event);
                }
                else if( typeClass == Equation.class )
                {
                    diagramElement.setRole(new Equation(diagramElement, Equation.TYPE_SCALAR, "unknown", "0"));
                }
                else if( typeClass == Function.class )
                {
                    diagramElement.setRole(new Function(diagramElement));
                }
                else if( typeClass == Constraint.class )
                {
                    diagramElement.setRole(new Constraint(diagramElement));
                }
                else if( typeClass == State.class )
                {
                    State event = new State(diagramElement);
                    event.addOnEntryAssignment(new Assignment("unknown", "0"), false);
                    event.addOnExitAssignment(new Assignment("unknown", "0"), false);
                    diagramElement.setRole(event);
                }
                else if( semanticController instanceof PathwaySimulationSemanticController && typeClass != Stub.Note.class  )
                {
                    VariableRole var = new VariableRole(diagramElement, 0);
                    diagramElement.setRole(var);
                }
            }

            if( !semanticController.canAccept(parent, diagramElement) )
                throw new Exception("Can't accept node '" + diagramElement.getName() + "' to compartment '" + parent.getName() + "'");
            if( semanticController instanceof XmlDiagramSemanticController )
            {
                DynamicPropertySet dps = ( (XmlDiagramSemanticController)semanticController ).createAttributes(typeStr);
                JSONArray attr = null;
                if(elementProperties == null)
                    throw new Exception("No properties were supplied");
                for( int j = 0; j < elementProperties.length(); j++ )
                {
                    JSONObject jsonObject = elementProperties.getJSONObject(j);
                    String name = jsonObject.getString("name");

                    if( name != null && name.equals("Attributes") )
                    {
                        attr = jsonObject.getJSONArray("value");
                        break;
                    }
                }
                if( attr != null )
                {
                    JSONUtils.correctBeanOptions(dps, attr);
                    Iterator<String> iter = dps.nameIterator();
                    while( iter.hasNext() )
                    {
                        diagramElement.getAttributes().add(dps.getProperty(iter.next()));
                    }
                }
                diagramElement = ( (XmlDiagramSemanticController)semanticController ).getPrototype().validate(parent, diagramElement);
            }

            final DiagramElement finalDiagramElement = diagramElement;
            final Class<?> finalTypeClass = typeClass;
            final String finalElementName = kernel.getName();

            performTransaction(diagram, "Add " + getPresentationName(finalDiagramElement), () -> {
                try
                {
                    //TODO: rewrite code to have unique name of the diagramElement, since helper can change element, and the original one is used below
                    helper.add(finalDiagramElement, location);
                    result.add( finalDiagramElement );

                    if( finalTypeClass != null && Stub.ConnectionPort.class.isAssignableFrom(finalTypeClass) )
                    {
                        String varName = finalElementName.substring(0, finalElementName.indexOf(Stub.ConnectionPort.SUFFIX));
                        finalDiagramElement.getAttributes().add(
                                new DynamicProperty(PortOrientation.ORIENTATION_ATTR, PortOrientation.class,
                                        finalTypeClass == Stub.InputConnectionPort.class ? PortOrientation.LEFT
                                                : PortOrientation.RIGHT));
                        finalDiagramElement.getAttributes().add(
                                new DynamicProperty(Stub.ConnectionPort.VARIABLE_NAME_ATTR, String.class, varName));

                        EModel emodel = diagram.getRole(EModel.class);
                        Object nodeObj = emodel.getVariable(varName).getParent();
                        if( nodeObj instanceof Node )
                            ( (ConnectionPort)kernel ).setTitle( ( (Node)nodeObj ).getTitle());
                        else
                            ( (ConnectionPort)kernel ).setTitle(varName);
                        if( nodeObj instanceof Node )
                        {
                            String name = DefaultSemanticController.generateUniqueNodeName(parent, varName + "_connection");
                            Stub.DirectedConnection sd = new Stub.DirectedConnection(diagram, name);
                            Edge edge = new Edge(diagram, sd, (Node)nodeObj, (Node)finalDiagramElement);
                            helper.add(edge, new Point(0, 0));
                        }
                    }
                }
                catch( Exception e )
                {
                    throw ExceptionRegistry.translateException( e );
                }
            });
        }
        catch( Exception e )
        {
            throw new Exception( "Can't add element on diagram " + diagram.getName() + ": " + e.getMessage(), e );
        }
        return result;
    }

    private static DiagramElement createDiagramElement(final Compartment parent, @Nonnull String typeStr, DataElement kernel) throws Exception
    {
        Diagram diagram = Diagram.getDiagram(parent);
        String elementName = kernel.getName();

        boolean isCompartment = diagram.getType() instanceof XmlDiagramType
                && ( (XmlDiagramType)diagram.getType() ).checkCompartment(typeStr);
        DiagramElement diagramElement = null;
        if( kernel instanceof Diagram && DiagramUtility.isComposite( diagram ))
        {
            if( ( (Diagram)kernel ).getRole() == null )
            {
                throw new Exception("Can't add diagram with empty model. Select another diagram.");
            }
            else if( diagram == kernel )
            {
                throw new Exception("Can't add element to itself. Select another element.");
            }
            else
            {
                diagramElement = new SubDiagram(diagram, ( (Diagram)kernel ).clone((DataCollection<?>)parent, elementName), elementName);
            }
        }
        else if( kernel instanceof biouml.standard.type.Compartment || isCompartment )
        {
            diagramElement = new Compartment(parent, elementName, (Base)kernel);
        }
        else
        {
            diagramElement = new Node(parent, elementName, (Base)kernel);
        }
        return diagramElement;
    }

    private static DataElement defineKernel(final Compartment parent, DataElementPath dcPath, String elementName, @Nonnull String typeStr)
            throws Exception
    {
        Class<?> typeClass = null;
        try
        {
            typeClass = ClassLoading.loadClass( typeStr );
        }
        catch( LoggedClassNotFoundException e )
        {
        }
        if( typeClass != null && typeClass != Stub.NoteLink.class && typeClass != SemanticRelation.class && typeClass != Reaction.class )
        {
            if( dcPath != null && !dcPath.isEmpty() )
            {
                DataElement kernel = dcPath.getChildPath(elementName).getDataElement();
                if( ! ( kernel instanceof Base ) && ! ( kernel instanceof Diagram ) )
                {
                    throw new Exception("Cannot add element '" + elementName + "' of type '" + typeStr + "' to diagram.");
                }
                return kernel;
            }
            if( typeClass == Stub.Note.class || typeClass == Stub.PlotElement.class || typeClass == Stub.Bus.class )
            {
                return (DataElement)typeClass.getConstructor(DataCollection.class, String.class).newInstance(null, elementName);
            }
            if( typeClass == Event.class )
            {
                return new Stub(null, elementName, Type.MATH_EVENT);
            }
            if( typeClass == Equation.class )
            {
                return new Stub(null, elementName, Type.MATH_EQUATION);
            }
            if( typeClass == Function.class )
            {
                return new Stub(null, elementName, Type.MATH_FUNCTION);
            }
            if( typeClass == Constraint.class )
            {
                return new Stub(null, elementName, Type.MATH_CONSTRAINT);
            }
            if( Stub.ConnectionPort.class.isAssignableFrom(typeClass) )
            {
                String newElementName = DefaultSemanticController.generateUniqueNodeName(parent, elementName + Stub.ConnectionPort.SUFFIX);
                if( typeClass == Stub.OutputConnectionPort.class )
                {
                    return new Stub.OutputConnectionPort(null, newElementName);
                }
                else if( typeClass == Stub.InputConnectionPort.class )
                {
                    return new Stub.InputConnectionPort(null, newElementName);
                }
                else if( typeClass == Stub.ContactConnectionPort.class )
                {
                    return new Stub.ContactConnectionPort(null, newElementName);
                }
            }
            if( typeClass == State.class )
            {
                return new Stub(null, elementName, Type.MATH_STATE);
            }
        }
        else //default
        {
            return new Stub(null, elementName, typeStr);
        }
        throw new Exception("Cannot create element of type "+typeStr);
    }

    public static List<DiagramElement> addDroppedDiagramElement(final Diagram diagram, final DataElementPath path, final Point location) throws Exception
    {
        final List<DiagramElement> result = new ArrayList<>();
        try
        {
            performTransaction(diagram, "Add " + path.getName(), () -> {
                DataElement de = path.optDataElement();
                try
                {
                    DiagramEditorHelper helper = new DiagramEditorHelper( diagram );
                    Compartment origin = (Compartment)helper.getOrigin( location );
                    if( de != null && de instanceof Diagram )
                        AddElementsUtils.addDiagram( origin, (Diagram)de, location );
                    else
                        AddElementsUtils.addElements( origin, new Element[] {new Element( path )}, location );
                }
                catch( Exception ex )
                {
                    throw ExceptionRegistry.translateException( ex );
                }
            });
        }
        catch( Exception e )
        {
            throw new Exception( "Can't add element on diagram " + diagram.getName() + ": " + e.getMessage(), e );
        }
        return result;
    }

    public static List<DiagramElement> addElementWithConverter(final Diagram diagram, final DataElementPath path, final Point location)
            throws Exception
    {
        List<DiagramElement> result = new ArrayList<>();
        DataElement de = path.optDataElement();

        if( de != null && de instanceof Base )
        {
            DiagramTypeConverter[] converters = AddElementsUtils.getAvailableConverters( diagram );
            if( converters.length == 0 )
                return result;

            final DiagramEditorHelper helper = new DiagramEditorHelper( diagram );
            Compartment origin = (Compartment)helper.getOrigin( location );
            final Compartment parent = origin == null ? diagram : origin;

            DiagramElement[] nodes = AddElementsUtils.createDiagramElements( parent, (Base)de, converters );

            if( Util.hasNodeWithKernel( parent, nodes[0].getKernel() ) )
                throw new WebException( "EX_QUERY_ELEMENT_EXIST", de.getName(), parent.getName() );

            performTransaction( diagram, "Add " + path.getName(), () -> {
                try
                {
                    AddElementsUtils.addDiagramElements( parent, nodes, true, location );
                    //TODO: move elements to specified location
                    result.addAll( Arrays.asList( nodes ) );
                }
                catch( Exception e )
                {
                    throw ExceptionRegistry.translateException( e );
                }
            } );

        }
        return result;
    }

    /**
     * Add diagram element (edge)
     * @return
     */
    public static boolean addDiagramEdgeElement(Diagram diagram, OutputStream out, String input, String output, String typeStr,
            Object additional) throws Exception
    {
        final DiagramEditorHelper helper = new DiagramEditorHelper(diagram);
        //init stub for view editor pane
        ViewEditorPaneStub viewEditorStub = new ViewEditorPaneStub(helper, diagram);

        Properties properties = diagram.getOrigin().getInfo().getProperties();
        final SemanticController semanticController = diagram.getType().getSemanticController();

        Node inputNode = diagram.findNode(input);
        Node outputNode = diagram.findNode(output);
        if( inputNode == null || outputNode == null )
            throw new IllegalArgumentException("Can not find input or output node");
        String name = inputNode.getName() + "->" + outputNode.getName();
        Base kernel = null;
        Edge edge = null;
        boolean mustRemove = false;

        try
        {
            Class<?> typeClass = ClassLoading.loadClass( typeStr, properties.getProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY) );
            if( typeClass == Stub.NoteLink.class )
            {
                kernel = new Stub.NoteLink(null, name);
            }
            else if( typeClass == SemanticRelation.class )
            {
                kernel = new SemanticRelation(null, name);
            }
            else if( typeClass == Stub.DirectedConnection.class || typeClass == Stub.UndirectedConnection.class )
            {
                if( inputNode == outputNode )
                    throw new IllegalArgumentException("It is not allowed to create connections between same nodes.");
                Module module = Module.getModule(diagram);
                ConnectionEdgePane cp = new ConnectionEdgePane(module, viewEditorStub, typeClass, diagram);
                cp.createRelation();
                Compartment origin = Node.findCommonOrigin(inputNode, outputNode);
                //connection between diagrams
                if( inputNode instanceof Diagram && ( outputNode instanceof Diagram
                        || ( inputNode.getKernel() != null && inputNode.getKernel().getType().equals( Type.TYPE_PLOT ) ) ) )
                {
                    Edge connectionEdge = ConnectionEdgePane.getEdgeIfAlreadyExists(inputNode, outputNode, origin, typeClass);
                    if( additional == null )
                    {
                        JSONArray portParametersInput = new JSONArray(PortProperties.getParameters((Diagram)inputNode));
                        JSONArray portParametersOutput = new JSONArray(PortProperties.getParameters((Diagram)outputNode));
                        JSONObject param = new JSONObject();
                        param.put("input", portParametersInput);
                        param.put("output", portParametersOutput);
                        String[][] existedConnections = cp.getExistedConnections(connectionEdge);
                        if( existedConnections != null && existedConnections.length > 0 )
                        {
                            JSONArray existed = new JSONArray();
                            for( String[] existedConnection : existedConnections )
                            {
                                JSONArray con = new JSONArray();
                                String from = existedConnection[0];
                                String to = existedConnection[1];
                                if( from != null && to != null )
                                {
                                    con.put(from);
                                    con.put(to);
                                    existed.put(con);
                                }
                            }
                            param.put("connections", existed);
                        }
                        new JSONResponse(out).sendAdditionalJSON(param);
                        return false;
                    }
                    JSONArray connectionTable = new JSONArray(additional.toString());
                    Set<Pair<String, String>> connections = new HashSet<>();
                    for( int i = 0; i < connectionTable.length(); i++ )
                    {
                        JSONArray con = (JSONArray)connectionTable.get(i);
                        connections.add( new Pair<>( con.get( 0 ).toString(), con.get( 1 ).toString() ) );
                    }
                    edge = cp.makeDiagramConnectionEdge(connectionEdge, inputNode, outputNode, origin, connections);
                    if( connectionEdge != null && edge == null )
                    {
                        mustRemove = true;
                        edge = connectionEdge;
                    }
                }
                else
                {
                    edge = cp.createConnection(inputNode, outputNode );
                    kernel = edge.getKernel();
                }
            }
            else
            {
                edge = semanticController.createEdge(inputNode, outputNode, typeStr, Node.findCommonOrigin(inputNode, outputNode));
                if ( edge != null )
                    kernel = edge.getKernel();
            }
        }
        catch( LoggedClassNotFoundException e )
        {
            kernel = new Stub(null, name, typeStr);
        }

        if( kernel == null )
            throw new Exception("Can not add edge on diagram " + diagram.getCompletePath() + ": no kernel");
        Compartment origin = Node.findCommonOrigin(inputNode, outputNode);
        
        if (edge == null)
            edge = new Edge(origin, kernel, inputNode, outputNode);
        edge = (Edge)semanticController.validate(origin, edge, true);
        if( edge == null )
            throw new Exception("Can not add edge on diagram " + diagram.getCompletePath() + ": validation failed");
        if( mustRemove )
        {
            final Edge removedEdge = edge;
            performTransaction(diagram, "Remove edge " + edge.getName(), () -> helper.removeView(removedEdge.getView()));
        }
        else
        {
            if( edge.getOrigin().get(edge.getName()) != null )
            {
                String newName = edge.getName();
                int i = 2;
                while( edge.getOrigin().contains(newName) )
                {
                    newName = edge.getName() + "(" + i + ")";
                    i++;
                }
                edge = edge.clone((Compartment)edge.getOrigin(), newName);
            }
            final Edge addedEdge = edge;
            performTransaction(diagram, "Add edge " + edge.getName(), () -> helper.add(addedEdge, new Point(0, 0)));
        }
        return true;
    }

    public static List<DiagramElement> addReactionElement(final Diagram diagram, String reactionName, JSONArray components,
            final String formula,
            final String title,
            final Point location)
            throws Exception
    {
        final List<DiagramElement> result = new ArrayList<>();
        try
        {
            DiagramEditorHelper helper = new DiagramEditorHelper(diagram);
            Compartment origin = (Compartment)helper.getOrigin(location);
            final Compartment parent = origin == null ? diagram : origin;

            final List<SpecieReference> componentSpecies = new ArrayList<>();
            for( int i = 0; i < components.length(); i++ )
            {
                //TODO: change parsing to bean
                JSONObject comp = (JSONObject)components.get(i);
                String deName = comp.getString( "id" );
                SpecieReference component = new SpecieReference( null, deName + " as " + comp.getString( "role" ),
                        comp.getString( "role" ) );
                component.setTitle(comp.getString("title"));
                component.setComment(comp.getString("comment"));
                component.setParticipation(comp.getString("participation"));
                component.setStoichiometry(comp.getString("stoichiometry"));
                DiagramElement de = (DiagramElement)CollectionFactory.getDataElement( deName, diagram );
                if( de != null )
                    component.setSpecie(de.getKernel().getName());
                else
                    component.setSpecie( deName );

                if( comp.has("modifier") )
                    component.setModifierAction(comp.getString("modifier"));
                componentSpecies.add(component);
            }
            performTransaction(diagram, "Create reaction " + formula, () -> {
                try
                {
                    Reaction reactionPrototype = new Reaction( parent, reactionName );
                    if( formula != null )
                        reactionPrototype.setFormula( formula );
                    if( title != null )
                        reactionPrototype.setTitle( title );
                    reactionPrototype.setSpecieReferences(componentSpecies.toArray(new SpecieReference[componentSpecies.size()]));
                    DiagramElementGroup reactionElements = diagram.getType().getSemanticController().createInstance( parent, Reaction.class,
                            location, reactionPrototype );
                    reactionElements.putToCompartment( );
                    result.addAll( reactionElements.getElements() );
                }
                catch( Exception e )
                {
                    throw ExceptionRegistry.translateException(e);
                }
            });
        }
        catch( Exception e )
        {
            throw new Exception("Cannot add reaction: " + e.getMessage());
        }
        return result;
    }

    /*
     * Refresh diagram view, clean cache and calculate changed area
     */
    protected synchronized static Rectangle updateDiagramView(String completeName, Diagram diagram)
    {
        Map<String, Object> cache = getMapCache();
        //remove maps from cache
        List<String> toRemove = new ArrayList<>();
        for( String key : cache.keySet() )
        {
            if( key.startsWith("map_" + completeName.replaceAll("[/ ]", "_")) )
                toRemove.add(key);
        }
        for( String key : toRemove )
            cache.remove(key);

        CompositeView oldView = getStoredView(diagram);
        diagram.setView(null);
        //create new view
        View newView = createView(diagram);

        if( oldView == null || ! ( newView instanceof CompositeView ) )
        {
            Dimension size = getDiagramSize(diagram);
            return new Rectangle(0, 0, size.width, size.height);
        }
        Rectangle intersect = intersectView(oldView, (CompositeView)newView);
        return new Rectangle(intersect.x + diagram.getView().getBounds().x - 12, intersect.y + diagram.getView().getBounds().y - 12,
                intersect.width + 4, intersect.height + 4);
    }

    protected static Rectangle intersectView(CompositeView view1, CompositeView view2)
    {
        Rectangle result = new Rectangle(0, 0, -1, -1);
        if( view1 == null || view2 == null )
            return result;
        int size1 = view1.size();
        int size2 = view2.size();
        int start2 = 0;
        if( size1 == 0 || size2 == 0 )
            return result;
        BitSet v1UsedView = new BitSet(size1);
        BitSet v2UsedView = new BitSet(size2);
        for( int i = 0; i < size1; i++ )
        {
            View v1 = view1.elementAt(i);
            Rectangle r1 = v1.getBounds();
            int j = start2;
            do
            {
                View v2 = view2.elementAt(j);
                if( !v2UsedView.get( j ) )
                {
                    Rectangle r2 = v2.getBounds();
                    if( v1.getModel() == null || v2.getModel() == null || v1.getModel() == v2.getModel() )
                    {
                        if( ( r1.x == r2.x ) && ( r1.y == r2.y ) && ( r1.width == r2.width ) && ( r1.height == r2.height ) )
                        {
                            v1UsedView.set( i );
                            v2UsedView.set( j );
                            if( ( v1 instanceof CompositeView ) && ( v2 instanceof CompositeView ) )
                            {
                                Rectangle childRect = intersectView((CompositeView)v1, (CompositeView)v2);
                                if( childRect.width >= 0 && childRect.height >= 0 )
                                {
                                    result = result.union(childRect);
                                }
                            }
                            start2 = ( j + 1 ) % size2;
                            break;
                        }
                    }
                }
                j = ( j + 1 ) % size2;
            }
            while( j != start2 );
        }
        for( int i = 0; i < size1; i++ )
        {
            if( !v1UsedView.get( i ) )
                result = result.union(view1.elementAt(i).getBounds());
        }
        for( int j = 0; j < size2; j++ )
        {
            if( !v2UsedView.get( j ) )
                result = result.union(view2.elementAt(j).getBounds());
        }
        return result;
    }

    private static final JSONObject emptyRect = getEmptyRect();
    private static JSONObject getEmptyRect()
    {
        try
        {
            return new JSONObject("{\"x\":0, \"y\":0, \"width\":0, \"height\":0}");
        }
        catch( JSONException e )
        {
            return null;
        }
    }

    protected static JSONObject getChangedDiagramView(Diagram diagram, boolean generate) throws Exception
    {
        View oldView = getStoredView(diagram);
        //if(oldView != null) deactivateElements(oldView);
        View newView;
        synchronized( diagram )
        {
            if( generate || diagram.getView() == null )
            {
                diagram.setView(null);
                //create new view
                newView = createView(diagram);
                deactivateElements(newView);
            }
            else
            {
                newView = diagram.getView();
                storeView(diagram, newView);
            }
        }
        JSONArray history = getHistory(diagram);
        if( newView == oldView )
        {
            JSONObject result = new JSONObject();
            result.put("view", ( new DummyView(diagram, newView.isActive()) ).toJSON());
            result.getJSONObject("view").put("repaintRect", emptyRect);
            result.put("users", getUsers(diagram));
            result.put("transactions", getTransactions(diagram));
            if( history != null )
                result.put("history", history);
            return result;
        }
        Rectangle intersect = oldView == null ? newView.getBounds() : intersectView((CompositeView)oldView, (CompositeView)newView);
        Rectangle repaintRect = new Rectangle(intersect.x + newView.getBounds().x - 12, intersect.y + newView.getBounds().y - 12,
                intersect.width + 4, intersect.height + 4);
        JSONObject jsonView = oldView == null ? newView.toJSON() : newView.toJSONIfChanged(oldView);
        JSONObject repaintRectJSON = createJSONRectangle(repaintRect);
        jsonView.put("repaintRect", repaintRectJSON);
        JSONObject result = new JSONObject();
        result.put("view", jsonView);
        result.put("users", getUsers(diagram));
        if( history != null )
            result.put("history", history);
        result.put("transactions", getTransactions(diagram));
        return result;
    }

    protected static JSONArray getUsers(Diagram diagram) throws JSONException
    {
        SelectionStorage storage = null;
        try
        {
            storage = (SelectionStorage)diagram.getAttributes().getValue(WEB_SELECTIONS_ATTRIBUTE);
        }
        catch( Exception e )
        {
        }
        JSONArray result = new JSONArray();
        List<String> users = SecurityManager.getUsersForElement(diagram, diagram.getCompletePath().toString());
        for( String user : users )
        {
            JSONObject jsonUser = new JSONObject();
            jsonUser.put("name", user);
            if( storage != null )
            {
                JSONArray selections = new JSONArray();
                for( Rectangle rect : storage.getSelections(diagram, user) )
                {
                    selections.put(createJSONRectangle(rect));
                }
                if( selections.length() > 0 )
                    jsonUser.put("selections", selections);
            }
            result.put(jsonUser);
        }
        return result;
    }

    /**
     * Create diagram layouter info object if possible
     * @throws Exception
     */
    public static void sendDiagramLayouterInfo(OutputStream out, Diagram diagram) throws Exception
    {
        JSONObject responseObj = new JSONObject();
        boolean check = true;

        Layouter layouter = diagram.getPathLayouter();
        if( layouter == null )
        {
            check = false;
            responseObj.put( "selected", "Hierarchic layout" );
        }

        JSONArray list = new JSONArray();
        List<LayouterDescriptor> layoutDescr = ru.biosoft.plugins.graph.GraphPlugin.loadLayouters();
        for( LayouterDescriptor ld : layoutDescr )
        {

            if( check && ld.getType().equals( layouter.getClass() ) )
            {
                responseObj.put( "selected", ld.getTitle() );
                ComponentModel model = ComponentFactory.getModel( layouter, Policy.UI, true );
                JSONArray jsonProperties = JSONUtils.getModelAsJSON( model );
                responseObj.put( "properties", jsonProperties );
                check = false;
                list.put( ld.getTitle() );
            }
            else if( ld.isPublic() )
                list.put( ld.getTitle() );
        }
        responseObj.put( "list", list );
        new JSONResponse( out ).sendJSON( responseObj );
    }

    /**
     * Returns new object of layouter by name
     */
    public static Layouter getLayouterByName(String name)
    {
        try
        {
            return (Layouter)BeanRegistry.getBean(LayoutBeanProvider.class, name);
        }
        catch( Exception e )
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns icon for XmlDiagramType
     */
    public static void sendToolbarIcon(Diagram diagram, String type, OutputStream out)
    {
        try
        {
            if( diagram != null && diagram.getType() instanceof XmlDiagramType )
            {
                XmlDiagramType xmlDiagramType = (XmlDiagramType)diagram.getType();
                Icon icon = xmlDiagramType.getDiagramViewBuilder().getIcon(xmlDiagramType.getKernelType(type));
                int width = icon.getIconWidth();
                if( width < 0 )
                    width = 0;
                int height = icon.getIconHeight();
                if( height < 0 )
                    height = 0;
                if( width > 0 && height > 0 )
                {
                    BufferedImage b = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D graphics = b.createGraphics();
                    icon.paintIcon(null, graphics, 0, 0);
                    graphics.dispose();
                    ImageIO.write(b, "PNG", out);
                }
            }

            out.close();
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not generate diagram image", e);
        }
    }

    /**
     * Layout diagram
     */
    public static void layoutDiagram(Diagram diagram, String layouterName, JSONArray jsonParams, final String jobID, OutputStream out)
    {
        JSONResponse response = new JSONResponse(out);
        try
        {
            Layouter layouter = getLayouterByName(layouterName);
            if( layouter != null )
            {
                JSONUtils.correctBeanOptions(layouter, jsonParams);

                Diagram diagramForLayout = diagram.clone(diagram.getOrigin(), diagram.getName());

                Graph graph = DiagramToGraphTransformer.generateGraph(diagramForLayout, null);

                final LayoutContext layoutContext = LayoutContext.getLayoutContext();
                layoutContext.setDiagram(diagramForLayout);
                layoutContext.setGraph(graph);

                PathwayLayouter pathwayLayouter = new PathwayLayouter(layouter);
                final LayoutJobControlImpl jobControl = new LayoutJobControlImpl(pathwayLayouter.estimate(graph, 0));
                final WebJob webJob = WebJob.getWebJob(jobID);

                jobControl.addListener(new JobControlListenerAdapter()
                {
                    @Override
                    public void resultsReady(JobControlEvent event)
                    {
                        BufferedImage resultImage = layoutContext.generatePreviewImage();
                        String imageName = "layout_preview";
                        if( resultImage != null )
                        {
                            WebSession.getCurrentSession().putImage(imageName, resultImage);
                        }
                        webJob.addJobMessage(imageName);
                    }
                });
                webJob.setJobControl(jobControl);

                LayoutThread t = new LayoutThread(diagramForLayout, pathwayLayouter, graph, jobControl);
                WebSession.addThread(t);
                t.setPriority(Thread.MIN_PRIORITY);
                t.start();

                response.sendString("Started");
            }
            else
            {
                response.error("Unknown layouter type: " + layouterName);
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not send layout response", e);
        }
    }

    /**
     * Save current document layout to context
     * @throws WebException
     */
    public static void saveLayout(Diagram diagram) throws WebException
    {
        Graph graph = DiagramToGraphTransformer.generateGraph(diagram, diagram.getType().getSemanticController().getFilter());
        LayoutContext layoutContext = LayoutContext.getLayoutContext();
        layoutContext.setGraph(graph);
        layoutContext.setDiagram(diagram);

        BufferedImage resultImage = layoutContext.generatePreviewImage(false);
        String imageName = "layout_preview";
        if( resultImage == null )
            throw new WebException("EX_INTERNAL_LAYOUT_SAVE");
        WebSession.getCurrentSession().putImage(imageName, resultImage);
    }

    /**
     * Apply layout to diagram
     * @throws Exception
     */
    public static void applyLayout(final Diagram diagram, String layouterName, JSONArray jsonParams) throws WebException
    {
        final Layouter layouter = getLayouterByName(layouterName);
        if( layouter == null )
            throw new WebException("EX_QUERY_LAYOUTER_NOT_FOUND", layouterName);
        try
        {
            JSONUtils.correctBeanOptions(layouter, jsonParams);
        }
        catch( Exception e1 )
        {
            throw new WebException(e1, "EX_INTERNAL_UPDATE_BEAN", layouterName);
        }
        performTransaction(diagram, "Apply " + layouterName, () -> {
            try
            {
                LayoutContext.getLayoutContext().applyLayout(diagram, layouter);
                boolean notificationEnabled = diagram.isNotificationEnabled();
                diagram.setNotificationEnabled( false );
                diagram.setPathLayouter( layouter );
                diagram.setNotificationEnabled( notificationEnabled );
            }
            catch( Exception e )
            {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Undo diagram
     * @param diagram to apply undo
     * @param change to undo (-1 to undo single change)
     * @throws Exception
     */
    public static void undoDiagram(Diagram diagram, int change) throws Exception
    {
        TransactionUndoManager undoManager = getUndoManager(diagram);
        synchronized( diagram )
        {
            if( change > -1 )
                while( undoManager.canRedo() )
                    undoManager.redo();
            int nChanges = change == -1 ? 1 : undoManager.getEdits().size() - change;
            for( int i = 0; i < nChanges; i++ )
            {
                if( undoManager.canUndo() )
                    undoManager.undo();
            }
        }
    }

    /**
     * Redo diagram
     * @throws Exception
     */
    public static void redoDiagram(Diagram diagram) throws Exception
    {
        TransactionUndoManager undoManager = getUndoManager(diagram);
        synchronized( diagram )
        {
            if( undoManager.canRedo() )
                undoManager.redo();
        }
    }

    /**
     * Add search elements to diagram
     */
    public static void addSearchElements(final Compartment compartment, String elementsStr) throws Exception
    {
        final List<SearchElement> searchElements = new ArrayList<>();
        String[] oneElementStrings = elementsStr.split("\n");
        Diagram diagram = Diagram.getDiagram( compartment );
        for( String oneElementString : oneElementStrings )
        {
            String[] fields = TextUtil.split( oneElementString, ';' );
            if( !fields[0].isEmpty() )
            {
                DataElement de = CollectionFactory.getDataElement(fields[0]);
                if( de == null )
                {
                    try
                    {
                        String name = fields[0];
                        if( name.contains("/") )
                            name = name.substring(name.lastIndexOf("/") + 1, name.length());
                        String className = fields[5];
                        Class<? extends DataElement> clazz = ClassLoading.loadSubClass(className, DataElement.class);
                        de = clazz.getConstructor(DataCollection.class, String.class).newInstance(null, name);
                    }
                    catch( Exception ex )
                    {
                    }
                }
                if( de == null )
                    continue;
                SearchElement se = new SearchElement((Base)de);
                if( fields.length > 4 )
                {
                    try
                    {
                        se.setLinkedDirection(Integer.parseInt(fields[1]));
                        se.setLinkedFromPath(fields[2]);
                        se.setLinkedLength(Float.parseFloat(fields[3]));
                        se.setRelationType(fields[4]);
                    }
                    catch( NumberFormatException e )
                    {
                    }
                }
                searchElements.add(se);
            }
        }

        performTransaction( diagram, "Add from search",
                () -> {
                    try
                    {
                        AddElementsUtils.addElements( compartment, searchElements.toArray( new SearchElement[searchElements.size()] ),
                                null );
                    }
                    catch( Exception e )
                    {
                        throw ExceptionRegistry.translateException( e );
                    }
                } );
    }

    public static String sendDiagramChanges(Diagram diagram, OutputStream out, String responseType) throws Exception
    {
        JSONResponse response = new JSONResponse(out);
        return sendDiagramChanges(diagram, response, responseType, null);
    }

    public static String sendDiagramChanges(Diagram diagram, OutputStream out, String responseType, List<DiagramElement> newElements) throws Exception
    {
        JSONResponse response = new JSONResponse(out);
        return sendDiagramChanges(diagram, response, responseType, newElements);
    }

    public static String sendDiagramChanges(Diagram diagram, JSONResponse response, String responseType, List<DiagramElement> newElements) throws Exception
    {
        if( "json".equals( responseType ) )
        {
            JSONObject changedView = getChangedDiagramView(diagram, true);
            if(newElements != null && !newElements.isEmpty())
            {
                JSONArray elements = new JSONArray();
                for(DiagramElement de : newElements)
                {
                    String relativePath = DataElementPath.create( de ).getPathDifference( diagram.getCompletePath() );
                    elements.put( relativePath );
                }
                changedView.put( "elements", elements );
            }
            response.sendJSON(changedView);
            response.getOutputStream().close();
            return "application/json";
        }
        Rectangle refreshArea = updateDiagramView(diagram.getCompletePath().toString(), diagram);
        response.sendSizeParameters(getDiagramSize(diagram), refreshArea);
        return "text/html";
    }

    private static void sendBase(List<DiagramElement> elements, OutputStream out) throws IOException
    {
        JSONArray results = new JSONArray();
        for( DiagramElement de : elements )
        {
            if( de instanceof Node )
            {
                Base base = ( (Node)de ).getKernel();
                DynamicProperty dp = base.getAttributes().getProperty(Util.ORIGINAL_PATH);
                if (dp != null)
                {
                    String originalPath = (String)dp.getValue();
                    DataElementPath dep = DataElementPath.create(originalPath);
                    DataElement element = dep.getDataElement();
                    if (element instanceof Base)
                        base = (Base)element;
                }
                if( base != null && base.getOrigin() != null )
                {
                    JSONObject baseJson = new JSONObject();
                    try
                    {
                        baseJson.put("name", DataElementPath.create(base).toString());
                        baseJson.put("title", base.getTitle());
                        results.put(baseJson);
                    }
                    catch( JSONException e )
                    {
                    }
                }
            }
        }
        new JSONResponse(out).sendJSON(results);
    }

    /**
     * Send type of diagram (like sendType), but without diagram instantiation if possible
     * @param path
     * @param out
     * @throws WebException
     */
    private boolean sendTypeFast(DataElementPath path, OutputStream out) throws IOException, WebException
    {
        DataElementDescriptor descriptor = path.getDescriptor();
        if(descriptor == null)
        {
            throw new WebException("EX_QUERY_NO_DIAGRAM", path);
        }
        String type = descriptor.getValue(Diagram.DIAGRAM_TYPE_PROPERTY);
        String composite = descriptor.getValue( Diagram.COMPOSITE_DIAGRAM_PROPERTY );
        if(type != null && composite != null)
        {
            JsonObject diagramInfo = new JsonObject();
            diagramInfo.add("type", type);
            diagramInfo.add( "composite", Boolean.valueOf(composite) );
            String roleClassName = descriptor.getValue(Diagram.DIAGRAM_ROLE_PROPERTY);
            Class<?> roleClass = null;
            try
            {
                roleClass = roleClassName == null ? null : ClassLoading.loadClass( roleClassName );
            }
            catch( Exception e )
            {
            }
            if(roleClass != null && EModelRoleSupport.class.isAssignableFrom(roleClass))
            {
                diagramInfo.add("model", "true");
                diagramInfo.add("modelClass", roleClass.getName());
            }
            else
            {
                diagramInfo.add("model", "false");
            }
            new JSONResponse(out).sendJSON(diagramInfo);
            return true;
        }
        return false;
    }

    /**
     * Send name of diagram type class
     * @throws IOException
     */
    public static void sendType(Diagram diagram, OutputStream out) throws IOException
    {
        String typeClass = diagram.getType().getClass().getName();
        JsonObject diagramInfo = new JsonObject();
        diagramInfo.add("type", typeClass);
        diagramInfo.add( "composite", DiagramUtility.isComposite( diagram ) );
        Role model = diagram.getRole();
        if( model != null && ( model instanceof EModelRoleSupport ) )
        {
            diagramInfo.add("model", "true");
            diagramInfo.add("modelClass", model.getClass().getName());
        }
        else
        {
            diagramInfo.add("model", "false");
        }
        new JSONResponse(out).sendJSON(diagramInfo);
    }

    /**
     * Send parameters names for port creation
     */
    public static void sendPortParameters(Diagram diagram, OutputStream out)
    {
        try
        {
            JSONArray portParameters = new JSONArray(PortProperties.getParameters(diagram));
            new JSONResponse(out).sendJSON(portParameters);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Cannot get parameters list", e);
        }
    }

    /**
     * Send kernel info for all elements of the diagram
     */
    private void sendKernels(Diagram diagram, OutputStream out) throws IOException
    {
        JSONArray elementsWithKernels = new JSONArray();
        diagram.recursiveStream().forEach( de -> {
            JSONObject deObj = new JSONObject();
            deObj.put( "name", de.getCompleteNameInDiagram() );
            Base kernel = de.getKernel();
            if( kernel != null )
            {
                JSONObject kernelObj = new JSONObject();
                kernelObj.put( "name", kernel.getName() );
                kernelObj.put( "type", kernel.getType() );
                deObj.put( "kernel", kernelObj );
            }
            elementsWithKernels.put( deObj );
        } );
        new JSONResponse( out ).sendJSON( elementsWithKernels );
    }

    /**
     * Return diagram clone using session
     */

    public static Diagram getDiagram(String completeName, boolean needView)
    {
        DataElement diagramElement = CollectionFactory.getDataElement(completeName);
        if( diagramElement != null && ! ( diagramElement instanceof Diagram ) )
            return null;

        Diagram diagram = (Diagram)diagramElement;
        if( diagram == null )
        {
            Object diagramObj = WebServicesServlet.getSessionCache().getObject(completeName);
            if( diagramObj instanceof Diagram )
            {
                diagram = (Diagram)diagramObj;
            }
            if( diagram == null )
            {
                try
                {
                    diagram = (Diagram)WebJob.getJobData(completeName);
                }
                catch(Throwable t)
                {
                    log.log(Level.SEVERE, "Can not getJobData for diagram=" + completeName, t);
                }
            }
        }

        if ( diagram == null || !needView || diagram.getView() != null )
            return diagram;

        //log.log( Level.INFO, "getDiagram: user = '" + WebSession.getCurrentSession().getUserName() + "', name = '" + diagram.getName() + "', path = '" + diagram.getCompletePath() + "'" );

        synchronized( diagram )
        {
            try
            {
                DiagramViewBuilder builder = diagram.getType().getDiagramViewBuilder();
                builder.createDiagramView(diagram, ApplicationUtils.getGraphics());
                diagram.getView().setLocation(10, 10);

                // ??
                DiagramFilter[] filterList = diagram.getFilterList();
                for( DiagramFilter filter : filterList )
                {
                    if( filter != null && filter.isEnabled() )
                        filter.apply( diagram );
                }

                return diagram;
            }
            catch(Throwable t)
            {
                log.log(Level.SEVERE, "Can not generate view for diagram=" + completeName, t);
            }

            diagram.notifyAll();
        }

        return null;        
    }

    public static Diagram getDiagramPrev(String completeName, boolean needView)
    {
        DataElement diagramElement = CollectionFactory.getDataElement(completeName);
        if( diagramElement != null && ! ( diagramElement instanceof Diagram ) )
            return null;
        Diagram diagram = (Diagram)diagramElement;
        if( diagram == null )
        {
            Object diagramObj = WebServicesServlet.getSessionCache().getObject(completeName);
            if( diagramObj instanceof Diagram )
            {
                diagram = (Diagram)diagramObj;
            }
            if( diagram == null )
            {
                try
                {
                    diagram = (Diagram)WebJob.getJobData(completeName);
                }
                catch( Exception e )
                {
                }
            }
        }
        if( !needView )
            return diagram;
        if( diagram != null )
        {
            //log.log( Level.INFO, "getDiagram: user = '" + WebSession.getCurrentSession().getUserName() + "', name = '" + diagram.getName() + "', path = '" + diagram.getCompletePath() + "'", new Exception() );
            boolean needRelayout = ( diagram.getView() == null );
            if( needRelayout )
            {
                synchronized( diagram )
                {
                    try
                    {
                        if( needRelayout && ( diagram.getOrigin() != null ) && ( diagram.getPathLayouter() == null ) )
                        {
                            DataCollectionInfo info = diagram.getOrigin().getInfo();
                            String layouterName = info.getProperty(Diagram.DEFAULT_LAYOUTER);
                            if( layouterName != null )
                            {
                                boolean notificationEnabled = diagram.isNotificationEnabled();
                                diagram.setNotificationEnabled(false);
                                try
                                {
                                    String pluginNames = info.getProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY);
                                    Class<? extends Layouter> layouterClass = ClassLoading.loadSubClass( layouterName, pluginNames, Layouter.class );
                                    diagram.setPathLayouter(layouterClass.newInstance());
                                }
                                catch( Throwable t )
                                {
                                    diagram.setPathLayouter(new ForceDirectedLayouter());
                                    log.log(Level.SEVERE, "Can not load default layouter", t);
                                }
                                diagram.setNotificationEnabled(notificationEnabled);
                            }
                            DiagramToGraphTransformer.layoutIfNeeded(diagram);
                            diagram.setView(null);
                        }

                        DiagramViewBuilder builder = diagram.getType().getDiagramViewBuilder();
                        if( diagram.getView() == null )
                        {
                            builder.createDiagramView(diagram, ApplicationUtils.getGraphics());
                            DiagramFilter[] filterList = diagram.getFilterList();
                            for( DiagramFilter filter : filterList )
                            {
                                if( filter != null && filter.isEnabled() )
                                    filter.apply( diagram );
                            }
                        }

                        if( diagram.getLabelLayouter() != null && ( ( diagram.getAttributes().getValue("layouted") == null ) || needRelayout ) )
                        {
                            DiagramToGraphTransformer.layoutLabels(diagram);
                            builder.createDiagramView(diagram, ApplicationUtils.getGraphics());
                            diagram.getAttributes().add(new DynamicProperty("layouted", Boolean.class, true));
                        }

                        if( diagram.getView() != null )
                        {
                            diagram.getView().setLocation(10, 10);
                        }
                    }
                    catch( Throwable e )
                    {
                        log.log(Level.SEVERE, "Can not layout labels", e);
                    }
                } // synchronized
            } // if( needRelayout )
            else
            {
                try
                {
                    if( diagram.getLabelLayouter() != null && ( diagram.getAttributes().getValue("layouted") == null ) )
                    {
                        synchronized( diagram )
                        {
                            if( diagram.getLabelLayouter() != null && ( diagram.getAttributes().getValue("layouted") == null ) )
                            {
                                DiagramViewBuilder builder = diagram.getType().getDiagramViewBuilder();
                                DiagramToGraphTransformer.layoutLabels(diagram);
                                builder.createDiagramView(diagram, ApplicationUtils.getGraphics());
                                diagram.getAttributes().add(new DynamicProperty("layouted", Boolean.class, true));
                            }
                        } 
                    }
                    diagram.getView().setLocation(10, 10);
                }
                catch( Throwable e )
                {
                    log.log(Level.SEVERE, "Can not layout labels", e);
                }
            }
            return diagram;
        }

        return null;
    }

    public static @Nonnull Diagram getDiagramChecked(DataElementPath path) throws WebException
    {
        Diagram diagram = getDiagram(path.toString(), true);
        if(diagram == null)
        {
            throw new WebException("EX_QUERY_NO_DIAGRAM", path);
        }
        return diagram;
    }

    /**
     * Get {@link TransactionUndoManager} for diagram
     */
    public synchronized static WebTransactionUndoManager getUndoManager(final Diagram diagram)
    {
        Object value = diagram.getAttributes().getValue(WEB_UNDO_MANAGER_ATTRIBUTE);
        WebTransactionUndoManager undoManager = value instanceof WebTransactionUndoManager ? (WebTransactionUndoManager)value : null;
        if( undoManager != null )
            return undoManager;
        undoManager = new WebTransactionUndoManager(diagram);

        DynamicProperty property = new DynamicProperty(WEB_UNDO_MANAGER_ATTRIBUTE, TransactionUndoManager.class, undoManager);
        DPSUtils.makeTransient(property);
        property.setHidden( true );
        diagram.getAttributes().add(property);

        return undoManager;
    }

    public static WebTransactionUndoManager initUndoManager(Diagram diagram)
    {
        getUndoListener(diagram);
        return getUndoManager(diagram);
    }

    public synchronized static DataCollectionUndoListener getUndoListener(Diagram diagram)
    {
        Object value = diagram.getAttributes().getValue(WEB_UNDO_LISTENER_ATTRIBUTE);
        DataCollectionUndoListener undoListener = value instanceof DataCollectionUndoListener ? (DataCollectionUndoListener)value : null;
        if( undoListener != null )
            return undoListener;
        TransactionUndoManager undoManager = getUndoManager(diagram);
        undoListener = new DataCollectionUndoListener(undoManager);

        DynamicProperty property = new DynamicProperty(WEB_UNDO_LISTENER_ATTRIBUTE, DataCollectionUndoListener.class, undoListener);
        DPSUtils.makeTransient(property);
        property.setHidden( true );
        diagram.getAttributes().add(property);
        diagram.addDataCollectionListener(undoListener);
        diagram.addPropertyChangeListener(undoListener);

        return undoListener;
    }

    public static void performTransaction(Diagram diagram, String name, Runnable runnable) throws WebException
    {
        synchronized( diagram )
        {
            WebServicesServlet.getSessionCache().setObjectChanged(diagram.getCompletePath().toString(), diagram);
            TransactionUndoManager undoManager = initUndoManager(diagram);
            undoManager.startTransaction(new TransactionEvent(undoManager, name));
            try
            {
                runnable.run();
            }
            catch( BiosoftCustomException e )
            {
                throw new WebException( "EX_QUERY_CUSTOM_MESSAGE", e.getMessage() );
            }
            catch( Throwable t )
            {
                throw new WebException(t, "EX_INTERNAL_DURING_ACTION", name);
            }
            finally
            {
                undoManager.completeTransaction();
            }
        }
    }

    protected static final String MAPS = "maps";

    /**
     * Return map cache for current session
     */
    protected static Map<String, Object> getMapCache()
    {
        Object mapObj = WebSession.getCurrentSession().getValue(MAPS);
        Map<String, Object> mapCache;
        if( mapObj == null || ! ( mapObj instanceof Map ) )
        {
            mapCache = new ConcurrentHashMap<>();
            WebSession.getCurrentSession().putValue(MAPS, mapCache);
        }
        else
        {
            mapCache = (Map)mapObj;
        }
        return mapCache;
    }

    /**
     * @param diagram - diagram to process
     * @param de - edge to process
     * @param action - action string
     * @param pointInPath - number of vertex in edge path
     * @param location - where to add or move a vertex
     */
    public static void processVertex(Diagram diagram, DiagramElement de, String action, int pointInPath, Point location)
            throws WebException
    {
        if( ! ( de instanceof Edge ) )
            throw new WebException("EX_QUERY_NOT_EDGE", de.getCompleteNameInDiagram());
        final Edge edge = (Edge)de;
        final SemanticController sc = diagram.getType().getSemanticController();

        SimplePath oldPath = edge.getSimplePath();
        if( oldPath == null )
            throw new WebException("EX_INTERNAL_INVALID_EDGE", de.getCompleteNameInDiagram());

        try
        {
            //hack: path has offset compared to one send as JSON
            Point offset = ( (ArrowView) ( (CompositeView)edge.getView() ) ).getPathOffset();
            location.x -= offset.x;
            location.y -= offset.y;
        }
        catch( Exception e )
        {
        }
        Path newPath = null;
        if( action.equals("add") )
        {
            int pos = PathUtils.getNearestSegment(oldPath, location);
            newPath = new Path();
            for( int i = 0; i <= pos; i++ )
            {
                newPath.addPoint(oldPath.xpoints[i], oldPath.ypoints[i], oldPath.pointTypes[i]);
            }
            newPath.addPoint(location.x, location.y);
            for( int i = pos + 1; i < oldPath.npoints; i++ )
            {
                newPath.addPoint(oldPath.xpoints[i], oldPath.ypoints[i], oldPath.pointTypes[i]);
            }
        }
        else if( action.equals("straighten") )
        {
            newPath = new Path();
            newPath.addPoint(oldPath.xpoints[0], oldPath.ypoints[0]);
            newPath.addPoint(oldPath.xpoints[oldPath.npoints - 1], oldPath.ypoints[oldPath.npoints - 1]);
        }
        else if( pointInPath > 0 && pointInPath < oldPath.npoints - 1 )
        {
            if( action.equals("remove") )
            {
                newPath = new Path();
                for( int i = 0; i < pointInPath; i++ )
                {
                    newPath.addPoint(oldPath.xpoints[i], oldPath.ypoints[i], oldPath.pointTypes[i]);
                }
                for( int i = pointInPath + 1; i < oldPath.npoints; i++ )
                {
                    newPath.addPoint(oldPath.xpoints[i], oldPath.ypoints[i], oldPath.pointTypes[i]);
                }
            }
            else if( action.equals("move") )
            {
                newPath = new Path(oldPath.xpoints, oldPath.ypoints, oldPath.pointTypes, oldPath.npoints);
                newPath.xpoints[pointInPath] = location.x;
                newPath.ypoints[pointInPath] = location.y;
            }
            else
            {
                newPath = new Path(oldPath.xpoints, oldPath.ypoints, oldPath.pointTypes, oldPath.npoints);
                if( action.equals("line") )
                {
                    newPath.pointTypes[pointInPath] = Path.LINE_TYPE;
                }
                else if( action.equals("quadric") )
                {
                    newPath.pointTypes[pointInPath] = Path.QUAD_TYPE;
                }
                else if( action.equals("cubic") )
                {
                    newPath.pointTypes[pointInPath] = Path.CUBIC_TYPE;
                }
            }
        }
        else if( action.equals( "move" ) && ( pointInPath == 0 || pointInPath == oldPath.npoints - 1 ) && edge.isFixedInOut() )
        {
            newPath = new Path( oldPath.xpoints, oldPath.ypoints, oldPath.pointTypes, oldPath.npoints );
            if( pointInPath == 0 )
            {
                Point p = Diagram.getDiagram( de ).getType().getDiagramViewBuilder().getNearestNodePoint( location, edge.getInput() );
                newPath.xpoints[pointInPath] = p.x;
                newPath.ypoints[pointInPath] = p.y;
            }
            else
            {
                Point p = Diagram.getDiagram( de ).getType().getDiagramViewBuilder().getNearestNodePoint( location, edge.getOutput() );
                newPath.xpoints[pointInPath] = p.x;
                newPath.ypoints[pointInPath] = p.y;
            }
        }
        if( newPath != null )
        {
            final Path path = newPath;
            performTransaction( diagram, action + " vertex of " + edge.getName(), () -> {
                edge.setPath( path );
                sc.recalculateEdgePath( edge );
            } );
        }
    }

    private static Diagram getDiagramWithState(Diagram diagram, int editFrom, int editTo) throws Exception
    {
        WebTransactionUndoManager undoManager = getUndoManager(diagram);
        List<UndoableEdit> edits = undoManager.getEdits();
        int curEdit = -1;
        UndoableEdit editToBeRedone = undoManager.editToBeRedone();
        if( editToBeRedone != null )
            curEdit = edits.indexOf(editToBeRedone);
        if( curEdit == -1 )
            curEdit = edits.size();
        if( editTo == -1 )
            editTo = curEdit;
        if( editFrom > editTo )
        {
            int tmp = editFrom;
            editFrom = editTo;
            editTo = tmp;
        }
        Diagram result = curEdit > editFrom ? DiagramStateUtility.getDiagramCloneWithUndo(diagram, edits.subList(editFrom, curEdit))
                : curEdit < editFrom ? DiagramStateUtility.getDiagramCloneWithRedo(diagram, edits.subList(curEdit, editFrom))
                        : diagram.clone(diagram.getOrigin(), diagram.getName());
        if( editTo > editFrom )
        {
            biouml.standard.state.State state = new biouml.standard.state.State(null, result, "", edits.subList(editFrom, editTo));
            result.addState(state);
            result.setStateEditingMode(state);
        }
        createView(result);
        return result;
    }

    protected static JSONArray getTransactions(Diagram diagram)
    {
        return initUndoManager(diagram).toJSON();
    }

    /**
     * Returns JSONArray representing information about history revisions. Result looks like transactions
     * Will work for various elements in future, though now implemented for Diagrams only
     * @param element element (diagram) to get versions of
     * @return null if no history supported for the element
     * @throws Exception
     */
    protected static JSONArray getHistory(DataElement element)
    {
        try
        {
            HistoryDataCollection historyCollection = HistoryFacade.getHistoryCollection(element);
            if( historyCollection == null )
                return null;
            List<String> elementNames = historyCollection.getHistoryElementNames(DataElementPath.create(element), 0);
            if( elementNames == null )
                return null;
            JSONArray result = new JSONArray();
            for( String name : elementNames )
            {
                HistoryElement he = (HistoryElement)historyCollection.get(name);
                JSONObject entry = new JSONObject();
                entry.put("user", he.getAuthor() == null ? "?" : he.getAuthor());
                entry.put("comment", he.getComment());
                entry.put("version", he.getVersion());
                String title = he.getVersion() + ": " + he.getComment();
                if( title.length() > 30 )
                    title = title.substring(0, 29) + "...";
                entry.put("name", title);
                entry.put("time", he.getTimestamp().getTime());
                result.put(entry);
            }
            return result;
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "While getting history for " + element + ": ", e);
            return null;
        }
    }

    /**
     * Highlight variables/parameters on diagram
     * @param diagram to process
     * @param elemens - array ov variable names
     * @param varType - "variables" or "parameters"
     */
    private void highlightVariables(Compartment diagram, Set<String> elements, String varType)
    {
        List<DiagramElement> des = new ArrayList<DiagramElement>();
        if( varType.equals( "compartments" ) || varType.equals( "entities" ) || varType.equals( "variables" ) )
        {
            diagram.recursiveStream().filter( de -> DAEModelUtilities.hasVariables( de, elements ) ).forEach( n -> des.add( n ) );
        }
        else if( varType.equals( "functions" ) )
        {
            diagram.recursiveStream().filter( de -> DAEModelUtilities.hasFunction( de, elements ) ).forEach( n -> des.add( n ) );
        }
        else if( varType.equals( "equations" ) || varType.equals( "events" ) || varType.equals( "constraints" )
                || varType.equals( "subdiagrams" ) )
        {
            StreamEx.of( elements ).map( element -> diagram.findNode( element ) ).nonNull().forEach( n -> des.add( n ) );
        }
        else if( varType.equals( "ports" ) || varType.equals( "buses" ) )
        {
            StreamEx.of( elements ).map( element -> diagram.findNode( element ) ).nonNull().forEach( n -> {
                des.add( n );
                des.addAll( Arrays.asList( n.getEdges() ) );
            } );
        }
        else if( varType.equals( "connections" ) )
        {
            StreamEx.of( elements ).map( el -> diagram.findDiagramElement( el ) ).nonNull().forEach( e -> des.add( e ) );
        }
        DiagramUtility.highlight( des );
        //        highlightFilter.setEnabled( true );
        Diagram d = Diagram.getDiagram( diagram );
        //        if( !DiagramUtility.hasFilter( d, highlightFilter ) )
        //            DiagramUtility.addFilter( d, highlightFilter );
    }

    private void removeVariables(Compartment diagram, Set<String> elements) throws Exception
    {
        if( diagram.getRole() instanceof EModel )
        {
            EModel model = diagram.getRole(EModel.class);
            List<DiagramElement> toRemove = new ArrayList<>();
            elements.stream().forEach(
                    name -> {
                        try
                        {
                            Variable var = model.getVariable( name );
                            if( var instanceof VariableRole )
                            {
                                DiagramElement[] nodes = ( (VariableRole)var ).getAssociatedElements();
                                toRemove.addAll( Arrays.asList( nodes ) );
                            }
                            else
                                model.getVariables().remove( name );
                        }
                        catch( Exception e )
                        {
                        }
                    } );
            if( !toRemove.isEmpty() )
                removeDiagramElement( Diagram.getDiagram( diagram ), toRemove );
        }
    }

    private void addVariable(Compartment diagram) throws Exception
    {
        if( diagram.getRole() instanceof EModel )
        {
            EModel emodel = diagram.getRole( EModel.class );
            String baseName = "parameter";
            int i = 2;
            String name = baseName + "_1";
            while( emodel.containsVariable( name ) )
                name = baseName + "_" + i++;
            emodel.declareVariable( name, Double.valueOf( 0 ) );
        }
    }

    /**
     * Send all variables from Role
     */
    private void sendAllVariables(Diagram diagram, OutputStream out) throws IOException
    {
        JSONArray roleVariables = new JSONArray();
        if (diagram.getRole() instanceof EModel)
        {
            EModel model = diagram.getRole(EModel.class);
            model.getVariables().forEach(variable -> {
                JSONObject variableObj = new JSONObject();
                variableObj.put("name", variable.getName());
                variableObj.put("value", variable.getInitialValue());
                roleVariables.put(variableObj);
            });
        }
        new JSONResponse( out ).sendJSON( roleVariables );
    }

}
