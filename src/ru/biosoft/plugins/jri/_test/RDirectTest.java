package ru.biosoft.plugins.jri._test;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import one.util.streamex.IntStreamEx;
import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access.ImageElement;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access._test.TestEnvironment;
import ru.biosoft.access.log.DefaultBiosoftLogger;
import ru.biosoft.access.log.StringBufferListener;
import ru.biosoft.access.script.LogScriptEnvironment;
import ru.biosoft.access.script.ScriptDataElement;
import ru.biosoft.access.script.ScriptEnvironment;
import ru.biosoft.access.script.ScriptTypeRegistry;
import ru.biosoft.access.script.ScriptJobControl.BreakType;
import ru.biosoft.plugins.jri.RElement;
import ru.biosoft.plugins.jri.rdirect.RDirectSession;
import ru.biosoft.plugins.jri.rdirect.RWriter;

public class RDirectTest extends AbstractBioUMLTest
{
    public RDirectTest()
    {

    }

    public RDirectTest(String name)
    {
        super( name );
    }

    public void testRDirect()
    {
        DefaultBiosoftLogger log = new DefaultBiosoftLogger();
        StringBuffer sb = new StringBuffer();
        try (StringBufferListener listener = new StringBufferListener( sb, log );
                RDirectSession session = RDirectSession.create(5000))
        {
            
            ScriptEnvironment env = new LogScriptEnvironment( log );
            session.setEnvironment( env );
            session.eval( "print(2+2)" );
            assertTrue(session.isValid());
            assertEquals( "INFO: [1] 4", sb.toString().trim() );
            sb.setLength( 0 );
            
            session.eval( "print('Hello!')" );
            assertEquals( "INFO: [1] \"Hello!\"", sb.toString().trim() );
            sb.setLength( 0 );
            session.eval( "print(c(1,2,3))" );
            assertEquals( "INFO: [1] 1 2 3", sb.toString().trim() );
            sb.setLength( 0 );
            session.eval( "unknownVariable" );
            String err = sb.toString();
            assertTrue( err, err.startsWith( "ERROR: " ) );
            assertTrue( err, err.contains( "'unknownVariable'" ) );
            sb.setLength( 0 );
            session.eval( "write('test', stderr())" );
            assertEquals( "ERROR: test", sb.toString().trim() );
            sb.setLength( 0 );
            session.eval( "a <- 5" );
            assertEquals( "", sb.toString().trim() );
            sb.setLength( 0 );
            session.eval( "print(a)" );
            assertEquals( "INFO: [1] 5", sb.toString().trim() );
            sb.setLength( 0 );
            session.assign( "data", new Object[] {new int[] {1, 2, 3, 4, 5}, "String", new double[] {1.0, 2.0, Double.NEGATIVE_INFINITY}} );
            session.eval( "print(data)" );
            assertEquals(
                    "INFO: [[1]]\nINFO: [1] 1 2 3 4 5\nINFO: \nINFO: [[2]]\nINFO: [1] \"String\"\nINFO: \nINFO: [[3]]\nINFO: [1]    1    2 -Inf\nINFO:",
                    sb.toString().trim() );
        }
    }

    public void testGetSet()
    {
        try(RDirectSession session = RDirectSession.create(5000))
        {
            session.assign( "x", 5 );
            session.eval( "y <- x*2" );
            assertEquals( "10", session.getAsString( "y" ) );
        }
    }
    
    public void testHelp()
    {
        try(RDirectSession session = RDirectSession.create(5000))
        {
            TestEnvironment env = new TestEnvironment();
            session.setEnvironment( env );
            session.help( "print" );
            assertEquals(1, env.help.size());
            assertTrue(env.help.get( 0 ).contains( "<head><title>R: Print Values</title>" ));
        }
    }

    public void testRVectorReturn()
    {
        final ScriptDataElement script = ScriptTypeRegistry.createScript( "R", null, "" );
        final TestEnvironment env = new TestEnvironment();

        final Map<String, Object> output = new HashMap<>();
        output.put( "out", null );

        final int[] inData = {1, 2, 3};
        final Map<String, Object> input = Collections.<String, Object>singletonMap( "inData", inData );

        final String rScript = "out <- inData;";
        script.execute( rScript, env, input, output, false );

        final Object out = output.get( "out" );
        final String expected = "[1, 2, 3]";
        assertEquals( expected, Arrays.toString( (int[])out) );
    }
    
