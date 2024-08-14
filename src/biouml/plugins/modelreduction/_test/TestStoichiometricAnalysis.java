package biouml.plugins.modelreduction._test;

import java.util.ArrayList;
import java.util.List;

import ru.biosoft.table.TableDataCollection;
import biouml.model.Diagram;
import biouml.plugins.modelreduction.StoichiometricAnalysis;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestStoichiometricAnalysis extends TestCase
{
    public TestStoichiometricAnalysis(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestStoichiometricAnalysis.class);
        return suite;
    }

    public void testStoichiometricAnalysis() throws Exception
    {
        Diagram diagram = TestUtils.createTestDiagram_1();
        TableDataCollection tdc = StoichiometricAnalysis.getStoichiometricMatrix(diagram);
        assertEquals(true, isResultOK(tdc));
    }

    private int e, s, c1, c2, p;

    private boolean isResultOK(TableDataCollection tdc)
    {
        List<String> species = new ArrayList<>();
        for( int i = 0; i < tdc.getSize(); ++i )
        {
            species.add(tdc.getAt(i).getName());
        }

        //reaction[0]: e + s -> c1
        initSt(tdc, species, 0);
        if( e != -1 || s != -1 || c1 != 1 || c2 != 0 || p != 0 )
            return false;

        //reaction[1]: c1 -> e + s
        initSt(tdc, species, 1);
        if( e != 1 || s != 1 || c1 != -1 || c2 != 0 || p != 0 )
            return false;

        //reaction[2]: c1 -> c2
        initSt(tdc, species, 2);
        if( e != 0 || s != 0 || c1 != -1 || c2 != 1 || p != 0 )
            return false;

        //reaction[3]: c2 -> c1
        initSt(tdc, species, 3);
        if( e != 0 || s != 0 || c1 != 1 || c2 != -1 || p != 0 )
            return false;

        //reaction[4]: c2 -> p + e
        initSt(tdc, species, 4);
        if( e != 1 || s != 0 || c1 != 0 || c2 != -1 || p != 1 )
            return false;

        return true;
    }

    private void initSt(TableDataCollection tdc, List<String> species, int reaction)
    {
        e = (Integer)tdc.getValueAt(species.indexOf("$e"), reaction);
        s = (Integer)tdc.getValueAt(species.indexOf("$s"), reaction);
        c1 = (Integer)tdc.getValueAt(species.indexOf("$c1"), reaction);
        c2 = (Integer)tdc.getValueAt(species.indexOf("$c2"), reaction);
        p = (Integer)tdc.getValueAt(species.indexOf("$p"), reaction);
    }
}
