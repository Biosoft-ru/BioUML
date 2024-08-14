package biouml.plugins.pharm.nlme;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import one.util.streamex.IntStreamEx;

import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.Simulator;
import biouml.plugins.simulation.Span;
import biouml.standard.simulation.ResultListener;

public class SteadyStateFinder implements ResultListener
{
    //parameters
    private double relativeTolerance = 1E-3;
    private double absoluteTolerance = 1E-7;
    private static final double validationSize = 100;
    private static final double startSearchTime = 0.0;

    public SteadyStateFinder(Simulator simulator, Span span, double aTol, double rTol)
    {
        this.simulator = simulator;
        this.span = span;
        relativeTolerance = rTol;
        absoluteTolerance = aTol;
    }

    //deque of variable values arrays
    private ArrayDeque<double[]> variableValues; //every time moment 10 consequent arrays are stored in memory for steady state checking

    private int[] checkIndexes;

    private double[] relativeAccuracies;

    private final Simulator simulator;
    private final Span span;

    public double[] findSteadyState(Model model, int[] indexes) throws Exception
    {
//        System.out.println("Started");
        this.checkIndexes = indexes;
        if( model == null )
            throw new Exception( "Model is null!" );

        start( model );
        simulator.start( model, span, new ResultListener[] {this}, null );
       
        return IntStreamEx.of( indexes ).elements( variableValues.getFirst() ).toArray();
    }


    @Override
    public void add(double t, double[] y) throws Exception
    {
        
//        StringBuffer result = new StringBuffer("incoming at t: "+t);
//        for (double v: y)
//            result.append(v+"\t");
//        System.out.println(result.toString());
        
        if( t < startSearchTime )
            return;

        updateRelativeAccuracies( y );

        if( variableValues.size() == validationSize )
            variableValues.pollFirst();
        variableValues.add( y );

        
        if( checkNaN( y ) || checkSteadyState() )
        {
//            System.out.println("Time: "+ t);
            stopSimulation();
        }
    }

    private void updateRelativeAccuracies(double[] y)
    {
        if( relativeAccuracies == null )
            relativeAccuracies = new double[y.length];

        for( int i = 0; i < y.length; ++i )
            relativeAccuracies[i] = Math.max( relativeTolerance * y[i], relativeAccuracies[i] );
    }

    private void stopSimulation()
    {
        simulator.stop();
    }


    private boolean checkNaN(double[] y)
    {
        return DoubleStream.of(y).anyMatch(Double::isNaN);
    }

    /**
     * Check if we already reached steady state
     * @return
     */
    private boolean checkSteadyState()
    {
        steadyStateReached = false;
        if( variableValues.size() < validationSize )
            return false;
        Iterator<double[]> iter = variableValues.iterator();
        double[] first = iter.next();
        
        double[] next = iter.next();
        while( iter.hasNext() )
        {
            if( !equals( first, next ) )
                return false;
            next = iter.next();
        }
        
//        StringBuffer result = new StringBuffer("Steady state reached: ");
//        for (double v: variableValues.getFirst())
//            result.append(v+"\t");
//
//        System.out.println(result.toString());
        
        steadyStateReached = true;
        return true;
    }

    private boolean steadyStateReached = false;

    public boolean isSteadyStateReached()
    {
        return this.steadyStateReached;
    }

    /**
     * check equality of <b>a</b> and <b>b</b> arrays, but do not take into account index <b>exclude</b>
     */
    private boolean equals(double[] a, double[] b)
    {
        return IntStream.of( checkIndexes )
                .allMatch( i -> Math.abs(a[i] - b[i]) < relativeAccuracies[i] && Math.abs(a[i] - b[i]) < absoluteTolerance );
    }

    @Override
    public void start(Object model)
    {
        variableValues = new ArrayDeque<>();
        relativeAccuracies = null;
    }
}