    public void testRAssignBig()
    {
        String[] vals = {"low", "moderate", "high"};
        String[] input = IntStreamEx.of( new Random(1), 5000, 0, 3 ).elements( vals ).toArray( String[]::new );
        try(RDirectSession session = RDirectSession.create(5000))
        {
            TestEnvironment env = new TestEnvironment();
            session.setEnvironment( env );
            session.assign( "test", input );
            session.eval( "result <- test[[1234]]" );
            assertEquals(input[1233], session.getAsString( "result" ));
        }
    }

    public void testRMatrixReturn()
    {
        final ScriptDataElement script = ScriptTypeRegistry.createScript( "R", null, "" );
        final TestEnvironment env = new TestEnvironment();

        final Map<String, Object> output = new HashMap<>();
        output.put( "out", null );

        final int[][] inData = {
                {1, 4, 3},
                {4, 6, 6}
        };
        final Map<String, Object> input = Collections.<String, Object>singletonMap( "inData", inData );

        final String rScript = "out <- inData;";
        script.execute( rScript, env, input, output, false );

        final List<int[]> out = (List<int[]>)output.get( "out" );
        assertEquals( "[1, 4, 3]", Arrays.toString( out.get(0) ) );
        assertEquals( "[4, 6, 6]", Arrays.toString( out.get(1) ) );
    }

    public void testRDataFrameFromMultiDimensionalArrayReturn()
    {
        final ScriptDataElement script = ScriptTypeRegistry.createScript( "R", null, "" );
        final TestEnvironment env = new TestEnvironment();

        final Map<String, Object> output = new HashMap<>();
        output.put( "out", null );

        final int[][] inData = {
                {1, 5, 3},
                {4, 6, 6}
        };
        final Map<String, Object> input = Collections.<String, Object>singletonMap( "inData", inData );

        final String rScript = "out <- data.frame(matrix(unlist(inData), nrow=length(inData), byrow=T));";
        script.execute( rScript, env, input, output, false );

        final List<int[]> out = (List<int[]>)output.get( "out" );
        assertEquals( "[1, 4]", Arrays.toString( out.get(0) ) );
        assertEquals( "[5, 6]", Arrays.toString( out.get(1) ) );
        assertEquals( "[3, 6]", Arrays.toString( out.get(2) ) );
    }

    public void testRDataFrameFromMultiDimensionalArrayAddNamedColumnReturn()
    {
        final ScriptDataElement script = ScriptTypeRegistry.createScript( "R", null, "" );
        final TestEnvironment env = new TestEnvironment();

        final Map<String, Object> output = new HashMap<>();
        output.put( "out", null );

        final int[][] inData = {
                {1},
                {4}
        };
        final Map<String, Object> input = Collections.<String, Object>singletonMap( "inData", inData );

        final String rScript =
                "out <- data.frame(matrix(unlist(inData), nrow=length(inData), byrow=T));" +
                "out[\"Trust\"] = T;";
        script.execute( rScript, env, input, output, false );

        final List<Object> out = (List<Object>) output.get( "out" );
        assertEquals( "[1, 4]", Arrays.toString( (int[]) out.get( 0 ) ) );
        assertEquals( "[true, true]", Arrays.toString( (boolean[]) out.get( 1 ) ) );
    }

    public void testDebugSimple()
    {
        try(RDirectSession session = RDirectSession.create(5000))
        {
            TestEnvironment env = new TestEnvironment();
            session.setEnvironment( env );
            if(!session.initDebug( "print(1);\n"
                    + "print(2);\n"
                    + "print(3);\n" ))
            {
                fail(env.error.toString());
            }
            assertTrue(env.print.toString(), env.print.isEmpty());
            assertEquals(0, session.getCurrentLine());
            assertTrue(session.step( BreakType.STEP_OVER ));
            assertEquals(1, session.getCurrentLine());
            
            assertTrue(session.isDebug());
            
            assertTrue(session.step( BreakType.STEP_OVER ));
            assertEquals("[[1] 1]", env.print.toString());
            assertEquals(2, session.getCurrentLine());
            env.print.clear();
            
            assertTrue(session.step( BreakType.STEP_OVER ));
            assertEquals("[[1] 2]", env.print.toString());
            assertEquals(3, session.getCurrentLine());
            env.print.clear();
            
            assertFalse(session.step( BreakType.STEP_OVER ));
            assertEquals("[[1] 3]", env.print.toString());
        }
    }
    
