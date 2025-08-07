package biouml.plugins.wdl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Option;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.wdl.diagram.WDLConstants;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataElementPath;

public class WDLUtil
{

    public static boolean isOfType(String type, DiagramElement de)
    {
        return type.equals( de.getKernel().getType() );

    }
    public static boolean isTask(Node node)
    {
        return isOfType( WDLConstants.TASK_TYPE, node );
    }

    public static boolean isCall(Node node)
    {
        return isOfType( WDLConstants.CALL_TYPE, node );
    }

    public static boolean isLink(DiagramElement de)
    {
        return isOfType( WDLConstants.LINK_TYPE, de );
    }

    public static boolean isInput(Node node)
    {
        return isOfType( WDLConstants.INPUT_TYPE, node );
    }

    public static boolean isExternalParameter(Node node)
    {
        return isOfType( WDLConstants.EXTERNAL_PARAMETER_TYPE, node );
    }

    public static boolean isExpression(Node node)
    {
        return isOfType( WDLConstants.EXPRESSION_TYPE, node );
    }

    public static boolean isCycleVariable(Node node)
    {
        return isOfType( WDLConstants.SCATTER_VARIABLE_TYPE, node );
    }

    public static boolean isCycle(Node node)
    {
        return isOfType( WDLConstants.SCATTER_TYPE, node );
    }

    public static boolean isOutput(Node node)
    {
        return isOfType( WDLConstants.OUTPUT_TYPE, node );
    }

