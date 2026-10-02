package biouml.plugins.wdl.nextflow;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.plugins.wdl.WorkflowUtil;
import biouml.plugins.wdl.WorkflowVelocityHelper;
import biouml.plugins.wdl.diagram.WDLConstants;
import biouml.plugins.wdl.model.CommandInfo;
import biouml.plugins.wdl.model.ExpressionInfo;
import one.util.streamex.StreamEx;

public class NextFlowVelocityHelper extends WorkflowVelocityHelper
{
    private boolean isEntryScript = true;
    private NextflowSettings settings;
    private String resultPath = "";

    public NextFlowVelocityHelper(Diagram diagram)
    {
        super( diagram );
    }

    public NextFlowVelocityHelper(Diagram diagram, boolean isEntryScript)
    {
        super( diagram );
        this.isEntryScript = isEntryScript;
    }
    
    public void setSettings(NextflowSettings settings)
    {
        this.settings = settings;
    } 
    
    public String printTaskInput(Node n)
    {
        if( n == null )
            return "??";
        String result = getType( n ) + " " + getName( n );

        String path = null;
        if( "path".equals( getType( n ) ) )
        {
            Compartment call = findCallByTask( n.getCompartment() );
            if( call != null )
            {
                Node callInput = call.stream( Node.class ).findAny( i -> getName( n ).equals( WorkflowUtil.getName( i ) ) ).orElse( null );
                Node source = WorkflowUtil.getSource( callInput );
                if( source != null )
                {
                    String expression = WorkflowUtil.getExpression( source );

                    Compartment sourceParent = source.getCompartment();
                    if( WorkflowUtil.isCall( sourceParent ) )
                        path = WorkflowUtil.getCallName( sourceParent );
                    if( expression != null && expression.contains( "/" ) )
                        path = path + "/" + expression.substring( 1, expression.lastIndexOf( "/" ) );
                }
            }
        }
        if( getType( n ).equals( "path" ) )
        {
            if( WorkflowUtil.getType( n ).contains( "Array" ) ) //WDL type
                result = result + ", stageAs: 'inputs/file??/*'";
            else if (path != null)
                result = result + ", stageAs: 'inputs/"+path+"/"+getName( n )+"_*'";
            else              
                result = result + ", stageAs: 'inputs/*'";
        }
        return result;
    }


    public Node getCallByOutput(Node node)
    {
        return node.edges().map( e -> e.getOtherEnd( node ) ).findAny( n -> WorkflowUtil.isCall( n ) ).orElse( null );
    }

    @Override
    public String getCommand(Compartment c)
    {
        String command = super.getCommand( c );
        if( command == null )
            return "";
        return command;
    }

    @Override
    public String getExpression(Node n)
    {
        String expression = super.getExpression( n );
        if( expression == null )
            return null;
        return expression;
    }

    @Override
    public String getType(Node n)
    {
        return getNextFlowType( super.getType( n ) );
    }

    /**
     * Transforms WDL type to Nextflow type
     */
    private static String getNextFlowType(String wdlType)
    {
        switch( wdlType )
        {
            case "File":
            case "File?":
            case "Array[File]":
            case "Array[File]?":
            case "Directory":
            case "Directory?":
                return "path";
            case "tuple":
                return "tuple";
            case "path":
                return "path";
            default:
                return "val";
        }
    }

    public String getCommandType(Compartment task)
    {
        return WorkflowUtil.getCommandType( task );
    }

    public boolean commandNeedsQuotes(String type)
    {
        return !type.equals( CommandInfo.TYPE_EXEC );
    }

    public String getExternalInput(Node n)
    {
        if( n == null )
            return "??";

        StringBuilder result = new StringBuilder();
        result.append( "params." + getName( n ) + " = " );
        String expression = getExpression( n );

        boolean isArray = WorkflowUtil.getType( n ).startsWith( "Array" );
        boolean isFile = WorkflowUtil.getType( n ).contains( "File" );
        if( expression != null && !expression.isEmpty() )
        {
            result.append( expression );
        }
        else if( isArray && isFile)
        {
            result.append( "[]" );
        }
        else
            result.append( "\"NO_VALUE\"" );
        return result.toString();
    }