    public void testDebugCollisions()
    {
        try(RDirectSession session = RDirectSession.create(5000))
        {
            TestEnvironment env = new TestEnvironment();
            session.setEnvironment( env );
            if(!session.initDebug( "print(1);\n"
                    + "write('debug at <text>#1: test\\n', stdout());\n"
                    + "print(3);\n"
                    //+ "write('debugging in: blahblah\\n', stdout());\n" -- cannot reliably detect such collision now
                    + "print(5);\n" ))
            {
                fail(env.error.toString());
            }
            assertTrue(env.print.toString(), env.print.isEmpty());
            assertEquals(0, session.getCurrentLine());
            assertTrue(session.step( BreakType.STEP_OVER ));
            assertEquals(1, session.getCurrentLine());
            
            assertTrue(session.isDebug());
            
            assertTrue(session.step( BreakType.STEP_OVER ));
            assertEquals("[[1] 1]", env.print.toString());
            assertEquals(2, session.getCurrentLine());
            env.print.clear();
            
            assertTrue(session.step( BreakType.STEP_OVER ));
            assertEquals("[debug at <text>#1: test, ]", env.print.toString());
            assertEquals(3, session.getCurrentLine());
            env.print.clear();
            
            assertFalse(session.step( BreakType.NONE ));
            assertEquals("[[1] 3, [1] 5]", env.print.toString());
        }
    }
    
    public void testDebugStepIn()
    {
        try(RDirectSession session = RDirectSession.create(5000))
        {
            TestEnvironment env = new TestEnvironment();
            session.setEnvironment( env );
            if(!session.initDebug( "x <- function() {\n"
                    + "print('X start');\n"
                    + "print('X end');\n"
                    + "}\n"
                    + "\n"
                    + "print('Start');\n"
                    + "x();\n"
                    + "x();" ))
            {
                fail(env.error.toString());
            }
            assertTrue(env.print.toString(), env.print.isEmpty());
            assertEquals(0, session.getCurrentLine());
            assertTrue(session.step( BreakType.STEP_IN ));
            assertEquals(1, session.getCurrentLine());
            assertTrue(session.step( BreakType.STEP_IN ));
            assertEquals(6, session.getCurrentLine());
            assertEquals("[]", env.print.toString());
            env.print.clear();

            assertTrue(session.step( BreakType.STEP_IN ));
            assertEquals(7, session.getCurrentLine());
            assertEquals("[[1] \"Start\"]", env.print.toString());
            env.print.clear();

            assertTrue(session.step( BreakType.STEP_OVER ));
            assertEquals(8, session.getCurrentLine());
            assertEquals("[[1] \"X start\", [1] \"X end\"]", env.print.toString());
            env.print.clear();

            if(session.isR3_1())
            {
                // In R prior to 3.1.0 there's no "step in" debug command
                assertTrue(session.step( BreakType.STEP_IN ));
                assertEquals(1, session.getCurrentLine());
                assertEquals("[]", env.print.toString());
                env.print.clear();

                assertTrue(session.step( BreakType.STEP_IN ));
                assertEquals(2, session.getCurrentLine());
                assertEquals("[]", env.print.toString());
                env.print.clear();

                assertTrue(session.step( BreakType.STEP_IN ));
                assertEquals(3, session.getCurrentLine());
                assertEquals("[[1] \"X start\"]", env.print.toString());
                env.print.clear();

                assertFalse(session.step( BreakType.STEP_IN ));
                assertEquals("[[1] \"X end\"]", env.print.toString());
                env.print.clear();
            }
        }
    }
    
