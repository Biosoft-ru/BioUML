package biouml.plugins.pharm.nlme;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import one.util.streamex.StreamEx;

import biouml.plugins.simulation.ArraySpan;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.Simulator;
import biouml.plugins.simulation.Span;
import biouml.plugins.simulation.java.EventLoopSimulator;
import biouml.plugins.simulation.ode.jvode.JVodeOptions;
import biouml.standard.simulation.ResultListener;

public class MixedEffectModelRunner
{
    private Map<String, Integer> varIndexMapping;
    private int resultIndex;
    private Model odeModel;
    private MixedEffectModel model;

    public void setMixedEffectModel(MixedEffectModel model)
    {
        this.model = model;
        this.varIndexMapping = model.getVarIndexMapping();
        this.resultIndex = model.getResultIndex();
        this.odeModel = model.getODEModel();
    }

    public double[] calculate(Map<String, double[]> parameters, double[] time, int[] subject)
    {
        double[] result = new double[subject.length];

        try
        {
            int startOfSubject = 0;
            int endOfSubject = 0;
            while( startOfSubject < subject.length )
            {
                int currentSubject = subject[startOfSubject];
                while( currentSubject == subject[endOfSubject] )
                {
                    endOfSubject++;

                    if( endOfSubject >= subject.length )
                        break;
                }

                double[] subjectTimes = new double[endOfSubject - startOfSubject];
                System.arraycopy( time, startOfSubject, subjectTimes, 0, subjectTimes.length );

                Map<String, Double> subjectParameters = new HashMap<>( model.getDoseParameters( currentSubject ) );
                for( Map.Entry<String, double[]> entry : parameters.entrySet() )
                    subjectParameters.put( entry.getKey(),  entry.getValue()[startOfSubject] );

                double[] resultValues = simulate( subjectParameters, new ArraySpan( subjectTimes ) );

                System.arraycopy( resultValues, 0, result, startOfSubject, resultValues.length );

                startOfSubject = endOfSubject;
            }
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
        return result;
    }
    
    public double[] calculateSteadyState(Map<String, double[]> parameters, int[] subject, String[] steadyStateParams, double finalTime, double timeStep, double aTol, double rTol)
    {
        double[] result = new double[subject.length];

        try
        {
            int startOfSubject = 0;
            int endOfSubject = 0;
            while( startOfSubject < subject.length )
            {
                int currentSubject = subject[startOfSubject];
                while( currentSubject == subject[endOfSubject] )
                {
                    endOfSubject++;

                    if( endOfSubject >= subject.length )
                        break;
                }
                
                String[] observedParameters = new String[endOfSubject - startOfSubject];
                System.arraycopy( steadyStateParams, startOfSubject, observedParameters, 0, observedParameters.length );

                Map<String, Double> subjectParameters = new HashMap<>( model.getDoseParameters( currentSubject ) );
                for( Map.Entry<String, double[]> entry : parameters.entrySet() )
                    subjectParameters.put( entry.getKey(), entry.getValue()[startOfSubject]);

                double[] resultValues = simulateSteadyState( subjectParameters, observedParameters, new ArraySpan(0, finalTime, timeStep), aTol, rTol );
                
                System.arraycopy( resultValues, 0, result, startOfSubject, resultValues.length );

                startOfSubject = endOfSubject;
            }
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
        return result;
    }
    
    private double[] simulate(Map<String, Double> paramsMapping, Span span) throws Exception
    {
        odeModel.init();
        odeModel.init( odeModel.getInitialValues(), paramsMapping );
        ResultCollector resultCollector = new ResultCollector();
        resultCollector.start( odeModel );
        EventLoopSimulator solver = new EventLoopSimulator();
        JVodeOptions options = (JVodeOptions)solver.getOptions();
        options.setAtol( atol );
        options.setRtol( rtol );
        solver.start( odeModel, span, new ResultListener[] {resultCollector}, null );
        return resultCollector.getResult();
    }
    
    private double[] simulateSteadyState(Map<String, Double> inputParameters, int[] observedIndexes, Span span, double aTol, double rTol) throws Exception
    {
        Simulator simulator = new EventLoopSimulator();
        JVodeOptions options = (JVodeOptions)simulator.getOptions();
        options.setAtol( atol );
        options.setRtol( rtol );
        odeModel.init();
        odeModel.init( odeModel.getInitialValues(), inputParameters );
        SteadyStateFinder finder = new SteadyStateFinder(simulator, span, aTol, rTol);
        double[] value = finder.findSteadyState( odeModel, observedIndexes );
        return value;
    }

    private double[] simulateSteadyState(Map<String, Double> inputParameters, String[] observedParams, Span span, double aTol, double rTol) throws Exception
    {
       return simulateSteadyState(inputParameters, StreamEx.of( observedParams ).mapToInt( varIndexMapping::get ).toArray(), span, aTol, rTol);
    }

    public class ResultCollector implements ResultListener
    {
        public List<double[]> results;

        @Override
        public void start(Object model)
        {
            results = new ArrayList<>();
        }

        @Override
        public void add(double t, double[] y) throws Exception
        {
            results.add( y );
        }

        public double[] getResult()
        {
            return StreamEx.of(results).mapToDouble(res -> res[resultIndex]).toArray();
        }

    }
    
    private double atol = 1E-12;
    public void setAtol(double atol)
    {
        this.atol = atol;
    }
    
    private double rtol = 1E-8;
    public void setRtol(double rtol)
    {
        this.rtol = rtol;
    }
}