    public String getVersion()
    {
        return WorkflowUtil.getVersion( diagram );
    }

    public String getCycleName(Compartment c)
    {
        Node cycleVarNode = WorkflowUtil.getCycleVariableNode( c );
        if( cycleVarNode == null )
            return null;
        Node source = WorkflowUtil.getSource( cycleVarNode );
        if( source == null )
            return null;
        return getName( source );
    }

    public String createChannelName(String input)
    {
        return input.replace( ".", "_" ) + "_ch";
    }

    public String getInputName(Node n)
    {
        Node source = WorkflowUtil.getSource( n );
        if( source != null )
            n = source;
        String name = WorkflowUtil.getName( n );
        String type = WorkflowUtil.getType( n );
        if( WorkflowUtil.isExternalParameter( n ) )
        {
            String result = name;
            if( "File".equals( type ) )
                result = "channel.fromPath(" + result + ")";
            return result;
        }
        if( WorkflowUtil.isCall( n.getCompartment() ) )
            return getResultName( n.getCompartment() ) + "." + name;
        return name;
    }


    /**
     * @return name of the result generated by call associated with compartment
     */
    public String getResultName(Compartment c)
    {
        return WorkflowUtil.getResultName( c );
    }

    public String getInputName(Compartment call)
    {
        List<Node> inputs = getOrderedInputs( call );
        if( inputs.isEmpty() )
            return "";

        if( WorkflowUtil.isCycle( call.getCompartment() ) )
            return "input_" + WorkflowUtil.getCallName( call );
        else
            return StreamEx.of( inputs ).map( n -> getFullName( WorkflowUtil.getSource( n ) ) ).joining( "," );
    }

