package biouml.plugins.keynodes._test;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.BioHubRegistry.BioHubInfo;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.biohub.TargetOptions.CollectionRecord;
import biouml.plugins.keynodes.BioHubSelector;

public class ReactomeBioHubTest extends TestCase
{
    public ReactomeBioHubTest(String name)
    {
        super(name);
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(ReactomeBioHubTest.class);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(ReactomeBioHubTest.class.getName());

        suite.addTest(new ReactomeBioHubTest("testSearch"));
        return suite;
    }

    public void testSearch() throws Exception
    {
        CollectionRecord collection = new CollectionRecord("databases/Reactome", true);
        Class.forName("com.mysql.jdbc.Driver");
        BioHub bioHub = null;
        BioHubInfo[] bioHubs = new BioHubSelector().getAvailableValues();
        for( BioHubInfo bioHubInfo : bioHubs )
        {
            if(bioHubInfo.getName().equals("Reactome database"))
            {
                bioHub = bioHubInfo.getBioHub();
                break;
            }
        }
        assertNotNull("Reactome hub is not found", bioHub);

        Element element = new Element("stub/%//163736");
//        163736
//        719415
//        49491
//        54659
//        111802
//        264932
//        264867
//        352174
//        70589

        Element[] re = bioHub.getReference(element, new TargetOptions(collection), null, 2, BioHub.DIRECTION_UP);
        assertTrue("No linked elements found for " + element.getAccession(), re != null && re.length > 0);
        
        if( re != null )
        {
            for( Element dr : re )
            {
                System.out.println(dr);
            }
        }
        else
        {
            System.out.println("result is empty");
        }
    }
}
