package biouml.plugins.wdl.cwl;

import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.yaml.snakeyaml.Yaml;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.plugins.wdl.WorkflowUtil;
import biouml.plugins.wdl.diagram.DiagramGenerator;
import biouml.plugins.wdl.diagram.WDLConstants;
import biouml.plugins.wdl.diagram.WDLDiagramType;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Stub;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;

public class CWLParser
{
    private static final String $GRAPH = "$graph";
    private static final String WORKFLOW = "Workflow";
    private static final String COMMAND_LINE_TOOL = "CommandLineTool";
    private Diagram diagram;

    public Diagram loadDiagram(File f, DataCollection dc, String name) throws Exception
    {
        String content = ApplicationUtils.readAsString( f );
        Diagram diagram = new WDLDiagramType().createDiagram( dc, name, new DiagramInfo( null, name ) );
        return loadDiagram( content, diagram );
    }

    public Diagram loadDiagram(String content, Diagram diagram) throws Exception
    {
        this.diagram = diagram;
        diagram.clear();
        diagram.setNotificationEnabled( false );
        Map<String, Object> map = parseYaml( content );

        if( map.containsKey( $GRAPH ) )
        {
            List<Object> graph = getList( map, $GRAPH );

            for( Map tool : findObjects( graph, COMMAND_LINE_TOOL ) )
                processCommandLineTool( tool );

            for( Map tool : findObjects( graph, WORKFLOW ) )//TODO: what to do if several workflows
                processWorkflow( tool );
        }
        else
        {
            String name = diagram.getName();
            if( name.contains( "." ) )
                name = name.substring( 0, name.indexOf( "." ) );
            if( isOfType( map, COMMAND_LINE_TOOL ) )
                processCommandLineTool( name, map );
            else if( isOfType( map, WORKFLOW ) )
                processWorkflow( map );
            else
                throw new Exception( "Unknown class: " + getString( map, "class" ) );
        }
        createRelations();
        diagram.setNotificationEnabled( true );
        return diagram;
    }

    public void processCommandLineTool(Map<String, Object> process) throws Exception
    {
        processCommandLineTool( null, process );
    }

    public void processCommandLineTool(String id, Map<String, Object> process) throws Exception
    {
        if( id == null )
            id = getString( process, "id" );

        Object baseCommand = process.get( "baseCommand" );
        List arguments = getList( process, "arguments" );
        Map<String, Object> inputs = getMap( process, "inputs" );
        Map<String, Object> outputs = getMap( process, "outputs" );
        String name = generateUniqueName( diagram, id );
        Compartment task = new Compartment( diagram, new Stub( null, name, WDLConstants.TASK_TYPE ) );
        task.setTitle( id );
        task.setNotificationEnabled( false );
        task.setShapeSize( new Dimension( 200, 0 ) );
        int inputCount = 0;
        for( String inputName : inputs.keySet() )
        {
            Map<String, Object> input = getMap( inputs, inputName );
            String type = getString( input, "type", "" );
            String portId = generateUniqueName( task, inputName );
            Node port = DiagramGenerator.addPort( portId, WDLConstants.INPUT_TYPE, inputCount, task );
            WorkflowUtil.setName( port, inputName );
            WorkflowUtil.setType( port, toBioUMLType( type ) );
            WorkflowUtil.setExpression( port, "" );//TODO: default values
            inputCount++;
        }
        int outputCount = 0;
        for( String outputName : outputs.keySet() )
        {
            Map<String, Object> output = getMap( outputs, outputName );
            String type = getString( output, "type", "" );
            Map outputBinding = getMap( output, "outputBinding" );
            String expression = getString( outputBinding, "glob", "" );
            String portId = generateUniqueName( task, outputName );
            Node port = DiagramGenerator.addPort( portId, WDLConstants.OUTPUT_TYPE, outputCount, task );
            WorkflowUtil.setName( port, outputName );
            WorkflowUtil.setType( port, toBioUMLType( type ) );
            WorkflowUtil.setExpression( port, toBioUMLExpression( expression ) );
            outputCount++;
        }

        String command = processCommand( baseCommand, arguments, inputs );
        WorkflowUtil.setCommand( task, command );

        int maxPorts = Math.max( inputCount, outputCount );
        int height = Math.max( 50, 24 * maxPorts + 8 );
        task.setShapeSize( new Dimension( 200, height ) );
        task.getAttributes().add( new DynamicProperty( "innerNodesPortFinder", Boolean.class, true ) );
        task.setNotificationEnabled( true );
        diagram.put( task );
    }

