package biouml.plugins.wdl.diagram;

import java.awt.Dimension;
import java.awt.Point;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.wdl.WorkflowUtil;
import biouml.plugins.wdl.model.CallInfo;
import biouml.plugins.wdl.model.ConditionalInfo;
import biouml.plugins.wdl.model.ExpressionInfo;
import biouml.plugins.wdl.model.ImportInfo;
import biouml.plugins.wdl.model.InputInfo;
import biouml.plugins.wdl.model.OutputInfo;
import biouml.plugins.wdl.model.ScatterInfo;
import biouml.plugins.wdl.model.ScriptInfo;
import biouml.plugins.wdl.model.StructInfo;
import biouml.plugins.wdl.model.TaskInfo;
import biouml.plugins.wdl.model.WorkflowInfo;
import biouml.standard.type.Stub;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPutException;

/**
 * Generates WDL diagram on the base of ScriptInfo object
 */
public class DiagramGenerator
{
    protected static final Logger log = Logger.getLogger(WDLImporter.class.getName());

    private boolean doImportDiagram = false;
    private Map<String, Diagram> imports = new HashMap<>();
    private Map<String, Compartment> tasks = new HashMap<>();
    private int externalPosition = 0;

    public Diagram generateDiagram(ScriptInfo script, DataCollection dc, String name) throws Exception
    {
        Diagram result = new WDLDiagramType().createDiagram(dc, name);
        return generateDiagram(script, result);
    }

    public Diagram generateDiagram(ScriptInfo script, Diagram diagram) throws Exception
    {
        diagram.clear();
        diagram.getAttributes().remove(WDLConstants.IMPORTS_ATTR);
        diagram.getAttributes().remove(WDLConstants.SETTINGS_ATTR);
        diagram.getAttributes().remove(WDLConstants.VERSION_ATTR);
        diagram.getAttributes().remove(WDLConstants.META_ATTR);
        diagram.getAttributes().remove(WDLConstants.PARAMETER_META_ATTR);

        externalPosition = 0;
        imports.clear();
        this.tasks.clear();

        for( ImportInfo importInfo : script.getImports() )
        {
            createImport(diagram, importInfo);
        }

        for( StructInfo structInfo : script.getStructs() )
        {
            createStruct(diagram, structInfo);
        }

        for( String taskName : script.getTaskNames() )
        {
            createTaskNode(diagram, script.getTask(taskName));
        }

        for( String workflowName : script.getWorkflowNames() )
        {
            WorkflowInfo workflow = script.getWorkflow(workflowName);//TODO" create compartment?
            WorkflowUtil.setMeta(diagram, workflow.getMeta());

            for( InputInfo input : workflow.getInputs() )
                createExternalParameterNode(diagram, input);

            for( OutputInfo output : workflow.getOutputs() )
                createOutputNode(diagram, output);

            for( Object object : workflow.getObjects() )
            {
                if( object instanceof ExpressionInfo )
                {
                    createExpressionNode(diagram, (ExpressionInfo)object);
                }
                else if( object instanceof CallInfo )
                {
                    createCallNode(diagram, (CallInfo)object);
                }
                else if( object instanceof ScatterInfo )
                {
                    createScatterNode(diagram, (ScatterInfo)object);
                }
                else if( object instanceof ConditionalInfo )
                {
                    createConditionalNode(diagram, (ConditionalInfo)object);
                }
            }
        }
        createLinks(diagram);
        return diagram;
    }

    public Node createStruct(Compartment parent, StructInfo structInfo) throws Exception
    {
        String name = structInfo.getName();
        Stub kernel = new Stub(null, name, WDLConstants.STRUCT_TYPE);
        Node node = new Node(parent, name, kernel);
        node.setTitle(name);
        node.setShapeSize(new Dimension(50, 50));
        parent.put(node);

        Iterable<ExpressionInfo> expressions = structInfo.getExpressions();
        ExpressionInfo[] declarations = StreamEx.of(expressions).toArray(ExpressionInfo[]::new);
        WorkflowUtil.setStructMembers(node, declarations);
        return node;
    }

