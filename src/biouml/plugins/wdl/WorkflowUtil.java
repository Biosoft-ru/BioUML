package biouml.plugins.wdl;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.wdl.diagram.WDLConstants;
import biouml.plugins.wdl.model.ExpressionInfo;
import biouml.plugins.wdl.model.MetaInfo;
import biouml.plugins.wdl.nextflow.NextFlowGenerator;
import biouml.plugins.wdl.parser.AstMeta;
import one.util.streamex.StreamEx;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.TextDataElement;
import ru.biosoft.access.generic.GenericDataCollection;

public class WorkflowUtil
{

    public static boolean isOfType(String type, DiagramElement de)
    {
        return type.equals( de.getKernel().getType() );

    }
    public static boolean isTask(Node node)
    {
        return isOfType( WDLConstants.TASK_TYPE, node );
    }

    public static boolean isStruct(Node node)
    {
        return isOfType( WDLConstants.STRUCT_TYPE, node );
    }

    public static boolean isCall(Node node)
    {
        return isOfType( WDLConstants.CALL_TYPE, node );
    }

    public static boolean isConditional(Node node)
    {
        return isOfType( WDLConstants.CONDITIONAL_TYPE, node );
    }

    public static boolean isCondition(Node node)
    {
        return isOfType( WDLConstants.CONDITION_TYPE, node );
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
        return isOfType( WDLConstants.WORKFLOW_INPUT_TYPE, node );
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
        return isOfType( WDLConstants.WORKFLOW_OUTPUT_TYPE, node );
    }

    public static List<Compartment> getTasks(Compartment c)
    {
        return c.stream( Compartment.class ).filter( n -> isTask( n ) ).toList();
    }
    public static List<Node> getStructs(Compartment c)
    {
        return c.stream( Node.class ).filter( n -> isStruct( n ) ).toList();
    }

    public static List<Compartment> getCycles(Compartment c)
    {
        return c.stream( Compartment.class ).filter( n -> isCycle( n ) ).toList();
    }

    public static Compartment getParentCycle(Node c)
    {
        Compartment parent = c.getCompartment();
        while( parent != null )
        {
            if( isCycle( parent ) )
                return parent;
            parent = parent.getCompartment();
        }
        return null;
    }

    public static List<Compartment> getParentCycles(Node c)
    {
        List<Compartment> result = new ArrayList<>();
        Compartment parent = c.getCompartment();
        while( ! ( parent instanceof Diagram ) )
        {
            if( isCycle( parent ) )
                result.add( parent );
            parent = parent.getCompartment();
        }
        return result;
    }

    public static List<Node> getExternalParameters(Diagram diagram)
    {
        return diagram.stream( Node.class ).filter( n -> isExternalParameter( n ) ).sorted( new PositionComparator() ).toList();
    }

    public static class PositionComparator implements Comparator<Node>
    {
        @Override
        public int compare(Node o1, Node o2)
        {
            int p1 = getPosition( o1 );
            int p2 = getPosition( o2 );
            return p1 > p2 ? 1 : p1 < p2 ? -1 : 0;
        }
    }

    public static Node getTarget(Node input)
    {
        return input.edges().map( e -> e.getOutput() ).findAny().orElse( null );
    }

    public static List<Compartment> getCalls(Compartment c)
    {
        return c.stream( Compartment.class ).filter( n -> isCall( n ) ).toList();
    }

    public static List<Node> getExpressions(Compartment c)
    {
        return c.stream( Node.class ).filter( n -> isExpression( n ) ).toList();
    }

    public static List<Compartment> getAllCalls(Compartment c)
    {
        return c.recursiveStream().select( Compartment.class ).filter( n -> isCall( n ) ).toList();
    }

    public static List<Node> getInputs(Compartment c)
    {
        return c.stream( Node.class ).filter( n -> isInput( n ) ).toList();
    }