    public void testDebugStepOutRun()
    {
        try(RDirectSession session = RDirectSession.create(5000))
        {
            TestEnvironment env = new TestEnvironment();
            session.setEnvironment( env );
            if(!session.initDebug( "print('Start');\n"
                    + "for(i in 1:10) {\n"
                    + "print(i);\n"
                    + "}\n"
                    + "print('Continue');\n"
                    + "for(i in 1:10) {\n"
                    + "print(i);\n"
                    + "}\n"
                    + "print('End');" ))
            {
                fail(env.error.toString());
            }
            assertTrue(env.print.toString(), env.print.isEmpty());
            assertEquals(0, session.getCurrentLine());
            assertTrue(session.step( BreakType.STEP_OVER ));
            assertEquals(1, session.getCurrentLine());
            
            assertTrue(session.step( BreakType.STEP_OVER ));
            assertEquals("[[1] \"Start\"]", env.print.toString());
            assertEquals(2, session.getCurrentLine());
            env.print.clear();

            if(!session.isR3_1())
                assertTrue(session.step( BreakType.STEP_OVER ));
            assertTrue(session.step( BreakType.STEP_OVER ));
            assertEquals("[]", env.print.toString());
            assertEquals(3, session.getCurrentLine());
            env.print.clear();

            if(!session.isR3_1())
                assertTrue(session.step( BreakType.STEP_OVER ));
            assertTrue(session.step( BreakType.STEP_OVER ));
            assertEquals("[[1] 1]", env.print.toString());
            assertEquals(3, session.getCurrentLine());
            env.print.clear();

            if(!session.isR3_1())
                assertTrue(session.step( BreakType.STEP_OVER ));
            assertTrue(session.step( BreakType.STEP_OVER ));
            assertEquals("[[1] 2]", env.print.toString());
            assertEquals(3, session.getCurrentLine());
            env.print.clear();

            if(!session.isR3_1())
                assertTrue(session.step( BreakType.STEP_OVER ));
            assertTrue(session.step( BreakType.STEP_OVER ));
            assertEquals("[[1] 3]", env.print.toString());
            assertEquals(3, session.getCurrentLine());
            env.print.clear();

            assertTrue(session.step( BreakType.STEP_OUT ));
            assertEquals("[[1] 4, [1] 5, [1] 6, [1] 7, [1] 8, [1] 9, [1] 10]", env.print.toString());
            assertEquals(5, session.getCurrentLine());
            env.print.clear();

            assertTrue(session.step( BreakType.STEP_OVER ));
            assertEquals("[[1] \"Continue\"]", env.print.toString());
            assertEquals(6, session.getCurrentLine());
            env.print.clear();

            assertTrue(session.step( BreakType.STEP_OVER ));
            if(!session.isR3_1())
                assertTrue(session.step( BreakType.STEP_OVER ));
            assertTrue(session.step( BreakType.STEP_OVER ));
            if(!session.isR3_1())
                assertTrue(session.step( BreakType.STEP_OVER ));
            assertTrue(session.step( BreakType.STEP_OVER ));
            assertEquals("[[1] 1, [1] 2]", env.print.toString());
            if(!session.isR3_1())
                assertTrue(session.step( BreakType.STEP_OVER ));
            assertEquals(7, session.getCurrentLine());
            env.print.clear();

            assertFalse(session.step( BreakType.NONE ));
            assertEquals("[[1] 3, [1] 4, [1] 5, [1] 6, [1] 7, [1] 8, [1] 9, [1] 10, [1] \"End\"]", env.print.toString());
        }
    }
    
    public void testRPlot()
    {
        TestEnvironment env = new TestEnvironment();
        new RElement( null, "", "" ).execute( "plot(rnorm(50));dev.new();hist(rnorm(70));", env, false );
        assertEquals(2, env.imageElements.size());
        BufferedImage image = env.imageElements.get( 0 ).getImage( new Dimension(300, 300) );
        assertEquals(300, image.getWidth());
        assertEquals(300, image.getHeight());
        image = env.imageElements.get( 1 ).getImage( new Dimension(400, 400) );
        assertEquals(400, image.getWidth());
        assertEquals(400, image.getHeight());
        BufferedImage image2 = env.imageElements.get( 1 ).getImage( new Dimension(400, 400) );
        assertImageEquals(image, image2);
        image = env.imageElements.get( 1 ).getImage( null );
        assertEquals(RElement.DEFAULT_WIDTH, image.getWidth());
        assertEquals(RElement.DEFAULT_HEIGHT, image.getHeight());
        for(ImageElement element : env.imageElements)
        {
            if(element instanceof Closeable)
            {
                try
                {
                    ((Closeable)element).close();
                }
                catch( IOException e )
                {
                }
            }
        }
    }

