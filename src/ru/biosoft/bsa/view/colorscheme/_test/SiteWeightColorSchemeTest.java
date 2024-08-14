package ru.biosoft.bsa.view.colorscheme._test;

import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.bsa.view.colorscheme.SiteWeightColorScheme;

/**
 *
 */
public class SiteWeightColorSchemeTest extends ColorSchemeTest
{
    public SiteWeightColorSchemeTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( CompositeColorSchemeTest.class.getName() );

        suite.addTest(new SiteWeightColorSchemeTest("testCreateColorScheme"));
        suite.addTest(new SiteWeightColorSchemeTest("testViewLegend"));
        suite.addTest(new SiteWeightColorSchemeTest("testViewModel"));

        return suite;
    }

    @Override
    public void testCreateColorScheme() throws Exception
    {
        colorScheme = new SiteWeightColorScheme();
        assertNotNull("ColorScheme instance is created", colorScheme);
    }

}
