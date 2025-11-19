package biouml.plugins.wdl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Node;
import biouml.plugins.wdl.diagram.WDLConstants;

public class CWLVelocityHelper extends WorkflowVelocityHelper
{
    private static String CWL_VERSION = "v1.2";

    public CWLVelocityHelper(Diagram diagram)
    {
        super( diagram );
    }

    public String getVersion()
    {
        return CWL_VERSION;
    }

    public String getContainer(Compartment process)
    {
        return getRuntimeProperty( process, "docker" );
    }

    public String getCleanExpression(Node node)
    {
        String expression = getExpression( node );
        expression = expression.replace( ".", "/" );
        Node source = WorkflowUtil.getSource( node );
        if( source == null || WorkflowUtil.isExternalParameter( source ) )
            return expression;
        String replacement = getCleanExpression( source );
        return expression.replace( source.getName(), replacement );
    }

    public String getOutputSource(Node output)
    {
        String source = getExpression( output );
        source = source.replace( ".", "/" );
        return source;
    }

    @Override
    public String getCommand(Compartment c)
    {
        return super.getCommand( c );//.replace( "~{", "${" );
    }

    public String getBaseCommand(Compartment c)
    {
        String command = getCommand( c );
        command = command.replace( "\\r?\\n", "" ).trim();
        return command.substring( 0, command.indexOf( " " ) );
    }

    public String[] getArguments(Compartment c)
    {
        String command = getCommand( c );
        String[] result = splitArguments( command )[0];

        //        String[] parts = command.split( " " );
        //        String[] result = new String[0];
        return result;
    }


    public List<String> getCommands(Compartment c)
    {
        String command = getCommand( c );
        return splitCommands( command );
    }
    
    public List<String> splitCommands(String script)
    {
        String[] lines = script.split( "\\r?\\n" );
        List<String> commands = new ArrayList<>();

        StringBuilder currentCommand = new StringBuilder();
        for( String line : lines )
        {
            line = line.trim();
            if( line.endsWith( "\\" ) )
                currentCommand.append( line.substring( 0, line.length() - 1 ) ).append( " " );

            else
            {
                currentCommand.append( line );
                if( !currentCommand.toString().isEmpty() )
                    commands.add( processCommand(currentCommand.toString()) );
                currentCommand = new StringBuilder();
            }
        }
        if( currentCommand.length() > 0 )
            commands.add( processCommand(currentCommand.toString()) );

       
        return commands;
    }

    public static String[][] splitArguments(String script)
    {
        // Step 1: normalize lines and join continued lines
        String[] lines = script.split( "\\r?\\n" );
        List<String> commands = new ArrayList<>();

        StringBuilder currentCommand = new StringBuilder();
        for( String line : lines )
        {
            line = line.trim();
            if( line.endsWith( "\\" ) )
            {
                currentCommand.append( line.substring( 0, line.length() - 1 ) ).append( " " );
            }
            else
            {
                currentCommand.append( line );
                if( !currentCommand.toString().isEmpty() )
                {
                    commands.add( currentCommand.toString() );
                }
                currentCommand = new StringBuilder();
            }
        }
        // In case last line ends with backslash
        if( currentCommand.length() > 0 )
        {
            commands.add( currentCommand.toString() );
        }

        // Step 2: split each command into parts by whitespace
        String[][] result = new String[commands.size()][];
        for( int i = 0; i < commands.size(); i++ )
        {
            // Split by whitespace; note this does not handle quoted strings as single tokens
            String[] args = commands.get( i ).split( "\\s+" );
            String[] commandRemoved = new String[args.length - 1];
            for( int j = 1; j < args.length; j++ )
                commandRemoved[j - 1] = processArgument( args[j] );
            result[i] = commandRemoved;
        }

        return result;
    }

    public String processCommand(String command)
    {
        Pattern pattern = Pattern.compile( "~\\{([A-Za-z0-9_.]+)\\}" );
        Matcher matcher = pattern.matcher( command );
        StringBuffer result = new StringBuffer();
        while( matcher.find() )
        {
            String text = matcher.group( 1 );
            DiagramElement de = this.diagram.findDiagramElement( text );
            boolean isFile = false;
            if (de instanceof Node)
            {
                isFile = WorkflowUtil.getType( (Node)de ).equals( "File" );
            }
            String replacement = isFile? "$(inputs." + text + ".basename)": "$(inputs." + text + ")";
            matcher.appendReplacement( result, Matcher.quoteReplacement( replacement ) );
        }
        matcher.appendTail( result );
        return result.toString();
    }

    public static String processArgument(String argument)
    {
        Pattern pattern = Pattern.compile( "~\\{([A-Za-z0-9_.]+)\\}" );
        Matcher matcher = pattern.matcher( argument );
        String result = argument;
        if( matcher.matches() )
            result = "valueFrom: $( inputs." + matcher.group( 1 ).trim() + ".basename)";
        return result;
    }

    public String getGlobExpression(Node node)
    {
        String expression = getExpression( node );

        Pattern pattern = Pattern.compile( "~\\{([A-Za-z0-9_.]+)\\}" );
        Matcher matcher = pattern.matcher( expression );
        StringBuffer result = new StringBuffer();
        while( matcher.find() )
        {
            String text = matcher.group( 1 );
            String replacement = "$(inputs." + text + ".basename)";
            matcher.appendReplacement( result, Matcher.quoteReplacement( replacement ) );
        }
        matcher.appendTail( result );

        return result.toString();
    }

    public static void main(String[] args)
    {
        String script = "set -o pipefail\n" + "pbmm2 align \\\n" + "  --sample ~{sample_name} \\\n" + "  --log-level INFO \\\n"
                + "  --preset CCS \\\n" + "  --sort \\\n" + "  --unmapped \\\n" + "  -c 0 -y 70 \\\n" + "  -j ~{threads} \\\n"
                + "  ~{reference_fasta} \\\n" + "  ~{movie} \\\n" + "  ~{output_bam}";

        String[][] split = splitArguments( script );

        for( String[] commandParts : split )
        {
            for( String part : commandParts )
            {
                System.out.print( "[" + part + "] " );
            }
            System.out.println();
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
                return parts[1];
            }
        }
        return null;
    }

    @Override
    public String getType(Node n)
    {
        String type = WorkflowUtil.getType( n );
        if( type.equals( "String" ) )
            type = "string";
        return type;
    }
}