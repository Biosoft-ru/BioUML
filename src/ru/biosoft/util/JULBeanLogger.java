package ru.biosoft.util;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.developmentontheedge.beans.log.BeanLogger;

public class JULBeanLogger implements BeanLogger
{
    private static final Logger log = Logger.getLogger( "com.developmentontheedge.beans" );

    @Override
    public void warn(String msg)
    {
        log.warning( msg );
    }

    @Override
    public void warn(String msg, Throwable t)
    {
        log.log( Level.WARNING, msg, t );

    }

    @Override
    public void error(String msg)
    {
        log.severe( msg );

    }

    @Override
    public void error(String msg, Throwable t)
    {
        log.log( Level.SEVERE, msg, t );
    }

    public static void install()
    {
        com.developmentontheedge.beans.log.Logger.setGlobalLogger( new JULBeanLogger() );
    }
}
