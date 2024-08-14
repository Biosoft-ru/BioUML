package biouml.workbench.diagram._test;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.LogManager;

import biouml.model.DiagramImporter;
import biouml.model.Module;
import biouml.plugins.cellml.CellMLImporter;
import biouml.plugins.sbml.SbmlImporter;
import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementImporter;


public class ImporterRegistryTest extends AbstractBioUMLTest
{

    public ImporterRegistryTest ( String name )
    {
        super ( name );

        // Setup log
        File configFile = new File( "./biouml/workbench/diagram/_test/log.lcf" );
        try( FileInputStream fis = new FileInputStream( configFile ) )
        {
            LogManager.getLogManager().readConfiguration( fis );
        }
        catch( Exception e1 )
        {
            System.err.println( "Error init logging: " + e1.getMessage() );
        }
    }

    /** Make suite if tests. */
    public static Test suite()
    {
        TestSuite suite = new TestSuite ( ImporterRegistryTest.class.getName() );
        suite.addTest ( new ImporterRegistryTest ( "testImport" ) );
        return suite;
    }

    static Module module;
    public static void testImport() throws Exception
    {
        if ( module == null )
        {
            DataCollection repository = CollectionFactory.createRepository( "../data/test/biouml/workbench/diagram" );
            assertNotNull ( "Can not load repository", repository );
            module = ( Module ) repository.get ( "test" );
            //module = ( Module ) repository;
            assertNotNull ( "Can not load module", module );
        }

        String format = "CellML";

        // GinML
        String ginMLfileName = "../data/test/biouml/workbench/diagram/arabidopsis.xml";
        File ginMLfile = new File ( ginMLfileName );

        assertTrue ( new CellMLImporter().accept ( ginMLfile ) == DataElementImporter.ACCEPT_UNSUPPORTED );
        assertTrue ( new SbmlImporter().accept ( ginMLfile ) == DataElementImporter.ACCEPT_UNSUPPORTED );

        // CellML
        String cellMLfileName = "../data/test/biouml/workbench/diagram/Ach_cascade_1995.xml";
        File cellMLfile = new File ( cellMLfileName );

        DiagramImporter cellMLImporter = new CellMLImporter();

        assertTrue ( new CellMLImporter().accept ( cellMLfile ) != DataElementImporter.ACCEPT_UNSUPPORTED );
        assertTrue ( new SbmlImporter().accept ( cellMLfile ) == DataElementImporter.ACCEPT_UNSUPPORTED );

        cellMLImporter.doImport ( module, cellMLfile, "diagram_CellML" );

        // SBML
        String sbmlMLfileName = "../data/test/biouml/workbench/diagram/algebraicRules-basic-l2.xml";
        File sbmlMLfile = new File ( sbmlMLfileName );

        DiagramImporter sbmlMLImporter = new SbmlImporter();

        assertTrue ( new CellMLImporter().accept ( sbmlMLfile ) == DataElementImporter.ACCEPT_UNSUPPORTED );

        System.err.println ( "new SbmlImporter().accept(sbmlMLfile) = " + new SbmlImporter().accept ( sbmlMLfile ) );

        assertTrue ( new SbmlImporter().accept ( sbmlMLfile ) != DataElementImporter.ACCEPT_UNSUPPORTED );

        sbmlMLImporter.doImport ( module, sbmlMLfile, "diagram_Sbml" );
    }

}