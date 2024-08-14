package ru.biosoft.access._test;

import ru.biosoft.access.log.DefaultBiosoftLogger;
import ru.biosoft.access.log.StandardStreamLogger;
import ru.biosoft.access.log.StringBufferListener;

public class TestLogging extends AbstractBioUMLTest
{
    public void testLogging()
    {
        DefaultBiosoftLogger logger = new DefaultBiosoftLogger();
        logger.info( "qq" );
        StringBuffer buf1 = new StringBuffer();
        StringBuffer buf2 = new StringBuffer();
        try(StringBufferListener l = new StringBufferListener( buf1, logger ))
        {
            logger.warn( "warning!\n" );
            logger.error( "error!" );
        }
        try(StringBufferListener l2 = new StringBufferListener( buf2, logger ))
        {
            logger.info( "ok!" );
        }
        assertEquals("WARN: warning!\nERROR: error!\n", buf1.toString());
        assertEquals("INFO: ok!\n", buf2.toString());
    }
    
    public void testSystemOutLog()
    {
        DefaultBiosoftLogger logger = new DefaultBiosoftLogger();
        StringBuffer sb = new StringBuffer();
        try(StringBufferListener l = new StringBufferListener( sb, logger ))
        {
            System.out.println("Before");
            StandardStreamLogger.withLogger( logger, () -> {
                System.out.println("Test out");
                System.err.println("Test err");
                System.out.println("Test out2");
            });
            System.err.println("After");
        }
        assertEquals("INFO: Test out\n" +
                "WARN: Test err\n" +
                "INFO: Test out2\n", sb.toString());
    }
}
