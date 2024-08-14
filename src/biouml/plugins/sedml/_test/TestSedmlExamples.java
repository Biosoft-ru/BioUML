package biouml.plugins.sedml._test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import one.util.streamex.StreamEx;

import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import biouml.model.Diagram;
import biouml.plugins.research.workflow.WorkflowDiagramType;
import biouml.plugins.sedml.SedmlImporter;
import biouml.plugins.sedml.SedmlImporter.ImportProperties;
import biouml.plugins.sedml.SedxImporter;

public class TestSedmlExamples extends AbstractSedmlTest
{
    private File sedxFile;
    private File expectedTable;
    public TestSedmlExamples(File file, File table, String testMethod)
    {
        super( testMethod );
        this.sedxFile = file;
        this.expectedTable = table;
    }
    
    public TestSedmlExamples()
    {
    }
    
    @Override
    public String getName()
    {
        return super.getName() + "_" + sedxFile.getName();
    }

    public static Test suite()
    {

        TestSuite suite = new TestSuite( TestSedmlExamples.class.getName() );
        
        File dir = new File( "../data/test/biouml/plugins/sedml/samples" );
        for( String sedxName : dir.list() )
        //for( String sedxName : new String[] {"v3-example3-repeated-stochastic-runs.sedx"} )
        {
            if( !sedxName.endsWith( ".sedx" ) )
                continue;
            String baseName = sedxName.substring( 0, sedxName.length() - 5 );
            File table = new File( dir, baseName + ".txt" );
            if( !table.exists() )
                continue;
            Test test = new TestSedmlExamples( new File( dir, sedxName ), table, "testRun" );
            suite.addTest( test );
            test = new TestSedmlExamples( new File( dir, sedxName ), table, "testExport" );
            suite.addTest( test );

        }
        return suite;
    }

    public void testRun() throws Exception
    {
        Diagram workflow = importSedx();
        runWorkflowAndCheckResult( workflow );

        /*
        TableElementExporter exporter = new TableElementExporter();
        Properties properties = new Properties();
        properties.put( DataElementExporterRegistry.SUFFIX, "txt" );
        exporter.init( properties );
        String name = sedxFile.getName();
        name = name.substring( 0, name.length() - 5 );
        File file = new File(sedxFile.getParentFile(), name + ".result");
        TableExporterProperties parameters = (TableExporterProperties)exporter.getProperties( result, file );
        parameters.setIncludeIds( false );
        exporter.doExport( result, file );
        */
    }
    
    public void testExport() throws Exception
    {
        Diagram workflow = importSedx();
        DataCollection<?> origin = workflow.getOrigin();
        String name = workflow.getName();
        File sedmlFile = getTestFile( sedxFile.getName().replaceFirst( "\\.sedx$", ".sedml" ) );
        exportSedml( sedmlFile , workflow );
        origin.remove( name );
        
        SedmlImporter importer = new SedmlImporter();
        ImportProperties properties = importer.getProperties( origin, sedmlFile, name );
        properties.setModelCollectionPath( origin.getCompletePath() );
        importer.doImport( origin, sedmlFile, name, null, log );
        workflow = (Diagram)origin.get( name );
        
        runWorkflowAndCheckResult( workflow );
    }
    
    private void runWorkflowAndCheckResult(Diagram workflow) throws Exception
    {
        runWorkflow( workflow );
        TableDataCollection result = DataElementPath.create( "root/results/table_of_all_data_generators" ).getDataElement(
                TableDataCollection.class );
        BufferedReader reader = new BufferedReader( new FileReader( expectedTable ) );
        String[] expectedHeader = reader.readLine().split( "\t" );
        String[] columnNames = result.getColumnModel().stream().map( TableColumn::getName ).toArray( String[]::new );
        assertArrayEquals( "Column names", expectedHeader, columnNames );        
        double[][] values = parseTable( reader );

        for( int i = 0; i < values.length; i++ )
            for( int j = 0; j < expectedHeader.length; j++ )
            {
                double expected = (double)result.getValueAt( i, j );
                double given = values[i][j];
                double atol = Math.abs( given - expected );
                double rtol = atol == 0 ? 0 : atol / Math.max( given, expected );
                assertTrue( "rtol <= 0.000001%", rtol <= 1e-8 );
            }
    }

    private double[][] parseTable(BufferedReader reader)
    {
        return StreamEx.ofLines( reader ).map( line -> StreamEx.split( line, "\t" ).mapToDouble( Double::parseDouble ).toArray() )
                .toArray( double[][]::new );
    }

    private Diagram importSedx() throws Exception
    {
        SedxImporter importer = new SedxImporter();
        importer.doImport( root, sedxFile, "sedx", null, log );
        return DataElementPath.create( "root/sedx" ).getDataCollection().stream( Diagram.class )
                .filter( d -> d.getType() instanceof WorkflowDiagramType ).findAny().get();
    }
}
