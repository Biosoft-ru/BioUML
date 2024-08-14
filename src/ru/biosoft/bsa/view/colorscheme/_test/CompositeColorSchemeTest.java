package ru.biosoft.bsa.view.colorscheme._test;

import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.bsa.view.colorscheme.CompositeColorScheme;
import ru.biosoft.bsa.view.colorscheme.SiteColorScheme;
import ru.biosoft.bsa.view.colorscheme.SiteWeightColorScheme;

/**
 *
 */
public class CompositeColorSchemeTest extends ColorSchemeTest
{
    public CompositeColorSchemeTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( CompositeColorSchemeTest.class.getName() );

        suite.addTest(new CompositeColorSchemeTest("testCreateColorScheme"));
        suite.addTest(new CompositeColorSchemeTest("testViewLegend"));
        suite.addTest(new CompositeColorSchemeTest("testViewModel"));

        return suite;
    }
        
    @Override
    public void testCreateColorScheme() throws Exception
    {
        CompositeColorScheme scheme = new CompositeColorScheme();
        SiteColorScheme[] childs = {new SiteWeightColorScheme()};
        scheme.setColorSchemes(childs);
    }

}
