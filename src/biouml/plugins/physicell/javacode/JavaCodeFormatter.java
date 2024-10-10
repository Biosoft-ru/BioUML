package biouml.plugins.physicell.javacode;

public class JavaCodeFormatter
{
    public String format(String code)
    {
        StringBuilder builder = new StringBuilder();
        int indentLevel = 0;

        code = clearFormat(code);
        code = code.replace( "@", "|@" );
        code = code.replace( "public", "|public" );
        code = code.replace( "for(", "#" );
        code = code.replace( "//", "$" );

        boolean noBrake = false;
        char prev = '.';
        for( int i = 0; i < code.length(); i++ )
        {
            char ch = code.charAt( i );

            if( ch == ')' )
                noBrake = false;

            if( ch == '$' )
            {   
                handleComment( code, i, builder, indentLevel, noBrake );
                prev = ';';
                continue;
            }

            switch( ch )
            {
                case '{':
                    newLine( builder, noBrake );
                    indent( builder, indentLevel );
                    indentLevel++;
                    builder.append( ch );
                    newLine( builder, noBrake );
                    break;
                case '}':
                    indentLevel--;
                    indent( builder, indentLevel );
                    builder.append( ch );
                    newLine( builder, noBrake );
                    break;
                case ';':
                    builder.append( ch );
                    newLine( builder, noBrake );
                    break;
                case '|':
                    newLine( builder, noBrake );
                    indent( builder, indentLevel );
                    break;
                case '#':
                    newLine( builder, noBrake );
                    indent( builder, indentLevel );
                    builder.append( "for(" );
                    noBrake = true;
                    break;
                case '\n':
                case '\r':
                    break;
                case ' ':
                    builder.append( ch );
                    break;
                default:
                    if( !noBrake && ( prev == ';' || prev == '}' || prev == '{' ) )
                        indent( builder, indentLevel );
                    builder.append( ch );
                    break;
            }
            if( ch != ' ' && ch != '\n' && ch != '\r')
                prev = ch;
        }

        String result = builder.toString().trim();
        return result;
    }
    
    protected String clearFormat(String code)
    {
        return code.replace( "\n", "" ).replace( "\r", "" ).replace( "\t", "" );
    }

    protected void handleComment(String code, int i, StringBuilder builder, int indentLevel, boolean noBrake)
    {     
        indent( builder, indentLevel );
        builder.append( "//" );
        char ch = '.';
        while( ch != '\n' )
        {
            i++;
            ch = code.charAt( i );
            builder.append( ch );
        }
        newLine( builder, noBrake );
    }
    
    private void newLine(StringBuilder code, boolean noBrake)
    {
        if( !noBrake )
            code.append( "\n" );
    }

    private void indent(StringBuilder code, int indentLevel)
    {
        for( int i = 0; i < indentLevel; i++ )
        {
            code.append( "\t" );
        }
    }
}
