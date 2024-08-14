package ru.biosoft.access.log;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class JULLoggerAdapter implements BiosoftLogger
{
    private static class JULLoggerAppender extends Handler
    {
        private final BiosoftLoggerListener listener;
        public JULLoggerAppender(BiosoftLoggerListener listener)
        {
            this.listener = listener;
        }
        @Override
        public void close() throws SecurityException
        {
        }

        @Override
        public void flush()
        {
        }

        @Override
        public void publish(LogRecord record)
        {

            if( record.getLevel() == Level.FINE || record.getLevel() == Level.FINER || record.getLevel() == Level.FINEST )
            {
                return;
            }
            EventType type = EventType.INFO;
            if( record.getLevel() == Level.SEVERE )
            {
                type = EventType.ERROR;
            }
            if( record.getLevel() == Level.WARNING )
            {
                type = EventType.WARN;
            }
            listener.messageAdded( type, String.valueOf( record.getMessage() ) );

        }

    }

    private final Logger log;

    public JULLoggerAdapter(Logger log)
    {
        this.log = log;
    }

    @Override
    public void info(String msg)
    {
        log.info( msg );
    }

    @Override
    public void warn(String msg)
    {
        log.warning( msg );
    }

    @Override
    public void error(String msg)
    {
        log.log( Level.SEVERE, msg );
    }

    @Override
    public void addListener(BiosoftLoggerListener listener)
    {
        log.addHandler( new JULLoggerAppender( listener ) );

    }

    @Override
    public void removeListener(BiosoftLoggerListener listener)
    {
        Handler[] allHandlers = log.getHandlers();
        for( Handler h : allHandlers )
        {
            if( h instanceof JULLoggerAppender && ( (JULLoggerAppender)h ).listener == listener )
            {
                log.removeHandler( h );
                return;
            }
        }
    }

}