    public String getFullName(Node node)
    {
        if( WorkflowUtil.isCall( node.getCompartment() ) )
        {
            return getResultName( node.getCompartment() ) + "." + getName( node );
        }
        return getName( node );
    }


   
//   public void printExpressionInCycle2(Node node, StringBuilder sb)
//   {
//       if ( getName(node).endsWith( "_wrapped" ) || getName(node).endsWith( "_collected" )) //this is autogenerated expression TODO: some clever marking
//       {
//           sb.append( "\n " );
//           sb.append( getName( node ) + " = " + getExpression( node ) );//simply repeat it wo changes
//           return;
//       }
//       List<Node> cycledSources = getCycledSources( node );
//
//       sb.append( "\n " );
//       if( cycledSources.size() == 0 )
//       {
//           sb.append( getName( node ) + " = " + getCallEmit( node ) );//simply repeat it wo changes
//       }
//       else if( cycledSources.size() == 1 )
//       {
//           Node cycledVar = cycledSources.get( 0 );
//           String cycledVarName = getName( cycledVar );
//           String channelName = getChannelName( cycledVar );
//           String expression = getCallEmit( node );
//           if( isCall( cycledVar.getCompartment() ) )
//               expression = expression.replace( getCallName( cycledVar.getCompartment() ) + ".out.", "" );
//           String innerName = cycledVarName+"_value";//TODO: try to create unique name
//           expression = renameVariable( expression, cycledVarName, innerName );
//           sb.append( getName( node ) + " = " + channelName + ".map { " + innerName + " -> " + expression + " }" );
//       }
//       else
//       {
//           List<Compartment> allCycles = WorkflowUtil.getParentCycles( node ).reversed();
//           List<Node> allNodes = new ArrayList<>();
//           sb.append( getName( node ) + " = " );
//           String expression = getCallEmit( node );
//           Map<String, List<Node>> cycledGroups = new HashMap<>();
//           for( int i = 0; i < cycledSources.size(); i++ )
//           {
//               Node cycledVar = cycledSources.get( i );
//               Compartment parentCycle = WorkflowUtil.getParentCycle( cycledVar );
//               cycledGroups.computeIfAbsent( parentCycle.getName(), k -> new ArrayList() ).add( cycledVar );
//
//               if( isCall( cycledVar.getCompartment() ) )//TODO: move somewhere
//                   expression = expression.replace( getCallName( cycledVar.getCompartment() ) + ".",
//                           getCallName( cycledVar.getCompartment() ) + "_" );
//           }
//
//           List<String> merged = new ArrayList<String>();
//           for( Compartment parentCycle : allCycles )
//           {
//               List<Node> nodes = cycledGroups.get( parentCycle.getName() );
//               allNodes.addAll( nodes );
//               merged.add( getChannelName( nodes.get( 0 ), false )
//                       + StreamEx.of( nodes ).skip( 1 ).map( n -> ".merge( " + getChannelName( n, false) + " )" ).joining() );
//           }
//           for( Node index : StreamEx.of( allNodes ) )
//               expression = renameVariable( expression, getName( index ), getName( index ) + "_value" );
//
////           sb.append( merged.get( 0 ) + StreamEx.of( merged ).skip( 1 ).map( n -> ".combine(" + n + ")" ).joining() );
//           sb.append( "combineAll(["+StreamEx.of(merged).joining(", ")+"])" );
//           sb.append( ".map { " );
//           sb.append( StreamEx.of( allNodes ).map( s -> getName( s )+"_value" ).joining( "," ) );
//           sb.append( " -> " );
//           sb.append( expression );
//           sb.append( " }" );
//       }
//   }
//   
// 
//    /**
//     * Creates channel for given input
//     * If input directly  depends on cycled variable it is added to channel
//     * If input directly depends on call inside cycle it is added to channel
//     * If input does not depend on anything from parent cycle - cycle variable is added to channel
//     * E.g.
//     * for (i...) 
//     * { 
//     *    result1 = call1( i )
//     *    for (j...) 
//     *    { 
//     *       result2 = call2( j + 2 )
//     *       for ( k...) 
//     *       {
//     *          result3 = call3(  result2 )
//     *       }
//     *    }
//     * }
//     * Will be translated to 
//     * 
//     * i_ch = toChannel( i )
//     * j_ch = toChannel( j )
//     * k_ch = toChannel( k )
//     * result1 = call1( i_ch )
//     * result2 = call2( i_ch.combine( j_ch ).map{ i,j -> j+2 )
//     * result3 = call3 (  result2.combine( k ).map {  result1, result2, k ->  i + result1 + result2 }
//     * TODO: add merge
//     */
//    private String printInputInCycle2(List<Compartment> parentCycles, List<Compartment> parentIfs, Node input)
//    {
//        StringBuilder sb = new StringBuilder();
//        String expression = getExpression( input );
//        List<Node> cycledSources = getCycledSources( input );
//        Map<Compartment, List<Node>> cycleToSources = new HashMap<>(); //cycle to all nodes in cycle from which input depends
//        for( Compartment parentCycle : parentCycles )
//            cycleToSources.put( parentCycle, new ArrayList<>() );
//
//        if( cycledSources.size() == 1 )
//        {
//            Node cycledSource = cycledSources.get( 0 );
//            List<Node> sources = WorkflowUtil.getSources( input ).toList();
//            Node otherSource = null;
//            if( sources.size() <= 2 )//sometimes edge is missing TODO: fix
//            {
//                otherSource = sources.size() == 2 ? StreamEx.of( sources ).without( cycledSource ).findAny().orElse( null )
//                        : sources.get( 0 );
//                Compartment cycle = WorkflowUtil.getParentCycle( input );
//                String sycledName = WorkflowUtil.getCycleVariable( cycle );
//                if( isCall( otherSource.getCompartment() ) )
//                {
//                    //Special case: we iterate through call output 
//                    String qualified = getCallName( otherSource.getCompartment() ) + "." + getName( otherSource );
//                    if( expression.equals( qualified + '[' + sycledName + ']' ) )
//                        return "result_" + qualified;
//                }
//                else if( WorkflowUtil.isExternalParameter( otherSource ) )
//                {
//                    if( expression.equals( getName( otherSource ) + '[' + sycledName + ']' ) )
//                    {
//                        return "toChannel(" + getName( otherSource ) + ")";
//                    }
//                }
//            }
//        }
//        else if( cycledSources.size() == 2 ) //special case: we iterate through call result, note: fancy indexing is not allowed
//        {
//            Node input1 = cycledSources.get( 0 );
//            Node input2 = cycledSources.get( 1 );
//            boolean isCallResult1 = isCall( input1.getCompartment() );
//            boolean isCallResult2 = isCall( input2.getCompartment() );
//            if( isCallResult1 != isCallResult2 )
//            {
//                Node callResult = isCallResult1 ? input1 : input2;
//                Compartment cycle = isCallResult1 ? WorkflowUtil.getParentCycle( input2 ) : WorkflowUtil.getParentCycle( input1 );
//                String cycleVariable = WorkflowUtil.getCycleVariable( cycle );
//                String callName = WorkflowUtil.getCallName( callResult.getCompartment() );
//                String qualified = callName + "." + WorkflowUtil.getName( callResult ) + "[" + cycleVariable + "]";
//                if( expression.replace( " ", "" ).equals( qualified ) )
//                    return callName + ".out." + WorkflowUtil.getName( callResult );
//            }
//        }
//        Set<Compartment> calls = new HashSet<Compartment>();
//        for( Node cycledSource : cycledSources )
//        {
//            Compartment cycle = WorkflowUtil.getParentCycle( cycledSource );
//            Compartment parent = cycledSource.getCompartment();
//            if( isCall( parent ) )
//                calls.add( parent );
//
//            cycleToSources.computeIfAbsent( cycle, k -> new ArrayList<>() ).add( cycledSource );
//        }
//
//        List<String> merged = new ArrayList<String>();
//        List<Node> indexNodes = new ArrayList<>();
//        for( Compartment cycle : parentCycles )
//        {
//            List<Node> toMerge = cycleToSources.get( cycle );
//            if( toMerge.isEmpty() )
//            {
//                toMerge.add( WorkflowUtil.getCycleVariableNode( cycle ) );
//            }
//
//            List<String> channels = StreamEx.of( toMerge ).map( n -> getChannelName( n, false ) ).toList();
//            merged.add( join( channels, ".merge( ", " )" ) );
//            indexNodes.addAll( toMerge );
//        }
//
//        sb.append( merged.size() == 1 ? "toChannel( " + merged.get( 0 ) + " )"
//                : StreamEx.of( merged ).joining( ", ", "combineAll( [ ", " ] )" ) );
//
//        for( Compartment call : calls )
//        {
//            for( Node indexNode : indexNodes )
//            {
//
//                Compartment compartment = indexNode.getCompartment();
//                if( WorkflowUtil.isCall( compartment ) )
//                {
//                    String name = WorkflowUtil.getCallName( compartment );
//                    if( name != null && name.equals( WorkflowUtil.getCallName( call ) ) )
//                        expression = replaceCallPrefix( expression, call );
//                }
//            }
//        }
//        
//        String condition = StreamEx.of( parentIfs ).map( parentIf -> WorkflowUtil.findCondition( parentIf ) ).joining("&&");        
//
//        List<String> indexesList = new ArrayList<>();
//        for (Node indexNode: indexNodes)
//        {
//            String s = withCallPrefix( indexNode ) ;
//            String oldName = WorkflowUtil.getName(indexNode);
//            String newName = oldName+"_value";
//            s = renameVariable(s, oldName, newName);
//            indexesList.add( s );
//            expression = renameVariable(expression, oldName, newName);
//            condition = renameVariable(condition, oldName, newName);
//        }
//        
//        String indexes = StreamEx.of( indexesList ).joining( ", " );
//       
//        if (!condition.isEmpty())
//        {
//            sb.append( ".map { " + indexes + " -> " + condition +"? "+ expression+": null}");
//        }
//        else
//        {
//            if( ! ( indexes.equals( expression ) ) )
//                sb.append( ".map { " + indexes + " -> " + expression + " }" );
//        }
//        return sb.toString();
//    }
    


