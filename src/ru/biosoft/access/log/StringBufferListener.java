package ru.biosoft.access.log;

public class StringBufferListener implements BiosoftLoggerListener, AutoCloseable
{
    private StringBuffer sb;
    private BiosoftLogger logger; 
    
    public StringBufferListener(StringBuffer sb, BiosoftLogger logger)
    {
        logger.addListener( this );
        this.sb = sb;
        this.logger = logger;
    }

    @Override
    public void messageAdded(EventType type, String message)
    {
        String msg = type + ": " + message + (message.endsWith( "\n" ) ? "" : "\n");
        sb.append( msg );
    }
    
    @Override
    public void close()
    {
        logger.removeListener( this );
        logger = null;
        sb = null;
    }

    @Override
    public String toString()
    {
        return sb.toString();
    }
}
