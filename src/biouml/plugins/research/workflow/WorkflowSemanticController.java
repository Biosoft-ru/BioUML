package biouml.plugins.research.workflow;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.model.Property;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.DiagramElementStyle;
import biouml.model.DiagramViewBuilder;
import biouml.model.DiagramViewOptions;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.research.BaseResearchSemanticController;
import biouml.plugins.research.workflow.engine.WorkflowEngine;
import biouml.plugins.research.workflow.items.DataElementType;
import biouml.plugins.research.workflow.items.VariableType;
import biouml.plugins.research.workflow.items.WorkflowExpression;
import biouml.plugins.research.workflow.items.WorkflowItem;
import biouml.plugins.research.workflow.items.WorkflowItemFactory;
import biouml.plugins.research.workflow.items.WorkflowParameter;
import biouml.plugins.research.workflow.items.WorkflowVariable;
import biouml.standard.diagram.CreateEdgeAction;
import biouml.standard.type.Base;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.DataElementPutException;
import ru.biosoft.access.exception.ParameterNotAcceptableException;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysiscore.AnalysisDPSUtils;
import ru.biosoft.analysiscore.AnalysisMethod;
import ru.biosoft.analysiscore.AnalysisMethodRegistry;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.analysiscore.ImportAnalysisParameters;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.tasks.TaskInfo;
import ru.biosoft.util.DPSUtils;
import ru.biosoft.util.FileItem;
import ru.biosoft.util.OkCancelDialog;
import ru.biosoft.util.PropertiesDialog;
import ru.biosoft.util.TextUtil2;

/**
 * Semantic controller for workflow diagrams
 */
public class WorkflowSemanticController extends BaseResearchSemanticController
{
    public static final String EDGE_VARIABLE = "variable";
    public static final String EDGE_ANALYSIS_PROPERTY = "analysis-property";
    public static final String EDGE_ANALYSIS_INPUT_PROPERTY = "analysis-input-property";
    public static final String EDGE_ANALYSIS_OUTPUT_PROPERTY = "analysis-output-property";

