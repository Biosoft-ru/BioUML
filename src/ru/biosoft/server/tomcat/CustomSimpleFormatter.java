package ru.biosoft.server.tomcat;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public class CustomSimpleFormatter extends SimpleFormatter
{
    static final DateFormat df = new SimpleDateFormat( "yyyy/MM/dd hh:mm:ss" );

    @Override
    public synchronized String format(LogRecord record)
    {
        StringBuilder builder = new StringBuilder( 1000 );
        builder.append( ">" ).append( df.format( new Date( record.getMillis() ) ) ).append( " " ).append( record.getLevel() ).append( " " );
        builder.append( formatMessage( record ) );
        builder.append( " [" ).append( record.getSourceClassName() ).append( "]" );
        builder.append( "\n" );
        if( record.getThrown() != null )
        {
            try( StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter( sw ) )
            {
                record.getThrown().printStackTrace( pw );
                builder.append( sw.toString() );
            }
            catch( Exception ex )
            {
            }
        }
        return builder.toString();
    }

}
