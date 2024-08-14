package biouml.plugins.agentmodeling.covid19;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import biouml.model.Diagram;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.SimulationEngine;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.simulation.SimulationResult;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.analysis.optimization.OptimizationProblem;
import ru.biosoft.analysis.optimization.Parameter;
import ru.biosoft.analysis.optimization.methods.SRESOptMethod;
import ru.biosoft.jobcontrol.JobControl;

public class ProcessEstimator
{
    private static String[] names6 = new String[] {"c1", "c2", "c3", "d1", "d2", "d3"};
    private static String[] names4 = new String[] {"c1", "c2", "d1", "d2"};
    private static String[] names2 = new String[] {"c1", "d1"};

    public SimulationResult simulate(Diagram d, EstimationResult estimationResult, double timeLimit) throws Exception
    {
        SimulationEngine engine = DiagramUtility.getPreferredEngine( d );
        engine.setDiagram( d );
        engine.setCompletionTime( timeLimit );
        Model model = engine.createModel();
        model.init();
        applyParameters( engine, model, estimationResult.getValues(), estimationResult.getNames() );
        SimulationResult result = engine.generateSimulationResult();
        engine.simulate( model, result );
        return result;
    }

    public SimulationResult simulate(Diagram d, double[] parameters, double timeLimit) throws Exception
    {
        SimulationEngine engine = DiagramUtility.getPreferredEngine( d );
        engine.setDiagram( d );
        engine.setCompletionTime( timeLimit );
        Model model = engine.createModel();
        model.init();
        applyParameters( engine, model, parameters, parameters.length == 2 ? names2 : parameters.length == 4 ? names4 : names6 );
        SimulationResult result = engine.generateSimulationResult();
        engine.simulate( model, result );
        return result;
    }

    private void applyParameters(SimulationEngine engine, Model model, double[] values, String[] names) throws Exception
    {
        double[] curValues = model.getCurrentValues();
        
        for( int i = 0; i < names6.length; i++ )
        {
            String name = names6[i];
            int index = engine.getVarIndexMapping().get( name );
            curValues[index] = 0;
        }
        
        for( int i = 0; i < names.length; i++ )
        {
            String name = names[i];
            double value = values[i];
            int index = engine.getVarIndexMapping().get( name );
            curValues[index] = value;
        }
        model.setCurrentValues( curValues );
    }

    public EstimationResult estimate(Diagram d, double[] quartiles, int size, double timeLimit) throws Exception
    {
        Function f = new Function( d, quartiles, size, timeLimit );
        SRESOptMethod method = new SRESOptMethod( null, "sresTest" );
        method.setOptimizationProblem( f );
        method.getParameters().setNumOfIterations( 200 );
        method.getParameters().setSurvivalSize( 20 );
        double[] solution = method.getSolution();
        double distance = f.calculateDistance( solution );

        return new EstimationResult( size == 2 ? names2 : size == 4 ? names4 : names6, round( solution, 3 ), round( quartiles, 3 ),
                distance );
    }

    private double round(double value, int places)
    {
        long factor = (long)Math.pow( 10, places );
        value = value * factor;
        long tmp = Math.round( value );
        return (double)tmp / factor;
    }

    private double[] round(double[] value, int places)
    {
        double[] result = new double[value.length];
        for( int i = 0; i < value.length; i++ )
            result[i] = round( value[i], places );
        return result;
    }

    public EstimationResult createResult(Diagram d, double[] values, double[] quartiles, double timeLimit) throws Exception
    {
        Function f = new Function( d, quartiles, values.length, timeLimit );
        double error = f.calculateDistance( values );

        if( values.length == 2 )
            return new EstimationResult( names2, values, quartiles, error );
        else if( values.length == 4 )
            return new EstimationResult( names4, values, quartiles, error );
        else if( values.length == 6 )
            return new EstimationResult( names6, values, quartiles, error );
        else
            return null;
    }


    public class Function implements OptimizationProblem
    {
        private double[] quartiles;
        private Model model;
        private SimulationEngine engine;
        private final List<Parameter> params2 = new ArrayList<>();
        private final List<Parameter> params4 = new ArrayList<>();
        private final List<Parameter> params6 = new ArrayList<>();
        int size;

        public Function(Diagram diagram, double[] quartiles, int size, double timeLimit) throws Exception
        {
            engine = DiagramUtility.getPreferredEngine( diagram );
            engine.setLogLevel( Level.OFF );
            engine.setCompletionTime( timeLimit );
            engine.setDiagram( diagram );
            model = engine.createModel();
            this.quartiles = quartiles;
            this.size = size;
            model.init();

            params2.add( new Parameter( "c1", 0, 0, 1 ) );
            params2.add( new Parameter( "d1", 0, 0, 20 ) );

            params4.add( new Parameter( "c1", 0, 0, 1 ) );
            params4.add( new Parameter( "c2", 0, 0, 1 ) );
            params4.add( new Parameter( "d1", 0, 0, 20 ) );
            params4.add( new Parameter( "d2", 0, 0, 20 ) );

            params6.add( new Parameter( "c1", 0, 0, 1 ) );
            params6.add( new Parameter( "c2", 0, 0, 1 ) );
            params6.add( new Parameter( "c3", 0, 0, 1 ) );
            params6.add( new Parameter( "d1", 0, 0, 10 ) );
            params6.add( new Parameter( "d2", 0, 0, 15 ) );
            params6.add( new Parameter( "d3", 0, 0, 20 ) );
        }

