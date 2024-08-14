package biouml.plugins.glycan.parser._test;

import java.io.StringReader;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import biouml.plugins.glycan.parser.GlycanParser;
import biouml.plugins.glycan.parser.GlycanTree;

public class GlycanParserTest extends TestCase
{
    public GlycanParserTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(GlycanParserTest.class.getName());

        suite.addTest(new GlycanParserTest("testParsing"));

        return suite;
    }

    private static GlycanParser parser = new GlycanParser();

    public void testParsing() throws Exception
    {
        String[][] tests = new String[][] {new String[] {"Ma2(Mb3)NN", "NN(b3M)a2M"}, new String[] {"GNb4G", "Gb4GN"},
                new String[] {"Ma3(Ma2Ma3(Ma6)Ma6)M", "M(a6M(a6M)a3Ma2M)a3M"},
                new String[] {"GNb2Ma3(Ma3(Ma6)Ma6)Mb4GN", "GNb4M(a6M(a6M)a3M)a3Mb2GN"},
                new String[] {"(Ma3(Ma6)Ma6)Mb4GN", "GNb4Ma6M(a6M)a3M"},
                new String[] {"(Ma3(Mb4))M", "[Syntax error: Branch should be connected to molecule. Error in: '(a3M(b4M))'.]"},
                new String[] {"Ma3(Ma2Ma3)(Ma6)Ma6M", "Ma6M(a6M)(a3Ma2M)a3M"}, new String[] {"AN", "AN"},
                new String[] {"Ab3(Fa4)GNb2Ma3(Ab3(Fa4)GNb2Ma6)Mb4GNb4(Fa6)GN", "GN(a6F)b4GNb4M(a6Mb2GN(a4F)b3A)a3Mb2GN(a4F)b3A"},
                new String[] {"Fa2Ab3(Fa4)GNb2Ma3(Ab3GNb2Ma6)Mb4GNb4(Fa6)GN", "GN(a6F)b4GNb4M(a6Mb2GNb3A)a3Mb2GN(a4F)b3Aa2F"},
                new String[] {"Ab4GNb2Ma3(Ab3GNb2(Ab4GNb6)Ma6)Mb4GNb4GN", "GNb4GNb4M(a6M(b6GNb4A)b2GNb3A)a3Mb2GNb4A"},
                new String[] {"(Ma2)(Mb3)NN", "NN(b3M)a2M"}, new String[] {"Ab3(Ma3(Ma2Ma3)(Ma6)Ma6)M", "M(a6M(a6M)(a3Ma2M)a3M)b3A"},
                new String[] {"GNb2Ma3(Ma3(Ma6))M", "[Syntax error: Branch should be connected to molecule. Error in: '(a3M(a6M))'.]"},};
        GlycanTree tree;
        for( String[] test : tests )
        {
            tree = parser.parse(new StringReader(test[0]));
            if( parser.getStatus() != GlycanParser.STATUS_OK )
            {
                assertEquals(test[1], parser.getMessages().toString());
            }
            else
            {
                String treeString = tree.toString();
                assertEquals(test[1], treeString);
            }
        }
    }
}