    public static String removeArrayAccess(String input, String callName)
    {
        String regex = Pattern.quote( callName ) + "\\[\\p{Alnum}+\\]";
        Matcher matcher = Pattern.compile( regex ).matcher( input );
        StringBuilder result = new StringBuilder();

        int lastEnd = 0;

        while( matcher.find() )
        {
            // Append text before match
            result.append( input, lastEnd, matcher.start() );
            // Append only the prefix part (without group capture)
            result.append( callName );
            lastEnd = matcher.end();
        }
        // Append remaining text
        result.append( input, lastEnd, input.length() );

        return result.toString();
    }

//    private String replaceCallPrefix(String expression, Compartment call)
//    {
//        return expression.replace( getCallName( call ) + ".", getCallName( call ) + "_" );
//    }
//
//    private String withCallPrefix(Node node)
//    {
//        if( isCall( node.getCompartment() ) )
//            return getCallName( node.getCompartment() ) + "_" + getName( node );
//        return getName( node );
//    }

    public String join(List<String> strings, String prefix, String suffix)
    {
        return strings.get( 0 ) + StreamEx.of( strings ).skip( 1 ).map( n -> prefix + n + suffix ).joining();
    }

    public String getCallInputName(Node input)
    {
        Compartment call = input.getCompartment();
        String type = WorkflowUtil.getType( input );
        boolean isArray = type.startsWith( "Array" );
        boolean isFile = type.contains( "File" ) || type.contains( "Directory" );
        if( !isInsideCycle( call ) || WorkflowUtil.isCallResult( input ) )
        {
            String expression = WorkflowUtil.getExpression( input );
            String result = getCallEmit( input );
            if( result == null )
                result = expression;

            List<Node> sources = getSources( input );
            for( Node source : sources )
            {
                if( isInsideCycle( source.getCompartment() ) )
                {
                    String callName = getCallName( source.getCompartment() );
                    result = result.replace( callName + "." + getName( source ), callName + "." + getName( source ) + ".collect()" );
                }
            }

            if( result == null || result.isEmpty() )
            {
                if( isArray || isFile )
                    result = "[]";
                else
                    result = "\"" + WDLConstants.NO_VALUE + "\"";
            }
            else if( result.startsWith( "[" ) && result.endsWith( "]" ) )
                result = result.substring( 1, result.length() - 1 );

            if( isInsideCycle( call ) )//TODO: probably move to preproccessing
            {
                Set<String> cycleVariables = StreamEx.of( WorkflowUtil.getParentCycles( call ) )
                        .map( cycle -> WorkflowUtil.getCycleVariable( cycle ) ).toSet();
                for( String cycleVariable : cycleVariables )
                    result = result.replace( "[" + cycleVariable + "]", "" );
            }

            if (WorkflowUtil.getDefaultValue( input ) != null)
                return "getDefault("+result+","+WorkflowUtil.getDefaultValue( input )+")";
            return result;
        }
        else
        {
            String result = getCallEmit( input );
            if( result == null )
                result = getExpression( input );
            if( result == null || result.isEmpty() )
                result = "\"" + WDLConstants.NO_VALUE + "\"";
            if( result.startsWith( "[" ) && result.endsWith( "]" ) )
                result = result.substring( 1, result.length() - 1 );
            return result;
        }
    }
    
