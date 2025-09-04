package biouml.plugins.wdl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.plugins.wdl.WDLUtil.ImportProperties;
import biouml.plugins.wdl.diagram.WDLConstants;
import one.util.streamex.StreamEx;

public class NextFlowVelocityHelper extends WorkflowVelocityHelper
{
    private boolean isEntryScript = true;

    public NextFlowVelocityHelper(Diagram diagram)
    {
        super( diagram );
    }

    public NextFlowVelocityHelper(Diagram diagram, boolean isEntryScript)
    {
        super( diagram );
        this.isEntryScript = isEntryScript;
    }

    public List<Node> orderCalls(Compartment compartment)
    {
        return WDLUtil.orderCallsScatters( compartment );
    }

    public String getName()
    {
        return diagram.getName();
    }

    public List<Node> getExternalParameters()
    {
        return WDLUtil.getExternalParameters( diagram );
    }

    public List<Node> getExternalOutputs()
    {
        return WDLUtil.getExternalOutputs( diagram );
    }

    public Node getCallByOutput(Node node)
    {
        return node.edges().map( e -> e.getOtherEnd( node ) ).findAny( n -> WDLUtil.isCall( n ) ).orElse( null );
    }

    public List<Compartment> getTasks()
    {
        return WDLUtil.getTasks( diagram );
    }

    public List<Node> getOrderedInputs(Compartment c)
    {
        return WDLUtil.getOrderedInputs( c );
    }

    public List<Node> getOutputs(Compartment c)
    {
        return WDLUtil.getOutputs( c );
    }

    public String getCommand(Compartment c)
    {
        String command = WDLUtil.getCommand( c );
        command = command.replace( "$", "\\$" );
        command = command.replace( "~{", "${" );
        return command;
    }

    public Map<String, String> getRequirements(Compartment c)
    {
        return WDLUtil.getRequirements( c );
    }

    public Map<String, String> getHints(Compartment c)
    {
        return WDLUtil.getHints( c );
    }

    public Map<String, String> getRuntime(Compartment c)
    {
        return WDLUtil.getRuntime( c );
    }

    public String getExpression(Node n)
    {
        String expression = WDLUtil.getExpression( n );
        if( expression == null )
            return null;
        expression = expression.replace( "~{", "${" );
        return expression;
    }

    public String getType(Node n)
    {
        return getNextFlowType( WDLUtil.getType( n ) );
    }

    public static Node getSource(Node node)
    {
        return WDLUtil.getSource( node );
    }

    public String getName(Node n)
    {
        String name = WDLUtil.getName( n );
        return name;
    }

    public String getDeclaration(Node n)
    {
        if( n == null )
            return "??";
        if( getExpression( n ) != null && !getExpression( n ).isEmpty() )
            return getType( n ) + " " + getName( n ) + " = " + getExpression( n );
        return getType( n ) + " " + getName( n );
    }

    public String getShortDeclaration(Node n)
    {
        if( n == null )
            return "??";
        return getType( n ) + " " + getName( n );
    }

    private static String getNextFlowType(String wdlType)
    {
        switch( wdlType )
        {
            case "File":
            case "Array[File]":
                return "path";
            default:
                return "val";
        }
    }

    public String getExternalInput(Node n)
    {
        if( n == null )
            return "??";

        StringBuilder result = new StringBuilder();

        result.append( "params." );
        result.append( getName( n ) );
        String expression = getExpression( n );
        if( expression != null && !expression.isEmpty() )
        {
            result.append( " = " + expression );
        }
        return result.toString();
    }

    public String getVersion()
    {
        return WDLUtil.getVersion( diagram );
    }

    public String getTaskRef(Compartment c)
    {
        return WDLUtil.getTaskRef( c );
    }

    public List<Compartment> getScatters(Compartment c)
    {
        return WDLUtil.getCycles( c );
    }

    public String getCycleName(Compartment c)
    {
        Node cycleVarNode = WDLUtil.getCycleVariableNode( c );
        if( cycleVarNode == null )
            return null;
        Node source = WDLUtil.getSource( cycleVarNode );
        if( source == null )
            return null;
        return getName( source );
    }

    public String getCycleVariable(Compartment c)
    {
        return WDLUtil.getCycleVariable( c );
    }

    public String createChannelName(String input)
    {
        return input.replace( ".", "_" ) + "_ch";
    }

    public List<Node> getCallsScatters(Compartment compartment)
    {
        List<Node> result = new ArrayList<>();
        for( Node c : orderedCalls )
        {
            if( c.getParent().equals( compartment ) )
                result.add( c );
        }
        return result;
    }

    public String getInputName(Node n)
    {
        Node source = WDLUtil.getSource( n );
        if( source != null )
            n = source;
        String name = WDLUtil.getName( n );
        String type = WDLUtil.getType( n );
        if( WDLUtil.isExternalParameter( n ) )
        {
            String result = name;
            if( "File".equals( type ) )
                result = "channel.fromPath(" + result + ")";
            return result;
        }
        if( WDLUtil.isCall( n.getCompartment() ) )
            return getResultName( n.getCompartment() ) + "." + name;
        return name;
    }

    public String getResultName(Compartment c)
    {
        return "result_" + WDLUtil.getCallName( c );
    }

    public String getInputName(Compartment call)
    {
        List<Node> inputs = getOrderedInputs( call );
        if( inputs.isEmpty() )
            return "";

        if( WDLUtil.isCycle( call.getCompartment() ) )
            return "input_" + WDLUtil.getCallName( call );
        else
            return StreamEx.of( inputs ).map( n -> getFullName( WDLUtil.getSource( n ) ) ).joining( "," );
    }

