package biouml.plugins.wdl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Node;
import biouml.plugins.wdl.diagram.WDLConstants;

public class WDLUtil
{
    public static boolean isTask(Node node)
    {
        return WDLConstants.TASK_TYPE.equals( node.getKernel().getType() );
    }

    public static boolean isCall(Node node)
    {
        return WDLConstants.CALL_TYPE.equals( node.getKernel().getType() );
    }

    public static boolean isLink(DiagramElement de)
    {
        return WDLConstants.LINK_TYPE.equals( de.getKernel().getType() );
    }

    public static boolean isInput(Node node)
    {
        return WDLConstants.INPUT_TYPE.equals( node.getKernel().getType() );
    }

    public static boolean isExternalParameter(Node node)
    {
        return WDLConstants.EXTERNAL_PARAMETER_TYPE.equals( node.getKernel().getType() );
    }

    public static boolean isExpression(Node node)
    {
        return WDLConstants.EXPRESSION_TYPE.equals( node.getKernel().getType() );
    }

    public static boolean isCycleVariable(Node node)
    {
        return WDLConstants.SCATTER_VARIABLE_TYPE.equals( node.getKernel().getType() );
    }

    public static boolean isCycle(Node node)
    {
        return WDLConstants.SCATTER_TYPE.equals( node.getKernel().getType() );
    }

    public static boolean isOutput(Node node)
    {
        return WDLConstants.OUTPUT_TYPE.equals( node.getKernel().getType() );
    }

    public static boolean isExternalOutput(Node node)
    {
        return WDLConstants.EXPRESSION_TYPE.equals( node.getKernel().getType() );
    }

    public static List<Compartment> getTasks(Compartment c)
    {
        return c.stream( Compartment.class ).filter( n -> isTask( n ) ).toList();
    }

    public static List<Compartment> getCycles(Compartment c)
    {
        return c.stream( Compartment.class ).filter( n -> isCycle( n ) ).toList();
    }

    public static List<Node> getExternalParameters(Diagram diagram)
    {
        return diagram.stream( Node.class ).filter( n -> isExternalParameter( n ) ).toList();
    }

    public static List<Compartment> getCalls(Compartment c)
    {
        return c.stream( Compartment.class ).filter( n -> isCall( n ) ).toList();
    }

    public static List<Compartment> getAllCalls(Compartment c)
    {
        return c.recursiveStream().select( Compartment.class ).filter( n -> isCall( n ) ).toList();
    }

    public static List<Node> getInputs(Compartment c)
    {
        return c.stream( Node.class ).filter( n -> isInput( n ) ).toList();
    }

    public static List<Node> getOutputs(Compartment c)
    {
        return c.stream( Node.class ).filter( n -> isOutput( n ) ).toList();
    }

    public static List<Node> getExternalOutputs(Diagram d)
    {
        return d.stream( Node.class ).filter( n -> isExternalOutput( n ) ).toList();
    }

    public static Map<String, String> getRequirements(Compartment c)
    {
        Object val = c.getAttributes().getValue( WDLConstants.REQUIREMENTS_ATTR );
        if( val instanceof Map )
            return (Map<String, String>)val;
        return null;
    }
    public static void setRequirements(Compartment c, Map<String, String> requirements)
    {
        c.getAttributes().add( new DynamicProperty( WDLConstants.REQUIREMENTS_ATTR, Map.class, requirements ) );
    }

    public static Map<String, String> getHints(Compartment c)
    {
        Object val = c.getAttributes().getValue( WDLConstants.HINTS_ATTR );
        if( val instanceof Map )
            return (Map<String, String>)val;
        return null;
    }
    public static void setHints(Compartment c, Map<String, String> hints)
    {
        c.getAttributes().add( new DynamicProperty( WDLConstants.HINTS_ATTR, Map.class, hints ) );
    }

    public static Map<String, String> getRuntime(Compartment c)
    {
        Object val = c.getAttributes().getValue( WDLConstants.RUNTIME_ATTR );
        if( val instanceof String[] )
        {
            String[] array = (String[])val;
            Map<String, String> result = new HashMap<>();
            for( String s : array )
            {
                String[] split = s.split( "#" );
                result.put( split[0], split[1] );
            }
            return result;
        }
        return null;
    }
    public static void setRuntime(Compartment c, Map<String, String> runtime)
    {
        String[] vals = runtime.entrySet().stream().map( e -> (String) ( e.getKey() + "#" + e.getValue() ) ).toArray( String[]::new );
        c.getAttributes().add( new DynamicProperty( WDLConstants.RUNTIME_ATTR, String[].class, vals ) );
    }

    public static String getDeclaration(Node n)
    {
        return getType( n ) + " " + getName( n ) + " = " + getExpression( n );
    }
    public static String getExpression(Node n)
    {
        return n.getAttributes().getValueAsString( WDLConstants.EXPRESSION_ATTR );
    }
    public static void setExpression(Node n, String expression)
    {
        n.getAttributes().add( new DynamicProperty( WDLConstants.EXPRESSION_ATTR, String.class, expression ) );
    }

