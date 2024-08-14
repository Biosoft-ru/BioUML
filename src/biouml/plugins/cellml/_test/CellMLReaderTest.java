package biouml.plugins.cellml._test;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.LogManager;

import ru.biosoft.util.TempFile;
import ru.biosoft.util.TempFiles;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.plugins.cellml.CellMLModelReader;
import biouml.plugins.cellml.CellMLModelWriter;

/** Batch unit test for biouml.model package. */
public class CellMLReaderTest extends TestCase
{
    /** Standart JUnit constructor */
    public CellMLReaderTest(String name)
    {
        super(name);

        // Setup log
        File configFile = new File( "./biouml/plugins/cellml/_test/log.lcf" );
        try( FileInputStream fis = new FileInputStream( configFile ) )
        {
            LogManager.getLogManager().readConfiguration( fis );
        }
        catch( Exception e1 )
        {
            System.err.println( "Error init logging: " + e1.getMessage() );
        }
    }

    //////////////////////////////////////////////////////////////////
    // Test cases
    //

    /** Make suite if tests. */
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(CellMLReaderTest.class.getName());

        suite.addTest(new CellMLReaderTest("testModel_two_reaction_model"));
        suite.addTest(new CellMLReaderTest("testModel_rice_model1_1999"));
        suite.addTest(new CellMLReaderTest("testModel_goldbeter_model_1991"));
        //suite.addTest(new CellMLReaderTest("testModel_leloup_gonze_goldbeter_1999_a"));

        return suite;
    }

    private static void readAndWrite(String name) throws Exception
    {
        Diagram diagram = testModel(name);
        assertNotNull("Cannot read model", diagram);
        assertTrue( diagram.getSize() > 0 );
        try(TempFile file = TempFiles.file( name+".xml" ))
        {
            testWriteModel(diagram, file);
            assertTrue(file.toString(), file.length() > 0);
        }
    }


    public static void testModel_two_reaction_model() throws Exception
    {
        readAndWrite( "two_reaction_model" );
    }

    public static void testModel_rice_model1_1999() throws Exception
    {
        readAndWrite( "rice_model1_1999" );
    }

    public static void testModel_goldbeter_model_1991() throws Exception
    {
        readAndWrite( "goldbeter_model_1991" );
    }

    /* Currently not supported
    public static void testModel_leloup_gonze_goldbeter_1999_a() throws Exception
    {
        readAndWrite( "leloup_gonze_goldbeter_1999_a" );
    }
    */

    ///////////////////////////////////////////////////////////////////
    // utilites
    //

    protected static Diagram testModel(String name) throws Exception
    {
        Diagram diagram = testReadModel(name);
        //testDiagramView(diagram);
        //testWriteModel(diagram, name);

        // check emodel
        System.out.println("Variables: ");
        EModel model = diagram.getRole(EModel.class);
        model.getVariableRoles().stream().map( param -> "    " + param ).forEach( System.out::println );

        System.out.println("Parameters: ");
        model.getParameters().stream().map( param -> "    " + param ).forEach( System.out::println );
        return diagram;
    }

    protected static Diagram testReadModel(String name) throws Exception
    {
        File file = new File("../data/test/biouml/plugins/cellml/" + name + ".xml");
        assertTrue("Can not find file: " + file.getCanonicalPath(), file.exists());

        long start = System.currentTimeMillis();
        CellMLModelReader cellmlReader = new CellMLModelReader(file);
        Diagram diagram = cellmlReader.read(null);
        assertNotNull("Diagram was not initialised properly.", diagram);
        long readTime = System.currentTimeMillis() - start;

        System.out.println("Model " + name + " reading time: " + readTime);
        return diagram;
    }

    protected static void testWriteModel(Diagram diagram, File file) throws Exception
    {
        long start = System.currentTimeMillis();
        CellMLModelWriter cellmlWriter = new CellMLModelWriter(file);
        cellmlWriter.write(diagram);
        long writeTime = System.currentTimeMillis() - start;

        System.out.println("Model " + file.getName() + " writingTime: " + writeTime);
    }

}