    public void createImport(Diagram diagram, ImportInfo importInfo) throws Exception
    {
        try
        {
            DataElementPath dep = diagram.getOrigin().getCompletePath();
            String path = importInfo.getSource();
            String[] parts = path.split("/");
            DataElementPath importPath = dep.getChildPath(parts);
            Diagram imported = importPath.getDataElement(Diagram.class);
            imports.put(importInfo.getAlias(), imported);
            if( imported == null )
                throw new Exception("Imported diagram " + importInfo.getSource() + " not found!");
            WorkflowUtil.addImport(diagram, imported, importInfo.getAlias());
        }
        catch( Exception ex )
        {
            log.info("Can not process import " + importInfo.toString() + ": " + ex.getMessage());
        }
    }

    public Node createExternalParameterNode(Compartment parent, ExpressionInfo expressionInfo)
    {
        String name = expressionInfo.getName();
        Stub kernel = new Stub(null, name, WDLConstants.WORKFLOW_INPUT_TYPE);
        Node node = new Node(parent, name, kernel);
        WorkflowUtil.setPosition(node, externalPosition++);
        setDeclaration(node, expressionInfo);
        node.setTitle(name);
        node.setShapeSize(new Dimension(80, 60));
        parent.put(node);
        return node;
    }

    public void createLinks(Diagram diagram)
    {
        for( Node node : diagram.recursiveStream().select(Node.class) )
        {
            String expression = WorkflowUtil.getExpression(node);
            if( expression == null )
                continue;

            List<String> args = WorkflowUtil.findPossibleArguments(expression);
            for( String arg : args )
            {
                Node source = WorkflowUtil.findExpressionNode(diagram, arg);
                if( source != null )
                    createLink(source, node, WDLConstants.LINK_TYPE);
            }
        }
    }

    public Node createConditionNode(Compartment parent, String expression)
    {
        String name = WDLSemanticController.uniqName(parent, "condition");
        Stub kernel = new Stub(null, name, WDLConstants.CONDITION_TYPE);
        Node node = new Node(parent, name, kernel);
        WorkflowUtil.setExpression(node, expression);
        node.setTitle(name);
        node.setShapeSize(new Dimension(80, 60));
        parent.put(node);
        return node;
    }

    public Node createExpressionNode(Compartment parent, ExpressionInfo expressionInfo)
    {
        String name = expressionInfo.getName();
        boolean noName = name == "";
        if (noName)
        {
            name = DefaultSemanticController.generateUniqueName( parent, "expression" );            
        }
        Stub kernel = new Stub(null, name, WDLConstants.EXPRESSION_TYPE);
        Node node = new Node(parent, name, kernel);
        setDeclaration(node, expressionInfo);
        node.setTitle(name);
        node.setShapeSize(new Dimension(80, 60));
        parent.put(node);
        return node;
    }


    public Node createOutputNode(Compartment parent, ExpressionInfo expressionInfo)
    {
        String name = expressionInfo.getName();
        Stub kernel = new Stub(null, name, WDLConstants.WORKFLOW_OUTPUT_TYPE);
        Node node = new Node(parent, name, kernel);
        setDeclaration(node, expressionInfo);
        node.setTitle(name);
        node.setShapeSize(new Dimension(80, 60));
        parent.put(node);
        return node;
    }

    public Compartment createTaskNode(Compartment parent, TaskInfo task)
    {
        String name = task.getName();
        name = WDLSemanticController.uniqName(parent, name);
        Stub kernel = new Stub(null, name, WDLConstants.TASK_TYPE);

        Compartment c = new Compartment(parent, name, kernel);
        WorkflowUtil.setBeforeCommand(c, task.getBeforeCommand().toArray(ExpressionInfo[]::new));
        WorkflowUtil.setCommand(c, task.getCommand().getScript());
        WorkflowUtil.setRuntime(c, task.getRuntime());
        c.setTitle(name);
        c.setNotificationEnabled(false);
        c.setShapeSize(new Dimension(200, 0));
        tasks.put(name, c);
        int maxPorts = 0;
        int i = 0;
        for( ExpressionInfo expression : task.getInputs() )
        {
            Node portNode = addPort(WDLSemanticController.uniqName(parent, "input"), WDLConstants.INPUT_TYPE, i++, c);
            setDeclaration(portNode, expression);
            //            }
            maxPorts = task.getInputs().size();
        }

        i = 0;
        for( ExpressionInfo expression : task.getOutputs() )
        {
            Node portNode = addPort(WDLSemanticController.uniqName(parent, "output"), WDLConstants.OUTPUT_TYPE, i++, c);
            setDeclaration(portNode, expression);
        }
        maxPorts = Math.max(maxPorts, task.getOutputs().size());
        int height = Math.max(50, 24 * maxPorts + 8);
        c.setShapeSize(new Dimension(200, height));
        c.getAttributes().add(new DynamicProperty("innerNodesPortFinder", Boolean.class, true));
        c.setNotificationEnabled(true);
        parent.put(c);
        return c;
    }

