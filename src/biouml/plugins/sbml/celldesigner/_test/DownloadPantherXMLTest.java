package biouml.plugins.sbml.celldesigner._test;

import java.io.File;

import com.developmentontheedge.application.ApplicationUtils;

import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;

/**
 * @author tolstyh
 * Download Panther XML files from http://www.pantherdb.org
 */
public class DownloadPantherXMLTest extends AbstractBioUMLTest
{
    protected static final String SOURCE_PATH = "../../models/CellDesigner"; // Path to PANTHER SBML(CellDesigner) files
    protected static final String SBML_FOLDER = "SBML"; //result folder
    protected static final String MODEL_LIST = "Model list.txt"; //file with list of models to test
    protected static final String LINK_PREFIX = "http://www.pantherdb.org/pathway/exportSBML.jsp?pathwayFile=";
    protected static final String STOP_KEYWORD = "END";

    public DownloadPantherXMLTest(String name)
    {
        super(name);
    }

    /** Make suite if tests. */
    public static Test suite()
    {
        TestSuite suite = new TestSuite(DownloadPantherXMLTest.class.getName());
        suite.addTest(new DownloadPantherXMLTest("testDownloadXML"));
        return suite;
    }

    public void testDownloadXML() throws Exception
    {
        File dir = new File(SOURCE_PATH);
        File newDir = new File(dir, SBML_FOLDER);
        newDir.mkdirs();
        File fileList = new File(SOURCE_PATH, MODEL_LIST);
        TestUtil.setProxy();
        for( String name : ApplicationUtils.readAsList(fileList) )
        {
            if( name.equals(STOP_KEYWORD) )
                break;
            String link = LINK_PREFIX + name + ".xml";
            File result = new File(newDir, name + ".xml");
            if( result.exists() )
            {
                System.out.println("SBML file" + name + " aleady exists.");
                continue;
            }
            try
            {
                TestUtil.download(link, result);
            }
            catch( Exception e )
            {
                e.printStackTrace();
                if( result.exists() )
                    result.delete();
                testDownloadXML();
            }
        }
    }
}
