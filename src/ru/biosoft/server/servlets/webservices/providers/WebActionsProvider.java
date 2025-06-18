package ru.biosoft.server.servlets.webservices.providers;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentFactory.Policy;
import com.developmentontheedge.beans.model.ComponentModel;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import biouml.model.Diagram;
import biouml.model.DiagramType;
import biouml.model.Module;
import biouml.model.dynamics.EModelRoleSupport;
import biouml.model.xml.XmlDiagramType;
import biouml.plugins.server.access.AccessProtocol;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.type.Reaction;
import biouml.standard.type.Stub;
import biouml.workbench.diagram.DiagramDynamicActionProperties;
import biouml.workbench.diagram.DiagramEditorHelper;
import biouml.workbench.perspective.Perspective;
import biouml.workbench.perspective.PerspectiveUI;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.subaction.BackgroundDynamicAction;
import ru.biosoft.access.subaction.DynamicAction;
import ru.biosoft.access.subaction.DynamicActionFactory;
import ru.biosoft.access.task.JobControlTask;
import ru.biosoft.access.task.TaskPool;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.LoggedClassCastException;
import ru.biosoft.exception.LoggedClassNotFoundException;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListenerAdapter;
import ru.biosoft.server.JSONUtils;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.BiosoftWebResponse;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.WebException;
import ru.biosoft.server.servlets.webservices.WebJob;
import ru.biosoft.server.servlets.webservices.WebServicesServlet;
import ru.biosoft.server.servlets.webservices.WebSession;
import ru.biosoft.util.Cache;
import ru.biosoft.util.JsonUtils;
import ru.biosoft.util.LazyValue;
import ru.biosoft.util.TextUtil2;

/**
 * Provides loading actions from plug-ins
 */
public class WebActionsProvider extends WebProviderSupport
{
    private static final String XML_TYPE_PREFIX = "xml:";

    protected static final Logger log = Logger.getLogger(WebActionsProvider.class.getName());

    public static final String SELECTION_BASE = "selectionBase";
    protected static final String ACTIONS_PATH = "files/actions/";

    public static class ActionDescriptor
    {
        JsonObject actionInfo;
        String product;

        public ActionDescriptor()
        {
        }

        public ActionDescriptor(JsonObject actionInfo, String product)
        {
            this.actionInfo = actionInfo;
            this.product = product;
        }

        public boolean isSeparator()
        {
            return actionInfo == null;
        }

        public boolean isAvailable()
        {
            return product == null || product.isEmpty() || SecurityManager.isProductAvailable(product);
        }

        public JsonObject getActionInfo()
        {
            return actionInfo == null ? new JsonObject() : actionInfo;
        }
    }

    private static final Function<String, ActionDescriptor[]> actionsTypeMap =
            Cache.hard( type -> {
                String namesFile = ACTIONS_PATH + type + "/names.txt";

                try(BufferedReader br = new BufferedReader(new InputStreamReader(WebSession.class.getResourceAsStream(namesFile))))
                {
                    return br.lines().map(String::trim).map(name ->
                    {
                        if( name.isEmpty() )
                        {
                            //separator
                            return new ActionDescriptor();
                        }
                        else
                        {
                            String[] fields = TextUtil2.split(name, ':');
                            String fileName = ACTIONS_PATH + type + "/" + fields[0];
                            try
                            {
                                String data = ApplicationUtils.readAsString(WebSession.class.getResourceAsStream(fileName));
                                data = data.replaceAll("//.*?[\r\n]","\n");
                                String filtered = data.replace( '\n', ' ' ).replace( '\r', ' ' ).replaceAll("/\\*.*?\\*/", "");
                                JsonObject object = JsonUtils.toMinimalJson( new JSONObject( filtered ) );
                                return new ActionDescriptor(object, fields.length>1?fields[1]:null);
                            }
                            catch( Exception e )
                            {
                                log.log(Level.SEVERE, "Can not create JSON object from file " + fileName + ":" + ExceptionRegistry.log(e));
                                return null;
                            }
                        }
                    }).filter(Objects::nonNull).toArray(ActionDescriptor[]::new);
                }
                catch( IOException | UncheckedIOException e )
                {
                    log.log(Level.SEVERE, "Can not read " + namesFile + ":" + ExceptionRegistry.log(e));
                    return new ActionDescriptor[0];
                }
            });
    private static final LazyValue<JsonObject[]> dynamicActions = new LazyValue<>("Dynamic actions", () ->
    DynamicActionFactory.dynamicActions()
            .map( action ->
                new JsonObject()
                    .add("id", action.getTitle())
                    .add("label", getLabel( action ))
                    .add("icon", "../biouml/web/action?type=dynamic&action=toolbar_icon&name=" + action.getTitle().replaceAll(" ", "%20"))
                    .add("numSelected", action.getNumSelected())
                    .add("acceptReadOnly", action.isAcceptReadOnly())
            ).toArray( JsonObject[]::new )
    );

