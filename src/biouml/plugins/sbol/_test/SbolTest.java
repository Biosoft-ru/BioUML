package biouml.plugins.sbol._test;

import java.io.File;

import biouml.model.Diagram;
import biouml.plugins.sbol.SbolImportProperties;
import biouml.plugins.sbol.SbolImporter;
import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

public class SbolTest extends AbstractBioUMLTest
{
    public static final String filesDir = "../data/test/biouml/plugins/sbol/files";
    public static final String repositoryPath = "../data/test/biouml/plugins/sbol/repo";
    private DataCollection module;

    public SbolTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(SbolTest.class.getName());

        suite.addTest(new SbolTest("testReadDiagram"));
        return suite;
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        CollectionFactory.unregisterAllRoot();
        DataCollection<?> root = CollectionFactory.createRepository(repositoryPath);
        assertNotNull("Can not load repository", root);
        module = (DataCollection<?>) root;
    }

    public void testReadDiagram() throws Exception
    {
        SbolImporter importer = new SbolImporter();
        
        File file = new File(filesDir + "/canv_test1_sbol2.xml");
        SbolImportProperties props = (SbolImportProperties) importer.getProperties(module, file, "canv_test1_sbol2");

        DataElement de = importer.doImport(module, file, props.getDiagramName(), null, null);
        assertNotNull("Diagram was not imported", de);
        assertTrue("Not a diagram", de instanceof Diagram);
        Diagram diagram = (Diagram) de;

        diagram.stream().forEach(elem -> System.out.println(elem.getName() + " " + elem.getKernel().getType()));
        assertEquals(4, diagram.getSize());
    }
}
