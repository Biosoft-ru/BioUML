package biouml.plugins.sabiork._test;

import org.eml.sdbv.sabioclient.GetAllPathways;
import org.eml.sdbv.sabioclient.GetReactionIDs;
import org.eml.sdbv.sabioclient.GetSBML;
import org.eml.sdbv.sabioclient.GetSBMLResponse;
import org.eml.sdbv.sabioclient.Sabiork_PortType;
import org.eml.sdbv.sabioclient.Sabiork_ServiceLocator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class LoadSBMLTest extends TestCase
{
    public LoadSBMLTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(LoadSBMLTest.class.getName());

        suite.addTest(new LoadSBMLTest("testLoadSBML"));

        return suite;
    }

    protected Sabiork_PortType spt;

    public void initService() throws Exception
    {
        String proxyHost = "amber.developmentontheedge.com";
        String proxyPort = "8080";
        System.setProperty("proxyHost", proxyHost);
        System.setProperty("proxyPort", proxyPort);
        System.setProperty("proxySet", "true");

        System.setProperty("http.proxyHost", proxyHost);
        System.setProperty("http.proxyPort", proxyPort);
        System.setProperty("http.proxySet", "true");

        System.setProperty("https.proxyHost", proxyHost);
        System.setProperty("https.proxyPort", proxyPort);
        System.setProperty("https.proxySet", "true");

        Sabiork_ServiceLocator service = new Sabiork_ServiceLocator();
        spt = service.get_8();
    }

    public void testLoadSBML() throws Exception
    {
        initService();

        GetAllPathways gp = new GetAllPathways();
        String[] pathways = spt.getAllPathways(gp);

        String pathwayName = pathways[0];

        int[] reactionIDs = spt.getReactionIDs(new GetReactionIDs(pathwayName));
        if( reactionIDs != null )
        {
            /*Integer[] reactions = new Integer[reactionIDs.length];
            for( int i = 0; i < reactionIDs.length; i++ )
            {
                reactions[i] = reactionIDs[i];
            }*/
            Integer[] kinlaws = {};
            Integer[] reactions ={};
            GetSBMLResponse sbmlResponse = spt.getSBML(new GetSBML(reactions, kinlaws, 2, 3, "aaa", false));
            System.out.println(sbmlResponse.get_return());
        }
    }
}
