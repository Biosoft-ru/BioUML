package ru.biosoft.plugins.javascript._test;

import java.util.Arrays;
import java.util.concurrent.SynchronousQueue;
import java.util.logging.Logger;

import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.FolderVectorCollection;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access._test.TestEnvironment;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.log.BiosoftLogger;
import ru.biosoft.access.log.DefaultBiosoftLogger;
import ru.biosoft.access.log.StringBufferListener;
import ru.biosoft.access.script.LogScriptEnvironment;
import ru.biosoft.access.script.NullScriptEnvironment;
import ru.biosoft.access.script.ScriptEnvironment;
import ru.biosoft.access.script.ScriptJobControl;
import ru.biosoft.access.script.ScriptJobControl.BreakType;
import ru.biosoft.access.script.ScriptJobControl.ScriptStackTraceElement;
import ru.biosoft.access.script.ScriptTypeRegistry;
import ru.biosoft.access.security.PrivilegedAction;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListenerAdapter;
import ru.biosoft.plugins.Plugins;
import ru.biosoft.plugins.javascript.JSElement;
import ru.biosoft.tasks.TaskInfo;
import ru.biosoft.tasks.TaskManager;

public class TestJSElement extends AbstractBioUMLTest
{
    private static final Logger log = Logger.getLogger( TestJSElement.class.getName() );

    public void testNoScopeTangling() throws Exception
    {
        ru.biosoft.access.security.SecurityManager.runPrivileged( new PrivilegedAction()
        {
            @Override
            public Object run() throws Exception
            {
                ScriptEnvironment env = new LogScriptEnvironment( log, false );

                ScriptTypeRegistry.execute( "js", "var x=1", env, false );
                String result = ScriptTypeRegistry.execute( "js", "if( typeof x === 'undefined') 'ok'; else 'error';", env, false );
                assertEquals( "ok", result );
                return null;
            }
        } );
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        Plugins.getPlugins();
    }

    public void testJSElementDebugger() throws Exception
    {
        JSElement jsElement = new JSElement(null, "hello.js", "function test()\n"
                + "{\n"
                + "print('Hello from test');\n"
                + "print('Goodbye from test');\n"
                + "}\n"
                + "print('Hello!');\n"
                + "test();\n"
                + "test();\n"
                + "print('Goodbye!');\n");
        StringBuffer buf = new StringBuffer();
        BiosoftLogger logger = new DefaultBiosoftLogger();
        try(StringBufferListener sb = new StringBufferListener( buf, logger ))
        {
            TaskInfo task = jsElement.createTask( jsElement.getContent(), new LogScriptEnvironment( logger ), false );
            DebuggerTester tester = new DebuggerTester( task );
            assertEquals(6, tester.step( BreakType.STEP_IN ));
            assertEquals("", buf.toString());
            assertEquals(7, tester.step( BreakType.STEP_OVER ));
            assertEquals("INFO: Hello!\n", buf.toString());
            buf.setLength( 0 );
            assertEquals(2, tester.step( BreakType.STEP_IN ));
            assertEquals("", buf.toString());
            assertEquals(3, tester.step( BreakType.STEP_IN ));
            assertEquals("", buf.toString());
            assertEquals(4, tester.step( BreakType.STEP_OVER ));
            assertEquals("INFO: Hello from test\n", buf.toString());
            buf.setLength( 0 );
            assertEquals(8, tester.step( BreakType.STEP_OUT ));
            assertEquals("INFO: Goodbye from test\n", buf.toString());
            buf.setLength( 0 );
            assertEquals(9, tester.step( BreakType.STEP_OVER ));
            assertEquals("INFO: Hello from test\nINFO: Goodbye from test\n", buf.toString());
            buf.setLength( 0 );
            assertEquals(9, tester.step( BreakType.NONE ));
            assertEquals(JobControl.COMPLETED, task.getJobControl().getStatus());
            assertTrue( buf.toString().startsWith( "INFO: Goodbye!\n" ) ); // System.out is appended which may contain logger messages, thus not equals check
        }
    }

