package biouml.plugins.bionetgen._test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import biouml.plugins.bionetgen.bnglparser._test.BionetgenParserTest;

public class AutoTest extends TestCase
{
    public static Test suite()
    {
        TestSuite suite = new TestSuite(AutoTest.class.getName());

        suite.addTest(BionetgenMoleculeTest.suite());
        suite.addTest(BionetgenToGlycanConverterTest.suite());
        suite.addTest(ReactionTemplateTest.suite());
        suite.addTest(BionetgenMoleculeTypeTest.suite());
        suite.addTest(BionetgenParserTest.suite());
        suite.addTest(BasicBionetgenTest.suite());
        suite.addTest(PropertyChangeBionetgenTest.suite());
        suite.addTest(BionetgenImportExportTest.suite());
        suite.addTest(BionetgenDiagramDeployerTest.suite());
        suite.addTestSuite( BionetgenRepresentationFactoryTest.class );

        return suite;
    }
}
