package ru.biosoft.access._test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ru.biosoft.access.log.DefaultBiosoftLogger;
import ru.biosoft.access.script.LogScriptEnvironment;

public class TestLogScriptEnvironment extends AbstractBioUMLTest
{
    public void testErrorList() throws Exception
    {
        final DefaultBiosoftLogger log = new DefaultBiosoftLogger();
        final LogScriptEnvironment env = new LogScriptEnvironment( log );

        List<String> errorList = env.getErrorList();
        String expected = Collections.<String>emptyList().toString();
        assertEquals( expected, errorList.toString() );

        final String testError1Msg = "testError1";
        env.error( testError1Msg );
        errorList = env.getErrorList();
        expected = Collections.singletonList( testError1Msg ).toString();
        assertEquals( expected, errorList.toString() );

        final String testError2Msg = "testError2";
        env.error( testError2Msg );
        errorList = env.getErrorList();
        expected = Arrays.asList( testError1Msg, testError2Msg ).toString();
        assertEquals( expected, errorList.toString() );
    }
}