    public void testBreakOnLine() throws Exception
    {
        JSElement jsElement = new JSElement(null, "hello.js", "print('First line');\n"
                + "\n"
                + "print('Third line');\n"
                + "print('Fourth line');\n"
                + "print('Fifth line');\n");
        StringBuffer buf = new StringBuffer();
        BiosoftLogger logger = new DefaultBiosoftLogger();
        try(StringBufferListener sb = new StringBufferListener( buf, logger ))
        {
            TaskInfo task = jsElement.createTask( jsElement.getContent(), new LogScriptEnvironment( logger ), false );
            DebuggerTester tester = new DebuggerTester( task );
            assertEquals(4, tester.step( BreakType.line( 4 ) ));
            assertEquals("INFO: First line\nINFO: Third line\n", sb.toString());
            tester.terminate();
        }
    }

    public void testDebuggerStatement() throws Exception
    {
        JSElement jsElement = new JSElement(null, "hello.js", "print('First line');\n"
                + "debugger;\n"
                + "print('Third line');\n");
        StringBuffer buf = new StringBuffer();
        BiosoftLogger logger = new DefaultBiosoftLogger();
        try(StringBufferListener sb = new StringBufferListener( buf, logger ))
        {
            TaskInfo task = jsElement.createTask( jsElement.getContent(), new LogScriptEnvironment( logger ), false );
            DebuggerTester tester = new DebuggerTester( task );
            assertEquals(1, tester.step( BreakType.NONE )); // ?? why 1?
            assertEquals("INFO: First line\n", sb.toString());
            assertEquals(3, tester.step( BreakType.NONE ));
            assertTrue( buf.toString().startsWith( "INFO: First line\nINFO: Third line\n" ) );
        }
    }

    public void testWatch() throws Exception
    {
        JSElement jsElement = new JSElement(null, "hello.js", "a = 5;\na = 7;");
        StringBuffer buf = new StringBuffer();
        BiosoftLogger logger = new DefaultBiosoftLogger();
        try(StringBufferListener sb = new StringBufferListener( buf, logger ))
        {
            TaskInfo task = jsElement.createTask( jsElement.getContent(), new LogScriptEnvironment( logger ), false );
            DebuggerTester tester = new DebuggerTester( task );
            assertEquals(1, tester.step( BreakType.STEP_IN ));
            assertEquals("[]", tester.sjc.getVariables().toString());
            Object calcResult = tester.sjc.calc( "2*a" );
            assertEquals("ReferenceError: \"a\" is not defined.", ((Exception)calcResult).getMessage());
            assertEquals(2, tester.step( BreakType.STEP_IN ));
            calcResult = tester.sjc.calc( "2*a" );
            assertEquals(10.0, calcResult);
            assertEquals("[a]", tester.sjc.getVariables().toString());
        }
    }

    public void testVariables() throws Exception
    {
        JSElement jsElement = new JSElement(null, "hello.js", "var a = 5;\na = 7;");
        TaskInfo task = jsElement.createTask( jsElement.getContent(), new NullScriptEnvironment(), false );
        DebuggerTester tester = new DebuggerTester( task );
        assertEquals(2, tester.step( BreakType.line( 2 ) ));
        assertEquals("[a]", tester.sjc.getVariables().toString());
        tester.step( BreakType.NONE );
    }