        private void applyParameters(SimulationEngine engine, Model model, double[] values) throws Exception
        {
            double[] curValues = model.getCurrentValues();
            List<Parameter> paramsToUse = size == 2 ? params2 : size == 4 ? params4 : params6;
            
            for( int i = 0; i < params6.size(); i++ )
            {
                String name = params6.get( i ).getName();
                int index = engine.getVarIndexMapping().get( name );
                curValues[index] = 0;
            }
            
            for( int i = 0; i < paramsToUse.size(); i++ )
            {
                String name = paramsToUse.get( i ).getName();
                double value = values[i];
                int index = engine.getVarIndexMapping().get( name );
                curValues[index] = value;
            }
            model.setCurrentValues( curValues );
        }

        protected double calculateDistance(double[] values) throws Exception
        {
            applyParameters( engine, model, values );
            SimulationResult result = engine.generateSimulationResult();
            engine.simulate( model, result );
            int l = result.getCount() - 1;
            double q1 = result.getValues( "Q_1" )[l];
            double q2 = result.getValues( "Q_2" )[l];
            double q3 = result.getValues( "Q_3" )[l];
            double q4 = result.getValues( "Q_4" )[l];
            double q2_5 = result.getValues( "Q_25" )[l];
            double q97_5 = result.getValues( "Q_975" )[l];

            if( quartiles.length == 3 )
            {
                return Math.sqrt( Math.pow( q1 - quartiles[0], 2 ) + Math.pow( q2 - quartiles[1], 2 ) + Math.pow( q3 - quartiles[2], 2 ) );
            }
            else if( quartiles.length == 4 )
            {
                return Math.sqrt( Math.pow( q1 - quartiles[0], 2 ) + Math.pow( q2 - quartiles[1], 2 ) + Math.pow( q3 - quartiles[2], 2 )
                        + Math.pow( q4 - quartiles[3], 2 ) );
            }
            {
                return Math.sqrt( Math.pow( q2_5 - quartiles[0], 2 ) + Math.pow( q1 - quartiles[1], 2 ) + Math.pow( q2 - quartiles[2], 2 )
                        + Math.pow( q3 - quartiles[3], 2 ) + Math.pow( q97_5 - quartiles[4], 2 ) );
            }
        }


        protected double calculatePenalty(double[] values)
        {
            return 0;
        }

        @Override
        public void stop()
        {
        }

        @Override
        public List<Parameter> getParameters()
        {
            return size == 2 ? params2 : size == 4 ? params4 : params6;
        }

        @Override
        public double[][] testGoodnessOfFit(double[][] values, JobControl jobControl) throws Exception
        {
            double[][] result = new double[values.length][];
            for( int i = 0; i < values.length; ++i )
                result[i] = new double[] {calculateDistance( values[i] ), calculatePenalty( values[i] )};
            return result;
        }

        @Override
        public double[] testGoodnessOfFit(double[] values, JobControl jobControl) throws Exception
        {
            return new double[] {calculateDistance( values ), calculatePenalty( values )};
        }

        @Override
        public Object[] getResults(double[] values, DataCollection<?> origin) throws Exception
        {
            return null;
        }

        @Override
        public int getEvaluationsNumber()
        {
            return 0;
        }
    }

    public static class EstimationResult
    {
        private String[] names;
        private double[] values;
        private double[] quartiles;
        private double error;
        int size;

        public EstimationResult(String[] names, double[] values, double[] quartiles, double error)
        {
            this.size = values.length;
            this.names = names;
            this.values = values;
            this.quartiles = quartiles;
            this.error = error;
        }

        public String[] getNames()
        {
            return names;
        }

        public double[] getValues()
        {
            return values;
        }

        public double[] getQuartiles()
        {
            return quartiles;
        }

        public double getError()
        {
            return error;
        }

        public String generateFormula(String x)
        {
            if( size == 2 )
                return values[0] + "*" + x + "( t - " + values[1] + " )";
            else if( size == 4 )
                return values[0] + "*" + x + "( t - " + values[2] + " ) + " + values[1] + "*" + x + "( t - " + values[3] + " )";
            return values[0] + "*" + x + "( t - " + values[3] + " ) + " + values[1] + "*" + x + "( t - " + values[4] + " ) + " + values[2]
                    + "*" + x + "( t - " + values[5] + " )";
        }
    }
}