package biouml.plugins.sbml.celldesigner._test;

import java.io.File;
import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;

import com.developmentontheedge.application.ApplicationUtils;

/**
 * @author tolstyh
 * Download Panther diagrams images from http://www.pantherdb.org
 */
public class DownloadPantherImagesTest extends AbstractBioUMLTest
{
    protected static final String SOURCE_PATH = "../../models/CellDesigner";//Path to PANTHER SBML(CellDesigner) files
    protected static final String IMAGES_CD = "Images"; //result folder
    protected static final String MODEL_LIST = "Model list.txt"; //file with list of models to test
    protected static final String LINK_PREFIX = "http://www.pantherdb.org/pathway/exportPathwayImage.jsp?pathwayImage=";
    protected static final String STOP_KEYWORD = "END";
            
    public DownloadPantherImagesTest(String name)
    {
        super(name);
    }

    /** Make suite if tests. */
    public static Test suite()
    {
        TestSuite suite = new TestSuite(DownloadPantherImagesTest.class.getName());
        suite.addTest(new DownloadPantherImagesTest("testDownloadImages"));
        return suite;
    }

    public void testDownloadImages() throws Exception
    {
        File listFile = new File(SOURCE_PATH, MODEL_LIST);
        File resultFolder = new File(SOURCE_PATH, IMAGES_CD);
        resultFolder.mkdir();
        TestUtil.setProxy();
        for( String name : ApplicationUtils.readAsList(listFile) )
        {
            if( name.equals(STOP_KEYWORD) )
                return;
            String link = LINK_PREFIX + name + "_sbgn.png";
            File result = new File(resultFolder, name + ".png");
            if( result.exists() )
            {
                System.out.println("Image" + name + " aleady exists.");
                continue;
            }
            try
            {
                TestUtil.downloadImage(link, result);
            }
            catch( Exception e )
            {
                e.printStackTrace();
                if (result.exists())
                    result.deleteOnExit();
                break;
            }
        }
    }
}