    public void testVariablesCalc() throws Exception
    {
        FolderVectorCollection fvc = new FolderVectorCollection( "data", null );
        CollectionFactory.registerRoot( fvc );
        DataCollectionUtils.createSubCollection( DataElementPath.create("data/aaa") );
        DataCollectionUtils.createSubCollection( DataElementPath.create("data/bbb") );
        DataCollectionUtils.createSubCollection( DataElementPath.create("data/ccc") );
        JSElement jsElement = new JSElement( null, "hello.js", "var a = 5;\n"
                + "var nativeJavaObject = data.get('data');\n"
                + "var nativeJavaArray = nativeJavaObject.getNameList().toArray();\n"
                + "var str = 'string';\n"
                + "var arr = [9,2,{a:str, b:500}];\n"
                + "var n = true;\n"
                + "n = null;\n"
                + "var car = {};\n"
                + "var b = 7;");
        TestEnvironment env = new TestEnvironment();
        TaskInfo task = jsElement.createTask( jsElement.getContent(), env, false );
        DebuggerTester tester = new DebuggerTester( task );
        assertEquals("[]", env.error.toString());
        assertEquals( 7, tester.step( BreakType.line( 7 ) ) );
        Object calcResult = tester.sjc.calc( "a" );
        assertEquals( "Number", tester.sjc.getObjectType( calcResult ) );
        calcResult = tester.sjc.calc( "nativeJavaObject" );
        assertEquals( "JavaObject: FolderVectorCollection", tester.sjc.getObjectType( calcResult ) );
        assertEquals( "ru.biosoft.access.FolderVectorCollection: data", tester.sjc.objectToString( calcResult ) );
        calcResult = tester.sjc.calc( "nativeJavaArray" );
        assertEquals( "JavaArray: Object[]", tester.sjc.getObjectType( calcResult ) );
        assertEquals( "[aaa, bbb, ccc]", tester.sjc.objectToString( calcResult ) );
        calcResult = tester.sjc.calc( "str" );
        assertEquals( "String", tester.sjc.getObjectType( calcResult ) );
        assertEquals( "\"string\"", tester.sjc.objectToString( calcResult ) );
        calcResult = tester.sjc.calc( "arr" );
        assertEquals( "Array", tester.sjc.getObjectType( calcResult ) );
        assertEquals( "[9.0, 2.0, {\"a\": \"string\", \"b\": 500.0}]", tester.sjc.objectToString( calcResult ) );
        calcResult = tester.sjc.calc( "n" );
        assertEquals( "Boolean", tester.sjc.getObjectType( calcResult ) );
        assertEquals( "true", tester.sjc.objectToString( calcResult ) );
        calcResult = tester.sjc.calc( "b" );
        assertEquals( "undefined", tester.sjc.getObjectType( calcResult ) );
        assertEquals( "undefined", tester.sjc.objectToString( calcResult ) );
        assertEquals( 9, tester.step( BreakType.line( 9 ) ) );
        calcResult = tester.sjc.calc( "n" );
        assertEquals( "null", tester.sjc.getObjectType( calcResult ) );
        assertEquals( "null", tester.sjc.objectToString( calcResult ) );
        calcResult = tester.sjc.calc( "car" );
        assertEquals( "Object", tester.sjc.getObjectType( calcResult ) );
        assertEquals( "{}", tester.sjc.objectToString( calcResult ) );
        assertEquals( "[a, arr, b, car, n, nativeJavaArray, nativeJavaObject, str]", tester.sjc.getVariables().toString() );
        tester.step( BreakType.NONE );
    }

    public void testBreakPoints() throws Exception
    {
        JSElement jsElement = new JSElement(null, "hello.js", "print('First line');\n"
                + "print('Second line');\n"
                + "print('Third line');\n"
                + "\n"
                + "\n"
                + "print('Sixth line');\n"
                + "print('Seventh line');\n");
        StringBuffer buf = new StringBuffer();
        BiosoftLogger logger = new DefaultBiosoftLogger();
        try(StringBufferListener sb = new StringBufferListener( buf, logger ))
        {
            TaskInfo task = jsElement.createTask( jsElement.getContent(), new LogScriptEnvironment( logger ), false );
            DebuggerTester tester = new DebuggerTester( task );
            assertEquals(1, tester.step( BreakType.STEP_IN ));
            tester.sjc.addBreakpoints( 3, 6 );
            assertEquals(3, tester.step( BreakType.NONE ));
            assertEquals(6, tester.step( BreakType.NONE ));
            assertEquals(7, tester.step( BreakType.NONE ));
        }
    }

