package biouml.plugins.antimony._test;

import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;

public class AutoTest extends AbstractBioUMLTest
{
    public static Test suite()
    {
        TestSuite suite = new TestSuite(AutoTest.class.getName());

        suite.addTestSuite(TestAntimonyRepresentationFactory.class);
        suite.addTest(BasicAntimonyTest.suite());
        suite.addTest(CompositeAntimonyTest.suite());
        suite.addTest(GenerateCompositeDiagramTest.suite());
        suite.addTest(GenerateDiagramTest.suite());
        suite.addTest(AntimonyImportExportTest.suite());
        suite.addTest(SbgnAntimonyTest.suite());
        suite.addTest(AntimonyAnnotationImportTest.suite());
        suite.addTest(AnnotationAntimonyTest.suite());
        //suite.addTest(TestYAML.suite());
        return suite;
    }
}
