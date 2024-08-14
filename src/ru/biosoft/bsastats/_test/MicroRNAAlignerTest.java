package ru.biosoft.bsastats._test;

import junit.framework.TestCase;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.bsa._test.BSATestUtils;
import ru.biosoft.bsastats.MicroRNAAligner;
import ru.biosoft.bsastats.MicroRNAAligner.Parameters;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.TableDataCollection;


public class MicroRNAAlignerTest extends TestCase
{
    public void atestOneThread() throws Exception
    {
        runAnalysis( 1000, 1 );
    }

    public void atestTwoThreads() throws Exception
    {
        runAnalysis( 1000, 2 );
    }

    public void atest3Threads() throws Exception
    {
        runAnalysis( 1000, 3 );
    }
    
    public void test4Threads() throws Exception
    {
        runAnalysis( 1000, 4 );
    }
    
    private void runAnalysis(int inputSize, int threads) throws Exception
    {
        DataCollection<DataCollection<?>> repository = BSATestUtils.createRepository();
        VectorDataCollection<TableDataCollection> outputDC = new VectorDataCollection<>( "output", StandardTableDataCollection.class,
                repository );
        repository.put( outputDC );

        MicroRNAAligner method = new MicroRNAAligner( null, "" );

        Parameters parameters = method.getParameters();
        parameters.setSource( "Solid" );
        parameters.setCsfasta( DataElementPath.create( "databases/solid/" + inputSize + ".csfasta" ) );
        parameters.setQual( DataElementPath.create( "databases/solid/" + inputSize + ".qual" ) );
        parameters.setMiRNASequences( DataElementPath.create( "databases/mirbase/rat_mature_mirna" ) );
        parameters.setAdapter( "CGCCTTGGCCGTACAGCAG" );
        parameters.setMatchScore( 1 );
        parameters.setMismatchPenalty( -1 );
        parameters.setGapPenalty( -1 );
        parameters.setAlignmentScoreThreshold( 12 );
        parameters.setThreadCount( threads );
        DataElementPath resultPath = DataElementPath.create( "databases/output/alignment" );
        parameters.setResults( resultPath );
        method.setParameters( parameters );
        method.validateParameters();

        long startTime = System.currentTimeMillis();
        method.getJobControl().run();
        System.out.println( "Run time " + ( System.currentTimeMillis() - startTime ) / 1000.0 + "s for " + inputSize + " sequences on " + threads + " threads");
        
        TableDataCollection result = resultPath.getDataElement( TableDataCollection.class );
        assertEquals( 34, result.getSize() );
    }

}
