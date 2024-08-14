package biouml.plugins.sedml._test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import one.util.streamex.EntryStream;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.graphics.access.ChartDataElement;
import ru.biosoft.graphics.access.FileChartTransformer;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.Maps;
import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.plugins.sbml.SbmlImporter;
import biouml.plugins.sedml.SedmlImporter;
import biouml.plugins.sedml.SedmlImporter.ImportProperties;
import biouml.standard.simulation.SimulationResult;

public class TestSedMlImporter extends AbstractSedmlTest
{

    public void testSedml12() throws Exception
    {
        runSedml( "test_sedml12.xml" );
        Diagram changedModel = DataElementPath.create( "root/results/model2" ).getDataElement( Diagram.class );
        assertEquals( 1.3e-5, changedModel.getRole( EModel.class ).getVariable( "ps_0" ).getInitialValue(), 0 );
        assertEquals( 0.013, changedModel.getRole( EModel.class ).getVariable( "ps_a" ).getInitialValue(), 0 );

        DataElementPath.create( "root/results/Simulation results/task2" )
                .getDataElement( SimulationResult.class );
        DataElementPath.create( "root/results/Simulation results/task1" ).getDataElement(
                SimulationResult.class );

        checkCharts( getFile( "sedml12_results" ),
                "plot1_Basic",
                "plot2_damped_oscillations",
                "plot3_normalized_protein_levels" );
    }

    public void testC1() throws Exception
    {
        runSedml( "C1.xml" );
        checkCharts( getFile( "C1_results" ),
                "plot3",
                "plot2",
                "plot1" );
    }

    public void testC1Export() throws Exception
    {
        Diagram workflow = importSedml( "C1.xml", null );
        File exportedFile = getTestFile( "C1.xml" );
        exportSedml( exportedFile, workflow );
        runSedml( exportedFile );
        checkCharts( getFile( "C1_results" ),
                "plot3",
                "plot2",
                "plot1" );
    }

    public void testC3() throws Exception
    {
        runSedml( "C3.xml" );
        checkCharts( getFile( "C3_results" ),
                "plot4",
                "plot2",
                "plot3",
                "plot1" );
    }

    public void testC3Export() throws Exception
    {
        Diagram workflow = importSedml( "C3.xml", null );
        File exportedFile = getTestFile( "C3.xml" );
        exportSedml( exportedFile, workflow );
        runSedml( exportedFile );
        checkCharts( getFile( "C3_results" ),
                "plot4",
                "plot2",
                "plot3",
                "plot1" );
    }

    public void testC41() throws Exception
    {
        runSedml( "C4.1.xml", "oscli.xml" );
        checkCharts( getFile( "C4.1_results" ),
                "plot1" );
    }

    public void testC41Export() throws Exception
    {
        Diagram workflow = importSedml( "C4.1.xml", "oscli.xml" );
        File exportedFile = getTestFile( "C4.1.xml" );
        exportSedml( exportedFile, workflow );
        runSedml( exportedFile, "oscli.xml" );
        checkCharts( getFile( "C4.1_results" ),
                "plot1" );
    }

    public void testC42() throws Exception
    {
        runSedml( "C4.2.xml", "oscli.xml" );
        checkCharts( getFile( "C4.2_results" ),
                "plot1" );
    }

    /* Failed due to piecewise range (convertion from js to mathml not yet supported) */
    public void testC42Export() throws Exception
    {
        Diagram workflow = importSedml( "C4.2.xml", "oscli.xml" );
        File exportedFile = getTestFile( "C4.2.xml" );
        exportSedml( exportedFile, workflow );
        runSedml( exportedFile, "oscli.xml" );
        checkCharts( getFile( "C4.2_results" ),
                "plot1" );
    }

    public void testC43() throws Exception
    {
        runSedml( "C4.3.xml", "borisejb.xml" );
        TableDataCollection t = DataElementPath.create( "root/results/table_of_all_data_generators" ).getDataElement(
                TableDataCollection.class );
        String[] columns = t.columns().map( TableColumn::getName ).toArray( String[]::new );
        Map<String, List<Integer>> columnGroups = getColumnGroups( columns );
        
        Map<String, double[]> meanValues = Maps.transformValues( columnGroups, indexes -> t.stream()
                .map( rde -> rde.getValues() )
                .mapToDouble( values -> IntStreamEx.of( indexes )
                        .mapToDouble( idx -> ( (Number)values[idx] ).doubleValue() )
                        .average().getAsDouble() )
                .toArray() );
        
        Map<String, double[]> expectedMeanValues = parseMeanValues();

        for( Map.Entry<String, double[]> entry : expectedMeanValues.entrySet() )
        {
            double[] expected = entry.getValue();
            double[] given = meanValues.get( entry.getKey() );
            assertEquals( expected.length, given.length );
            for( int i = 0; i < expected.length; i++ )
            {
                double diff = Math.abs( expected[i] - given[i] );
                assertTrue( diff < 100 );
            }
        }
    }

    private Map<String, List<Integer>> getColumnGroups(String[] columns)
    {
        return EntryStream.of( columns ).invert().mapKeys( this::getGroupName ).grouping();
    }

    private String getGroupName(String name)
    {
        int idx = name.indexOf( '_' );
        return idx == -1 ? name : name.substring( idx, name.length() );
    }