    public void testRWriter()
    {
        assertEquals( "list(c(1L, 2L, 3L, 4L, 5L), \"String\", c(1.0, 2.0, -Inf))",
                RWriter.getRString( new Object[] {new int[] {1, 2, 3, 4, 5}, "String", new double[] {1.0, 2.0, Double.NEGATIVE_INFINITY}} ) );
        assertEquals("\"\\t\\n\\'\\\"\\`\\u1234\\u0001\\\\\"", RWriter.getRString( "\t\n\'\"`\u1234\001\\" ));
    }
    
    public void testGetObject()
    {
        try(RDirectSession session = RDirectSession.create(5000))
        {
            TestEnvironment env = new TestEnvironment();
            session.setEnvironment( env );
            session.eval( "a <- c(1,2,3)");
            double[] resDoubleArray = (double[])session.getAsObject( "a" );
            assertEquals(3, resDoubleArray.length);
            assertEquals(1.0, resDoubleArray[0]);
            assertEquals(2.0, resDoubleArray[1]);
            assertEquals(3.0, resDoubleArray[2]);

            session.eval( "a <- c(Inf, -Inf, NaN)");
            resDoubleArray = (double[])session.getAsObject( "a" );
            assertEquals(3, resDoubleArray.length);
            assertEquals(Double.POSITIVE_INFINITY, resDoubleArray[0]);
            assertEquals(Double.NEGATIVE_INFINITY, resDoubleArray[1]);
            assertEquals(Double.NaN, resDoubleArray[2]);

            session.eval( "a <- 2");
            assertEquals(2.0, session.getAsObject( "a" ));
            
            session.eval( "a <- list(1,2,3)" );
            assertEquals(Arrays.asList( 1.0, 2.0, 3.0 ), session.getAsObject( "a" ));
            
            session.eval( "a <- list(c(1,2,3))" );
            List<Object> resList = (List<Object>)session.getAsObject( "a" );
            assertEquals(1, resList.size());
            resDoubleArray = (double[])resList.get( 0 );
            assertEquals(3, resDoubleArray.length);
            assertEquals(1.0, resDoubleArray[0]);
            assertEquals(2.0, resDoubleArray[1]);
            assertEquals(3.0, resDoubleArray[2]);
            
            session.eval( "a <- list(list(1,2),list(3,4))" );
            assertEquals( Arrays.asList( Arrays.asList( 1.0, 2.0 ), Arrays.asList( 3.0, 4.0 ) ), session.getAsObject( "a" ) );
            
            session.eval( "a <- 'Hello!'");
            assertEquals("Hello!", session.getAsObject( "a" ));
            
            session.eval( "a <- " + RWriter.getRString( "test\neof\"quote\\slash\b\rspecial symbols" ) );
            assertEquals("[]", env.error.toString());
            assertEquals("test\neof\"quote\\slash\b\rspecial symbols", session.getAsObject( "a" ));
        
            session.eval( "a <- list('a','b','c')" );
            assertEquals(Arrays.asList("a", "b", "c"), session.getAsObject( "a" ));
            
            session.eval( "a <- list(TRUE,FALSE,TRUE)" );
            assertEquals(Arrays.asList(true, false, true), session.getAsObject( "a" ));
            
            session.eval( "a <- factor(c('a','b','a'), levels = c('a','b','c','d'))" );
            assertEquals(Arrays.asList("a", "b", "a"), Arrays.asList((String[])session.getAsObject( "a" )));
        }
    }

    public static Test noRTests()
    {
        TestSuite suite = new TestSuite();
        suite.addTest( new RDirectTest( "testRWriter" ) );
        return suite;
    }
}
