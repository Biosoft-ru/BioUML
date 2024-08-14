package biouml.standard._test;

import biouml.model.Module;
import biouml.standard.diagram.DatabaseReferencesEditDialog;
import biouml.standard.type.Concept;
import biouml.standard.type.DatabaseReference;
import biouml.standard.type.Referrer;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;

/**
 * Batch unit test for biouml.model package.
 */
public class DatabaseReferenceEditorTest extends TestCase
{
    private static final String repositoryPath = "../data";

    /** Standart JUnit constructor */
    public DatabaseReferenceEditorTest(String name)
    {
        super(name);
    }

    /**
     * Run test in TestRunner.
     * If args[0].startsWith("text") then textui runner runs,
     * otherwise swingui runner runs.
     * @param args[0] Type of test runner.
     */
    public static void main(String[] args)
    {
        if( args != null && args.length > 0 && args[0].startsWith("text") )
        {
            junit.textui.TestRunner.run(suite());
        }
        else
        {
            junit.swingui.TestRunner.run(DatabaseReferenceEditorTest.class);
        }
    }

    /** Make suite if tests. */
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(DatabaseReferenceEditorTest.class.getName());
        suite.addTest(new DatabaseReferenceEditorTest("testEditor"));
        return suite;
    }

    //////////////////////////////////////////////////////////////////
    // Test cases
    //

    public void testEditor() throws Exception
    {
        DataCollection<?> repository = CollectionFactory.createRepository(repositoryPath);
        assertNotNull(repository);
        Module module = (Module)CollectionFactory.getDataElement("databases/Biopath");
        DatabaseReference[] references = new DatabaseReference[3];
        DatabaseReference ref1 = new DatabaseReference("DBI016", "7794", "qqq1");
        ref1.setRelationshipType("Transcript");
        ref1.setComment("comment1");
        references[0] = ref1;
        DatabaseReference ref2 = new DatabaseReference("DBI017", "431926", "qqq2");
        ref2.setRelationshipType("Transcript");
        ref2.setComment("comment2");
        references[1] = ref2;
        DatabaseReference ref3 = new DatabaseReference("DBI018", "4791", "qqq3");
        ref3.setRelationshipType("Transcript");
        ref3.setComment("comment3");
        references[2] = ref3;

        Referrer referrer = new Concept(module, "example");
        referrer.setDatabaseReferences(references);
        DatabaseReferencesEditDialog dialog;
        dialog = new DatabaseReferencesEditDialog(null, referrer);

        dialog.doModal();
    }
}