    private Map<String, double[]> parseMeanValues() throws IOException
    {
        BufferedReader reader = new BufferedReader( new FileReader( getFile( "C4.3_results.txt" ) ) );
        String[] columns = reader.readLine().split( "\t" );

        Map<String, List<Integer>> columnGroups = getColumnGroups( columns );

        double[][] table = parseTable( reader );

        Map<String, double[]> meanValues = new HashMap<>();

        for( Map.Entry<String, List<Integer>> e : columnGroups.entrySet() )
        {
            double[] result = new double[table.length];
            for( int i = 0; i < table.length; i++ )
            {
                for( Integer idx : e.getValue() )
                    result[i] += table[i][idx];
                result[i] /= e.getValue().size();
            }
            meanValues.put( e.getKey(), result );
        }

        return meanValues;
    }

    private double[][] parseTable(BufferedReader reader)
    {
        return StreamEx.ofLines( reader ).map( line -> StreamEx.split( line, "\t" ).mapToDouble( Double::parseDouble ).toArray() )
                .toArray( double[][]::new );
    }

    /*
    public void testC43Export() throws Exception
    {
        Diagram workflow = importSedml( "C4.3.xml", "borisejb.xml" );
        File exportedFile = getTestFile( "C4.3.xml" );
        exportSedml(exportedFile, workflow);
        runSedml(exportedFile, "borisejb.xml" );
        checkCharts( getFile("C4.3_results"),
                "MAPK feedback ( Kholodenko , 2000) ( stochastic trace ) (plot1)");
    }
    */

    public void testC44() throws Exception
    {
        runSedml( "C4.4.xml", "oscli.xml" );
        checkCharts( getFile( "C4.4_results" ),
                "plot1" );
    }

    public void testC44Export() throws Exception
    {
        Diagram workflow = importSedml( "C4.4.xml", "oscli.xml" );
        File exportedFile = getTestFile( "C4.4.xml" );
        exportSedml( exportedFile, workflow );
        runSedml( exportedFile, "oscli.xml" );
        checkCharts( getFile( "C4.4_results" ),
                "plot1" );//no dot in 0.4 due to special meaning of dot in the name of diagram element
    }

    /* The results actually incorrect due to steady state found at negative concentrations,
       currently steady state solver doesn't support constraints. */
    public void testC45() throws Exception
    {
        runSedml( "C4.5.xml", "borisejb.xml" );
        checkCharts( getFile( "C4.5_results" ),
                "plot1" );
    }

    public void testC45Export() throws Exception
    {
        Diagram workflow = importSedml( "C4.5.xml", "borisejb.xml" );
        File exportedFile = getTestFile( "C4.5.xml" );
        exportSedml( exportedFile, workflow );
        runSedml( exportedFile, "borisejb.xml" );
        checkCharts( getFile( "C4.5_results" ),
                "plot1" );
    }

    private void runSedml(String sedml) throws Exception
    {
        runSedml( sedml, null );
    }
    private void runSedml(File sedmlFile) throws Exception
    {
        runSedml( sedmlFile, null );
    }
    private void runSedml(String sedml, String model) throws Exception
    {
        runSedml( getFile( sedml ), model );
    }
    private void runSedml(File sedmlFile, String model) throws Exception
    {
        Diagram workflow = importSedml( sedmlFile, model );
        runWorkflow( workflow );
    }

    private Diagram importSedml(String sedml, String model) throws Exception
    {
        File sedmlFile = getFile( sedml );
        return importSedml( sedmlFile, model );
    }
    private Diagram importSedml(File sedmlFile, String model) throws Exception
    {
        SedmlImporter importer = new SedmlImporter();
        String name = "sedml_workflow";
        ImportProperties properties = importer.getProperties( root, sedmlFile, name );
        if( model != null )
        {
            DataElementPath modelCollectionPath = DataElementPath.create( "root" );
            importModel( getFile( model ), modelCollectionPath );
            properties.setModelCollectionPath( modelCollectionPath );
        }
        importer.doImport( root, sedmlFile, name, null, log );
        return DataElementPath.create( "root", name ).getDataElement( Diagram.class );
    }

    private File getFile(String name)
    {
        return new File( "../data/test/biouml/plugins/sedml/", name );
    }

    private void importModel(File file, DataElementPath modelCollectionPath) throws Exception
    {
        SbmlImporter sbmlImporter = new SbmlImporter();
        sbmlImporter.doImport( modelCollectionPath.getDataCollection(), file, file.getName().replaceAll( ".xml$", "" ), null, log );
    }

    private void checkCharts(File resultsFolder, String ... chartNames) throws Exception
    {
        for( String name : chartNames )
        {
            ChartDataElement chart = DataElementPath.create( "root/results", name ).getDataElement( ChartDataElement.class );
            checkChart( chart, new File( resultsFolder, name ) );
        }
    }

    private void checkChart(ChartDataElement actual, File expectedFile) throws Exception
    {
        FileChartTransformer transformer = new FileChartTransformer();
        ChartDataElement expected = transformer.load( expectedFile, "expected", null );
        assertEquals( expectedFile.getName(), expected.getChart().getData(), actual.getChart().getData() );
    }
}
