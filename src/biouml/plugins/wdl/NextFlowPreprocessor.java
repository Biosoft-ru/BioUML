package biouml.plugins.wdl;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Node;

public class NextFlowPreprocessor
{
    private WorkflowSettings settings;
    public void NextFlowPreprocessor()
    {

    }

    public void setExportPath(WorkflowSettings settings)
    {
        this.settings = settings;
    }

    public Diagram preprocess(Diagram diagram) throws Exception
    {
        Diagram result = diagram.clone( diagram.getOrigin(), diagram.getName() );

        for( Compartment task : WDLUtil.getTasks( result ) )
        {
            for( Node input : WDLUtil.getInputs( task ) )
            {
                String expression = WDLUtil.getExpression( input );
                if( expression != null && !expression.isEmpty() )
                {
                    String name = WDLUtil.getName( input );
                    WDLUtil.addBeforeCommand( task, new Declaration( WDLUtil.getType( input ), name, "getDefault("+name+", "+expression+")"));
                }
            }

            String command = WDLUtil.getCommand( task );
            Set<String> seps = findSeps( command );
            for( String sep : seps )
            {
                String name = getSepName( sep );
                Declaration dec = new Declaration( "String", name + "_str", name + ".join()" );
                WDLUtil.addBeforeCommand( task, dec );
                command = command.replace( sep, "~{" + name + "_str}" );
            }
            WDLUtil.setCommand( task, command );
        }

        for( Node node : result.recursiveStream().select( Node.class ) )
        {
            String expression = WDLUtil.getExpression( node );
            if( expression != null && !expression.isEmpty() )
            {
                expression = processArrayElements( result, expression );
                expression = removeGlobs( expression );
                WDLUtil.setExpression( node, expression );
            }
        }
        return result;
    }

    public String preprocess(String s) throws Exception
    {
        return s;
    }

    public static String getSepName(String sep)
    {
        return sep.substring( 9, sep.length() - 1 ).trim();
    }

    public static Set<String> findSeps(String input)
    {
        Set<String> result = new HashSet<>();
        String regex = "~\\{sep=\" \" ([a-zA-Z_][a-zA-Z0-9_]*)}";
        Pattern pattern = Pattern.compile( regex );
        Matcher matcher = pattern.matcher( input );
        while( matcher.find() )
        {
            result.add( matcher.group() );
        }
        return result;
    }

    public static String processArrayElements(Diagram diagram, String input)
    {
        String regex = "^([a-zA-Z_][a-zA-Z0-9_\\.]*)\\[\\s*([a-zA-Z0-9_\\.]+)\\s*\\]$";
        Pattern pattern = Pattern.compile( regex );
        Matcher matcher = pattern.matcher( input );
        if( matcher.matches() )
        {
            String arrayName = matcher.group( 1 );
            String index = matcher.group( 2 );

            Node arrayIndex = diagram.recursiveStream().select( Node.class ).filter(n->WDLUtil.isCycleVariable( n ) && WDLUtil.getName( n ).equals( index )).findAny().orElse( null );
            if (arrayIndex != null)
                return input;
            String nextFlowStyle = arrayName + ".map{v->v[" + index + "]}";
            input = input.replace( matcher.group(), nextFlowStyle );
        }
        return input;
    }

    public static String removeGlobs(String input)
    {
        String regex = "glob\\((['\"])([a-zA-Z0-9./*_]+)\\1\\)";
        return input.replaceAll( regex, "\"$2\"" );
    }
}