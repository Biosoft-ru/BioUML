package ru.biosoft.server.servlets.webservices.providers;

import java.awt.Color;
import java.awt.Point;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.swing.undo.UndoableEdit;

import org.json.JSONArray;
import org.json.JSONObject;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.eclipsesource.json.JsonObject;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.Edge;
import biouml.model.InitialElementProperties;
import biouml.model.ModelDefinition;
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
import biouml.model.xml.XmlDiagramSemanticController;
import biouml.model.xml.XmlDiagramType;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.PathwaySimulationSemanticController;
import biouml.standard.diagram.PortProperties;
import biouml.standard.diagram.ReactionInitialProperties;
import biouml.standard.diagram.Util;
import biouml.standard.state.DiagramStateUtility;
import biouml.standard.type.Base;
import biouml.standard.type.Reaction;
import biouml.standard.type.SemanticRelation;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;
import biouml.standard.type.Stub.ConnectionPort;
import biouml.workbench.diagram.DiagramEditorHelper;
import biouml.workbench.diagram.SetInitialValuesAction;
import biouml.workbench.diagram.ViewEditorPaneStub;
import biouml.workbench.diagram.viewpart.HighlightFilter;
import one.util.streamex.StreamEx;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementDescriptor;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.history.HistoryFacade;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.LoggedClassNotFoundException;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.server.JSONUtils;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.BiosoftWebResponse;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.WebException;
import ru.biosoft.server.servlets.webservices.WebTransactionUndoManager;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.datatype.DataType;

/**
 * Process web requests for model-related features of Diagram
 */

public class WebModelProvider extends WebDiagramsProvider
{