    public void testRemoveBreakPoints() throws Exception
    {
        JSElement jsElement = new JSElement(null, "hello.js", "for(var i=0; i<10; i++) {\n"
                + "print(i);\n"
                + "}");
        StringBuffer buf = new StringBuffer();
        BiosoftLogger logger = new DefaultBiosoftLogger();
        try(StringBufferListener sb = new StringBufferListener( buf, logger ))
        {
            TaskInfo task = jsElement.createTask( jsElement.getContent(), new LogScriptEnvironment( logger ), false );
            DebuggerTester tester = new DebuggerTester( task );
            tester.sjc.addBreakpoints( 2 );
            assertEquals(2, tester.step( BreakType.NONE ));
            assertEquals(2, tester.step( BreakType.NONE ));
            assertEquals(2, tester.step( BreakType.NONE ));
            assertEquals("INFO: 0\nINFO: 1\n", buf.toString());
            tester.sjc.removeBreakpoints( 2 );
            tester.step( BreakType.NONE );
            assertTrue(buf.toString().startsWith( "INFO: 0\nINFO: 1\nINFO: 2\nINFO: 3\nINFO: 4\nINFO: 5\n" ));
        }
    }

    public void testCallStack() throws Exception
    {
        JSElement jsElement = new JSElement(null, "hello.js", "function f1() {\n"
                + "f2();\n"
                + "}\n"
                + "function f2(a,b,c) {\n"
                + "f3();\n"
                + "}\n"
                + "function f3(d) {\n"
                + "print('Hello from f3');\n"
                + "}\n"
                + "f1();\n");
        StringBuffer buf = new StringBuffer();
        BiosoftLogger logger = new DefaultBiosoftLogger();
        try(StringBufferListener sb = new StringBufferListener( buf, logger ))
        {
            TaskInfo task = jsElement.createTask( jsElement.getContent(), new LogScriptEnvironment( logger ), false );
            DebuggerTester tester = new DebuggerTester( task );
            assertEquals(8, tester.step( BreakType.line( 8 ) ));
            ScriptStackTraceElement[] stackTrace = tester.sjc.getStackTrace();
            assertEquals(4, stackTrace.length);
            ScriptStackTraceElement[] expected = new ScriptStackTraceElement[] {
                    new ScriptStackTraceElement( 10, "(top)" ),
                    new ScriptStackTraceElement( 2, "f1()" ),
                    new ScriptStackTraceElement( 5, "f2(a, b, c)" ),
                    new ScriptStackTraceElement( 8, "f3(d)" )
            };
            assertTrue(Arrays.asList( stackTrace ).toString(), Arrays.equals( expected, stackTrace ));
            tester.terminate();
        }
    }

    static class DebuggerTester
    {
        private final TaskInfo task;
        private final ScriptJobControl sjc;
        private final SynchronousQueue<Boolean> lock = new SynchronousQueue<>();
        private final JobControlListenerAdapter listener;

        public DebuggerTester(TaskInfo task)
        {
            this.task = task;
            this.sjc = (ScriptJobControl)task.getJobControl();
            this.listener = new JobControlListenerAdapter() {
                @Override
                public void jobTerminated(JobControlEvent event)
                {
                    offer( lock );
                    sjc.removeListener( this );
                }

                @Override
                public void jobPaused(JobControlEvent event)
                {
                    offer( lock );
                }

                protected void offer(final SynchronousQueue<Boolean> lock)
                {
                    while( true )
                    {
                        try
                        {
                            lock.put( Boolean.TRUE );
                            break;
                        }
                        catch( InterruptedException e )
                        {
                            continue;
                        }
                    }
                }
            };
            sjc.addListener( listener);
        }

        public int step(BreakType type)
        {
            sjc.breakOn( type );
            if(sjc.getStatus() == JobControl.PAUSED)
                sjc.resume();
            else
                TaskManager.getInstance().runTask( task );
            while( true )
            {
                try
                {
                    lock.take();
                    break;
                }
                catch( InterruptedException e )
                {
                    continue;
                }
            }
            return sjc.getCurrentLine();
        }

        public void terminate()
        {
            sjc.removeListener( listener );
            sjc.terminate();
        }
    }

}