    public static boolean isExternalOutput(Node node)
    {
        for( Edge e : node.getEdges() )
        {
            if( e.getInput().equals( node ) )
                return false;
        }
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
        List<Node> preliminary = c.stream( Node.class ).filter( n -> isInput( n ) ).toList();
        Node[] result = new Node[preliminary.size()];
        for (Node node: preliminary)
        {
            Object posObj = node.getAttributes().getValue( WDLConstants.POSITION_ATTR );
            if (posObj instanceof Integer)
                result[(Integer)posObj] = node; 
        }
        return StreamEx.of(result).toList();
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

    public static String getShortDeclaration(Node n)
    {
        return getType( n ) + " " + getName( n );
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

    public static String getCallName(Node n)
    {
        return n.getAttributes().getValueAsString( WDLConstants.CALL_NAME_ATTR );
    }

    public static void setCallName(Node n, String name)
    {
        n.getAttributes().add( new DynamicProperty( WDLConstants.CALL_NAME_ATTR, String.class, name ) );
    }

    public static ImportProperties[] getImports(Diagram diagram)
    {
        DynamicProperty dp = diagram.getAttributes().getProperty( WDLConstants.IMPORTS_ATTR );
        if( dp == null || ! ( dp.getValue() instanceof ImportProperties[] ) )
            return new ImportProperties[0];
        return (ImportProperties[])dp.getValue();
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

    public static void setBeforeCommand(Compartment compartment, Declaration[] beforeCommand)
    {
        compartment.getAttributes().add( new DynamicProperty( WDLConstants.BEFORE_COMMAND_ATTR, Declaration[].class, beforeCommand ) );
    }
    public static Object getBeforeCommand(Compartment c)
    {
        return c.getAttributes().getValue( WDLConstants.BEFORE_COMMAND_ATTR );
    }

    public static void setTaskRef(Compartment c, String ref)
    {
        c.getAttributes().add( new DynamicProperty( WDLConstants.TASK_REF_ATTR, String.class, ref ) );
    }
    public static String getTaskRef(Compartment c)
    {
        return c.getAttributes().getValueAsString( WDLConstants.TASK_REF_ATTR );
    }

    public static void setDiagramRef(Compartment c, String ref)
    {
        c.getAttributes().add( new DynamicProperty( WDLConstants.EXTERNAL_DIAGRAM, String.class, ref ) );
    }
    public static String getDiagramRef(Compartment c)
    {
        return c.getAttributes().getValueAsString( WDLConstants.EXTERNAL_DIAGRAM );
    }

    public static void setExternalDiagramAlias(Compartment c, String ref)
    {
        c.getAttributes().add( new DynamicProperty( WDLConstants.EXTERNAL_DIAGRAM_ALIAS_ATTR, String.class, ref ) );
    }
    public static String getExternalDiagramAlias(Compartment c)
    {
        return c.getAttributes().getValueAsString( WDLConstants.EXTERNAL_DIAGRAM_ALIAS_ATTR );
    }

    public static Compartment findCall(String taskName, Diagram diagram)
    {
        return diagram.recursiveStream().select( Compartment.class ).filter( c -> isCall( c ) && getCallName( c ).equals( taskName ) )
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
            if( callNode == null )
                return null;
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

    public static Node getCycleNode(Compartment c)
    {
        for( Node node : c.getNodes() )
        {
            if( isCycleVariable( node ) )
            {
                Node arrayNode = getSource( node );
                if( arrayNode != null )
                    return arrayNode;
            }
        }
        return null;
    }

    public static Node getSource(Node node)
    {
        return node.edges().filter( e -> e.getOutput().equals( node ) ).map( e -> e.getInput() ).findAny().orElse( null );
    }

    public static String getCycleName(Compartment c)
    {
        Node cycleNode = getCycleNode( c );
        return cycleNode == null ? null : getName( cycleNode );
    }

    public static List<Node> orderCallsScatters(Compartment compartment)
    {
        List<Node> result = new ArrayList<>();
        Map<Node, Set<Node>> previousSteps = new HashMap<>();
        for( Node c : compartment.stream( Node.class ).filter( c -> isCall( c ) || isCycle( c ) || isExpression( c ) ) )
            previousSteps.put( c, getPreviousSteps( c, compartment ) );

        Set<Node> added = new HashSet<>();
        while( previousSteps.size() > 0 )
        {
            for( Node key : previousSteps.keySet() )
            {
                Set<Node> steps = previousSteps.get( key );
                steps.removeAll( added );
                if( steps.isEmpty() )
                {
                    result.add( key );
                    added.add( key );
                }
            }
            for( Node c : added )
                previousSteps.remove( c );
        }
        return result;
    }

    public static Set<Node> getPreviousSteps(Node n, Compartment threshold)
    {
        return getEdges( n ).map( e -> getCallOrCycle( e.getInput(), threshold ) ).without( n ).nonNull().toSet();
    }

    private static StreamEx<Edge> getEdges(Node node)
    {
        Set<Edge> result = node.edges().toSet();
        if( node instanceof Compartment )
        {
            Compartment c = (Compartment)node;
            result.addAll( c.recursiveStream().select( Node.class ).flatMap( n -> n.edges() )
                    .filter( e -> !isInside( e.getInput(), c ) && isInside( e.getOutput(), c ) ).toSet() );

        }
        return StreamEx.of( result );
    }

    private static Node getCallOrCycle(Node node, Compartment threshold)
    {
        if( !isInside( node, threshold ) )
            return null;

        if( isExpression( node ) )
            return node;//todo: expression inside workflows!

        Compartment c = node.getCompartment();
        LinkedList<Compartment> parents = new LinkedList<>();
        while( !threshold.equals( c ) )
        {
            parents.add( c );
            c = c.getCompartment();
        }

        while( !parents.isEmpty() )
        {
            Compartment lastParent = parents.pollLast();
            if( isCycle( lastParent ) || isCall( lastParent ) )
                return lastParent;
        }
        return null;
    }

    private static boolean isInside(Node node, Compartment c)
    {
        Compartment parent = node.getCompartment();
        while( true )
        {
            if( parent.equals( c ) )
                return true;
            else if( parent instanceof Diagram )
                return false;
            parent = parent.getCompartment();
        }
    }

    public static class ImportProperties extends Option
    {
        private DataElementPath source;
        private String alias;

        public ImportProperties()
        {

        }

        public ImportProperties(DataElementPath source, String alias)
        {
            this.alias = alias;
            this.source = source;
        }
        public DataElementPath getSource()
        {
            return source;
        }
        public String getSourceName()
        {
            return source.getName();
        }
        public void setSource(DataElementPath source)
        {
            this.source = source;
        }
        public String getAlias()
        {
            return alias;
        }
        public void setAlias(String alias)
        {
            this.alias = alias;
        }
    }

    public static class ImportPropertiesBeanInfo extends BeanInfoEx
    {
        public ImportPropertiesBeanInfo()
        {
            super( ImportProperties.class );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add( "source" );
            add( "alias" );
        }
    }

    public static void addImport(Diagram diagram, Diagram source, String alias)
    {
        DynamicProperty dp = diagram.getAttributes().getProperty( WDLConstants.IMPORTS_ATTR );
        if( dp == null )
        {
            dp = new DynamicProperty( WDLConstants.IMPORTS_ATTR, ImportProperties[].class, new ImportProperties[0] );
            diagram.getAttributes().add( dp );
        }
        ImportProperties[] value = (ImportProperties[])dp.getValue();
        
        for( ImportProperties ip : value )
        {
            if( ip.alias.equals( alias ) && ip.source.toString().equals( source.getCompletePath().toString() ) )
                return;
        }
        ImportProperties[] newValue = new ImportProperties[value.length + 1];
        System.arraycopy( value, 0, newValue, 0, value.length );
        newValue[value.length] = new ImportProperties( source.getCompletePath(), alias );
        dp.setValue( newValue );
    }

    public static String getAlias(Compartment call)
    {
        DynamicProperty dp = call.getAttributes().getProperty( WDLConstants.CALL_NAME_ATTR );
        if( dp == null )
            return null;
        return dp.getValue().toString();
    }

    public static String findExpression(String variable, Compartment process)
    {
        Object beforeCommand = getBeforeCommand( process );
        if( beforeCommand instanceof Declaration[] )
        {
            for( Declaration declaration : (Declaration[])beforeCommand )
            {
                if( declaration.getName().equals( variable ) )
                    return declaration.getExpression();
            }
        }
        return null;
    }

    public static List<String> findVariables(String input)
    {
        List<String> matches = new ArrayList<>();
        // Regex to match ~{...} including braces, non-greedy inside
        Pattern pattern = Pattern.compile( "~\\{([^}]+)\\}" );
        Matcher matcher = pattern.matcher( input );
        while( matcher.find() )
            matches.add( matcher.group( 1 ) );
        return matches;
    }
}