    public static List<Node> getOrderedInputs(Compartment c)
    {
        List<Node> preliminary = getInputs( c );
        Node[] result = new Node[preliminary.size()];
        for( Node node : preliminary )
        {
            Object posObj = node.getAttributes().getValue( WDLConstants.POSITION_ATTR );
            if( posObj instanceof Integer )
                result[(Integer)posObj] = node;
        }
        for( Node n : result )
        {
            if( n == null )
                System.out.println( "" );
        }
        return StreamEx.of( result ).toList();
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
        setMapAttribute( c, WDLConstants.RUNTIME_ATTR, runtime );
    }

    public static Map<String, String> getMeta(Compartment c)
    {
        Map<String, String> result = new HashMap<>();
        Object val = c.getAttributes().getValue( WDLConstants.META_ATTR );
        if( val instanceof String[] )
        {
            String[] array = (String[])val;
            for( String s : array )
            {
                String[] split = s.split( "#" );
                result.put( split[0], split[1] );
            }
        }
        return result;
    }

    public static void setMeta(Compartment c, MetaInfo meta)
    {
        String name = meta.getName();
        String attr = name.equals( "meta" ) ? WDLConstants.META_ATTR : WDLConstants.PARAMETER_META_ATTR;
        setMapAttribute( c, attr, meta.getValues() );
    }

    public static void setMeta(Compartment c, AstMeta meta)
    {
        String name = meta.getName();
        String attr = name.equals( "meta" ) ? WDLConstants.META_ATTR : WDLConstants.PARAMETER_META_ATTR;
        setMapAttribute( c, attr, meta.getMetaValues() );
    }

    public static void setMapAttribute(Compartment c, String attributeName, Map<String, String> values)
    {
        String[] vals = values.entrySet().stream().map( e -> (String) ( e.getKey() + "#" + toWDL( e.getValue() ) ) )
                .toArray( String[]::new );
        c.getAttributes().add( new DynamicProperty( attributeName, String[].class, vals ) );
    }

    public static Map<String, String> getParameterMeta(Compartment c)
    {
        Map<String, String> result = new HashMap<>();
        Object val = c.getAttributes().getValue( WDLConstants.PARAMETER_META_ATTR );
        if( val instanceof String[] )
        {
            String[] array = (String[])val;
            for( String s : array )
            {
                String[] split = s.split( "#" );
                result.put( split[0], split[1] );
            }
        }
        return result;
    }

