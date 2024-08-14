package ru.biosoft.access._test;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import junit.framework.TestCase;

import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.core.runtime.spi.RegistryContributor;

import ru.biosoft.access.AccessCoreInit;
import ru.biosoft.access.BiosoftIconManager;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionListenerRegistry;
import ru.biosoft.access.QuerySystemRegistry;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.Environment;
import ru.biosoft.access.security.BiosoftClassLoading;
import ru.biosoft.graphics.View;
import ru.biosoft.graphics.access.DataElementModelResolver;
import ru.biosoft.util.JULBeanLogger;
import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.application.UIStrategy;

/**
 * @author lan
 *
 */
public abstract class AbstractBioUMLTest extends TestCase
{
    private static final File TEST_RESULTS_ROOT = new File("../test");
    private static final DataElementModelResolver viewModelResolver = new DataElementModelResolver();

    static
    {
        AccessCoreInit.init();
        try
        {
            Object masterRegistryKey = new Object();
            Object userRegistryKey = new Object();
            IExtensionRegistry defaultRegistry = RegistryFactory.createRegistry(null, masterRegistryKey, userRegistryKey);
            int i=0;
            for(File pluginsDir : new File[] {new File("../plugins"), new File("../plugconfig")})
            {
                if(!pluginsDir.exists() || !pluginsDir.isDirectory())
                {
                    throw new Exception("No plugins directory found! Check current directory of your test (must be 'BioUML/src')");
                }
                for(final File subDir: pluginsDir.listFiles())
                {
                    if(!subDir.isDirectory()) continue;
                    File pluginFile = new File(subDir, "plugin.xml");
                    if(!pluginFile.exists()) continue;
                    String id = String.valueOf(++i);
                    int dashPos = subDir.getName().lastIndexOf('_');
                    String extensionName = dashPos == -1 ? subDir.getName() : subDir.getName().substring(0, dashPos);
                    defaultRegistry.addContribution(new BufferedInputStream(new FileInputStream(pluginFile)), new RegistryContributor(id,
                            extensionName, id, extensionName), true, pluginFile.toString(), null, masterRegistryKey);
                }
            }
            Application.setExtensionRegistry(defaultRegistry);
        }
        catch( Exception e )
        {
            System.out.println("Unable to initialize registry!");
            e.printStackTrace();
        }
        QuerySystemRegistry.initQuerySystems();
        DataCollectionListenerRegistry.initDataCollectionListeners();
        JULBeanLogger.install();
        setUpLogger();
        Application.setUIStrategy( new UIStrategy()
        {
            @Override
            public void showInfoBox(String msg, String title)
            {
                System.out.println("Info box: "+msg+"\n"+title);
            }

            @Override
            public void showErrorBox(String msg, String title)
            {
                throw new RuntimeException(title+": "+msg);
            }
        } );
        CollectionFactoryUtils.init();
        View.setModelResolver( viewModelResolver );
    }

    private static void setUpLogger()
    {
        Logger rootLogger = Logger.getLogger( "" );
        Handler[] handlers = rootLogger.getHandlers();
        for( Handler h : handlers )
        {
            if( h instanceof ConsoleHandler )
            {
                h.setLevel( Level.OFF );
            }
        }
        rootLogger.addHandler( new DualConsoleHandler() );
    }

    public static class DualConsoleHandler extends StreamHandler
    {

        private final ConsoleHandler stderrHandler = new ConsoleHandler();

        public DualConsoleHandler()
        {
            super( System.out, new SimpleFormatter() );
        }

        @Override
        public void publish(LogRecord record)
        {
            if( record.getLevel().intValue() <= Level.INFO.intValue() )
            {
                super.publish( record );
                super.flush();
            }
            else
            {
                stderrHandler.publish( record );
                stderrHandler.flush();
            }
        }
    }

    public AbstractBioUMLTest()
    {
    }

    public AbstractBioUMLTest(String name)
    {
        super(name);
    }

