
package ru.biosoft.bsa.view.colorscheme._test;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.bsa.Basis;
import ru.biosoft.bsa.Const;
import ru.biosoft.bsa.LinearSequence;
import ru.biosoft.bsa.Nucleotide5LetterAlphabet;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SiteType;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.view.colorscheme.KeyBasedSiteColorScheme;
import ru.biosoft.bsa.view.colorscheme.SiteColorScheme;
import ru.biosoft.bsa.view.colorscheme.SiteTypeKeyGenerator;

/**
 * Testing XML transformer for ColorScheme
 */
public class TransformerTest extends TestCase
{
    protected String repository = "../data";

    /** Standart JUnit constructor */
    public TransformerTest(String name)
    {
        super(name);
    }

    /** Make suite of tests. */
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(TransformerTest.class.getName());

        suite.addTest(new TransformerTest("testTransformer"));

        return suite;
    }


    public void testTransformer() throws Exception
    {
        CollectionFactory.createRepository(repository);
        DataCollection<DataElement> dc = Const.FULL_COLORSCHEMES.getDataCollection();
        SiteColorScheme testObject = getTestObject();
        dc.put(testObject);
        dc.release(testObject.getName());
        DataElement testObject2 = dc.get(testObject.getName());
        assertFalse(testObject == testObject2);
        assertEquals(testObject2.getClass(), testObject.getClass());
        Site s = new SiteImpl( null, "1", SiteType.TYPE_MRNA, Basis.BASIS_ANNOTATED, 1, 10, StrandType.STRAND_PLUS, new LinearSequence(
                "ACGTACGTACGTACGATC", Nucleotide5LetterAlphabet.getInstance() ) );
        assertEquals(testObject.getBrush(s), ((SiteColorScheme)testObject2).getBrush(s));
        dc.remove(testObject.getName());
    }

    protected SiteColorScheme getTestObject()
    {
        KeyBasedSiteColorScheme scheme = new KeyBasedSiteColorScheme();
        scheme.setKeyGenerator(new SiteTypeKeyGenerator());

        SiteTypeKeyGenerator.SiteTypeColor[] types = SiteTypeKeyGenerator.typeMapping;
        for( int i = 0; i < types.length; i++ )
            scheme.setBrush(types[i].key, types[i].color);

        return scheme;
    }
}