    public Compartment createConditionalNode(Compartment parent, ConditionalInfo conditional) throws Exception
    {
        String name = WDLSemanticController.uniqName(parent, "conditional");
        Stub kernel = new Stub(null, name, WDLConstants.CONDITIONAL_TYPE);
        Compartment c = new Compartment(parent, name, kernel);
        c.setShapeSize(new Dimension(700, 700));
        String expression = conditional.getExpression();
        Node conditionNode = createConditionNode(parent, expression);
        createLink(conditionNode, c, WDLConstants.LINK_TYPE);
        parent.put(c);

        for( Object obj : conditional.getObjects() )
        {
            if( obj instanceof ExpressionInfo )
            {
                createExpressionNode(c, (ExpressionInfo)obj);
            }
            else if( obj instanceof CallInfo )
            {
                createCallNode(c, ( (CallInfo)obj ));
            }
            else if( obj instanceof ScatterInfo )
            {
                createScatterNode(c, (ScatterInfo)obj);
            }
            else if( obj instanceof ConditionalInfo )
            {
                createConditionalNode(c, (ConditionalInfo)obj);
            }
        }
        return c;
    }

    public Compartment createScatterNode(Compartment parent, ScatterInfo scatter) throws Exception
    {
        String name = "scatter";
        name = WDLSemanticController.uniqName(parent, name);
        Stub kernel = new Stub(null, name, WDLConstants.SCATTER_TYPE);
        Compartment c = new Compartment(parent, name, kernel);
        c.setShapeSize(new Dimension(500, 300));
        String variable = scatter.getVariable();
        String array = scatter.getExpression();
        Node arrayNode = Diagram.getDiagram(parent).findNode(array.toString());

        if( arrayNode == null )
            arrayNode = createExpression(array, "Array[Int]", parent);
        name = WDLSemanticController.uniqName(parent, variable);
        Node variableNode = new Node(c, name, new Stub(null, name, WDLConstants.SCATTER_VARIABLE_TYPE));
        WorkflowUtil.setName(variableNode, variable);
        c.put(variableNode);
        createLink(arrayNode, variableNode, WDLConstants.LINK_TYPE);
        parent.put(c);
        for( Object obj : scatter.getObjects() )
        {
            if( obj instanceof ExpressionInfo )
            {
                createExpressionNode(c, (ExpressionInfo)obj);
            }
            else if( obj instanceof CallInfo )
            {
                createCallNode(c, (CallInfo)obj);
            }
            else if( obj instanceof ScatterInfo )
            {
                createScatterNode(c, (ScatterInfo)obj);
            }
            else if( obj instanceof ConditionalInfo )
            {
                createConditionalNode(c, (ConditionalInfo)obj);
            }
        }
        return c;
    }

    //    private Node createExpression(ExpressionInfo expression, String type, Compartment parent)
    //    {
    //        String name = DefaultSemanticController.generateUniqueName(parent, "expression");
    //        Node resultNode = new Node(parent, name, new Stub(null, name, WDLConstants.EXPRESSION_TYPE));
    //        WorkflowUtil.setExpression(resultNode, expression.toString());
    //        WorkflowUtil.setName(resultNode, name);
    //        WorkflowUtil.setType(resultNode, type);
    //        parent.put(resultNode);
    //        return resultNode;
    //    }

    private Node createExpression(String expression, String type, Compartment parent)
    {
        String name = DefaultSemanticController.generateUniqueName(parent, "expression");
        Node resultNode = new Node(parent, name, new Stub(null, name, WDLConstants.EXPRESSION_TYPE));
        WorkflowUtil.setExpression(resultNode, expression);
        WorkflowUtil.setName(resultNode, name);
        WorkflowUtil.setType(resultNode, type);
        parent.put(resultNode);
        return resultNode;
    }

    private static void setDeclaration(Node node, ExpressionInfo declaration)
    {
        WorkflowUtil.setName(node, declaration.getName());
        WorkflowUtil.setType(node, declaration.getType());
        WorkflowUtil.setExpression(node, declaration.getExpression());
    }

