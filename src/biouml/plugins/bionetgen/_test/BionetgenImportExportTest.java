package biouml.plugins.bionetgen._test;

import java.io.File;

import javax.annotation.Nonnull;

import junit.framework.TestSuite;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access._test.AbstractBioUMLTest;
import biouml.model.Diagram;
import biouml.plugins.bionetgen.diagram.BionetgenExporter;
import biouml.plugins.bionetgen.diagram.BionetgenImporter;
import biouml.plugins.bionetgen.diagram.BionetgenUtils;

public class BionetgenImportExportTest extends AbstractBioUMLTest
{
    protected static final DataElementPath COLLECTION_NAME = DataElementPath.create("databases/test/Diagrams");

    protected static final String dir = "biouml/plugins/bionetgen/_test/test_suite/models/test_examples/";
    protected static final String MODEL_NAME = "bionetgen_ex";
    protected static final String RESULT_MODEL_NAME = "bionetgen_res";
    protected static final String FILE_PATH = dir + MODEL_NAME + ".bngl";
    protected static final String RESULT_FILE_PATH = dir + RESULT_MODEL_NAME + ".bngl";

    public BionetgenImportExportTest(String name)
    {
        super(name);
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(BionetgenImportExportTest.class.getName());

        suite.addTest(new BionetgenImportExportTest("testImport"));
        suite.addTest(new BionetgenImportExportTest("testExport"));

        return suite;
    }

    protected @Nonnull File file = new File(FILE_PATH);
    protected @Nonnull File resultFile = new File(RESULT_FILE_PATH);

    public void testImport() throws Exception
    {
        BionetgenTestUtility.initPreferences();

        BionetgenImporter importer = new BionetgenImporter();
        assertTrue(importer.accept(file) == DataElementImporter.ACCEPT_HIGH_PRIORITY);

        DataCollection<?> collection = COLLECTION_NAME.getDataCollection();

        Diagram diagram = (Diagram)importer.doImport(collection, file, MODEL_NAME, null, Logger.getLogger(BionetgenImporter.class.getName()));
        assertNotNull(diagram);
        assertNotNull(diagram.findNode("Molecule_Type_4"));
        assertNotNull(diagram.findNode("A(b)"));
        assertNotNull(diagram.findNode("B(a)"));
        assertNotNull(diagram.findNode("C(c~uP)"));
        assertNotNull(diagram.findNode("C(c~P)"));
        assertNotNull(diagram.findNode("observable"));
        assertNotNull(diagram.findNode("observable_1"));
        assertTrue(BionetgenUtils.isReaction(diagram.findNode("j01")));
        assertTrue(BionetgenUtils.isReaction(diagram.findNode("j02")));
    }

    public void testExport() throws Exception
    {
        BionetgenTestUtility.initPreferences();

        DataCollection<?> collection = COLLECTION_NAME.getDataCollection();
        Diagram diagram = (Diagram)collection.get(MODEL_NAME);

        BionetgenExporter exporter = new BionetgenExporter();
        assertTrue(diagram != null);
        assertTrue(exporter.accept(diagram));

        exporter.doExport(diagram, resultFile);
        assertFileEquals(file, resultFile);

        collection.remove(diagram.getName());
    }
}