    @Override
    public void process(BiosoftWebRequest arguments, BiosoftWebResponse resp) throws Exception
    {
        String diagramAction = arguments.optAction();
        if( diagramAction == null )
        {
            super.process( arguments, resp );
            return;
        }

        OutputStream out = resp.getOutputStream();
        resp.setContentType( "application/json" );

        if( diagramAction.equals( "get_type" ) && sendTypeFast( arguments.getDataElementPath(), out ) )
            return;

        Diagram diagram = getDiagramChecked( arguments.getDataElementPath() );
        int editFrom = arguments.optInt( "editFrom", -1 );
        if( editFrom >= 0 )
        {
            int editTo = arguments.optInt( "editTo", -1 );
            diagram = getDiagramWithState( diagram, editFrom, editTo );
        }

        int version = arguments.optInt( "version", -2 );
        int version2 = arguments.optInt( "version2", -2 );
        diagram = getDiagramVersion( diagram, version, version2 );

        String type = arguments.get( TYPE );
        if( diagramAction.equals( "check_diagram_element" ) )
        {
            String nodeName = arguments.getString( "node" );
            DiagramElement de = diagram.findDiagramElement( DiagramUtility.toDiagramPath( nodeName ) );
            // not only subdiagrams are presented on the composite diagram
            // there are also equations, events, plots, etc.
            if( Util.isSubDiagram( de ) )
            {
                SubDiagram subDiagram = castDataElement( de, SubDiagram.class );
                new JSONResponse( out ).sendString( subDiagram.getDiagramPath() );
            }
            else if( de instanceof ModelDefinition )
            {
                new JSONResponse( out ).sendString( ((ModelDefinition) de).getDiagramPath() );
            }
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
                    if( ((ReactionInitialProperties) prop).acceptForReaction( (Node) de ) )
                    {
                        new JSONResponse( out ).send( new byte[0], 0 );
                        return;
                    }
            }
            new JSONResponse( out ).error( "Component " + de.getName() + " can not be added to reaction" );
            return;
        }
        else if( diagramAction.equals( "add_reaction" ) ) //Add reaction
        {
            JSONArray components = arguments.optJSONArray( "components" );
            String responseType = arguments.get( "resptype" );
            if( components != null )
            {
                try
                {
                    String name = arguments.get( "name" );
                    String formula = arguments.get( "formula" );
                    String title = arguments.get( "title" );
                    List<DiagramElement> elements = addReactionElement( diagram, name, components, formula, title, arguments.getPoint() );
                    sendDiagramChanges( diagram, out, responseType, elements );
                    return;
                }
                catch (Exception e)
                {
                    new JSONResponse( out ).error( "Can not add reaction element" );
                    return;
                }
            }
        }
        else if( diagramAction.equals( "get_port_parameters" ) )
        {
            sendPortParameters( diagram, out );
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
            if( model != null && (model instanceof EModel) )
            {
                ((EModel) model).detectVariableTypes();
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
        else if( diagramAction.equals( "set_initial" ) )
        {
            setInitial( diagram, arguments, out );
            return;
        }
        else
        {
            super.process( arguments, resp );
        }

    }

    /**
     * Returns older saved version of diagram or diff between two versions
     * 
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
            Diagram diagram1 = version == -1 ? diagram : (Diagram) HistoryFacade.getVersion( diagram, version );
            if( diagram1 == null )
                throw new WebException( "EX_QUERY_INVALID_VERSION", diagram.getCompletePath(), version );
            if( version2 >= -1 )
            {
                Diagram diagram2 = version2 == -1 ? diagram : (Diagram) HistoryFacade.getVersion( diagram, version2 );
                if( diagram2 == null )
                    throw new WebException( "EX_QUERY_INVALID_VERSION", diagram.getCompletePath(), version2 );

                diagram = (Diagram) HistoryFacade.getDiffElement( diagram1, diagram2 );
            }
            else
                diagram = diagram1;
            createView( diagram );
        }
        return diagram;
    }

    
    /**
     * Send type of diagram (like sendType), but without diagram instantiation if possible
     * @param path
     * @param out
     * @throws WebException
     */
    @Override
    protected boolean sendTypeFast(DataElementPath path, OutputStream out) throws IOException, WebException
    {
        DataElementDescriptor descriptor = path.getDescriptor();
        if( descriptor == null )
        {
            throw new WebException( "EX_QUERY_NO_DIAGRAM", path );
        }
        String type = descriptor.getValue( Diagram.DIAGRAM_TYPE_PROPERTY );
        String composite = descriptor.getValue( Diagram.COMPOSITE_DIAGRAM_PROPERTY );
        if( type != null && composite != null )
        {
            JsonObject diagramInfo = new JsonObject();
            diagramInfo.add( "type", type );
            diagramInfo.add( "composite", Boolean.valueOf( composite ) );
            String roleClassName = descriptor.getValue( Diagram.DIAGRAM_ROLE_PROPERTY );
            Class<?> roleClass = null;
            try
            {
                roleClass = roleClassName == null ? null : ClassLoading.loadClass( roleClassName );
            }
            catch (Exception e)
            {
            }
            if( roleClass != null && EModelRoleSupport.class.isAssignableFrom( roleClass ) )
            {
                diagramInfo.add( "model", "true" );
                diagramInfo.add( "modelClass", roleClass.getName() );
            }
            else
            {
                diagramInfo.add( "model", "false" );
            }
            new JSONResponse( out ).sendJSON( diagramInfo );
            return true;
        }
        return false;
    }

    /**
     * Send name of diagram type class
     * @throws IOException
     */
    @Override
    public void sendType(Diagram diagram, OutputStream out) throws IOException
    {
        String typeClass = diagram.getType().getClass().getName();
        JsonObject diagramInfo = new JsonObject();
        diagramInfo.add( "type", typeClass );
        diagramInfo.add( "composite", DiagramUtility.isComposite( diagram ) );
        Role model = diagram.getRole();
        if( model != null && (model instanceof EModelRoleSupport) )
        {
            diagramInfo.add( "model", "true" );
            diagramInfo.add( "modelClass", model.getClass().getName() );
        }
        else
        {
            diagramInfo.add( "model", "false" );
        }
        new JSONResponse( out ).sendJSON( diagramInfo );
    }

    /**
     * Send parameters names for port creation
     */
    public static void sendPortParameters(Diagram diagram, OutputStream out)
    {
        try
        {
            JSONArray portParameters = new JSONArray( PortProperties.getParameters( diagram ) );
            new JSONResponse( out ).sendJSON( portParameters );
        }
        catch (Exception e)
        {
            log.log( Level.SEVERE, "Cannot get parameters list", e );
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
     * Highlight variables/parameters on diagram
     * 
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
        else if( varType.equals( "equations" ) || varType.equals( "events" ) || varType.equals( "constraints" ) || varType.equals( "subdiagrams" ) )
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
        HighlightFilter highlightFilter = new HighlightFilter( Color.yellow );
        highlightFilter.setEnabled( true );
        Diagram d = Diagram.getDiagram( diagram );
        if( !DiagramUtility.hasFilter( d, highlightFilter ) )
            DiagramUtility.addFilter( d, highlightFilter );
    }

    private void removeVariables(Compartment diagram, Set<String> elements) throws Exception
    {
        if( diagram.getRole() instanceof EModel )
        {
            EModel model = diagram.getRole( EModel.class );
            List<DiagramElement> toRemove = new ArrayList<>();
            elements.stream().forEach( name -> {
                try
                {
                    Variable var = model.getVariable( name );
                    if( var instanceof VariableRole )
                    {
                        DiagramElement[] nodes = ((VariableRole) var).getAssociatedElements();
                        toRemove.addAll( Arrays.asList( nodes ) );
                    }
                    else
                        model.getVariables().remove( name );
                }
                catch (Exception e)
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
            while ( emodel.containsVariable( name ) )
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
        if( diagram.getRole() instanceof EModel )
        {
            EModel model = diagram.getRole( EModel.class );
            model.getVariables().forEach( variable -> {
                JSONObject variableObj = new JSONObject();
                variableObj.put( "name", variable.getName() );
                variableObj.put( "value", variable.getInitialValue() );
                roleVariables.put( variableObj );
            } );
        }
        new JSONResponse( out ).sendJSON( roleVariables );
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
            response.error( "The table '" + table.getName() + "' must contain the column '" + SetInitialValuesAction.VALUE_COLUMN + "' including new initial values to be set." );
            return;
        }

        if( !table.getColumnModel().getColumn( ind ).getType().equals( DataType.Float ) )
        {
            response.error( "The column '" + SetInitialValuesAction.VALUE_COLUMN + "' in the table '" + table.getName() + "' must be of the type 'Float'." );
            return;
        }

        EModel emodel = diagram.getRole( EModel.class );

        Set<String> errors = new HashSet<>();
        for ( Variable p : emodel.getVariables() )
        {
            if( table.contains( p.getName() ) )
            {
                try
                {
                    double value = (double) table.get( p.getName() ).getValues()[ind];
                    p.setInitialValue( value );
                }
                catch (Exception e)
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

    // TODO: rewrite completely!
    @Override
    protected List<DiagramElement> addDiagramElement(final Diagram diagram, String elementName, final Point location, @Nonnull String typeStr, DataElementPath dcPath,
            JSONArray elementProperties) throws Exception
    {
        final List<DiagramElement> result = new ArrayList<>();
        try
        {
            final DiagramEditorHelper helper = new DiagramEditorHelper( diagram );
            Compartment origin = (Compartment) helper.getOrigin( location );
            final Compartment parent = origin == null ? diagram : origin;
            final SemanticController semanticController = diagram.getType().getSemanticController();
            final ViewEditorPane viewEditor = new ViewEditorPaneStub( helper, diagram );

            final Object bean = semanticController.getPropertiesByType( parent, typeStr, location );

            if( bean instanceof InitialElementProperties )
            {
                if( elementName != null && parent.contains( elementName ) )
                    throw new WebException( "EX_QUERY_ELEMENT_EXIST", elementName, parent.getName() );
                JSONUtils.correctBeanOptions( bean, elementProperties );
                //init stub for view editor pane
                performTransaction( diagram, "Add items (" + typeStr + ")", () -> {
                    try
                    {
                        DiagramElementGroup elements = ((InitialElementProperties) bean).createElements( parent, location, viewEditor );
                        //TODO: remove check after all InitialElementProperties will separately add/create elements
                        if( bean instanceof ReactionInitialProperties )
                            elements.putToCompartment();
                        result.addAll( elements.getElements() );
                    }
                    catch (Exception e)
                    {
                        throw ExceptionRegistry.translateException( e );
                    }
                } );
                return result;
            }

            Class<?> typeClass = null;
            try
            {
                typeClass = ClassLoading.loadClass( typeStr );
            }
            catch (LoggedClassNotFoundException e)
            {
            }
            final ru.biosoft.access.core.DataElement kernel = defineKernel( parent, dcPath, elementName, typeStr );
            DiagramElement diagramElement = createDiagramElement( parent, typeStr, kernel );

            if( !semanticController.canAccept( parent, diagramElement ) )
                throw new Exception( "Can't accept node '" + diagramElement.getName() + "' to compartment '" + parent.getName() + "'" );
            if( semanticController instanceof XmlDiagramSemanticController )
            {
                DynamicPropertySet dps = ((XmlDiagramSemanticController) semanticController).createAttributes( typeStr );
                JSONArray attr = null;
                if( elementProperties == null )
                    throw new Exception( "No properties were supplied" );
                for ( int j = 0; j < elementProperties.length(); j++ )
                {
                    JSONObject jsonObject = elementProperties.getJSONObject( j );
                    String name = jsonObject.getString( "name" );

                    if( name != null && name.equals( "Attributes" ) )
                    {
                        attr = jsonObject.getJSONArray( "value" );
                        break;
                    }
                }
                if( attr != null )
                {
                    JSONUtils.correctBeanOptions( dps, attr );
                    Iterator<String> iter = dps.nameIterator();
                    while ( iter.hasNext() )
                    {
                        diagramElement.getAttributes().add( dps.getProperty( iter.next() ) );
                    }
                }
                diagramElement = ((XmlDiagramSemanticController) semanticController).getPrototype().validate( parent, diagramElement );
            }

            final DiagramElement finalDiagramElement = diagramElement;
            final Class<?> finalTypeClass = typeClass;
            final String finalElementName = kernel.getName();

            performTransaction( diagram, "Add " + getPresentationName( finalDiagramElement ), () -> {
                try
                {
                    //TODO: rewrite code to have unique name of the diagramElement, since helper can change element, and the original one is used below
                    helper.add( finalDiagramElement, location );
                    result.add( finalDiagramElement );

                    if( finalTypeClass != null && Stub.ConnectionPort.class.isAssignableFrom( finalTypeClass ) )
                    {
                        String varName = finalElementName.substring( 0, finalElementName.indexOf( Stub.ConnectionPort.SUFFIX ) );
                        finalDiagramElement.getAttributes().add( new DynamicProperty( PortOrientation.ORIENTATION_ATTR, PortOrientation.class,
                                finalTypeClass == Stub.InputConnectionPort.class ? PortOrientation.LEFT : PortOrientation.RIGHT ) );
                        finalDiagramElement.getAttributes().add( new DynamicProperty( Stub.ConnectionPort.VARIABLE_NAME_ATTR, String.class, varName ) );

                        EModel emodel = diagram.getRole( EModel.class );
                        Object nodeObj = emodel.getVariable( varName ).getParent();
                        if( nodeObj instanceof Node )
                            ((ConnectionPort) kernel).setTitle( ((Node) nodeObj).getTitle() );
                        else
                            ((ConnectionPort) kernel).setTitle( varName );
                        if( nodeObj instanceof Node )
                        {
                            String name = DefaultSemanticController.generateUniqueNodeName( parent, varName + "_connection" );
                            Stub.DirectedConnection sd = new Stub.DirectedConnection( diagram, name );
                            Edge edge = new Edge( diagram, sd, (Node) nodeObj, (Node) finalDiagramElement );
                            helper.add( edge, new Point( 0, 0 ) );
                        }
                    }
                }
                catch (Exception e)
                {
                    throw ExceptionRegistry.translateException( e );
                }
            } );
        }
        catch (Exception e)
        {
            throw new Exception( "Can't add element on diagram " + diagram.getName() + ": " + e.getMessage(), e );
        }
        return result;
    }

    @Override
    protected DiagramElement createDiagramElement(final Compartment parent, @Nonnull String typeStr, DataElement kernel) throws Exception
    {
        Diagram diagram = Diagram.getDiagram( parent );
        String elementName = kernel.getName();

        final SemanticController semanticController = diagram.getType().getSemanticController();

        DiagramElement diagramElement = null;
        if( kernel instanceof Diagram && DiagramUtility.isComposite( diagram ) )
        {
            if( ((Diagram) kernel).getRole() == null )
            {
                throw new Exception( "Can't add diagram with empty model. Select another diagram." );
            }
            else if( diagram == kernel )
            {
                throw new Exception( "Can't add element to itself. Select another element." );
            }
            else
            {
                diagramElement = new SubDiagram( diagram, ((Diagram) kernel).clone( (DataCollection<?>) parent, elementName ), elementName );
            }
        }
        else if( diagram.getType().isCompartment( (Base) kernel ) )
        {
            diagramElement = new Compartment( parent, elementName, (Base) kernel );
        }
        else
        {
            diagramElement = new Node( parent, elementName, (Base) kernel );
        }

        Class<?> typeClass = null;
        try
        {
            typeClass = ClassLoading.loadClass( typeStr );
        }
        catch (LoggedClassNotFoundException e)
        {
        }
        //set roles
        if( typeClass != null )
        {
            if( typeClass == Event.class )
            {
                Event event = new Event( diagramElement );
                diagramElement.setRole( event );
            }
            else if( typeClass == Equation.class )
            {
                diagramElement.setRole( new Equation( diagramElement, Equation.TYPE_SCALAR, "unknown", "0" ) );
            }
            else if( typeClass == Function.class )
            {
                diagramElement.setRole( new Function( diagramElement ) );
            }
            else if( typeClass == Constraint.class )
            {
                diagramElement.setRole( new Constraint( diagramElement ) );
            }
            else if( typeClass == State.class )
            {
                State event = new State( diagramElement );
                event.addOnEntryAssignment( new Assignment( "unknown", "0" ), false );
                event.addOnExitAssignment( new Assignment( "unknown", "0" ), false );
                diagramElement.setRole( event );
            }
            else if( semanticController instanceof PathwaySimulationSemanticController && typeClass != Stub.Note.class )
            {
                VariableRole var = new VariableRole( diagramElement, 0 );
                diagramElement.setRole( var );
            }
        }

        return diagramElement;
    }

    @Override
    protected DataElement defineKernel(final Compartment parent, DataElementPath dcPath, String elementName, @Nonnull String typeStr) throws Exception
    {
        Class<?> typeClass = null;
        try
        {
            typeClass = ClassLoading.loadClass( typeStr );
        }
        catch (LoggedClassNotFoundException e)
        {
            throw new Exception( "Can not load class " + typeStr + ", error " + e.getMessage() );
        }
        if( typeClass != null && typeClass != Stub.NoteLink.class && typeClass != SemanticRelation.class && typeClass != Reaction.class )
        {
            if( dcPath != null && !dcPath.isEmpty() )
            {
                DataElement kernel = dcPath.getChildPath( elementName ).getDataElement();
                if( !(kernel instanceof Base) && !(kernel instanceof Diagram) )
                {
                    throw new Exception( "Cannot add element '" + elementName + "' of type '" + typeStr + "' to diagram." );
                }
                return kernel;
            }
            if( typeClass == Stub.Note.class || typeClass == Stub.PlotElement.class || typeClass == Stub.Bus.class )
            {
                return (DataElement) typeClass.getConstructor( DataCollection.class, String.class ).newInstance( null, elementName );
            }
            if( typeClass == Event.class )
            {
                return new Stub( null, elementName, Type.MATH_EVENT );
            }
            if( typeClass == Equation.class )
            {
                return new Stub( null, elementName, Type.MATH_EQUATION );
            }
            if( typeClass == Function.class )
            {
                return new Stub( null, elementName, Type.MATH_FUNCTION );
            }
            if( typeClass == Constraint.class )
            {
                return new Stub( null, elementName, Type.MATH_CONSTRAINT );
            }
            if( Stub.ConnectionPort.class.isAssignableFrom( typeClass ) )
            {
                String newElementName = DefaultSemanticController.generateUniqueNodeName( parent, elementName + Stub.ConnectionPort.SUFFIX );
                if( typeClass == Stub.OutputConnectionPort.class )
                {
                    return new Stub.OutputConnectionPort( null, newElementName );
                }
                else if( typeClass == Stub.InputConnectionPort.class )
                {
                    return new Stub.InputConnectionPort( null, newElementName );
                }
                else if( typeClass == Stub.ContactConnectionPort.class )
                {
                    return new Stub.ContactConnectionPort( null, newElementName );
                }
            }
            if( typeClass == State.class )
            {
                return new Stub( null, elementName, Type.MATH_STATE );
            }
        }
        else //default
        {
            return new Stub( null, elementName, typeStr );
        }
        throw new Exception( "Cannot create element of type " + typeStr );
    }

}
