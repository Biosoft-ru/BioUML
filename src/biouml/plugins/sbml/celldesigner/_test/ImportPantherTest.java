package biouml.plugins.sbml.celldesigner._test;

import java.io.File;
import java.util.logging.Logger;

import biouml.model.Diagram;
import biouml.plugins.sbml.celldesigner.CellDesignerImporter;
import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementImporter;

/**
 * @author tolstyh
 * Import diagrams to PantherDB
 */
public class ImportPantherTest extends AbstractBioUMLTest
{
    private static final Logger log = Logger.getLogger(ImportPantherTest.class.getName());
    /**
     * Path to PANTHER SBML(CellDesigner) files
     */
    protected static final String SOURCE_PATH = "../../models/CellDesigner";//../data/test/CellDesigner";
    protected static final String SBML_DIR = SOURCE_PATH + "/SBML";
    protected static final String MODEL_LIST = "Model list.txt"; //file with list of models to test
    protected static final String DATA_PATH = "../data";
    protected static final String DATABASE_PATH = "databases/PantherDB/Diagrams";
    protected static final String STOP_KEYWORD = "END";
    
    public ImportPantherTest(String name)
    {
        super(name);
    }

    /** Make suite if tests. */
    public static Test suite()
    {
        TestSuite suite = new TestSuite(ImportPantherTest.class.getName());
        suite.addTest(new ImportPantherTest("testGenerateImages"));
        return suite;
    }

    public void testGenerateImages() throws Exception
    {
        //init repository
        CollectionFactory.createRepository(DATA_PATH);
        File fileList = new File(SOURCE_PATH, MODEL_LIST);
        DataCollection parent = CollectionFactory.getDataCollection(DATABASE_PATH);
        
        for (Object n: parent.getNameList())
            parent.remove(n.toString());
        
        for (File file: new File(SBML_DIR).listFiles())
        {
            Diagram diagram = importDiagram(parent, file);
            if( diagram == null )
                System.err.println("Diagram is null: " + file.getName());
        }
    }

    protected Diagram importDiagram(DataCollection parent, File file) throws Exception
    {
        CellDesignerImporter importer = new CellDesignerImporter();
        if( importer.accept(file) != DataElementImporter.ACCEPT_UNSUPPORTED )
            return (Diagram)importer.doImport(parent, file, file.getName(), null, log);
        return null;
    }
}