    private static final Map<String, JsonObject[]> diagramTypesMap = new ConcurrentHashMap<>();

    public static ActionDescriptor[] loadActions(String key)
    {
        return actionsTypeMap.apply( key );
    }

    /**
     * Load diagram elements actions
     */
    public static JsonObject[] loadDiagramActions(Diagram diagram)
    {
        String key = null;
        if( diagram.getType() instanceof XmlDiagramType )
        {
            key = ( (XmlDiagramType)diagram.getType() ).getName();
        }
        else
        {
            key = diagram.getType().getClass().getName();
        }
        DataElementPath modulePath = Module.optModulePath(diagram);
        if( modulePath != null )
            key += modulePath;

        return diagramTypesMap.computeIfAbsent( key, k ->
            EntryStream.of( diagram.getType().getNodeTypes(), true,
                            diagram.getType().getEdgeTypes(), false )
                .nonNullKeys().flatMapKeys( Arrays::stream )
                .filterKeys( type -> diagram.getType().getDiagramViewBuilder().getIcon(type) != null )
                .prepend( null, true )
                .mapKeyValue( (type, isEdge) -> getActionForDiagramElementType(diagram, type, isEdge))
                .toArray( JsonObject[]::new )
        );
    }
    /**
     * Create JSON action for diagram element
     */
    protected static JsonObject getActionForDiagramElementType(Diagram diagram, Object type, boolean isNode)
    {
        String name;
        String icon;
        String callbackFunction;
        boolean instantAction = false;
        String compositeParameter = ( DiagramUtility.isComposite(diagram) ) ? ", 'composite'" : "";
        if( type == null )
        {
            icon = name = "Select";
            callbackFunction = "function(event){this.setSelectMode();return true;}";
            instantAction = true;
        }
        else if( type instanceof Class )
        {
            Class typeClass = (Class)type;
            String typeName = typeClass.getName();
            if( diagram.getType() instanceof XmlDiagramType )
            {
                icon = name = ( (XmlDiagramType)diagram.getType() ).getKernelTypeName(type);
                typeName = name;
            }
            else
            {
                icon = typeClass.getSimpleName();
                try
                {
                    BeanInfo bi = Introspector.getBeanInfo(typeClass);
                    BeanDescriptor bd = bi.getBeanDescriptor();
                    name = bd.getDisplayName();
                }
                catch( IntrospectionException e )
                {
                    name = typeClass.getName();
                }
            }

            if( typeClass == Reaction.class )
            {
                callbackFunction = "function(event){this.createNewReaction(event);return false;}";
            }
            else
            {
                String dcCompleteName;
                if( isModuleRequired( typeClass ) && ! ( diagram.getType() instanceof XmlDiagramType )
                        && diagram.getType().getSemanticController().getPropertiesByType( diagram, typeClass, new Point( 0, 0 ) ) == null )
                    dcCompleteName = "not available";
                else
                    dcCompleteName = "";
                try
                {
                    DataCollection category = Module.getModule(diagram).getCategory(typeClass);
                    if( category != null )
                        dcCompleteName = category.getCompletePath().toString();
                }
                catch( Exception e )
                {
                }
                if( isNode )
                {
                    callbackFunction = "function(event){this.createNewNode(event, '" + StringEscapeUtils.escapeJavaScript(dcCompleteName)
                            + "', '" + typeName + "');return false;}";
                }
                else
                {
                    callbackFunction = "function(event){this.createNewEdge(event, '" + StringEscapeUtils.escapeJavaScript(dcCompleteName)
                            + "', '" + typeName + "'" + compositeParameter + ");return false;}";
                    instantAction = true;
                }
            }
        }
        else
        {
            icon = name = type.toString();
            if( isNode )
            {
                callbackFunction = "function(event){this.createNewNode(event, '', '" + type.toString() + "');return false;}";
            }
            else
            {
                callbackFunction = "function(event){this.createNewEdge(event, '', '" + type.toString() + "'" + compositeParameter
                        + ");return false;}";
                instantAction = true;
            }
        }
        JsonObject json = new JsonObject();
        String id = name.toLowerCase().replaceAll(" ", "_");
        json.add( "id", id );
        json.add( "label", name.toLowerCase() );
        json.add( "icon", icon.equals("Select") ? "select.gif" : "/web/action?action=toolbar_icon&type=diagram&name=" + TextUtil2.encodeURL( icon ) + "&diagramType="
                + TextUtil2.encodeURL( typeToString( diagram.getType() ) ) );
        json.add( "visible", "function(node, treeObj){return true;}" );
        json.add(
                "action",
                "function(node, treeObj){var activeDocument = opennedDocuments[activeDocumentId];if (activeDocument instanceof Diagram || activeDocument instanceof CompositeDiagram){"
                        + ( instantAction ? "(" + callbackFunction + ").apply(activeDocument);" : "activeDocument.selectControl("
                                + callbackFunction + compositeParameter + ");" ) + "}}" );
        return json;
    }