    public String getOutputExpression(Node node)
    {
        return getCallEmit( node );
    }

    /**
     * @param node
     * @return
     */
    public String getCallEmit(Node node)
    {
        String expression = getExpression( node );
        List<Node> sources = getSources( node );
        Set<String> replaced = new HashSet<>();
        for( Node source : sources )
        {
            if( source != null && isCall( source.getCompartment() ) )
            {
                String result = getResultName( source.getCompartment() );
                String name = getCallName( source.getCompartment() );

                if (replaced.contains( name ))
                        continue;
                
                replaced.add( name );
                if( result != null )
                    expression = expression.replace( name + ".", result  + ".");
                else
                    expression = expression.replace( name + ".", name + ".out." );
            }
        }
        return expression;
    }

    public static String replace(String expr, String toReplace, String replacement)
    {
        String regex = "(?<![A-Za-z0-9_.])" + Pattern.quote( toReplace ) + "(?![A-Za-z0-9_.])";
        return expr.replaceAll( regex, replacement );
    }


    /**
     * replace all variables with 
     * e.g. i * 2 -> array.map { i->i*2}
     */
    public String processExpression(Node node)
    {
        String expression = getCallEmit( node );
        Compartment cycle = getClosestCycle( node );
        if( cycle == null )
            return expression;

        String variable = getCycleVariable( cycle );
        String name = getCycleName( cycle );
        //        if (expression.matches( name ))

        return name + ".map {" + variable + "->" + expression + " }";
    }



