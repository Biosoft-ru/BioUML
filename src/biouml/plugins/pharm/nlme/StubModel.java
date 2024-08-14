package biouml.plugins.pharm.nlme;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import one.util.streamex.StreamEx;

import biouml.plugins.simulation.ArraySpan;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.Span;
import biouml.plugins.simulation.ode.jvode.JVodeSolver;
import biouml.standard.simulation.ResultListener;

public class StubModel
{
    Model model;

    public StubModel(int[] subject, double[] dose, double atol, double rtol) throws Exception
    {
        model = new ODEModel();
        subjectToDose = new HashMap<>();
        for( int i = 0; i < subject.length; i++ )
        {
            subjectToDose.putIfAbsent( subject[i], dose[i] );
        }
    }

    private final HashMap<Integer, Double> subjectToDose;

    public double[] calc(double[] ka, double[] ke, double[] CL, double[] time, int[] subject)
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

                double kaValue = ka[startOfSubject];
                double keValue = ke[startOfSubject];
                double CLValue = CL[startOfSubject];
                double[] times = new double[endOfSubject - startOfSubject];
                System.arraycopy( time, startOfSubject, times, 0, times.length );

                Map<String, Double> paramsMapping = new HashMap<>();

                paramsMapping.put( "ka", kaValue );
                paramsMapping.put( "ke", keValue );
                paramsMapping.put( "CL", CLValue );

                double doseValue = subjectToDose.get( currentSubject );
                double[] resultValues = simulate( doseValue, paramsMapping, new ArraySpan( times ) );

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


    private double[] simulate(double dose, Map<String, Double> paramsMapping, Span span) throws Exception
    {
        model = new ODEModel();
        model.init( new double[] {0, dose}, paramsMapping );
        ResultCollector resultCollector = new ResultCollector();
        resultCollector.start( model );
        JVodeSolver solver = new JVodeSolver();
        solver.start( model, span, new ResultListener[] {resultCollector}, null );
        return resultCollector.getResult();
    }

    public void initRegime(int subject)
    {
        
    }

    public static class ResultCollector implements ResultListener
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
            return StreamEx.of(results).mapToDouble(res -> res[0]).toArray();
        }

    }
    //
    //    public static Diagram generateDiagram() throws Exception
    //    {
    //        DiagramGenerator generator = new DiagramGenerator( "model" );
    //        generator.createEquation( "y1", "-exp(ka)*y1", Equation.TYPE_RATE );
    //        generator.createEquation( "y2", "exp(ka)*y1-exp(ke)*y2", Equation.TYPE_RATE );
    //        generator.createEquation( "conc", "y2/exp(CL)*exp(ke)", Equation.TYPE_SCALAR );
    //        return generator.getDiagram();
    //    }
}