    public String getFullName(Node node)
    {
        if( WDLUtil.isCall( node.getCompartment() ) )
        {
            return getResultName( node.getCompartment() ) + "." + getName( node );
        }
        return getName( node );
    }

    public boolean isInsideCycle(Compartment call)
    {
        return ! ( call instanceof Diagram ) && WDLUtil.isCycle( call.getCompartment() );
    }

    public String prepareInputs(Compartment call)
    {
        StringBuilder sb = new StringBuilder( "  " );
        Compartment cycle = call.getCompartment();
        String cycleVar = getCycleVariable( cycle );
        String cycleName = getCycleName( cycle );
        Set<Node> arrayInputs = getArrayDepenedantInputs( cycleVar, call );
        for( Node input : arrayInputs )
        {
            String expression = WDLUtil.getExpression( input );
            if( ( cycleVar.equals( expression ) ) )
                sb.append( getName( input ) + " = " + cycleName );
            else
            sb.append( getName( input ) + " = " + expression.substring( 0, expression.indexOf( "[" ) ) );
            //            if( ! ( cycleVar.equals( expression ) ) )
            //                sb.append( ".map{" + cycleVar + " -> " + expression + "}" );
            sb.append( "\n" );
        }
        return sb.toString();
    }

    public String getCallInputName(Node input)
    {
        Compartment call = input.getCompartment();
        if( !isInsideCycle( call ) )
        {
            Node source = getSource( input );
            String result = getCallEmit( input );
            if( result == null )
                result = WDLUtil.getExpression( input );
            if( result == null || result.isEmpty() )
                result = "\"" + WDLConstants.NO_VALUE + "\"";
            if( source != null && isInsideCycle( source.getCompartment() ) )
                result += ".collect()";
            if (result.startsWith( "[" ) && result.endsWith( "]" ))
                result =  result.substring( 1, result.length() - 1 );
            return result;
        }
        else
        {
            Compartment cycle = call.getCompartment();
            String cycleVar = getCycleVariable( cycle );
            if( isArray( cycleVar, input ) )
                return getName(input);//getExpression(input).replaceAll("\\[\\s*"+cycleVar+"\\s*\\]", "");//getName( input ) + "_ch";
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
    }

    public String getCallEmit(Node node)
    {
        Node source = getSource( node );
        if( source != null && isCall( source.getCompartment() ) )
        {
            String result = getResultName( source.getCompartment() );
            String expression = getExpression( node );
            boolean startBracket = expression.startsWith( "[" );
            expression = expression.substring( expression.indexOf( "." ) + 1 );
            if( startBracket )
                result = "[" + result;
            return result + "." + expression;
        }
        else
        {
            return getExpression(node);
        }
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
                return substituteVariables( parts[1], process );
            }
        }
        return null;
    }

    private String substituteVariables(String expression, Compartment process)
    {
        Map<String, String> replacements = new HashMap<>();
        List<String> variables = WDLUtil.findVariables( expression );
        for( String variable : variables )
        {
            String variableExpression = WDLUtil.findExpression( variable, process );
            if( variableExpression != null )
            {
                replacements.put( "~{" + variable + "}", variableExpression );
            }
        }
        for( Entry<String, String> e : replacements.entrySet() )
        {
            expression = expression.replace( e.getKey(), e.getValue() );
        }
        return expression;
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

    public boolean isCall(Node node)
    {
        return WDLUtil.isCall( node );
    }

    public static boolean isExpression(Node node)
    {
        return WDLUtil.isExpression( node ) && !WDLUtil.isExternalOutput( node );
    }

    public boolean isCycle(Node node)
    {
        return WDLUtil.isCycle( node );
    }

    public String getFunctions()
    {
        return "basename; sub; length; range; createChannelIfNeeded; getDefault";
    }

    public ImportProperties[] getImports()
    {
        return WDLUtil.getImports( diagram );
    }

    public Compartment[] getImportedCalls()
    {
        return WDLUtil.getAllCalls( diagram ).stream().filter( c -> WDLUtil.getDiagramRef( c ) != null ).toArray( Compartment[]::new );
    }

    public String getImportedDiagram(Compartment call)
    {
        return WDLUtil.getDiagramRef( call );
    }

    public String getImportedAlias(Compartment call)
    {
        return WDLUtil.getCallName( call );
    }

    public String getCallName(Compartment call)
    {
        return WDLUtil.getCallName( call );
    }

    public String writePrivateDeclaration(Declaration declaration)
    {
        String expression = declaration.getExpression();
        expression = expression.replace( "~{", "${" );
        return declaration.getName() + " = " + expression;
    }

    public Object getPrivateDeclarations(Compartment task)
    {
        return WDLUtil.getBeforeCommand( task );
    }

    /**
     * @return names of all inputs for call which depends on scatter array
     */
    public Set<Node> getArrayDepenedantInputs(String cycleVar, Compartment call)
    {
        Set<Node> result = new HashSet<>();
        List<Node> inputs = WDLUtil.getInputs( call );
        for( Node input : inputs )
        {
            if( isArray( cycleVar, input ) )
                result.add( input );
        }
        return result;
    }

    public boolean isArray(String cycleVar, Node input)
    {
        return input.edges().map( e -> e.getInput() ).anyMatch( n -> WDLUtil.isCycleVariable( n ) );
    }

    public String getExternalParamaterName(Node input)
    {
        String result = "params." + getName( input );
        if( "File".equals( WDLUtil.getType( input ) ) )
            return "file(" + result + ")";
        else if( WDLUtil.getType( input ).contains( "Array" ) )
            return "createChannelIfNeeded(" + result + ").flatten()";
        else
            return result;
    }

    public boolean isNotEmpty()
    {
        return !WDLUtil.getAllCalls( diagram ).isEmpty();
    }

    public boolean isEntryScript()
    {
        return isEntryScript;
    }

}