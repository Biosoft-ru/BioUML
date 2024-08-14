package biouml.plugins.sbml.biomodels._test;

import java.io.File;

import biouml.plugins.sbml.celldesigner._test.TestUtil;
import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;

/**
 * @author axec
 * Download Biomodels SBML models from https://www.ebi.ac.uk/
 */
public class DownloadBiomodelsTest extends AbstractBioUMLTest
{
    private static final String SBML_FOLDER = "/home/axec/Biomodels"; //result folder   

    private int modelsLimit = 706;
    private static final String LINK_PREFIX = "https://www.ebi.ac.uk/biomodels-main/download?mid=";
    private static final String LINK_POSTFIX = "&anno=urn";

    public DownloadBiomodelsTest(String name)
    {
        super( name );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( DownloadBiomodelsTest.class.getName() );
        suite.addTest( new DownloadBiomodelsTest( "test" ) );
        return suite;
    }

    public void test() throws Exception
    {
        File dir = new File( SBML_FOLDER );
        dir.mkdirs();


        for( int i = 1; i <= modelsLimit; i++ )
        {

            String name = "BIOMD" + String.format( "%010d", i );

            String link = LINK_PREFIX + name + LINK_POSTFIX;

            File result = new File( dir, name + ".sbml" );
            if( result.exists() )
            {
                System.out.println( "SBML file" + name + " aleady exists." );
                continue;
            }
            try
            {
                TestUtil.download( link, result );
            }
            catch( Exception e )
            {
                e.printStackTrace();
                if( result.exists() )
                    result.delete();
            }
        }
    }
}