    public Compartment createCallNode(Compartment parent, CallInfo call) throws Exception
    {
        Diagram diagram = Diagram.getDiagram(parent);
        String name = call.getTaskName();

        Compartment taskСompartment = tasks.get(name);
        String taskRef = name;
        String diagramRef = null;
        String diagramAlias = null;
        boolean externalDiagram = false;
        if( taskСompartment == null )
        {
            taskRef = name;
            if( name.contains(".") )
            {
                String[] parts = name.split("\\.");
                diagramAlias = parts[0];
                name = parts[1];//name.replace( ".", "_" );
                taskRef = parts[1];
                Diagram importedDiagram = imports.get(diagramAlias);
                diagramRef = importedDiagram.getName();

                if( taskRef.equals(WDLConstants.MAIN_WORKFLOW) )
                {
                    taskСompartment = importedDiagram;
                    externalDiagram = true;
                }
                else
                {
                    DiagramElement de = importedDiagram.get(taskRef);
                    if( ! ( de instanceof Compartment ) )
                        throw new Exception("Can not resolve call " + call.getTaskName());
                    taskСompartment = (Compartment)importedDiagram.get(taskRef);
                }
            }
        }
        else
            taskRef = taskСompartment.getName();
        String title = name;
        String alias = call.getAlias();
        if( alias != null )
        {
            //            name = alias;
            title = alias;
        }

        name = DefaultSemanticController.generateUniqueName(diagram, "Call_" + name);
        Stub kernel = new Stub(null, name, WDLConstants.CALL_TYPE);

        Compartment c = new Compartment(parent, name, kernel);
        c.setShapeSize(new Dimension(200, 0));
        c.setTitle(title);
        WorkflowUtil.setTaskRef(c, taskRef);
        WorkflowUtil.setCallName(c, title);

        //        for( AstMeta meta : WorkflowUtil.findChild( call, AstMeta.class ) )
        //            WorkflowUtil.setMeta( diagram, call.getMeta() );

        if( diagramRef != null )
            WorkflowUtil.setDiagramRef(c, diagramRef);
        if( diagramAlias != null )
            WorkflowUtil.setExternalDiagramAlias(c, diagramAlias);
        c.setNotificationEnabled(false);

        int inputs = 0;
        int outputs = 0;

        //        AstSymbol[] inputSymbols = call.getInputs();
        Collection<InputInfo> inputsInfo = call.getInputs();
        Set<String> addedInputs = new HashSet<>();//TODO: refactor this
        //        for( AstSymbol symbol : inputSymbols )
        for( InputInfo symbol : inputsInfo )
        {
            String inputName = symbol.getName();
            addedInputs.add(inputName);
            String expression = symbol.getExpression();
            //            String expression = expressionInfo.getExpression();
            if( expression == null )
                expression = inputName;
            //            AstExpression expr = null;
            //            if( symbol.getChildren() != null )
            //            {
            //                List<AstExpression> exprs = WorkflowUtil.findChild( symbol, AstExpression.class );
            //                if( !exprs.isEmpty() )
            //                {
            //                    expr = exprs.get( 0 );
            //                    expression = expr.toString();
            //                }
            //            }

            Node portNode = addPort(inputName, WDLConstants.INPUT_TYPE, inputs++, c);

            if( taskСompartment instanceof Diagram )
            {
                for( Node node : WorkflowUtil.getExternalParameters((Diagram)taskСompartment) )
                {
                    String varName = WorkflowUtil.getName(node);
                    if( varName.equals(inputName) )
                    {
                        WorkflowUtil.setPosition(portNode, WorkflowUtil.getPosition(node));
                        Node inputNode = WorkflowUtil.getTarget(node);
                        if( inputNode != null )
                            node = inputNode;
                        WorkflowUtil.setName(portNode, WorkflowUtil.getName(node));
                        WorkflowUtil.setType(portNode, WorkflowUtil.getType(node));
                        WorkflowUtil.setExpression(portNode, expression);
                    }
                }
            }
            else
            {
                for( Node node : taskСompartment.getNodes() )
                {
                    String varName = WorkflowUtil.getName(node);
                    if( varName.equals(inputName) )
                    {
                        WorkflowUtil.setName(portNode, WorkflowUtil.getName(node));
                        WorkflowUtil.setType(portNode, WorkflowUtil.getType(node));
                        WorkflowUtil.setExpression(portNode, expression);
                        WorkflowUtil.setPosition(portNode, WorkflowUtil.getPosition(node));
                    }
                }
            }

            //            if( expressionInfo != null )
            //            {
            //                for( String argument : expressionInfo.getArguments() )
            //                {
            //                    Node expressionNode = WorkflowUtil.findExpressionNode(diagram, argument);
            //                    if( expressionNode != null )
            //                        createLink(expressionNode, portNode, WDLConstants.LINK_TYPE);
            //                }
            //            }
        }

        if( taskСompartment instanceof Diagram )
        {
            for( Node node : WorkflowUtil.getExternalOutputs((Diagram)taskСompartment) )
            {
                Node portNode = addPort(node.getName(), WDLConstants.OUTPUT_TYPE, outputs++, c);
                WorkflowUtil.setName(portNode, WorkflowUtil.getName(node));
                WorkflowUtil.setType(portNode, WorkflowUtil.getType(node));
                WorkflowUtil.setExpression(portNode, WorkflowUtil.getExpression(node));
                WorkflowUtil.setPosition(portNode, WorkflowUtil.getPosition(node));
            }
            for( Node node : WorkflowUtil.getExternalParameters((Diagram)taskСompartment) )
            {
                if( !addedInputs.contains(WorkflowUtil.getName(node)) )
                {
                    Node portNode = addPort(node.getName(), WDLConstants.INPUT_TYPE, inputs++, c);
                    WorkflowUtil.setName(portNode, WorkflowUtil.getName(node));
                    WorkflowUtil.setType(portNode, WorkflowUtil.getType(node));
                    WorkflowUtil.setExpression(portNode, WorkflowUtil.getExpression(node));
                    WorkflowUtil.setPosition(portNode, WorkflowUtil.getPosition(node));
                }
            }
        }
        else
        {
            for( Node node : taskСompartment.getNodes() )
            {
                Node portNode = null;
                if( WDLConstants.OUTPUT_TYPE.equals(node.getKernel().getType()) )
                {
                    portNode = addPort(node.getName(), WDLConstants.OUTPUT_TYPE, outputs++, c);
                    WorkflowUtil.setName(portNode, WorkflowUtil.getName(node));
                    WorkflowUtil.setType(portNode, WorkflowUtil.getType(node));
                    WorkflowUtil.setExpression(portNode, WorkflowUtil.getExpression(node));
                    //WDLUtil.setPosition( portNode, WDLUtil.getPosition( node ) );
                }
                else if( !addedInputs.contains(WorkflowUtil.getName(node)) )
                {
                    portNode = addPort(node.getName(), WDLConstants.INPUT_TYPE, inputs++, c);
                    WorkflowUtil.setName(portNode, WorkflowUtil.getName(node));
                    WorkflowUtil.setType(portNode, WorkflowUtil.getType(node));
                    //WDLUtil.setExpression( portNode, WDLUtil.getExpression( node ) );
                    WorkflowUtil.setPosition(portNode, WorkflowUtil.getPosition(node));
                }
            }
        }
        int maxPorts = Math.max(inputs, outputs);
        int height = Math.max(50, 24 * maxPorts + 8);
        c.setShapeSize(new Dimension(200, height));
        c.getAttributes().add(new DynamicProperty("innerNodesPortFinder", Boolean.class, true));
        c.setNotificationEnabled(true);
        parent.put(c);
        return c;
    }


    public static Node addPort(String name, String nodeType, int position, Compartment parent) throws DataElementPutException
    {
        Node inNode = new Node(parent, new Stub(parent, name, nodeType));
        WorkflowUtil.setPosition(inNode, position);
        inNode.setFixed(true);
        Point parentLoc = parent.getLocation();
        Dimension parentDim = parent.getShapeSize();
        if( WDLConstants.INPUT_TYPE.equals(nodeType) )
            inNode.setLocation(parentLoc.x + 2, parentLoc.y + position * 24 + 8);
        else
            inNode.setLocation(parentLoc.x + parentDim.width - 16 - 2, parentLoc.y + position * 24 + 8);
        parent.put(inNode);
        return inNode;
    }

    public static Edge createLink(Node input, Node output, String type)
    {
        String name = input.getName() + "_to_" + output.getName();
        Diagram d = Diagram.getDiagram(input);
        name = DefaultSemanticController.generateUniqueName(d, name);
        Edge e = new Edge(new Stub(null, name, type), input, output);
        e.getCompartment().put(e);
        return e;
    }

}
