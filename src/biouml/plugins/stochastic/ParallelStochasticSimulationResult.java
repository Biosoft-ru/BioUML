package biouml.plugins.stochastic;

import java.util.ArrayList;
import java.util.List;

import biouml.plugins.simulation.CycledResultListener;
import biouml.standard.simulation.SimulationResult;
import biouml.standard.simulation.StochasticSimulationResult;
import ru.biosoft.access.core.DataCollection;

@SuppressWarnings ( "serial" )
public class ParallelStochasticSimulationResult extends StochasticSimulationResult implements CycledResultListener
{
    public static String TYPE_STOCHASTIC = "Stochastic";

    List<StochasticSimulationResult> results;
    private int threads = 2;

    public ParallelStochasticSimulationResult(DataCollection<?> origin, String name)
    {
        super( origin, name );
    }

    public ParallelStochasticSimulationResult(DataCollection<?> origin, String name, int threads)
    {
        super( origin, name );
        setThreads( threads );
    }

    public void setThreads(int threads)
    {
        this.threads = threads;
        results = new ArrayList<>();
        for( int i = 0; i < threads; i++ )
            results.add( new StochasticSimulationResult( getOrigin(), name ) );
    }

    public StochasticSimulationResult get(int i)
    {
        return results.get( i );
    }

    public String getType()
    {
        return TYPE_STOCHASTIC;
    }

    @Override
    public void add(double t, double[] y)
    {

    }


    @Override
    public void startCycle()
    {
    }

    @Override
    public void start(Object model)
    {
        super.start( model );
        //        for( int i = 0; i < threads; i++ )
        //            results.get( i ).start( model );
    }

    @Override
    public void finish()
    {
        //some of tinner results may not be initialized
        double time = System.currentTimeMillis();
        int actualThreads = 0;
        for( int i = 0; i < threads; i++ )
        {
            if( results.get( i ).getFullValues() != null )
                actualThreads++;
        }
        double[][][][] threadValues = new double[actualThreads][][][];
        int index = 0;
        for( int i = 0; i < actualThreads; i++ )
        {
            if( results.get( i ).getFullValues() != null )
                threadValues[index] = results.get( i ).getFullValues();
            index++;
        }
        fullValues = join( threadValues );
        times = results.get( 0 ).getTimes().clone();
        super.finish();
        System.out.println( "Joining:" + ( System.currentTimeMillis() - time ) / 1000 );
    }

    private double[][][] join(double[][][] ... arrs)
    {
        int size = arrs[0].length;
        int vars = arrs[0][0].length;
        int totalRepeats = 0;
        for( int i = 0; i < arrs.length; i++ )
            totalRepeats += arrs[i][0][0].length;
        double[][][] result = new double[size][vars][totalRepeats];
        int offset = 0;
        for( int i = 0; i < arrs.length; i++ )
        {
            double[][][] valsi = arrs[i];
            int repeats = valsi[0][0].length;
            for( int j = 0; j < size; j++ )
            {
                for( int k = 0; k < vars; k++ )
                {
                    double[] vals = valsi[j][k];
                    System.arraycopy( vals, 0, result[j][k], offset, vals.length );
                }
            }
            offset += repeats;
        }
        return result;
    }

    @Override
    public double[] getTimes()
    {
        return times;
    }

    @Override
    public double[][] getValues()
    {
        return values;
    }

    @Override
    public void addAsFirst(double t, double[] y)
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void update(double t, double[] y)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public ParallelStochasticSimulationResult clone(DataCollection origin, String name)
    {
        ParallelStochasticSimulationResult simulationResult = new ParallelStochasticSimulationResult( origin, name );
        simulationResult.setSpanSize( size );
        simulationResult.setThreads( threads );
        simulationResult.setCycles( cycles );
        simulationResult.setVars( vars );
        simulationResult.setTimes( getTimes() );
        simulationResult.setValues( getValues() );
        simulationResult.q1 = q1;
        simulationResult.q3 = q3;
        simulationResult.median = median;
        simulationResult.fullValues = fullValues;
        simulationResult.values = values;
        simulationResult.setDiagramPath( getDiagramPath() );
        simulationResult.setDescription( getDescription() );
        simulationResult.setCompletionTime( getCompletionTime() );
        simulationResult.setInitialTime( getInitialTime() );
        simulationResult.setSimulatorName( getSimulatorName() );
        simulationResult.setTitle( getTitle() );
        simulationResult.setVariableMap( getVariableMap() );
        simulationResult.setVariablePathMap( getVariablePathMap() );

        simulationResult.results.clear();
        try
        {
            for( SimulationResult innerResult : this.results )
                simulationResult.results.add( (StochasticSimulationResult)innerResult.clone() );
        }
        catch( Exception ex )
        {

        }
        return simulationResult;
    }

    @Override
    public void setVars(int vars)
    {
        super.setVars( vars );
        for( StochasticSimulationResult innerResult : results )
            innerResult.setVars( vars );
    }
}