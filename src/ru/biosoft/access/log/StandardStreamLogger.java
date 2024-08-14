package ru.biosoft.access.log;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import ru.biosoft.exception.InternalException;

public class StandardStreamLogger
{
    private static final PerThreadStream outStream, errStream;

    static class PerThreadStream extends OutputStream
    {
        private final ThreadLocal<OutputStream> threadStream;
        private final OutputStream defaultStream;

        PerThreadStream(OutputStream defaultStream)
        {
            threadStream = ThreadLocal.withInitial( () -> defaultStream );
            this.defaultStream = defaultStream;
        }

        void set(OutputStream os)
        {
            threadStream.set( os );
        }

        void clear()
        {
            threadStream.set( defaultStream );
        }

        @Override
        public void write(byte[] b) throws IOException
        {
            threadStream.get().write( b );
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException
        {
            threadStream.get().write( b, off, len );
        }

        @Override
        public void write(int b) throws IOException
        {
            threadStream.get().write( b );
        }
    }

    static class LoggerOutputStream extends OutputStream
    {
        StringBuilder sb = new StringBuilder();
        private final BiosoftLogger logger;
        private final EventType type;

        public LoggerOutputStream(BiosoftLogger logger, EventType type)
        {
            this.logger = logger;
            this.type = type;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException
        {
            String str = new String( b, off, len, StandardCharsets.UTF_8 );
            String[] split = str.replace( "\r", "" ).split( "\n", -1 );
            if( split.length > 1 )
            {
                logger.log( type, sb + split[0] );
                sb.setLength( 0 );
            }
            for( int i = 1; i < split.length - 1; i++ )
            {
                logger.log( type, split[i] );
            }
            sb.append( split[split.length - 1] );
        }

        @Override
        public void write(int b) throws IOException
        {
            write( new byte[] {(byte)b} );
        }

    }

    static
    {
        outStream = new PerThreadStream( System.out );
        errStream = new PerThreadStream( System.err );
        try
        {
            System.setOut( new PrintStream( outStream, true, "UTF-8" ) );
            System.setErr( new PrintStream( errStream, true, "UTF-8" ) );
        }
        catch( UnsupportedEncodingException e )
        {
            throw new InternalException( e );
        }
    }

    public static void withLogger(BiosoftLogger log, Runnable run)
    {
        withLogger( log, () -> {
            run.run();
            return null;
        } );
    }

    public static <T> T withLogger(BiosoftLogger log, Supplier<T> run)
    {
        outStream.set( new LoggerOutputStream( log, EventType.INFO ) );
        errStream.set( new LoggerOutputStream( log, EventType.WARN ) );
        try
        {
            return run.get();
        }
        finally
        {
            outStream.clear();
            errStream.clear();
        }
    }
}