    private static boolean isModuleRequired(Class<?> type)
    {
        if( Stub.class.isAssignableFrom(type) || EModelRoleSupport.class.isAssignableFrom(type) )
            return false;
        return true;
    }

    public static void getToolbarIcon(String actionName, BiosoftWebResponse resp)
    {
        try
        {
            DynamicAction action = DynamicActionFactory.getDynamicAction(actionName);
            Object iconObj = action.getValue(Action.SMALL_ICON);
            if( iconObj instanceof ImageIcon )
            {
                sendIcon( resp, (ImageIcon)iconObj );
            }
        }
        catch( IOException e )
        {
            log.log(Level.SEVERE, "Can not send icon image", e);
        }
    }

    private static void sendIcon(BiosoftWebResponse resp, ImageIcon icon) throws IOException
    {
        int width = icon.getIconWidth();
        if( width < 0 )
            width = 0;
        int height = icon.getIconHeight();
        if( height < 0 )
            height = 0;
        if( width > 0 && height > 0 )
        {
            BufferedImage b = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            icon.paintIcon(null, b.createGraphics(), 0, 0);
            resp.setContentType("image/png");
            ImageIO.write(b, "PNG", resp.getOutputStream());
        }
    }

    public static boolean isDynamicActionVisible(String actionName, Object model)
    {
        DynamicAction action = DynamicActionFactory.getDynamicAction(actionName);
        Perspective perspective = PerspectiveUI.getCurrentPerspective();
        return perspective.isActionAvailable( action.getTitle() ) ? action.isApplicable( model ) : false;
    }

    public static JsonArray getVisibleActions(Object model)
    {
        JsonArray result = new JsonArray();
        Perspective perspective = PerspectiveUI.getCurrentPerspective();
        DynamicActionFactory.dynamicActions().filter( action -> perspective.isActionAvailable( action.getTitle() ) )
                .filter( action -> action.isApplicable( model ) )
            .forEach( action -> {
                if( !result.isEmpty()
                        && !DynamicActionFactory.getDynamicAction( result.get( result.size() - 1 ).asString() ).isSameGroup( action ) )
                    result.add( "" );
                result.add(action.getTitle());
            });
        return result;
    }

