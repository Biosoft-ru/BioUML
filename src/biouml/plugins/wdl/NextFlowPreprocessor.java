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

        for( Compartment task : WorkflowUtil.getTasks( result ) )
        {
            for( Node input : WorkflowUtil.getInputs( task ) )
            {
                String expression = WorkflowUtil.getExpression( input );
                if( expression != null && !expression.isEmpty() )
                {
                    String name = WorkflowUtil.getName( input );
                    WorkflowUtil.addBeforeCommand( task,
                            new Declaration( WorkflowUtil.getType( input ), name, "getDefault(" + name + ", " + expression + ")" ) );
                }
            }

            String command = WorkflowUtil.getCommand( task );
            Set<String> seps = findSeps( command );
            for( String sep : seps )
            {
                String name = getSepName( sep );
                Declaration dec = new Declaration( "String", name + "_str", name + ".join()" );
                WorkflowUtil.addBeforeCommand( task, dec );
                command = command.replace( sep, "~{" + name + "_str}" );
            }
            WorkflowUtil.setCommand( task, command );
        }

        for( Node node : result.recursiveStream().select( Node.class ) )
        {
            String expression = WorkflowUtil.getExpression( node );
            if( expression != null && !expression.isEmpty() )
            {
                expression = processArrayElements( result, expression );
                expression = removeGlobs( expression );
                expression = processTernary( expression );
                WorkflowUtil.setExpression( node, expression );
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

            Node arrayIndex = diagram.recursiveStream().select( Node.class )
                    .filter( n -> WorkflowUtil.isCycleVariable( n ) && WorkflowUtil.getName( n ).equals( index ) ).findAny().orElse( null );
            if( arrayIndex != null )
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

    public static String processTernary(String ternary)
    {
        String patternString = "if\\s+(.+?)\\s+then\\s+(.+?)\\s+else\\s+(.+)";

        Pattern pattern = Pattern.compile( patternString );
        Matcher matcher = pattern.matcher( ternary );

        if( matcher.matches() )
        {
            String condition = matcher.group( 1 ).trim();
            String truePart = matcher.group( 2 ).trim();
            String falsePart = matcher.group( 3 ).trim();
            return condition +" ? " +truePart+" : "+falsePart;
        }
        return ternary;
    }
}