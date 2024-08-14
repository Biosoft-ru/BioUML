package ru.biosoft.access._test;

import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.LoggedException;

/**
 * @author lan
 *
 */
public class TestExceptionHandling extends AbstractBioUMLTest
{
    public void testExceptionBasics()
    {
        LoggedException exception = ExceptionRegistry.translateException( new Error( "test" ) );
        //assertEquals("Unexpected internal error occured Error: test. Please check server error log for BUEX#1.", exception.getMessage());
        assertEquals( "Unexpected internal error occured: Error - test.", exception.getMessage() );
        assertEquals( "EX#1", exception.getId() );
        exception.log();
        exception.log();
        exception = ExceptionRegistry.translateException( new NullPointerException() );
        //assertEquals( "Internal error occured (null pointer exception).", exception.getMessage() );
        assertEquals( "Unexpected internal error occured: NullPointerException - null.", exception.getMessage() );
        assertEquals( "EX#2", exception.getId() );
    }
}