    public static void validateDynamicAction(String actionName, DataElement actionModel, List<DataElement> selectedItems, JSONArray properties, JSONResponse response) throws IOException, WebException
    {
        DynamicAction action = DynamicActionFactory.getDynamicAction(actionName);
        if( action != null )
        {
            action.validateParameters(actionModel, selectedItems);
            Object pd = getActionProperties(actionName, action, actionModel, selectedItems, properties==null);
            if( pd != null && !pd.getClass().equals(DiagramDynamicActionProperties.class))
            {
                try
                {
                    ComponentModel model = ComponentFactory.getModel(pd, Policy.DEFAULT, true);
                    if( properties != null )
                    {
                        JSONUtils.correctBeanOptions(model, properties);
                    }
                    JSONArray jsonProperties = JSONUtils.getModelAsJSON(model);
                    response.sendJSONBean(jsonProperties);
                }
                catch( Exception e )
                {
                    throw new WebException(e, "EX_INTERNAL_DURING_ACTION", actionName);
                }
            }
            else
            {
                String confirmation = action.getConfirmationMessage(actionModel, selectedItems);
                if( confirmation != null )
                {
                    response.sendJSON(new JsonObject().add("confirm", confirmation));
                }
                else
                {
                    response.send(new byte[0], 0);
                }
            }
        }
    }

    private static Object getActionProperties(String actionName, DynamicAction action, DataElement actionModel,
            List<DataElement> selectedItems, boolean isNew)
    {
        String completeName = "dynamicAction/"+actionName+"/"+DataElementPath.create(actionModel);
        Object pd = isNew ? null : WebServicesServlet.getSessionCache().getObject(completeName);
        if(pd == null)
        {
            pd = action.getProperties(actionModel, selectedItems);
            if(pd != null)
            {
                WebServicesServlet.getSessionCache().addObject(completeName, pd, true);
            }
        }
        return pd;
    }

    public static String runDynamicAction(final String actionName, final ru.biosoft.access.core.DataElement actionModel, List<DataElement> selectedItems, JSONArray properties, String jobID) throws WebException
    {
        DynamicAction action = DynamicActionFactory.getDynamicAction(actionName);
        if( action == null )
            throw new WebException("EX_QUERY_NO_ACTION", actionName);
        try
        {
            action.validateParameters(actionModel, selectedItems);
            Object pd = action.getProperties(actionModel, selectedItems);
            if( properties != null && pd != null )
            {
                ComponentModel model = ComponentFactory.getModel(pd, Policy.DEFAULT, true);
                JSONUtils.correctBeanOptions(model, properties);
            }
            if( action instanceof BackgroundDynamicAction && jobID != null )
            {
                if(actionModel instanceof Diagram && pd instanceof DiagramDynamicActionProperties)
                {
                    Diagram diagram = (Diagram)actionModel;
                    DiagramEditorHelper helper = new DiagramEditorHelper(diagram);
                    ((DiagramDynamicActionProperties)pd).setHelper(helper);
                }
                final JobControl jc = ( (BackgroundDynamicAction)action ).getJobControl(actionModel, selectedItems, pd);
                final WebJob webJob = WebJob.getWebJob(jobID);
                webJob.setJobControl(jc);
                jc.addListener(new JobControlListenerAdapter()
                {
                    @Override
                    public void jobTerminated(JobControlEvent event)
                    {
                        if( event.getStatus() == JobControl.TERMINATED_BY_ERROR && event.getException() != null )
                        {
                            webJob.addJobMessage("ERROR - " + ExceptionRegistry.log(event.getException().getError()) + "\n");
                        }
                    }
                });
                TaskPool.getInstance().submit(new JobControlTask("Action: "+actionName+" (user: "+SecurityManager.getSessionUser()+")", jc)
                {
                    @Override
                    public void doRun()
                    {
                        try
                        {
                            if(actionModel instanceof Diagram)
                            {
                                WebDiagramsProvider.performTransaction((Diagram)actionModel, actionName, jc);
                            } else
                                jc.run();
                        }
                        catch( Exception e )
                        {
                            log.log(Level.SEVERE, e.getMessage(), e);
                        }
                    }
                });
                return "job started";
            }
            action.performAction(actionModel, selectedItems, pd);
            return "action finished";
        }
        catch( LoggedException e )
        {
            throw e;
        }
        catch( Exception e )
        {
            throw new WebException(e, "EX_INTERNAL_DURING_ACTION", actionName);
        }
    }

    private JsonObject[] getActions(String type)
    {
        return StreamEx.of( actionsTypeMap.apply( type ) )
                .filter( ActionDescriptor::isAvailable )
                .map( ActionDescriptor::getActionInfo )
                .toArray( JsonObject[]::new );
    }