    public String getRuntimeProperty(Compartment process, String name)
    {
        DynamicProperty dp = process.getAttributes().getProperty( WDLConstants.RUNTIME_ATTR );
        if( dp == null || ! ( dp.getValue() instanceof String[] ) )
            return null;
        String[] options = (String[])dp.getValue();
        for( String option : options )
        {
            String[] parts = option.split( "#" );
            if( parts[0].equals( name ) )
            {
                if (name.equals( "publishDir" ))
                        return parts[1];
                return "{"+ substituteVariables( parts[1], process ) +"}";//substituteVariables( parts[1], process );
            }
        }
        return null;
    }

    private String substituteVariables(String expression, Compartment process)
    {
        Map<String, String> replacements = new HashMap<>();
        List<String> variables = WorkflowUtil.findVariables( expression );
        for( String variable : variables )
        {
            String variableExpression = WorkflowUtil.findExpression( variable, process );
            if( variableExpression != null )
            {
                replacements.put( "~{" + variable + "}", "${" + variableExpression + "}" );
            }
        }
        for( Entry<String, String> e : replacements.entrySet() )
        {
            expression = expression.replace( e.getKey(), e.getValue() );
        }
        return expression;
    }

    public String getPublishDir(Compartment process)
    {
        return getRuntimeProperty( process, "publishDir" );
    }

    public String getTag(Compartment process)
    {
        return getRuntimeProperty( process, "tag" );
    }

    public String getContainer(Compartment process)
    {
        return getRuntimeProperty( process, "docker" );
    }

    public String getCPUs(Compartment process)
    {
        return getRuntimeProperty( process, "cpu" );
    }

    public String getMemory(Compartment process)
    {
        return getRuntimeProperty( process, "memory" );
    }

    public String getMaxRetries(Compartment process)
    {
        return getRuntimeProperty( process, "maxRetries" );
    }

    public boolean shouldCollect(Compartment producer, Compartment consumer)
    {
        return true;
    }


    public String[] getMandatoryFunctions()
    {
        return new String[] {"toChannel", "get", "getDefault", "combineAll", "mergeAll","saveOutputs", "noNull", "fileOrNull", "orNull", "pair", "range",
                "stringify_wdl", "toArray", "sep_wdl", "collectScatterValues", "addValue", "addValues", "addScatter", "collectScatter", "createScatter", "calcInScatter", "condition", "addConditionalValue", "addConditionalValues", "contextValue"};
    }
    
    /**
     * Functions that should be imported from genespace_function.nf
     */
    public static String[] getWDLFunctions()
    {
        return new String[] {"defined", "basename", "sub", "length", "range", "read_int", "read_string", "read_float", "read_boolean",
                "read_lines", "read_map", "write_lines", "read_tsv", "write_tsv", "numerate", "select_first", "select_all", "quote",
                "squote", "sep", "ceil", "floor", "as_map", "keys", "zip", "round", "write_json", "prefix", "suffix", "collect_by_key",
                "size", "cross", "transpose", "unzip", "contains", "flatten", "write_map", "as_pairs", "read_json", "min", "max", "glob"};
    }

    public static String toNextflowFunction(String name, boolean inCommand)
    {
        if( inCommand && bashFunctions.contains( name ) )
            return name + "_bash";
        return name + "_wdl";
    }

    public static Set<String> bashFunctions = Set.of( "read_int", "read_string", "read_float", "read_boolean", /**"read_lines",**/ "read_tsv", "size" );//, "write_lines");

    public String getFunctions()
    {
        Set<String> result = StreamEx.of( getMandatoryFunctions() ).toSet();
        String[] funNames = getWDLFunctions();

        for( String funName : funNames )
        {
            if( isFunctionCalled( funName.trim() ) )
                result.add( funName + "_wdl" );
            if( isFunctionCalledInCommand( funName.trim() ) )
            {
                if( bashFunctions.contains( funName.trim() ) )
                    result.add( funName + "_bash" );
                else
                    result.add( funName + "_wdl" );
            }
        }
        return StreamEx.of( result ).joining( "; " );
    }

