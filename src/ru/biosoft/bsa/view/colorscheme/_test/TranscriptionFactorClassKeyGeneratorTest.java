
package ru.biosoft.bsa.view.colorscheme._test;

import java.io.File;
import java.io.FileInputStream;
import java.util.Iterator;
import java.util.List;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import biouml.model.Module;
import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.Const;
import ru.biosoft.bsa.TransfacTranscriptionFactor;
import ru.biosoft.bsa._test.BSATestUtils;

/** General class to test a SiteColorScheme. */
public class TranscriptionFactorClassKeyGeneratorTest extends AbstractBioUMLTest
{
    protected Logger cat = Logger.getLogger(TranscriptionFactorClassKeyGeneratorTest.class.getName());
    private static final String TRANSFAC_COLLECTION_PREFIX = "transfac";
    public TranscriptionFactorClassKeyGeneratorTest(String name)
    {
        super(name);

        File configFile = new File( "./ru/biosoft/bsa/view/colorscheme/_test/test.lcf" );
        try( FileInputStream fis = new FileInputStream( configFile ) )
        {
            LogManager.getLogManager().readConfiguration( fis );
        }
        catch( Exception e1 )
        {
            System.err.println( "Error init logging: " + e1.getMessage() );
        }
        cat.info("Start test: " + name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite();
        suite.addTest(new TranscriptionFactorClassKeyGeneratorTest("testCreateRepository"));
        suite.addTest(new TranscriptionFactorClassKeyGeneratorTest("testTranscriptionFactorData"));
        return suite;
    }

    ////////////////////////////////////////
    // Test methods
    //

    public void testCreateRepository() throws Exception
    {
        DataCollection<?> repository = CollectionFactory.createRepository("../data");
        List<String> nameList = repository.getNameList();
        for (Iterator<String> iter = nameList.iterator(); iter.hasNext();)
        {
            String name = iter.next();
            if(name.toLowerCase().startsWith(TRANSFAC_COLLECTION_PREFIX))
            {
                DataCollection<?> transfacDC = (DataCollection<?>)repository.get(name);
                assertNotNull("Transfac DC", transfacDC);
                DataElementPath transfacDCPath = DataElementPath.create(transfacDC);
                assertNotNull("Transcription factor DC", transfacDCPath.getChildPath(Module.DATA).getRelativePath(Const.TRANSFAC_FACTORS).getDataElement());
                break;
            }
        }
    }

    public void testTranscriptionFactorData(String factorName) throws Exception
    {
        DataCollection<?> repository = BSATestUtils.createRepository();
        DataElementPath factorPath = DataElementPath.create("databases").getChildPath("transfac_test", Const.TRANSFAC_FACTORS, factorName);
        TransfacTranscriptionFactor tf = factorPath.getDataElement(TransfacTranscriptionFactor.class);
        assertNotNull("get transcription factor = " + factorName, tf);
        assertNotNull("transcription factor class name", tf.getGeneralClassPath());
        assertNotNull("transcription factor class unit", tf.getGeneralClass());
    }

    public void testTranscriptionFactorData() throws Exception
    {
        testTranscriptionFactorData("T02466");
        testTranscriptionFactorData("T00033");
        testTranscriptionFactorData("T02468");
    }

    // test for one: "V$AP4_01"
    // test for common ancestor: "V$AP2_Q6", "V$ER_Q6"
    // test for unclassified
    // test that hashmap is used
    static public void main(String[] args)
    {
        junit.textui.TestRunner.run(suite());
    }
}