    //    Map<String, Object> inputBinding = getMap( process, "inputBinding" );
    //    String position = getString( inputBinding, "position" );
    //    if( position != null )
    //    {
    //        int positonInCommand = Integer.parseInt( position );
    //        inputsInCommand.put( inputName, positonInCommand );
    //    }
    public String processCommand(Object baseCommand, List<Object> arguments, Map<String, Object> inputs) throws Exception
    {
        Map<Integer, List<String>> args = new TreeMap<>();
        //special case
        if( arguments.size() == 2 && arguments.get( 0 ).equals( "-c" ) && arguments.get( 1 ) instanceof Map )
        {
            if( baseCommand.equals( "sh" ) )
                return ( (Map)arguments.get( 1 ) ).get( "valueFrom" ).toString();
            else
                return baseCommand.toString() + " " + ( (Map)arguments.get( 1 ) ).get( "valueFrom" ).toString();
        }

        for( String inputName : inputs.keySet() )
        {
            Map input = getMap( inputs, inputName );
            String inputType = getString( input, "type" );
            Map<String, Object> inputBinding = getMap( input, "inputBinding" );
            String positionStr = getString( inputBinding, "position" );
            int position = Integer.parseInt( positionStr );
            args.computeIfAbsent( position, i -> new ArrayList<>() ).add( inputName );
        }

        for( Object argument : arguments )
        {
            int position = 0;
            String argumentString = null;
            if( argument instanceof String )
                argumentString = argument.toString();
            else if( argument instanceof Map )
            {
                Map<String, Object> argumentMap = (Map)argument;
                String prefix = getString( argumentMap, "prefix" );
                String value = getString( argumentMap, "valueFrom" );
                position = getInteger( argumentMap, "position", 0 );
                if( prefix != null )
                    argumentString = prefix + " " + value;
                else
                    argumentString = value;
            }
            if( argumentString != null )
                args.computeIfAbsent( position, i -> new ArrayList<>() ).add( argumentString );
        }

        StringBuilder inputStringBuilder = new StringBuilder();
        for( Entry<Integer, List<String>> inputEntry : args.entrySet() )
        {
            inputStringBuilder.append( StreamEx.of( inputEntry.getValue() ).joining( " " ) );
        }

        if( baseCommand instanceof List )
            baseCommand = StreamEx.of( (List)baseCommand ).joining( " " );
        return baseCommand + " " + inputStringBuilder.toString();
    }

    public void processWorkflow(Map<String, Object> workflow) throws Exception
    {
        processInputs( workflow );
        processOutputs( workflow );
        processSteps( workflow );
    }

    public void processInputs(Map<String, Object> element)
    {
        Map<String, Object> inputs = getMap( element, "inputs" );
        for( String key : inputs.keySet() )
        {
            Map<String, Object> input = getMap( inputs, key );
            String type = getString( input, "type", "" );
            createExpression( diagram, type, key, "", WDLConstants.WORKFLOW_INPUT_TYPE );
        }
    }

    public void processOutputs(Map<String, Object> element)
    {
        Map<String, Object> outputs = getMap( element, "outputs" );
        for( String key : outputs.keySet() )
        {
            Map<String, Object> output = getMap( outputs, key );
            String type = getString( output, "type", "" );
            String source = getString( output, "outputSource", "" );
            createExpression( diagram, type, key, source, WDLConstants.WORKFLOW_OUTPUT_TYPE );
        }
    }
    public void processSteps(Map<String, Object> element) throws Exception
    {
        Map<String, Object> steps = getMap( element, "steps" );
        for( String key : steps.keySet() )
        {
            Map<String, Object> step = getMap( steps, key );
            createCall( diagram, key, step );
        }
    }

    public Node createExpression(Compartment parent, String type, String name, String expression, String expressionType)
    {
        String id = generateUniqueName( parent, name );
        Node node = new Node( diagram, new Stub( diagram, id, expressionType ) );
        WorkflowUtil.setType( node, toBioUMLType( type ) );
        WorkflowUtil.setName( node, name );
        WorkflowUtil.setExpression( node, toBioUMLExpression( expression ) );
        node.setShapeSize( new Dimension( 80, 60 ) );
        parent.put( node );
        return node;
    }

    public Compartment createCall(Compartment parent, String name, Map<String, Object> callMap) throws Exception
    {
        Map<String, Object> inMap = getMap( callMap, "in" );
        List<Object> outList = getList( callMap, "out" );
        String taskRef = getString( callMap, "run" );
        if( taskRef.startsWith( "#" ) )
            taskRef = taskRef.substring( 1 );

        Compartment task = (Compartment)diagram.findNode( taskRef );
        String id = generateUniqueName( parent, name );
        Compartment call = new Compartment( parent, new Stub( null, id, WDLConstants.CALL_TYPE ) );
        call.setShapeSize( new Dimension( 200, 0 ) );
        call.setNotificationEnabled( false );
        WorkflowUtil.setTaskRef( call, taskRef );
        WorkflowUtil.setCallName( call, name );
        int inputCount = 0;
        for( String inKey : inMap.keySet() )
        {
            String value = inMap.get( inKey ).toString();
            String portId = generateUniqueName( parent, inKey );
            Node port = DiagramGenerator.addPort( portId, WDLConstants.INPUT_TYPE, inputCount++, call );
            Node originalPort = task.findNode( inKey );
            WorkflowUtil.setType( port, WorkflowUtil.getType( originalPort ) );
            WorkflowUtil.setPosition( port, WorkflowUtil.getPosition( originalPort ) );
            WorkflowUtil.setExpression( port, toBioUMLExpression( value ) );
            WorkflowUtil.setName( port, inKey );
        }
        int outputCount = 0;
        for( Object out : outList )
        {
            if( out instanceof String )
            {
                String value = out.toString();
                String portId = generateUniqueName( diagram, value );
                Node port = DiagramGenerator.addPort( portId, WDLConstants.OUTPUT_TYPE, outputCount++, call );
                Node originalPort = task.findNode( value );
                WorkflowUtil.setName( port, value );
                WorkflowUtil.setType( port, WorkflowUtil.getType( originalPort ) );
                WorkflowUtil.setPosition( port, WorkflowUtil.getPosition( originalPort ) );
                WorkflowUtil.setExpression( port, toBioUMLExpression( value ) );
            }
            else
                throw new Exception( "Unknown out type: " + out );
        }

        int maxPorts = Math.max( inputCount, outputCount );
        int height = Math.max( 50, 24 * maxPorts + 8 );
        call.setShapeSize( new Dimension( 200, height ) );
        call.getAttributes().add( new DynamicProperty( "innerNodesPortFinder", Boolean.class, true ) );
        call.setNotificationEnabled( true );
        parent.put( call );
        return call;
    }