    @Override
    protected void setUp() throws Exception
    {
        System.out.println("[=== TEST: "+getClass().getName()+"."+getName()+" ===]");
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception
    {
        CollectionFactory.unregisterAllRoot();
    }

    protected void assertImageEquals(BufferedImage expectedImage, BufferedImage testImage)
    {
        if(expectedImage == testImage)
            return;
        assertEquals("Width differs", expectedImage.getWidth(), testImage.getWidth());
        assertEquals("Height differs", expectedImage.getHeight(), testImage.getHeight());
        int[] expectedRaster = expectedImage.getRGB( 0, 0, expectedImage.getWidth(), expectedImage.getHeight(), null, 0, expectedImage.getWidth() );
        int[] testRaster = testImage.getRGB( 0, 0, testImage.getWidth(), testImage.getHeight(), null, 0, testImage.getWidth() );
        if(!Arrays.equals( expectedRaster, testRaster ))
            fail("Raster differs");
    }

    protected static void assertFileEquals(File expectedFile, File testFile) throws Exception
    {
        assertFileEquals("", expectedFile, testFile);
    }

    protected static void assertFileEquals(String message, File expectedFile, File testFile) throws Exception
    {
        message = cleanUpMessage(message);
        try(BufferedReader brOrig = ApplicationUtils.utfReader( expectedFile );
                BufferedReader brTest = ApplicationUtils.utfReader( testFile ))
        {
            int i = 0;
            while( true )
            {
                i++;
                String a = brOrig.readLine();
                String b = brTest.readLine();
                if( a == null )
                {
                    if( b == null )
                    {
                        break;
                    }
                    fail(message + "Different line count");
                }
                else
                {
                    if( b == null )
                    {
                        fail(message + "Different line count");
                    }
                }
                assertEquals(message + "Line [" + i + "] does not match: ", a.trim(), b.trim());
            }
        }
    }

    protected static void assertFileContentEquals(String expectedContent, File file) throws Exception
    {
        assertFileContentEquals("", expectedContent, file);
    }

    protected static void assertFileContentEquals(String message, String expectedContent, File file) throws Exception
    {
        String result = ApplicationUtils.readAsString(file);
        assertEquals(cleanUpMessage(message)+"Content of "+file+" doesn't match", expectedContent, result.replace("\r", "").replaceAll("\\s*$", "").trim());
    }

    protected static void assertArrayEquals(String message, Object[] expected, Object[] actual)
    {
        if(expected == null && actual == null)
            return;
        if(expected == null)
            assertEquals(cleanUpMessage( message )+": expected null", null, Arrays.asList( actual ));
        if(actual == null)
            assertEquals(cleanUpMessage( message )+": actual null", Arrays.asList( expected ), null);
        assertEquals(cleanUpMessage( message )+": length differs", expected.length, actual.length);
        for(int i=0; i<expected.length; i++)
            assertEquals(cleanUpMessage( message )+": ["+i+"] differs", expected[i], actual[i]);
    }

    protected static void assertArrayEquals(String message, double[] expected, double[] actual, double error)
    {
        if(expected == null && actual == null)
            return;
        if(expected == null)
            assertTrue(cleanUpMessage(message) + ": expected null", actual == null);
        if( actual == null )
            assertTrue(cleanUpMessage(message) + ": actual null", expected == null);
        assertEquals(cleanUpMessage( message )+": length differs", expected.length, actual.length);
        for(int i=0; i<expected.length; i++)
            assertEquals(cleanUpMessage( message )+": ["+i+"] differs", expected[i], actual[i], error);
    }

    private static String cleanUpMessage(String message)
    {
        if(message == null)
            message = "";
        if(!message.isEmpty())
            message+=": ";
        return message;
    }

    /**
     * Creates directory to store test results and return it
     * @return directory
     */
    public static File getTestDir()
    {
        for(StackTraceElement ste : new Exception().getStackTrace())
        {
            if(ste.getClassName().equals( AbstractBioUMLTest.class.getName() ))
                continue;
            if(ste.getClassName().contains( "._test." ))
            {
                String folderName = ste.getClassName().substring( 0, ste.getClassName().indexOf( "._test." ) );
                File f = new File(TEST_RESULTS_ROOT, folderName);
                if(!f.isDirectory())
                    f.mkdirs();
                if(!f.isDirectory())
                {
                    throw new RuntimeException( "Unable to create "+f );
                }
                return f;
            }
        }
        throw new RuntimeException( "getTestDir() must be called from unit-test inside _test package" );
    }

    /**
     * @param fileName
     * @return File object with given name inside the directory for tests (directory will be created if necessary)
     */
    public static File getTestFile(String fileName)
    {
        return new File(getTestDir(), fileName);
    }
}