    public boolean isFunctionCalled(String funName)
    {
        for( String value : diagram.recursiveStream().select( Node.class ).map( n -> WorkflowUtil.getExpression( n ) ) )
        {
            if( value != null && value.toString().contains( funName ) )
                return true;
        }

        for( Compartment compartment : diagram.recursiveStream().select( Compartment.class ).filter( c -> WorkflowUtil.isTask( c ) ) )
        {
            Object before = WorkflowUtil.getBeforeCommand( compartment );
            if( before instanceof ExpressionInfo[] )
            {
                for( ExpressionInfo declaration : (ExpressionInfo[])before )
                {
                    if( declaration.getExpression().contains( funName ) )
                        return true;
                }
            }

        }
        return false;
    }

    public boolean isFunctionCalledInCommand(String funName)
    {
        for( Compartment compartment : diagram.recursiveStream().select( Compartment.class ).filter( c -> WorkflowUtil.isTask( c ) ) )
        {
            String command = WorkflowUtil.getCommand( compartment );
            if( command != null && command.contains( funName ) )
                return true;

        }
        return false;
    }

    public Compartment[] getImportedCalls()
    {
        return WorkflowUtil.getAllCalls( diagram ).stream().filter( c -> WorkflowUtil.getDiagramRef( c ) != null )
                .toArray( Compartment[]::new );
    }

    public String getImportedAlias(Compartment call)
    {
        return WorkflowUtil.getCallName( call );
    }

    public String writePrivateDeclaration(ExpressionInfo declaration)
    {
        String expression = declaration.getExpression();
        expression = expression.replace( "~{", "${" );
        return declaration.getName() + " = " + expression;
    }

    public Object getPrivateDeclarations(Compartment task)
    {
        return super.getBeforeCommand( task );
    }

    /**
     * @return names of all inputs for call which depends on scatter array
     */
    public Set<Node> getArrayDepenedantInputs(String cycleVar, Compartment call)
    {
        Set<Node> result = new HashSet<>();
        List<Node> inputs = WorkflowUtil.getInputs( call );
        for( Node input : inputs )
        {
            if( isArray( cycleVar, input ) )
                result.add( input );
        }
        return result;
    }

    public boolean isArray(String cycleVar, Node input)
    {
        return input.edges().map( e -> e.getInput() ).anyMatch( n -> WorkflowUtil.isCycleVariable( n ) );
    }

    public boolean isNotEmpty()
    {
        return !WorkflowUtil.getAllCalls( diagram ).isEmpty();
    }

    public boolean isEntryScript()
    {
        return isEntryScript;
    }

    public static class CallInfo
    {
        public Compartment compartment;
        public Map<String, String> inputString = new HashMap<>();

        public CallInfo(Compartment compartment)
        {
            this.compartment = compartment;
        }
    }

    public List<Compartment> getNamedWorkflows()
    {
        return WorkflowUtil.getWorkflows( diagram );
    }

    public String printDirectives(Compartment task, String offset)
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( "\n" + offset + "fair true");
        String tag = getTag( task );
        if( tag != null )
            sb.append( "\n" + offset + "tag " + tag );

        String container = getContainer( task );
        if( container != null )
            sb.append( "\n" + offset + "container " + container );

        String cpus = getCPUs( task );
        if( cpus != null )
            sb.append( "\n" + offset + "cpus " + cpus );

        String memory = getMemory( task );
        if( memory != null )
            sb.append( "\n" + offset + "memory " + memory );

        String maxRetries = getMaxRetries( task );
        if( maxRetries != null )
            sb.append( "\n" + offset + "maxRetries " + maxRetries );

//        String publishDir = getPublishDir( task );
//        if( publishDir != null )
//        {
//            sb.append( "\n" + offset + "publishDir " + publishDir );
//            if( settings != null )
//                sb.append( ", mode: '" + settings.getPublishOutput() + "', overwrite: 'true'" );
//        }

