package biouml.plugins.sabiork._test;

import java.util.List;

import biouml.model.Diagram;
import biouml.plugins.sabiork.PathwayProvider;
import biouml.standard.type.Reaction;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;

public class CacheReactionsTest extends TestCase
{
    public static final String repositoryPath = "../data";

    public CacheReactionsTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(CacheReactionsTest.class.getName());

        suite.addTest(new CacheReactionsTest("testLoadReactions"));

        return suite;
    }

    public void testLoadReactions() throws Exception
    {
        CollectionFactory.createRepository(repositoryPath);
        DataCollection<Diagram> diagrams = DataElementPath.create("databases/SABIO-RK/Diagrams").getDataCollection(Diagram.class);
        DataCollection<Reaction> reactions = DataElementPath.create("databases/SABIO-RK/Data/reaction").getDataCollection(Reaction.class);
        List<String> nameList = diagrams.getNameList();

        PathwayProvider provider = new PathwayProvider();
        int i = 0;
        for( String name : nameList )
        {
            int[] reactionIDs = provider.getReactionsIDs(name);
            int j = 0;
            for( int id : reactionIDs )
            {
                reactions.get(Integer.toString(id));
                System.err.println("\t   -" + ++j + " of " + reactionIDs.length);
            }
            System.err.println("\t " + ++i + " of " + nameList.size());
        }
    }
}