    private static JSONArray getActionProperties(BiosoftWebRequest arguments) throws WebException
    {
        return arguments.optJSONArray("properties");
    }

    private static List<DataElement> getSelectedItems(BiosoftWebRequest arguments) throws WebException
    {
        DataCollection<?> selectionBase = arguments.getDataCollection( TextUtil2.isEmpty( arguments.get( SELECTION_BASE ) )
                ? AccessProtocol.KEY_DE : SELECTION_BASE );
        String[] rows = arguments.optStrings("jsonrows");
        if( rows == null )
            return Collections.emptyList();
        if( rows.length == 1 && rows[0].equals("all") )
            return selectionBase.stream().filter( DataElement.class::isInstance ).map( e -> (DataElement)e ).collect( Collectors.toList() );
        return StreamEx.of(rows).map( name -> {
            DataElement de = CollectionFactory.getDataElement(name, selectionBase);
            if(de == null) de = new Stub(selectionBase, name);
            return de;
        }).toList();
    }

    private static String getLabel(DynamicAction action)
    {
        Object label = action.getValue(Action.NAME);
        if( label == null )
            label = action.getValue(Action.SHORT_DESCRIPTION);
        if( label == null )
            label = action.getTitle();
        return label.toString();
    }

    private static String typeToString(DiagramType type)
    {
        if(type instanceof XmlDiagramType)
        {
            return XML_TYPE_PREFIX+((XmlDiagramType)type).getCompletePath();
        }
        return type.getClass().getName();
    }

    private static DiagramType stringToType(String type)
    {
        if(type.startsWith( XML_TYPE_PREFIX ))
        {
            return DataElementPath.create( type.substring( XML_TYPE_PREFIX.length() ) ).getDataElement( XmlDiagramType.class );
        }
        try
        {
            return ClassLoading.loadSubClass( type, DiagramType.class ).newInstance();
        }
        catch( LoggedClassNotFoundException | LoggedClassCastException | InstantiationException | IllegalAccessException e )
        {
            throw ExceptionRegistry.translateException( e );
        }
    }

    @Override
    public void process(BiosoftWebRequest arguments, BiosoftWebResponse resp) throws Exception
    {
        String type = arguments.getString("type");
        JsonObject[] actions = null;
        JSONResponse response = new JSONResponse(resp);
        if( type.equals("diagram") )
        {
            String action = arguments.optAction();
            if("toolbar_icon".equals( action ))
            {
                String name = arguments.getString( "name" ); // type of node/edge
                DiagramType diagramType = stringToType(arguments.getString( "diagramType" ));
                Icon icon = diagramType.getDiagramViewBuilder().getIcon( name );
                sendIcon( resp, (ImageIcon)icon );
                return;
            }
            actions = loadDiagramActions(WebDiagramsProvider.getDiagramChecked(arguments.getDataElementPath()));
        }
        else if( type.equals("dynamic") )
        {
            String action = arguments.getAction();
            if( action.equals("load") )
            {
                actions = dynamicActions.get();
                response.sendActions(actions);
                return;
            }
            else if( action.equals("toolbar_icon") )
            {
                String path = arguments.getString("name");
                getToolbarIcon(path, resp);
                return;
            }

            DataElement dc = arguments.getDataElement();
            if( action.equals("visibleall") )
            {
                response.sendJSON(getVisibleActions(dc));
                return;
            }

            String actionName = arguments.getString("name");
            if( action.equals("visible") )
            {
                if( isDynamicActionVisible(actionName, dc) )
                    response.send(new byte[0], 0);
                else
                    response.error("Action is not visible");
                return;
            }
            if( action.equals("validate") )
            {
                validateDynamicAction(actionName, dc, getSelectedItems(arguments), getActionProperties(arguments), response);
            }
            else if( action.equals("run") )
            {
                String result = runDynamicAction(actionName, dc, getSelectedItems(arguments), getActionProperties(arguments), arguments.get("jobID"));
                response.sendString(result);
            }
            else
                throw new WebException("EX_QUERY_PARAM_INVALID_VALUE", BiosoftWebRequest.ACTION);
        }
        else
        {
            actions = getActions(type);
        }

        if( actions != null )
        {
            response.sendActions(actions);
        }
        return;
    }
}
