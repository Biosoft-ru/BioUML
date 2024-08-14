package com.developmentontheedge.log;

import java.io.IOException;
import java.io.Writer;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public class WriterHandler extends Handler
{
    private Writer writer;

    public WriterHandler(Writer writer)
    {
        this.writer = writer;
        setFormatter( new SimpleFormatter() );
    }

    public WriterHandler(Writer writer, Formatter formatter)
    {
        this.writer = writer;
        setFormatter( formatter );
    }

    @Override
    public void close() throws SecurityException
    {
        try
        {
            writer.close();
        }
        catch( IOException e )
        {
        }

    }

    @Override
    public void flush()
    {
        try
        {
            writer.flush();
        }
        catch( IOException e )
        {
        }

    }

    @Override
    public void publish(LogRecord record)
    {
        if( isLoggable( record ) )
        {
            try
            {
                writer.write( getFormatter().format( record ) );
                writer.flush();
            }
            catch( IOException e )
            {
            }
        }
    }
}