    public void createRelations()
    {
        for( Compartment c : WorkflowUtil.getCalls( diagram ) )
        {
            for( Node input : WorkflowUtil.getInputs( c ) )
            {
                addSourceEdges( input );
            }

            for( Node expression : WorkflowUtil.getExpressions( diagram ) )
            {
                addSourceEdges( expression );
            }

            for( Node output : WorkflowUtil.getExternalOutputs( diagram ) )
            {
                addSourceEdges( output );
            }
        }
    }

    public void addSourceEdges(Node node)
    {
        String expression = WorkflowUtil.getExpression( node );
        if( expression.contains( "." ) )
        {
            String[] parts = expression.split( "\\." );
            String sourceCallName = parts[0];
            String sourcePortName = parts[1];
            Node sourceCall = WorkflowUtil.findCall( sourceCallName, diagram );
            Node sourcePort = (Node) ( (Compartment)sourceCall ).get( sourcePortName );
            DiagramGenerator.createLink( sourcePort, node);
        }
        else
        {
            Node source = diagram.findNode( expression );
            DiagramGenerator.createLink( source, node );
        }
    }

    public static String getString(Map<String, Object> map, String name, String defaultValue)
    {
        Object obj = map.get( name );
        if( obj != null )
            return obj.toString();
        return defaultValue;
    }

    public static String getString(Map<String, Object> map, String name)
    {
        Object obj = map.get( name );
        if( obj != null )
            return obj.toString();
        return null;
    }

    public static int getInteger(Map<String, Object> map, String name, int defaultValue)
    {
        Object obj = map.get( name );
        if( obj == null )
            return defaultValue;
        try
        {
            return Integer.parseInt( obj.toString() );
        }
        catch( Exception ex )
        {
            return defaultValue;
        }
    }
    
    public static boolean getBoolean(Map<String, Object> map, String name, boolean defaultValue)
    {
        Object obj = map.get( name );
        if( obj == null )
            return defaultValue;
        try
        {
            return Boolean.parseBoolean( obj.toString() );
        }
        catch( Exception ex )
        {
            return defaultValue;
        }
    }

    public static Map<String, Object> getMap(Map<String, Object> map, String name)
    {
        Object obj = map.get( name );
        if( obj instanceof Map )
            return (Map)obj;
        return new HashMap<String, Object>();
    }

    public static List<Object> getList(Map<String, Object> map, String name)
    {
        Object obj = map.get( name );
        if( obj instanceof List )
            return (List)obj;
        return new ArrayList<Object>();
    }

    public static String toBioUMLExpression(String cwlExpression)
    {
        String result = cwlExpression.replace( "/", "." );
        if( result.startsWith( "glob:" ) )
            result = result.replace( "glob:", "" ).trim();
        return result;
    }

    public static String toBioUMLType(String cwlType)
    {
        if( cwlType.equals( "string" ) )
            return "String";
        return cwlType;
    }

    public static Map<String, Object> parseYaml(String text)
    {
        Yaml parser = new Yaml();

        Object root;
        try
        {
            root = parser.load( text );
        }
        catch( Exception e )
        {
            return null;
        }
        if( ! ( root instanceof Map ) )
            return null;

        Map<?, ?> rootMap = (Map<?, ?>)root;
        System.out.println( rootMap );
        return (Map<String, Object>)rootMap;
    }

    public static StreamEx<Map> findObjects(List<Object> graph, String type)
    {
        return StreamEx.of( graph ).select( Map.class ).filter( m -> isOfType( m, type ) );
    }

    public static boolean isOfType(Map map, String type)
    {
        return type.equals( getString( map, "class" ) );
    }

    public static String generateUniqueName(Compartment compartment, String baseName)
    {
        Set<String> names = compartment.stream().map( de -> de.getName() ).toSet();

        int index = 1;

        if( !names.contains( baseName ) )
            return baseName;

        String result = baseName + "_" + index;
        while( names.contains( result ) )
        {
            index++;
            result = baseName + "_" + index;
        }
        return result;
    }
}