    public static void setType(Node n, String type)
    {
        n.getAttributes().add( new DynamicProperty( WDLConstants.TYPE_ATTR, String.class, type ) );
    }

    public static String getType(Node n)
    {
        return n.getAttributes().getValueAsString( WDLConstants.TYPE_ATTR );
    }

    public static String getName(Node n)
    {
        return n.getAttributes().getValueAsString( WDLConstants.NAME_ATTR );
    }
    public static void setName(Node n, String name)
    {
        n.getAttributes().add( new DynamicProperty( WDLConstants.NAME_ATTR, String.class, name ) );
    }

    public static void setVersion(Diagram diagram, String version)
    {
        diagram.getAttributes().add( new DynamicProperty( WDLConstants.VERSION_ATTR, String.class, version ) );
    }
    public static String getVersion(Diagram d)
    {
        return d.getAttributes().getValueAsString( WDLConstants.VERSION_ATTR );
    }

    public static void setCommand(Compartment compartment, String command)
    {
        compartment.getAttributes().add( new DynamicProperty( WDLConstants.COMMAND_ATTR, String.class, command ) );
    }
    public static String getCommand(Compartment c)
    {
        return c.getAttributes().getValueAsString( WDLConstants.COMMAND_ATTR );
    }

    public static void setTaskRef(Compartment c, String ref)
    {
        c.getAttributes().add( new DynamicProperty( WDLConstants.TASK_REF_ATTR, String.class, ref ) );
    }
    public static String getTaskRef(Compartment c)
    {
        return c.getAttributes().getValueAsString( WDLConstants.TASK_REF_ATTR );
    }

    public static Compartment findCall(String taskName, Diagram diagram)
    {
        return diagram.recursiveStream().select( Compartment.class ).filter( c -> isCall( c ) && getTaskRef( c ).equals( taskName ) )
                .findAny().orElse( null );
    }

    public static Node findExpressionNode(Diagram diagram, String name)
    {
        if( name.contains( "." ) )
        {
            String[] parts = name.split( "\\." );
            String call = parts[0];
            String varName = parts[1];
            Compartment callNode = WDLUtil.findCall( call, diagram );
            Node port = callNode.stream( Node.class ).filter( n -> varName.equals( getName( n ) ) ).findAny().orElse( null );
            return port;
        }
        return diagram.recursiveStream().select( Node.class )
                .filter( n -> ( isExternalParameter( n ) || isExpression( n ) || isCycleVariable( n ) ) )
                .findAny( n -> name.equals( getName( n ) ) ).orElse( null );
    }

    public static <T> T findChild(biouml.plugins.wdl.parser.Node node, Class<T> c)
    {
        for( int i = 0; i < node.jjtGetNumChildren(); i++ )
        {
            biouml.plugins.wdl.parser.Node child = node.jjtGetChild( i );
            if( c.isInstance( child ) )
            {
                return c.cast( child );
            }
        }
        return null;
    }

    public static String getCycleVariable(Compartment c)
    {
        for( Node node : c.getNodes() )
        {
            if( isCycleVariable( node ) )
                return getName( node );
        }
        return null;
    }

    public static Node getCycleVariableNode(Compartment c)
    {
        for( Node node : c.getNodes() )
        {
            if( isCycleVariable( node ) )
                return node;
        }
        return null;
    }

    public static String getCycleName(Compartment c)
    {
        for( Node node : c.getNodes() )
        {
            if( isCycleVariable( node ) )
            {
                Node arrayNode = node.edges().map( e -> e.getOtherEnd( node ) ).findAny().orElse( null );
                if( arrayNode != null )
                    return getName( arrayNode );
            }
        }
        return null;
    }

    public static List<Compartment> orderCalls(Diagram diagram)
    {
        List<Compartment> result = new ArrayList<>();
        Map<Compartment, Set<Compartment>> previousSteps = new HashMap<>();
        for( Compartment compartment : diagram.recursiveStream().select( Compartment.class ).filter( c -> isCall( c ) ) )
            previousSteps.put( compartment, getPreviousSteps( compartment ) );

        Set<Compartment> added = new HashSet<>();
        while( previousSteps.size() > 0 )
        {
            for( Compartment key : previousSteps.keySet() )
            {
                Set<Compartment> steps = previousSteps.get( key );
                steps.removeAll( added );
                if( steps.isEmpty() )
                {
                    result.add( key );
                    added.add( key );
                }
            }
            for( Compartment c : added )
                previousSteps.remove( c );
        }
        return result;
    }

    public static Set<Compartment> getPreviousSteps(Compartment c)
    {
        return c.stream( Node.class ).flatMap( n -> n.edges() ).map( e -> e.getInput().getCompartment() ).filter( call->isCall(call) ).without( c ).toSet();
    }
}