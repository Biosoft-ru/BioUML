package biouml.workbench.module.xml.editor._test;

import javax.swing.JFrame;

import biouml.workbench.module.xml.XmlModule;
import biouml.workbench.module.xml.editor.XmlModuleDialog;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.graphics.editor.ViewPane;

/**
 * Batch unit test for biouml.model package.
 */
public class EditorTest extends TestCase
{
    static String repositoryPath = "../data";

    protected ViewPane viewPane;

    /** Standart JUnit constructor */
    public EditorTest(String name)
    {
        super(name);
    }

    /**
     * Run test in TestRunner.
     * If args[0].startsWith("text") then textui runner runs,
     * otherwise swingui runner runs.
     * @param args[0] Type of test runner.
     */
    public static void main(String[] args)
    {
        if( args != null && args.length > 0 && args[0].startsWith("text") )
        {
            junit.textui.TestRunner.run(suite());
        }
        else
        {
            junit.swingui.TestRunner.run(EditorTest.class);
        }
    }

    /** Make suite if tests. */
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(EditorTest.class.getName());
        suite.addTest(new EditorTest("testEditor"));
        return suite;
    }

    //////////////////////////////////////////////////////////////////
    // Test cases
    //

    public void testEditor() throws Exception
    {
        JFrame frame = new JFrame("");
        frame.show();

        DataCollection repository = CollectionFactory.createRepository(repositoryPath);
        XmlModule module = (XmlModule)repository.get("XmlSQLModule");
        module.initXmlModule();

        XmlModuleDialog xmd = new XmlModuleDialog(frame, "XmlModule editor test", null, module);
        xmd.doModal();
    }
}