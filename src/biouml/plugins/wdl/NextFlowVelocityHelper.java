package biouml.plugins.wdl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.plugins.wdl.WDLUtil.ImportProperties;
import biouml.plugins.wdl.diagram.WDLConstants;
import one.util.streamex.StreamEx;

public class NextFlowVelocityHelper
{
    private Diagram diagram;
    private List<Node> orderedCalls;

    public NextFlowVelocityHelper(Diagram diagram)
    {
        this.diagram = diagram;
        orderedCalls = WDLUtil.orderCallsScatters( diagram );
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



    public List<Node> getInputs(Compartment c)
    {
        return WDLUtil.getInputs( c );
    }

    /**
     * returns node which is connected with this input node of a call
     */
    //    public Node getSource(Node node)
    //    {
    //        return node.edges().filter(e->e.getOutput().equals( node )).map( e -> e.getInput() ).findAny().orElse( null );
    //    }

    public List<Node> getOutputs(Compartment c)
    {
        return WDLUtil.getOutputs( c );
    }

    public String getCommand(Compartment c)
    {
        String command = WDLUtil.getCommand( c );
        command = command.replace( "${", "\\${" );
        command  = command.replace( "~{", "${" );
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
        //        String expression = getExpression( n );
        //        if( expression != null && !expression.isEmpty() )
        //        {
        //            String type = getType( n );
        //            if( type.equals( "path" ) )
        //                expression = "file(" + expression + ")";
        //            result.append( " = " + expression );
        //        }
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
            String result =  name;
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

    public String createChannelFromArrray(Compartment cycle)
    {
        Compartment firstCall = null;
        for( Node node : WDLUtil.orderCallsScatters( cycle ) )
            if( node instanceof Compartment )
                firstCall = (Compartment)node;
        String cycleVar = getCycleVariable( cycle );
        Node arrayNode = WDLUtil.getCycleNode( cycle );
        String cycleName = getCycleName( cycle );
        String type = WDLUtil.getType( arrayNode );

        List<String> arrayInputs = getArrayDepenedantInputs( cycleVar, firstCall );

//        if (arrayInputs.size() == 1)
        //        if( type.equals( "Array[File]" ) || type.equals( "Directory" ) )
        //        {
        //            return "Channel.fromPath(" + cycleName + ").set{" + cycleVar + " }";
        //        }
        String channel = "Channel.from(" + cycleName + ")";

        if( arrayInputs.size() > 1 )
        {
            //            .map { idx -> tuple(x[idx], y[idx], idx) }
            channel += ".map { " + cycleName + " -> tuple(" + StreamEx.of( arrayInputs ).joining( "," ) + ") }";
        }
        channel += ".set{" + cycleVar + " }";
        return channel;
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
                return substituteVariables(parts[1], process);
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
            String variableExpression = WDLUtil.findExpression( variable,  process);
            if( variableExpression != null )
            {
                replacements.put( "~{"+variable+"}", variableExpression );
            }
        }
        for (Entry<String, String> e: replacements.entrySet())
        {
            expression = expression.replace(e.getKey(), e.getValue());
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
        return "basename; sub; length; range";
    }

    public ImportProperties[] getImports()
    {
        return WDLUtil.getImports( diagram );
    }

    public Compartment[] getImportedCalls()
    {
        return WDLUtil.getCalls( diagram ).stream().filter( c -> WDLUtil.getDiagramRef( c ) != null ).toArray( Compartment[]::new );
    }

    //    public String getImportedTask(Compartment call)
    //    {
    //        String taskRef = WDLUtil.getTaskRef( call );
    //        return taskRef.substring( taskRef.lastIndexOf( "." ) + 1 );
    //    }

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

    public String writeDeclaration(Declaration declaration)
    {
        return getNextFlowType( declaration.getType() ) + " " + declaration.getName() + " = " + declaration.getExpression();
    }

    public Object getBeforeCommand(Compartment task)
    {
        return WDLUtil.getBeforeCommand( task );
    }

    /**
     * @return names of all inputs for call which depends on scatter array
     */
    public List<String> getArrayDepenedantInputs(String cycleVar, Compartment call)
    {
        List<String> result = new ArrayList<>();
        List<Node> inputs = WDLUtil.getInputs( call );
        //        String cycleVar = this.getCycleVariable( call );
        for( Node input : inputs )
        {
            String expression = WDLUtil.getExpression( input );
            if( expression.contains( cycleVar ) )
                result.add( expression );
        }
        return result;
    }
}