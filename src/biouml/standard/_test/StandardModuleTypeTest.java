package biouml.standard._test;

import java.io.File;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Module;
import biouml.standard.StandardModuleType;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access.Repository;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.util.ExProperties;
import ru.biosoft.util.TempFiles;

/**
 * Batch unit test for biouml.model package.
 */
public class StandardModuleTypeTest extends TestCase
{
    /** Standart JUnit constructor */
    public StandardModuleTypeTest( String name )
    {
        super(name);
    }

    /**
     * Run test in TestRunner.
     * If args[0].startsWith("text") then textui runner runs,
     * otherwise swingui runner runs.
     * @param args[0] Type of test runner.
     */
    public static void main( String[] args )
    {
        if ( args != null && args.length>0 && args[0].startsWith( "text" ) )
            { junit.textui.TestRunner.run( suite() ); }
        else { junit.swingui.TestRunner.run( StandardModuleTypeTest.class ); }
    }

    /** Make suite if tests. */
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite ( StandardModuleTypeTest.class.getName() );
        suite.addTest(new StandardModuleTypeTest("testCreateModule"));
        return suite;
    }

    //////////////////////////////////////////////////////////////////
    // Test cases
    //

    public void testCreateModule() throws Exception
    {
        File dir = TempFiles.dir("example");
        
        ApplicationUtils.copyFile ( new File(dir, "default.config"), new File("../data/test/biouml/standard/default.config") );

        StandardModuleType standardModuleType = new StandardModuleType();

        ExProperties propRepository = new ExProperties(new File(dir, DataCollectionConfigConstants.DEFAULT_CONFIG_FILE ));
        propRepository.put( DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, dir.getAbsolutePath() );
        CollectionFactory.unregisterAllRoot();
        DataCollection dc = CollectionFactory.createCollection( null, propRepository );
        assertNotNull(dc);

        Module module = standardModuleType.createModule((Repository)dc, "examples");
        assertNotNull("Error creating Module", module);
        assertEquals("Module size error", 4, module.getSize());
    }
}