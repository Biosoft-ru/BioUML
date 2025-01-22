package biouml.plugins.physicell.javacode;

public class JavaCodeHTMLFormatter extends JavaCodeFormatter
{

    protected void handleComment(String code, int i, StringBuilder builder, int indentLevel, boolean noBrake)
    {
        builder.append( "<p style=\"color:green;\">" );
        indent( builder, indentLevel );
        builder.append( "//" );
        char ch = '.';
        while( ch != '\n' )
        {
            i++;
            ch = code.charAt( i );
            builder.append( ch );
        }
        builder.append( "<p>" );
        newLine( builder, noBrake );

    }

    @Override
    protected String clearFormat(String code)
    {
        return code.replace( "<br>", "" ).replace( "&emsp;", "" );
    }
    
    protected void newLine(StringBuilder code, boolean noBrake)
    {
        if( !noBrake )
            code.append( "<br>" );
    }

    protected void indent(StringBuilder code, int indentLevel)
    {
        for( int i = 0; i < indentLevel; i++ )
        {
            code.append( "&emsp;" );
        }
    }
}