    @Override
    public DiagramElementGroup createInstance(@Nonnull Compartment parent, Object type, Point point, ViewEditorPane viewEditor)
    {
        DiagramElementGroup result = super.createInstance( parent, type, point, viewEditor );
        if( result != null && !result.equals( DiagramElementGroup.EMPTY_EG ))
            return result;

        final String typeString = (String)type;

        WorkflowItem item = WorkflowItemFactory.getWorkflowItem(parent, typeString);
        if( item != null )
        {
            item.setName(generateUniqueNodeName(parent, typeString));
            PropertiesDialog dialog = new PropertiesDialog(Application.getApplicationFrame(), "New " + typeString, item);
            if( dialog.doModal() )
                return new DiagramElementGroup( item.getNode() );
            return DiagramElementGroup.EMPTY_EG;
        }
        if( Type.ANALYSIS_SCRIPT.equals(typeString) )
        {
            try
            {
                Node node = createScriptNode(parent, "", "js");
                PropertiesDialog dialog = new PropertiesDialog(Application.getApplicationFrame(), "New script", WorkflowEngine
                        .getScriptParameters(node));
                if( dialog.doModal() )
                    return new DiagramElementGroup( node );
            }
            catch( Exception e )
            {
            }
            return DiagramElementGroup.EMPTY_EG;
        }
        if( Type.ANALYSIS_CYCLE.equals(typeString) )
        {
            item = WorkflowItemFactory.getWorkflowItem(parent, Type.ANALYSIS_CYCLE_VARIABLE);
            item.setName(generateUniqueNodeName(parent, Type.ANALYSIS_CYCLE_VARIABLE));
            PropertiesDialog dialog = new PropertiesDialog(Application.getApplicationFrame(), "New " + typeString, item);
            if( dialog.doModal() )
                return new DiagramElementGroup( createCycleNode( parent, item ) );
            return DiagramElementGroup.EMPTY_EG;
        }

        if( Base.TYPE_DIRECTED_LINK.equals(typeString) )
        {
            final String name = generateUniqueNodeName(parent, typeString);
            new CreateEdgeAction().createEdge(point, viewEditor, (in, out, temporary) -> {
                Edge edge = new Edge(new Stub(null, name, typeString), in, out);
                if(!temporary)
                {
                    WorkflowSemanticController.this.annotateEdge(edge);
                }
                return edge;
            });
            return DiagramElementGroup.EMPTY_EG;
        }

        return DiagramElementGroup.EMPTY_EG;
    }
    @Override
    public boolean canAccept(Compartment compartment, DiagramElement de)
    {
        if( super.canAccept(compartment, de) )
            return true;
        String type = de.getKernel().getType();
        if( compartment instanceof Diagram )
        {
            if( de instanceof Node && WorkflowItemFactory.getWorkflowItem((Node)de) != null )
            {
                return true;
            }
        }
        if( compartment.getKernel().getType().equals(Type.ANALYSIS_CYCLE) )
        {
            if( de instanceof Node && WorkflowItemFactory.getWorkflowItem((Node)de) instanceof WorkflowExpression )
            {
                return true;
            }
        }
        else if( compartment.getKernel().getType().equals(Type.ANALYSIS_METHOD) )
        {
            if( Type.TYPE_DATA_ELEMENT_IN.equals(type) || Type.TYPE_DATA_ELEMENT_OUT.equals(type) )
            {
                if( compartment.equals( de.getParent() ) )
                    return true;
            }
        }

        if( ( de instanceof Edge ) && Base.TYPE_DIRECTED_LINK.equals(type) )
        {
            if( ( (Edge)de ).getInput() == ( (Edge)de ).getOutput() )
                return false;
            WorkflowItem inputItem = WorkflowItemFactory.getWorkflowItem( ( (Edge)de ).getInput());
            WorkflowItem outputItem = WorkflowItemFactory.getWorkflowItem( ( (Edge)de ).getOutput());
            String inType = ( (Edge)de ).getInput().getKernel().getType();
            String outType = ( (Edge)de ).getOutput().getKernel().getType();
            if( inputItem instanceof WorkflowVariable && outputItem instanceof WorkflowVariable )
                return true;
            if( ( inputItem instanceof WorkflowVariable && Type.TYPE_DATA_ELEMENT_IN.equals(outType) )
                    || ( outputItem instanceof WorkflowVariable && Type.TYPE_DATA_ELEMENT_OUT.equals(inType) ) )
                return true;
            if( ( inputItem instanceof WorkflowVariable && Type.ANALYSIS_SCRIPT.equals(outType) )
                    || ( outputItem instanceof WorkflowVariable && Type.ANALYSIS_SCRIPT.equals(inType) ) )
                return true;
            if( ( Type.TYPE_DATA_ELEMENT_OUT.equals(inType) || Type.TYPE_DATA_ELEMENT.equals(inType) || Type.ANALYSIS_SCRIPT.equals(inType) || Type.ANALYSIS_QUERY
                    .equals(inType) )
                    && ( Type.TYPE_DATA_ELEMENT_IN.equals(outType) || Type.TYPE_DATA_ELEMENT.equals(outType)
                            || Type.ANALYSIS_SCRIPT.equals(outType) || Type.ANALYSIS_QUERY.equals(outType) ) )
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Create node for analysis element
     */
    @Override
    public Compartment createAnalysisNode(Compartment parent, String analysisName, DynamicPropertySet attributes) throws ParameterNotAcceptableException
    {
        AnalysisMethod analysisMethod = AnalysisMethodRegistry.getAnalysisMethod(analysisName);
        if(analysisMethod == null)
        {
            throw new ParameterNotAcceptableException( "analysisName", analysisName );
        }
        Stub kernel = new Stub(null, analysisMethod.getName(), Type.ANALYSIS_METHOD);
        AnalysisParameters parameters = AnalysisDPSUtils.readParametersFromAttributes(attributes);
        String name = analysisMethod.getName().replaceFirst("\\s*\\(.*\\..*\\)$", "");
        Compartment anNode = new Compartment(parent, name, kernel);
        anNode.setTitle(AnalysisMethodRegistry.getMethodInfo(analysisName).getDisplayName());
        anNode.setNotificationEnabled(false);
        anNode.setShapeSize(new Dimension(200, Math.max(50, 30 * Math.max(parameters.getInputNames().length,
                parameters.getOutputNames().length))));
        anNode.getAttributes().add(new DynamicProperty("innerNodesPortFinder", Boolean.class, true));
        try
        {
            AnalysisDPSUtils.writeParametersToNodeAttributes(analysisName, parameters, anNode.getAttributes());
            ComponentModel paramsModel = ComponentFactory.getModel( parameters );
            String[] inputNames = parameters.getInputNames();
            for( int i = 0; i < inputNames.length; i++ )
                addInOutNode(inputNames[i] , Type.TYPE_DATA_ELEMENT_IN, i, anNode, paramsModel );
            String[] outputNames = parameters.getOutputNames();
            for( int i = 0; i < outputNames.length; i++ )
                addInOutNode(outputNames[i] , Type.TYPE_DATA_ELEMENT_OUT, i, anNode, paramsModel );
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not add analsis parameters", e);
        }
        anNode.setNotificationEnabled(true);
        return anNode;
    }

    private void addInOutNode(String name, String nodeType, int position, Compartment parent, ComponentModel paramsModel ) throws DataElementPutException
    {
        Node inNode = new Node( parent, new Stub( parent, name.replace( '/', ':' ), nodeType ) );
        inNode.getAttributes().add( new DynamicProperty( "position", Integer.class, position ) );
        inNode.setFixed( true );

        Property property = paramsModel;
        StringBuilder description = new StringBuilder();
        for( String propertyPart : TextUtil2.split( name, '/' ) )
        {
            property = property.findProperty( propertyPart );
            if( description.length() > 0 )
                description.append( ": " );
            description.append( property.getDisplayName() );
        }
        inNode.getAttributes().add( new DynamicProperty("description", String.class, description.toString()) );

        String iconId = DataElementPathEditor.getIconId( property );
        inNode.getAttributes().add( new DynamicProperty( "iconId", String.class, iconId ) );

        parent.put( inNode );
    }

    /**
     * Update node for analysis element adding/removing input/output ports if necessary
     */
    public Compartment updateAnalysisNode(Compartment anNode)
    {
        try
        {
            AnalysisParameters parameters = WorkflowEngine.getAnalysisParametersByNode(anNode, true);
            String[] inputNames = parameters.getInputNames();
            Set<String> usedNames = new HashSet<>();
            for( int i=0; i<inputNames.length; i++ )
            {
                String inputName = inputNames[i];
                String name = inputName.replace('/', ':');
                usedNames.add(name);
                Node inNode = (Node)anNode.get(name);
                if(inNode == null)
                {
                    inNode = new Node(anNode, new Stub(anNode, name, Type.TYPE_DATA_ELEMENT_IN));
                    inNode.setFixed(true);
                    anNode.put(inNode);
                }
                inNode.getAttributes().add(new DynamicProperty("position", Integer.class, i));
            }
            String[] outputNames = parameters.getOutputNames();
            for( int i=0; i<outputNames.length; i++ )
            {
                String outputName = outputNames[i];
                String name = outputName.replace('/', ':');
                usedNames.add(name);
                Node outNode = (Node)anNode.get(name);
                if(outNode == null)
                {
                    outNode = new Node(anNode, new Stub(anNode, name, Type.TYPE_DATA_ELEMENT_OUT));
                    outNode.setFixed(true);
                    anNode.put(outNode);
                }
                outNode.getAttributes().add(new DynamicProperty("position", Integer.class, i));
            }
            for( String name : anNode.names().collect( Collectors.toList() ) )
            {
                if(!usedNames.contains(name))
                {
                    try
                    {
                        remove(anNode.get(name));
                    }
                    catch( Exception e )
                    {
                    }
                }
            }
            anNode.setShapeSize(new Dimension(200, Math.max(50, 30 * Math.max(inputNames.length,
                    outputNames.length))));
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not update analysis parameters for "+anNode.getCompletePath(), e);
        }
        return anNode;
    }

    /**
     * @param parent
     * @param item
     * @return
     */
    public Compartment createCycleNode(Compartment parent, WorkflowItem item)
    {
        Compartment cycle = createCycleNode( parent );
        Node var = item.getNode().clone(cycle, item.getNode().getName());
        try
        {
            cycle.put(var);
        }
        catch( Exception e )
        {
            return null;
        }
        return cycle;
    }
    public Compartment createCycleNode(Compartment parent)
    {
        String name = generateUniqueNodeName(parent, "Cycle");
        Stub kernel = new Stub(null, name, Type.ANALYSIS_CYCLE);
        Compartment cycle = new Compartment(parent, name, kernel);
        cycle.setNotificationEnabled(false);
        cycle.setShapeSize(new Dimension(400, 200));
        cycle.setPredefinedStyle(DiagramElementStyle.STYLE_NOT_SELECTED);
        cycle.getCustomStyle().setBrush(new Brush(Color.WHITE));
        cycle.setNotificationEnabled(true);
        return cycle;
    }

    /**
     * Create node for data element expression
     */
    @Override
    public Node createDataElementNode(Compartment parent, DataElementPath dePath)
    {
        String name = dePath.getName().replace(".", "_");
        if( parent.contains(name) )
        {
            int i = 2;
            while( parent.contains(name + "(" + i + ")") )
            {
                i++;
            }
            name = name + "(" + i + ")";
        }
        WorkflowExpression expression = (WorkflowExpression)WorkflowItemFactory.getWorkflowItem(parent, Type.ANALYSIS_EXPRESSION);
        expression.setName(name);
        expression.setType(VariableType.getType(DataElementPath.class));
        expression.setExpression(dePath.toString());
        return expression.getNode();
    }

    @Override
    protected Node getDataElementNode(Compartment compartment, DataElementPath path) throws Exception
    {
        for( Node node : compartment.getNodes() )
        {
            WorkflowItem item = WorkflowItemFactory.getWorkflowItem(node);
            if( item instanceof WorkflowVariable )
            {
                WorkflowVariable variable = (WorkflowVariable)item;
                if( variable.getType().getTypeClass().equals(DataElementPath.class) )
                {
                    Object value = variable.getValue();
                    if( value instanceof DataElementPath && value.equals(path) )
                        return node;
                }
            }
        }
        return null;
    }

    @Override
    public DiagramElement validate(Compartment compartment, @Nonnull DiagramElement de, boolean newElement) throws Exception
    {
        if(de instanceof Edge)
        {
            annotateEdge((Edge)de);
        }
        return de;
    }

    public void annotateEdge(Edge newEdge)
    {
        //add analysis properties if needed
        Node input = newEdge.getInput();
        Node output = newEdge.getOutput();
        WorkflowItem inputItem = WorkflowItemFactory.getWorkflowItem(input);
        WorkflowItem outputItem = WorkflowItemFactory.getWorkflowItem(output);
        if( input.getKernel().getType().equals(Type.TYPE_DATA_ELEMENT_OUT) && outputItem instanceof WorkflowVariable )
        {
            newEdge.getAttributes()
            .add(new DynamicProperty(EDGE_ANALYSIS_PROPERTY, String.class, input.getName().replaceAll(":", "/")));
            newEdge.getAttributes().add(new DynamicProperty(EDGE_VARIABLE, String.class, output.getName()));

            if( outputItem instanceof WorkflowParameter )
            {
                WorkflowParameter parameter = (WorkflowParameter)outputItem;
                configureWorkflowParameter(input, parameter);
                if( parameter.getRole().equals(WorkflowParameter.ROLE_DEFAULT) )
                    parameter.setRole(WorkflowParameter.ROLE_OUTPUT);
            }
        }
        if( output.getKernel().getType().equals(Type.TYPE_DATA_ELEMENT_IN) && inputItem instanceof WorkflowVariable )
        {
            newEdge.getAttributes().add(
                    new DynamicProperty(EDGE_ANALYSIS_PROPERTY, String.class, output.getName().replaceAll(":", "/")));
            newEdge.getAttributes().add(new DynamicProperty(EDGE_VARIABLE, String.class, input.getName()));

            if( inputItem instanceof WorkflowParameter )
            {
                WorkflowParameter parameter = (WorkflowParameter)inputItem;
                configureWorkflowParameter(output, parameter);
                if( parameter.getRole().equals(WorkflowParameter.ROLE_DEFAULT) )
                    parameter.setRole(WorkflowParameter.ROLE_INPUT);
            }
        }
        if( input.getKernel().getType().equals(Type.TYPE_DATA_ELEMENT_OUT)
                && output.getKernel().getType().equals(Type.TYPE_DATA_ELEMENT_IN) )
        {
            newEdge.getAttributes().add(
                    new DynamicProperty(EDGE_ANALYSIS_INPUT_PROPERTY, String.class, input.getName().replaceAll(":", "/")));
            newEdge.getAttributes().add(
                    new DynamicProperty(EDGE_ANALYSIS_OUTPUT_PROPERTY, String.class, output.getName().replaceAll(":", "/")));
        }
    }

    /**
     * Write the corresponding data from analysis into workflow parameter when edge is drawn
     * @param node
     * @param parameter
     */
    private void configureWorkflowParameter(Node node, WorkflowParameter parameter)
    {
        AnalysisParameters analysisParameters = WorkflowEngine.getInitialParametersByNode((Node)node.getOrigin());
        ComponentModel model = ComponentFactory.getModel(analysisParameters);
        Property property = model.findProperty(node.getName().replace(":", "/"));
        if( property != null )
        {
            if( ( property.getValueClass().equals(DataElementPath.class) || property.getValueClass().equals(DataElementPathSet.class) )
                    && !parameter.getType().getTypeClass().equals(DataElementPath.class)
                    && !parameter.getType().getTypeClass().equals(DataElementPathSet.class) )
                parameter.setType(VariableType.getType(DataElementPath.class));
            else if( File.class.isAssignableFrom(property.getValueClass()) )
                parameter.setType(VariableType.getType(FileItem.class));
            Object elementClassObj = property.getDescriptor().getValue(DataElementPathEditor.ELEMENT_CLASS);
            if( elementClassObj instanceof Class<?> )
                parameter.setDataElementType(DataElementType.getType((Class<? extends DataElement>)elementClassObj));
            Object referenceTypeObj = property.getDescriptor().getValue(DataElementPathEditor.REFERENCE_TYPE);
            if( referenceTypeObj instanceof Class<?> )
                parameter.setReferenceType(ReferenceTypeRegistry.getReferenceType((Class<? extends ReferenceType>)referenceTypeObj)
                        .toString());
        }
    }

    @SuppressWarnings ( "serial" )
    public static class NewNameDialog extends OkCancelDialog
    {
        protected JTextField id;

        public NewNameDialog(Component parent, String title)
        {
            super(parent, title);
            init();
        }

        public String getValue()
        {
            return id.getText();
        }

        private void init()
        {
            JPanel content = new JPanel( new GridBagLayout() );
            content.setBorder(new EmptyBorder(10, 10, 10, 10));
            setContent(content);

            String idName = "Name";
            content.add(new JLabel(idName), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 0), 0, 0));
            id = new JTextField(15);
            content.add(id, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                    new Insets(5, 5, 0, 0), 0, 0));
        }
    }

    @Override
    public DiagramElement[] addTaskInfoItemsToDiagram(Compartment parent, Iterator<TaskInfo> iter, Point inPoint, ViewEditorPane viewPane)
            throws Exception
    {
        List<DiagramElement> createdElements = new ArrayList<>();
        Diagram diagram = Diagram.getDiagram(parent);
        DiagramViewBuilder viewBuilder = diagram.getType().getDiagramViewBuilder();
        DiagramViewOptions viewOptions = diagram.getViewOptions();
        Graphics graphics = ApplicationUtils.getGraphics();
        int avgPos = 150, minSpan = 40;
        while( iter.hasNext() )
        {
            TaskInfo action = iter.next();
            try
            {
                if( action.getType().equals(TaskInfo.ANALYSIS) || action.getType().equals(TaskInfo.IMPORT) )
                {
                    Compartment node;
                    DynamicPropertySet attributes = action.getAttributes();
                    if( action.getType().equals(TaskInfo.IMPORT) ) // Convert import into analysis node
                    {
                        String format = (String)action.getAttributes().getValue(TaskInfo.IMPORT_FORMAT_PROPERTY);
                        String output = (String)action.getAttributes().getValue(TaskInfo.IMPORT_OUTPUT_PROPERTY);
                        AnalysisMethod importer = AnalysisMethodRegistry.getAnalysisMethod(format);
                        ImportAnalysisParameters parameters = (ImportAnalysisParameters)importer.getParameters();
                        Object properties = parameters.getProperties();
                        if( properties != null )
                        {
                            DPSUtils.readBeanFromDPS(properties, action.getAttributes(), DPSUtils.PARAMETER_ANALYSIS_PARAMETER + ".");
                        }
                        parameters.setResultPath(DataElementPath.create(output));
                        attributes = new DynamicPropertySetAsMap();
                        AnalysisDPSUtils.writeParametersToNodeAttributes(format, parameters, attributes);
                        node = createAnalysisNode(parent, format, attributes);
                    }
                    else
                    {
                        node = createAnalysisNode(parent, action.getSource().getName(), action.getAttributes());
                    }
                    if( parent.get(node.getName()) != null )
                    {
                        String name = node.getName();
                        int i = 2;
                        while( parent.get(name) != null )
                        {
                            name = node.getName() + "(" + i + ")";
                            i++;
                        }
                        node = node.clone(parent, name);
                    }
                    viewPane.add(node, inPoint);
                    Rectangle nodeBounds = viewBuilder.createCompartmentView(node, viewOptions, graphics).getBounds();
                    createdElements.add(node);
                    AnalysisParameters parameters = AnalysisDPSUtils.readParametersFromAttributes(attributes);
                    for( String pName : parameters.getInputNames() )
                    {
                        DataElementPath dePath = getParameterDataElement( parameters, pName );
                        if( dePath != null )
                        {
                            Node inNode = getDataElementNode( parent, dePath );
                            Node outNode = (Node)node.get( pName.replace( "/", ":" ) );
                            if( inNode == null )
                            {
                                inNode = createDataElementNode( parent, dePath );
                                Rectangle inNodeBounds = viewBuilder.createNodeView( inNode, viewOptions, graphics ).getBounds();
                                Rectangle outNodeBounds = viewBuilder.createNodeView( outNode, viewOptions, graphics ).getBounds();
                                int dy = (int) ( outNodeBounds.getCenterY() - nodeBounds.getCenterY() );
                                Point point = new Point(
                                        nodeBounds.x - ( inNodeBounds.getWidth() > ( avgPos - minSpan ) * 2
                                                ? minSpan + (int)inNodeBounds.getWidth() : ( avgPos + (int)inNodeBounds.getWidth() / 2 ) ),
                                        (int) ( nodeBounds.getCenterY() + dy * 1.5 - inNodeBounds.getHeight() / 2 ) );
                                viewPane.add( inNode, point );
                                Node parentNode = null;
                                String expression = dePath.toString();
                                for( DataElementPath parentPath = dePath.getParentPath(); !parentPath.isEmpty(); parentPath = parentPath
                                        .getParentPath() )
                                {
                                    parentNode = getDataElementNode( parent, parentPath );
                                    if( parentNode != null )
                                    {
                                        expression = "$" + parentNode.getName() + "$"
                                                + dePath.toString().substring( parentPath.toString().length() );
                                        break;
                                    }
                                }
                                if( parentNode != null )
                                {
                                    Edge parentEdge = createDirectedLink( parent, parentNode, inNode );
                                    viewPane.add( parentEdge, new Point( 0, 0 ) );
                                    ( (WorkflowExpression) ( WorkflowItemFactory.getWorkflowItem( inNode ) ) ).setExpression( expression );
                                }
                            }
                            Edge edge = createDirectedLink( parent, inNode, outNode );
                            annotateEdge( edge );
                            viewPane.add( edge, new Point( 0, 0 ) );
                        }
                    }
                    for( String pName : parameters.getOutputNames() )
                    {
                        DataElementPath dePath = getParameterDataElement( parameters, pName );
                        if( dePath != null )
                        {
                            Node inNode = (Node)node.get( pName.replace( "/", ":" ) );
                            Node outNode = getDataElementNode( parent, dePath );
                            if( outNode == null )
                            {
                                outNode = createDataElementNode( parent, dePath );
                                Rectangle inNodeBounds = viewBuilder.createNodeView( inNode, viewOptions, graphics ).getBounds();
                                Rectangle outNodeBounds = viewBuilder.createNodeView( outNode, viewOptions, graphics ).getBounds();
                                int dy = (int) ( inNodeBounds.getCenterY() - nodeBounds.getCenterY() );
                                Point point = new Point(
                                        nodeBounds.x + nodeBounds.width
                                        + ( outNodeBounds.getWidth() > ( avgPos - minSpan ) * 2 ? minSpan
                                                : ( avgPos - (int)outNodeBounds.getWidth() / 2 ) ),
                                        (int) ( nodeBounds.getCenterY() + dy * 1.5 - outNodeBounds.getHeight() / 2 ) );
                                viewPane.add( outNode, point );
                                Node parentNode = null;
                                String expression = dePath.toString();
                                for( DataElementPath parentPath = dePath.getParentPath(); !parentPath.isEmpty(); parentPath = parentPath
                                        .getParentPath() )
                                {
                                    parentNode = getDataElementNode( parent, parentPath );
                                    if( parentNode != null )
                                    {
                                        expression = "$" + parentNode.getName() + "$"
                                                + dePath.toString().substring( parentPath.toString().length() );
                                        break;
                                    }
                                }
                                if( parentNode != null )
                                {
                                    ( (WorkflowExpression) ( WorkflowItemFactory.getWorkflowItem( outNode ) ) ).setExpression( expression );
                                }
                            }
                            Edge edge = createDirectedLink( parent, inNode, outNode );
                            annotateEdge( edge );
                            viewPane.add( edge, new Point( 0, 0 ) );
                        }
                    }
                }
                else if( action.getType().equals(TaskInfo.SCRIPT) )
                {
                    // TODO: support non-JS scripts
                    Compartment node = createScriptNode(parent, action.getData(), "js");
                    viewPane.add(node, inPoint);
                    createdElements.add(node);
                }
                else if( action.getType().equals(TaskInfo.SCRIPT_DOCUMENT) )
                {
                    Compartment node = createScriptNode(parent, action.getSource());
                    viewPane.add(node, inPoint);
                    createdElements.add(node);
                }
                else if( action.getType().equals(TaskInfo.SQL) )
                {
                    String host = (String)action.getAttributes().getValue("host");
                    Compartment node = createSQLNode(parent, action.getData(), host);
                    viewPane.add(node, inPoint);
                    createdElements.add(node);
                }
            }
            catch( Exception e )
            {
                throw new Exception("Paste action error: " + e.getMessage());
            }
        }
        return createdElements.toArray(new DiagramElement[createdElements.size()]);
    }

    public void bindParameter(@Nonnull Node variable, @Nonnull Node analysis, Property property, ViewEditorPane editorPane)
    {
        Edge edge = bindParameter( variable, analysis, property.getCompleteName(), false );
        editorPane.add(edge, new Point(0, 0));
    }

    public Edge bindParameter(@Nonnull Node variable, @Nonnull Node analysis, String propertyName, boolean putEdge)
    {
        Compartment parent = Node.findCommonOrigin(variable, analysis);
        return bindParameter( parent, variable, analysis, propertyName, putEdge );
    }

    public Edge bindParameter(Compartment parent, @Nonnull Node variable, @Nonnull Node analysis, String propertyName, boolean putEdge)
    {
        String name = DefaultSemanticController.generateUniqueNodeName(parent, Base.TYPE_DIRECTED_LINK);
        Edge edge = new Edge(parent, new Stub(null, name, Base.TYPE_DIRECTED_LINK), variable, analysis);
        edge.getAttributes().add(new DynamicProperty(EDGE_ANALYSIS_PROPERTY, String.class, propertyName));
        edge.getAttributes().add(new DynamicProperty(EDGE_VARIABLE, String.class, variable.getName()));

        if( putEdge )
            parent.put( edge );

        return edge;
    }

    public boolean isAnalysisCircle(Node node)
    {
        return node.getKernel() != null && node.getKernel().getType().equals(Type.ANALYSIS_CYCLE);
    }

    public boolean contains(Compartment cmp, Node node)
    {
        return cmp.recursiveStream().has( node );
    }

    @Override
    public boolean remove(DiagramElement de) throws Exception
    {
        if( de.getKernel().getType().equals(Type.ANALYSIS_CYCLE_VARIABLE) )
            return false;
        return super.remove(de);
    }
    @Override
    public boolean isResizable(DiagramElement diagramElement)
    {
        if( diagramElement.getKernel() != null && diagramElement.getKernel().getType().equals(Type.ANALYSIS_METHOD) )
            return false;
        return super.isResizable(diagramElement);
    }
    @Override
    public Object getPropertiesByType(Compartment compartment, Object type, Point point)
    {
        try
        {
            Object bean = super.getPropertiesByType(compartment, type, point);
            if(bean != null) return bean;
            if( type.equals(Type.ANALYSIS_SCRIPT) )
            {
                return WorkflowEngine.getScriptParameters(createScriptNode(compartment, "", "js"));
            }
            if( type.equals(Type.ANALYSIS_CYCLE) )
            {
                return WorkflowItemFactory.getWorkflowItem(compartment, Type.ANALYSIS_CYCLE_VARIABLE);
            }
            return WorkflowItemFactory.getWorkflowItem(compartment, type.toString());
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException(e);
        }
    }
}