    public static String getShortDeclaration(Node n)
    {
        return getType( n ) + " " + getName( n );
    }
    public static String getDeclaration(Node n)
    {
        return getType( n ) + " " + getName( n ) + " = " + getExpression( n );
    }
    public static String[] getArguments(Node n)
    {
        Object value = n.getAttributes().getValue( WDLConstants.ARGUMENTS_ATTR );
        if( value instanceof String[] )
            return (String[])value;
        return null;
    }
    public static void setArguments(Node n, Set<String> args)
    {
        n.getAttributes()
                .add( new DynamicProperty( WDLConstants.ARGUMENTS_ATTR, String[].class, StreamEx.of( args ).toArray( String[]::new ) ) );
    }
    public static String getExpression(Node n)
    {
        return n.getAttributes().getValueAsString( WDLConstants.EXPRESSION_ATTR );
    }
    public static void setExpression(Node n, String expression)
    {
        n.getAttributes().add( new DynamicProperty( WDLConstants.EXPRESSION_ATTR, String.class, expression ) );
    }
    public static Integer getPosition(Node n)
    {
        Object val = n.getAttributes().getValue( WDLConstants.POSITION_ATTR );
        if( val instanceof Integer )
            return (Integer)val;
        return -1;
    }
    public static void setPosition(Node n, int expression)
    {
        n.getAttributes().add( new DynamicProperty( WDLConstants.POSITION_ATTR, Integer.class, expression ) );
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

    public static Node findConditionNode(Compartment conditional)
    {
        return conditional.edges().map( e -> e.getOtherEnd( conditional ) ).findAny( n -> isCondition( n ) ).orElse( null );
    }

    public static String findCondition(Compartment conditional)
    {
        Node condition = conditional.edges().map( e -> e.getOtherEnd( conditional ) )
                .findAny( n -> isOfType( WDLConstants.CONDITION_TYPE, n ) ).orElse( null );
        if( condition == null )
            return "true";
        else
            return getExpression( condition );
    }

    public static void setCallName(Node n, String name)
    {
        n.getAttributes().add( new DynamicProperty( WDLConstants.CALL_NAME_ATTR, String.class, name ) );
    }

    public static List<ImportProperties> getImports(Diagram diagram)
    {
        return gatherImports( diagram );
        //        DynamicProperty dp = diagram.getAttributes().getProperty( WDLConstants.IMPORTS_ATTR );
        //        if( dp == null || ! ( dp.getValue() instanceof ImportProperties[] ) )
        //            return new ImportProperties[0];
        //        return (ImportProperties[])dp.getValue();
    }

    public static List<ImportProperties> gatherImports(Diagram diagram)
    {
        Map<String, ImportProperties> result = new HashMap<>();

        for( Compartment call : diagram.recursiveStream().select( Compartment.class ).filter( n -> isCall( n ) ) )
        {
            String alias = getExternalDiagramAlias( call );
            String diagramName = getDiagramRef( call );
            if( diagramName != null )
            {
                ImportProperties ip = new ImportProperties( diagramName, alias );
                result.put( diagramName, ip );
            }
        }
        return StreamEx.of( result.values() ).toList();
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

    public static void addBeforeCommand(Compartment compartment, ExpressionInfo dec)
    {
        ExpressionInfo[] newValue = null;
        Object before = getBeforeCommand( compartment );
        if( before instanceof ExpressionInfo[] )
        {
            ExpressionInfo[] oldValue = (ExpressionInfo[])before;
            newValue = new ExpressionInfo[oldValue.length + 1];
            System.arraycopy( oldValue, 0, newValue, 0, oldValue.length );
            newValue[oldValue.length] = dec;
        }
        else
        {
            newValue = new ExpressionInfo[] {dec};
        }
        setBeforeCommand( compartment, newValue );
    }

    public static void setBeforeCommand(Compartment compartment, ExpressionInfo[] beforeCommand)
    {
        compartment.getAttributes().add( new DynamicProperty( WDLConstants.BEFORE_COMMAND_ATTR, ExpressionInfo[].class, beforeCommand ) );
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

    public static List<Compartment> findCalls(String taskName, Diagram diagram)
    {
        return diagram.recursiveStream().select( Compartment.class ).filter( c -> isCall( c ) && getCallName( c ).equals( taskName ) )
                .toList();
    }

    public static List<Node> findSources(String arg, Diagram diagram)
    {
        List<Node> sources = new ArrayList<>();
        if( arg.contains( "." ) )
        {
            String first = arg.split( "\\." )[0];
            String second = arg.split( "\\." )[1];

            if( first.equals( "params" ) )
            {
                for (Node source: WorkflowUtil.findExpressionNodes( diagram, arg ))
                    sources.add( source );
                return sources;
            }
            
            List<Compartment> calls = WorkflowUtil.findCalls( first, diagram );
            for( Compartment call : calls )
            {
                Node source = call.stream( Node.class ).filter( n -> WorkflowUtil.isOutput( n ) && second.equals( WorkflowUtil.getName( n ) ) )
                        .findAny().orElse( null );
                if( source != null )
                    sources.add( source );
            }
            for ( Node source : WorkflowUtil.findExpressionNodes( diagram, first ))
                sources.add( source );
        }
        else
        {
            List<Compartment> calls = WorkflowUtil.findCalls( arg, diagram );
            for( Compartment call : calls )
            {
                Node source = call.stream( Node.class ).filter( n -> WorkflowUtil.isOutput( n ) ).findAny().orElse( null );
                if( source != null )
                    sources.add( source );
            }

            for ( Node source :  WorkflowUtil.findExpressionNodes( diagram, arg ))
                sources.add( source );
        }
        return sources;
    }

    /** 
     * Finds input node inside this task or call by its variable name
     */
    public static Node findInput(String name, Compartment parent)
    {
        return parent.stream( Node.class ).filter( n -> isInput( n ) && getName( n ).equals( name ) ).findAny().orElse( null );
    }

    public static List<String> findPossibleArguments(String input)
    {
        List<String> matches = new ArrayList<>();
        Pattern pattern = Pattern.compile( "[A-Za-z][A-Za-z0-9_.]*" );
        Matcher matcher = pattern.matcher( input );
        while( matcher.find() )
        {
            matches.add( matcher.group() );
        }
        List<String> paramsModified = new ArrayList<>();
        for( String match : matches )
        {
            if( match.startsWith( "params." ) )//TODO: fix this hack
                paramsModified.add( match.substring( 7 ) );

            if( match.endsWith( ".out" ) )
            {
                paramsModified.add( match.substring( 0, match.lastIndexOf( "." ) ) );
            }
            else if( match.contains( ".out." ) )
            {
                paramsModified.add( match.substring( 0, match.indexOf( "." ) ) );
            }
        }

        matches.addAll( paramsModified );
        return matches;
    }

    public static Set<Node> findExpressionNodes(Diagram diagram, String name)
    {
//        if( name.contains( "." ) )
//        {
//            String[] parts = name.split( "\\." );
//            String call = parts[0];
//            String varName = parts[1];
//            Compartment callNode = WorkflowUtil.findCall( call, diagram );
//            if( callNode == null )
//                return null;
//            Node port = callNode.stream( Node.class ).filter( n -> varName.equals( getName( n ) ) ).findAny().orElse( null );
//            return port;
//        }
        return diagram.recursiveStream().select( Node.class )
                .filter( n -> ( isExternalParameter( n ) || isExpression( n ) || isCycleVariable( n ) ) )
                .filter( n -> name.equals( getName( n ) )).toSet();
    }

    public static <T> List<T> findChild(biouml.plugins.wdl.parser.Node node, Class<T> c)
    {
        List<T> result = new ArrayList<>();
        for( int i = 0; i < node.jjtGetNumChildren(); i++ )
        {
            biouml.plugins.wdl.parser.Node child = node.jjtGetChild( i );
            if( c.isInstance( child ) )
            {
                result.add( c.cast( child ) );
            }
        }
        return result;
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

    public static Node getCycleVariableNode(Compartment cycle)
    {
        for( Node node : cycle.getNodes() )
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

    /**
     * Returns node which is a source for current node expression
     * TODO: maybe in some cases there will be several sources i.e. x = y + z
     * @param node
     * @return
     */
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
        for( Node c : compartment.stream( Node.class ).filter( c -> isCall( c ) || isCycle( c ) || isExpression( c ) || isConditional( c )
                || isExternalParameter( c ) || isCycleVariable( c ) || isCondition( c ) ) )
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
        return getEdges( n ).filter( e -> isInside( e.getInput(), threshold ) ).map( e -> getCallOrCycle( e.getInput(), threshold ) )
                .without( n ).nonNull().toSet();
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

        //        if( isExpression( node ) )
        //            return node;//todo: expression inside workflows!

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
            if( isCycle( lastParent ) || isCall( lastParent ) || isConditional( lastParent ) )
                return lastParent;
        }
        return node;
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

    //    public static void addImport(Diagram diagram, Diagram source, String alias)
    //    {
    //        DynamicProperty dp = diagram.getAttributes().getProperty( WDLConstants.IMPORTS_ATTR );
    //        if( dp == null )
    //        {
    //            dp = new DynamicProperty( WDLConstants.IMPORTS_ATTR, ImportProperties[].class, new ImportProperties[0] );
    //            diagram.getAttributes().add( dp );
    //        }
    //        ImportProperties[] value = (ImportProperties[])dp.getValue();
    //
    //        for( ImportProperties ip : value )
    //        {
    //            if( ip.getAlias().equals( alias ) && ip.getSource().toString().equals( source.getCompletePath().toString()) )
    //                return;
    //        }
    //        ImportProperties[] newValue = new ImportProperties[value.length + 1];
    //        System.arraycopy( value, 0, newValue, 0, value.length );
    //        newValue[value.length] = new ImportProperties( source.getCompletePath(), alias );
    //        dp.setValue( newValue );
    //    }

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
        if( beforeCommand instanceof ExpressionInfo[] )
        {
            for( ExpressionInfo declaration : (ExpressionInfo[])beforeCommand )
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

    public static void export(DataElement de, File dir) throws Exception
    {
        if( !dir.exists() && !dir.mkdirs() )
            throw new Exception( "Failed to create directory '" + dir.getName() + "'." );
        if( de instanceof TextDataElement )
        {
            String str = ( (TextDataElement)de ).getContent();
            File exported = new File( dir, de.getName() );
            ApplicationUtils.writeString( exported, str );
        }
        else if( de instanceof Diagram )
        {
            NextFlowGenerator generator = new NextFlowGenerator( false );
            String nextFlow = generator.generate( (Diagram)de );
            File exported = new File( dir, de.getName() );
            ApplicationUtils.writeString( exported, nextFlow );
        }
        else if( de instanceof GenericDataCollection )
        {
            File exportedDir = new File( dir, de.getName() );
            exportedDir.mkdirs();
            for( Object innerDe : ( (DataCollection<?>)de ) )
                export( (DataElement)innerDe, new File( dir, de.getName() ) );
        }
        else
        {
            File exported = new File( dir, de.getName() );
            File sourceFile = DataCollectionUtils.getElementFile( de );
            ApplicationUtils.linkOrCopyFile( exported, sourceFile, null );
            //            FileExporter exporter = new FileExporter();
            //            exporter.doExport( de, exported );
        }
    }

    public static String toWDL(Object obj)
    {
        if( obj instanceof Map )
        {
            Map<String, Object> map = (Map<String, Object>)obj;
            StringBuilder builder = new StringBuilder();
            builder.append( " { " );
            boolean isCommaNeeded = false;
            for( Entry<String, Object> e : map.entrySet() )
            {
                String key = e.getKey();
                if( isCommaNeeded )
                    builder.append( ", " );
                isCommaNeeded = true;
                builder.append( key );
                builder.append( ": " );
                Object value = e.getValue();
                if( value instanceof String )
                {
                    builder.append( value );
                }
                else if( value instanceof Map )
                {
                    builder.append( toWDL( (Map<String, Object>)value ) );
                }
            }
            builder.append( " } " );
            return builder.toString();
        }
        return obj.toString();
    }

    /**
     * Removes input from task or call reordering remaining inputs
     */
    public static void removeInput(String name, Compartment c) throws Exception
    {
        Node node = findInput( name, c );
        if( node == null )
            throw new Exception( "Can not fint input " + name + " in " + c.getName() );

        c.remove( node.getName() );
        int position = getPosition( node );
        List<Node> inputs = c.stream( Node.class ).filter( n -> isInput( n ) ).toList();
        for( Node otherInput : inputs )
        {
            int otherPos = getPosition( otherInput );
            if( otherPos > position )
                setPosition( otherInput, otherPos - 1 );
        }
    }

    public static void setStructMembers(Node node, ExpressionInfo[] declarations)
    {
        node.getAttributes().add( new DynamicProperty( WDLConstants.STRUCT_MEMBERS_ATTR, ExpressionInfo[].class, declarations ) );
    }

    public static ExpressionInfo[] getStructMembers(Node node)
    {
        Object declarations = node.getAttributes().getValue( WDLConstants.STRUCT_MEMBERS_ATTR );
        if( declarations instanceof ExpressionInfo[] )
            return (ExpressionInfo[])declarations;
        return new ExpressionInfo[0];
    }

    public static boolean isDependent(Node node, Node from)
    {
        for( Node source : getSources( node ) )
        {
            if( isDependent( source, from ) )
                return true;
        }
        return false;
    }

    /**
     * Returns stream of all immediate sources of current node
     */
    public static StreamEx<Node> getSources(Node node)
    {
        return node.edges().map( e -> e.getInput() ).without( node );
    }

    public static boolean isCallResult(Node node)
    {
        return getSources( node ).anyMatch( n -> isCall( n.getCompartment() ) );
    }

}