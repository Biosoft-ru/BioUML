package biouml.plugins.bionetgen._test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import biouml.plugins.bionetgen.diagram.BionetgenToGlycanConverter;

public class BionetgenToGlycanConverterTest extends TestCase
{
    public BionetgenToGlycanConverterTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(BionetgenToGlycanConverterTest.class.getName());

        suite.addTest(new BionetgenToGlycanConverterTest("testToGlycanConverter"));

        return suite;
    }

    public void testToGlycanConverter()
    {
        String[][] tests = new String[][] {
                new String[] {
                        "M(a2!1,a2,a3,a3,a6,a6,b2,b4,b4,b6).M(a2!1,a2!2,a3,a3,a6,a6,b2,b4,b4,b6).M(a2,a2!2,a3!3,a3,a6,a6,b2,b4,b4,b6)."
                                + "M(a2!4,a2,a3,a3,a6,a6,b2,b4,b4,b6).M(a2!4,a2,a3!5,a3,a6,a6,b2,b4,b4,b6).M(a2!6,a2,a3,a3,a6,a6,b2,b4,b4,b6)."
                                + "M(a2!6,a2,a3,a3,a6!7,a6,b2,b4,b4,b6).M(a2,a2,a3!5,a3,a6!7,a6!8,b2,b4,b4,b6).M(a2,a2,a3!3,a3,a6!8,a6,b2,b4!9,b4,b6)"
                                + ".GN(a3,a4,a6,b2,b3,b3,b4!9,b4!10,b6,r~0).GN(a3,a4,a6,b2,b3,b3,b4!10,b4,b6,r~1)",
                        "Ma2Ma2Ma3(Ma2Ma3(Ma2Ma6)Ma6)Mb4GNb4GN"},
                new String[] {
                        "M(a2!1,a2,a3,a3,a6,a6,b2,b4,b4,b6).M(a2!1,a2!2,a3,a3,a6,a6,b2,b4,b4,b6).M(a2,a2!2,a3!3,a3,a6,a6,b2,b4,b4,b6)"
                                + ".M(a2,a2,a3!4,a3,a6,a6,b2,b4,b4,b6).M(a2!5,a2,a3,a3,a6,a6,b2,b4,b4,b6).M(a2!5,a2,a3,a3,a6!6,a6,b2,b4,b4,b6)"
                                + ".M(a2,a2,a3!4,a3,a6!6,a6!7,b2,b4,b4,b6).M(a2,a2,a3!3,a3,a6!7,a6,b2,b4!8,b4,b6)."
                                + "GN(a3,a4,a6,b2,b3,b3,b4!8,b4!9,b6,r~0).GN(a3,a4,a6,b2,b3,b3,b4!9,b4,b6,r~1)",
                        "Ma2Ma2Ma3(Ma3(Ma2Ma6)Ma6)Mb4GNb4GN"},
                new String[] {
                        "G(a3!11).M(a2!1,a2,a3!11,a3,a6,a6,b2,b4,b4,b6).M(a2!1,a2!2,a3,a3,a6,a6,b2,b4,b4,b6)."
                                + "M(a2,a2!2,a3!3,a3,a6,a6,b2,b4,b4,b6).M(a2!4,a2,a3,a3,a6,a6,b2,b4,b4,b6).M(a2!4,a2,a3!5,a3,a6,a6,b2,b4,b4,b6)."
                                + "M(a2!6,a2,a3,a3,a6,a6,b2,b4,b4,b6).M(a2!6,a2,a3,a3,a6!7,a6,b2,b4,b4,b6).M(a2,a2,a3!5,a3,a6!7,a6!8,b2,b4,b4,b6)"
                                + ".M(a2,a2,a3!3,a3,a6!8,a6,b2,b4!9,b4,b6).GN(a3,a4,a6,b2,b3,b3,b4!9,b4!10,b6,r~0)."
                                + "GN(a3,a4,a6,b2,b3,b3,b4!10,b4,b6,r~1)", "Ga3Ma2Ma2Ma3(Ma2Ma3(Ma2Ma6)Ma6)Mb4GNb4GN"},
                //does not contain r~1
                new String[] {
                        "M(a2,a2,a3!3,a3,a6,a6,b2,b4!1,b4,b6).GN(a3,a4,a6,b2,b3,b3,b4!1,b4!2,b6,r~0).GN(a3,a4,a6,b2,b3,b3,b4!2,b4,b6,r~0)",
                        "Incorrect BNGL representatopn of glycan"},
                //incorrect molecule name
                new String[] {"R(l!8).L(r!8,w!3).W(l!3)", "Incorrect BNGL representatopn of glycan"},};

        for( String[] test : tests )
        {
            try
            {
                assertEquals(test[0] + " converting: ", BionetgenToGlycanConverter.convert(test[0]), test[1]);
            }
            catch( IllegalArgumentException e )
            {
                assertEquals(test[0] + " converting: ", e.getMessage(), test[1]);
            }
        }
    }
}