        return sb.toString();
    }

    @Override
    public String findCondition(Compartment conditional)
    {
        Node conditionPort = conditional.stream( Node.class ).findAny( n -> WorkflowUtil.isConditionalPort( n ) ).orElse( null );
        if( conditionPort == null )
            return "true";
        Node condition = conditionPort.edges().map( e -> e.getOtherEnd( conditionPort ) ).findAny( n -> WorkflowUtil.isCondition( n ) )
                .orElse( null );
        if( condition == null )
            return "true";
        return getCallEmit( condition );
    }

    public boolean isStdout(Node output)
    {
        return getExpression( output ).contains( "stdout" );
    }

    /**
     * Generates line for input for entry workflow
     */
    public String getEntryInput(Node input)
    {
        if(  WorkflowUtil.getType( input ).equals( "File?" ) )
            return "fileOrNull( params." + getName( input ) + ")";
        else if (WorkflowUtil.getType( input ).equals( "File" ) )
            return  "file( params." + getName( input ) + ")";
        if( WorkflowUtil.getType( input ).equals( "Array[File]" ) )
            return "params." + getName( input ) + ".collect { file(it) }";
        if( WorkflowUtil.getType( input ).endsWith( "?" ) )
            return "orNull( params." + getName( input ) + ")";
        return "noNull(params." + getName( input )+")";
    }

    public String generateWorkflowPublish(Diagram diagram)
    {
        String workflowName = getWorkflowName( diagram );

        boolean isWindows = System.getProperty( "os.name" ).startsWith( "Windows" );
        String outputDirArg = "\"" + getResultPath() + "\"";
        String idt = "    ";
        StringBuilder sb = new StringBuilder();
        sb.append( "saveOutputs(\n" );
        sb.append( idt + "[\n" );
        List<Node> outputs = WorkflowUtil.getExternalOutputs( diagram );
        for( int i = 0; i < outputs.size(); i++ )
        {
            Node output = outputs.get( i );
            boolean isLast = i == outputs.size() - 1;
            String outputName = getName( output );
            String channel = workflowName + ".out." + outputName;
            String channelType = "\"" +WorkflowUtil.getType( output )+ "\"";
            String channelName = "\"" + workflowName + "." + outputName + "\"";
           
            if( diagram.getAttributes().getProperty( "autoOutputs" ) != null )
            {
                Node source = WorkflowUtil.getSource( output );
                String name = getName( source );
                if (name.endsWith( "_collected" ))
                {
                    source = WorkflowUtil.getSource( source );
                }
                Compartment call = null;
                if( source != null )
                    call = source.getCompartment();
                if( WorkflowUtil.isCall( call ) )
                    channelName = "\"" + workflowName + "." + WorkflowUtil.getCallName( call ) + "." + getName( source ) + "\"";
            }
            sb.append( idt + idt + "[\n" );
            sb.append( idt + idt + idt + "channel: " + channel + ",\n" );
            sb.append( idt + idt + idt + "name: " + channelName + ",\n" );
            sb.append( idt + idt + idt + "type: " + channelType + "\n" );
            sb.append( idt + idt + "]" );
            if( !isLast )
                sb.append( "," );
            sb.append( "\n" );
        }
        sb.append( idt + "],\n" );
        sb.append( idt + outputDirArg + ",\n" );
        sb.append( idt + isWindows + "\n" );
        sb.append( ")" );

        return sb.toString();
    }
    
    public boolean isOptional(Node node)
    {
        return super.getType( node ).endsWith( "?" );
    }

    public String getResultPath()
    {
        return resultPath;
    }

    public void setResultPath(String resultPath)
    {
        this.resultPath = resultPath;
    }

    public Compartment findCallByTask(Compartment task)
    {
        return Diagram.getDiagram( task ).recursiveStream().select( Compartment.class )
                .findAny( c -> task.getName().equals( WorkflowUtil.getTaskRef( c ) ) ).orElse( null );
    }  
    
    public String printCycle(Compartment cycle)
    {
        return new ScatterProcessor(diagram).printCycle(cycle);
    }
}