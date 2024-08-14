package biouml.plugins.antimony._test;

import java.io.File;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.beans.Preferences;

import biouml.model.Diagram;
import biouml.plugins.antimony.AntimonyExporter;
import biouml.plugins.antimony.AntimonyImporter;
import biouml.standard.type.Reaction;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.util.ApplicationUtils;

public class AntimonyImportExportTest extends AbstractBioUMLTest
{
    final static DataElementPath COLLECTION_NAME = DataElementPath.create("databases/test/Antimony/Diagrams");

    final static String FILE_PATH = "biouml/plugins/antimony/astparser_v2/_test/antimony.txt";
    final static String FILE_PATH_RESULT = "biouml/plugins/antimony/astparser_v2/_test/antimony_re.txt";

    public AntimonyImportExportTest(String name)
    {
        super(name);
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(AntimonyImportExportTest.class.getName());
        suite.addTest(new AntimonyImportExportTest("testImport"));
        suite.addTest(new AntimonyImportExportTest("testExport"));
        return suite;
    }

    @Nonnull
    File file = new File(FILE_PATH);
    @Nonnull
    File fileResult = new File(FILE_PATH_RESULT);

    public void testImport() throws Exception
    {
        setPreferences();

        AntimonyImporter importer = new AntimonyImporter();
        assertTrue(importer.accept(file) == DataElementImporter.ACCEPT_HIGH_PRIORITY);

        DataCollection<?> collection = COLLECTION_NAME.getDataCollection();

        String name = ApplicationUtils.getFileNameWithoutExtension(file.getName());

        Diagram diagram = (Diagram)importer.doImport(collection, file, name, null, Logger.getLogger(AntimonyImporter.class.getName()));
        assertNotNull(diagram);
        assertNotNull(diagram.findNode("s1"));
        assertTrue(diagram.findNode("r1").getKernel() instanceof Reaction);
    }

    public void testExport() throws Exception
    {
        setPreferences();

        DataCollection<?> collection = COLLECTION_NAME.getDataCollection();
        Diagram diagram = (Diagram)collection.get(ApplicationUtils.getFileNameWithoutExtension(file.getName()));
        assertNotNull(diagram);

        AntimonyExporter exporter = new AntimonyExporter();
        assertTrue(exporter.accept(diagram));

        exporter.doExport(diagram, fileResult);
        assertFileEquals(file, fileResult);
    }

    protected void setPreferences() throws Exception
    {
        CollectionFactory.createRepository("../data");
        Application.setPreferences(new Preferences());
    }
}
