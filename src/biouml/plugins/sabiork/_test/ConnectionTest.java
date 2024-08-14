package biouml.plugins.sabiork._test;

import org.eml.sdbv.sabioclient.GetAllCompoundIDs;
import org.eml.sdbv.sabioclient.GetAllEnzymes;
import org.eml.sdbv.sabioclient.GetAllPathways;
import org.eml.sdbv.sabioclient.GetAllReactionIDs;
import org.eml.sdbv.sabioclient.Sabiork_PortType;
import org.eml.sdbv.sabioclient.Sabiork_ServiceLocator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class ConnectionTest extends TestCase
{
    public ConnectionTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(ConnectionTest.class.getName());

        suite.addTest(new ConnectionTest("testConnection"));

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

    public void testConnection() throws Exception
    {
        initService();

        getPathwayNamelist();
        getReactionList();
        getSubstanceList();
        getProteinList();
    }

    public void getPathwayNamelist() throws Exception
    {
        GetAllPathways gp = new GetAllPathways();
        String[] result = spt.getAllPathways(gp);

        System.out.println("- 0 - pathways ----------");
        for( String pathwayName : result )
        {
            System.out.println(pathwayName);
        }
    }

    public void getReactionList() throws Exception
    {
        GetAllReactionIDs gr = new GetAllReactionIDs();
        int[] result = spt.getAllReactionIDs(gr);

        System.out.println("- 1 - reactions ----------");
        for( int id : result )
        {
            System.out.println(id);
        }
    }

    public void getSubstanceList() throws Exception
    {
        GetAllCompoundIDs gc = new GetAllCompoundIDs();
        int[] result = spt.getAllCompoundIDs(gc);

        System.out.println("- 2 - substabces ----------");
        for( int id : result )
        {
            System.out.println(id);
        }
    }

    public void getProteinList() throws Exception
    {
        GetAllEnzymes gp = new GetAllEnzymes();
        String[] result = spt.getAllEnzymes(gp);

        System.out.println("- 3 - proteins ----------");
        for( String id : result )
        {
            System.out.println(id);
        }
    }
}
