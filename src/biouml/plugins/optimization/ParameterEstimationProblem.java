package biouml.plugins.optimization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import biouml.model.Diagram;
import one.util.streamex.StreamEx;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.optimization.OptimizationProblem;
import ru.biosoft.analysis.optimization.Parameter;
import ru.biosoft.analysis.optimization.Parameter.Locality;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.table.TableDataCollection;

public class ParameterEstimationProblem implements OptimizationProblem
{
    private final OptimizationParameters params;

    private final SingleExperimentParameterEstimation[] singleEstimations;

    private int evaluations;

    public ParameterEstimationProblem(OptimizationParameters params)
    {
        this(params, null);
    }

    public ParameterEstimationProblem(OptimizationParameters params, OptimizationConstraintCalculator calculator)
    {
        this.params = params;

        List<OptimizationExperiment> experiments = params.getOptimizationExperiments();

        evaluations = 0;

        singleEstimations = new SingleExperimentParameterEstimation[experiments.size()];

        List<OptimizationConstraint> constraints = params.getOptimizationConstraints();

        OptimizationConstraintCalculator calc = calculator;
        if( calc == null && constraints != null && constraints.size() > 0 )
        {
            calc = new OptimizationConstraintCalculator();
            calc.parseConstraints(constraints, params.getDiagram());
        }

        for( int i = 0; i < experiments.size(); ++i )
        {
            OptimizationExperiment exp = experiments.get( i );

            String stateName = exp.getDiagramStateName();
            Diagram diagram = params.getDiagram();
            diagram.setCurrentStateName( stateName );
            singleEstimations[i] = new SingleExperimentParameterEstimation( params.getSimulationTaskParameters().get( exp.getName() ), exp,
                    getParameters(), constraints );

            if( params.optimizerParameters != null ) //in some weird cases it is not initialized TODO: fix this
                singleEstimations[i].setApplyState( params.optimizerParameters.isApplyState());
            singleEstimations[i].setCalculator(calc);
        }
    }

    @Override
    public void stop()
    {
        for( SingleExperimentParameterEstimation singleEstimation : singleEstimations )
            singleEstimation.stop();
    }

    private List<Parameter> fParams;
    @Override
    public List<Parameter> getParameters()
    {
        if( fParams == null )
        {
            fParams = new ArrayList<>();

            //create a list of parameters to fit
            for( Parameter next : params.getFittingParameters() )
            {
                List<Parameter> newParameters = createParameters( next );
                for( Parameter newParameter : newParameters )
                    fParams.add( newParameter );
            }

            //set values if needed
            if( params.optimizerParameters != null && params.optimizerParameters.isUseStartingParameters() )
            {
                DataElementPath path = params.optimizerParameters.getStartingParameters();
                TableDataCollection tdc = path.getDataElement( TableDataCollection.class );

                for( Parameter next : fParams )
                {
                    String column = "Value";
                    if( next.isLocal() )
                    {
                        column = next.getScope().get( 0 );
                    }

                    try
                    {
                        String param = next.getName();
                        if( tdc.get( param ) != null && tdc.get( param ).getValue( column ) != null )
                        {
                            double value = Double.parseDouble( tdc.get( param ).getValue( column ).toString() );
                            if( !Double.isNaN( value ) )
                                next.setValue( value );
                        }
                    }
                    catch( Exception e )
                    {
                    }
                }
            }
        }
        return fParams;
    }
    
    private List<Parameter> createParameters(Parameter prototype)
    {
        List<Parameter> result = new ArrayList<Parameter>();
        if( !prototype.isLocal() )
        {
            result.add( prototype.copy() );
        }
        else
        {
            if( prototype.getLocality().equals( Locality.LOCAL_IN_EXPERIMENTS.toString() ) )
                for( OptimizationExperiment exp : params.getOptimizationExperiments() )
                {
                    List<String> scope = new ArrayList<>();
                    scope.add( exp.getName() );
                    result.add( createLocalParameter( prototype, scope ) );
                }

            if( prototype.getLocality().equals( Locality.LOCAL_IN_CELL_LINES.toString() ) )
            {
                Map<String, List<String>> cellLines = new HashMap<>();
                for( OptimizationExperiment exp : params.getOptimizationExperiments() )
                {
                    String cl = exp.getCellLine();
                    if( !cl.equals( "" ) )
                    {
                        if( cellLines.containsKey( cl ) )
                            cellLines.get( cl ).add( exp.getName() );
                        else
                        {
                            List<String> scope = new ArrayList<>();
                            scope.add( exp.getName() );
                            cellLines.put( cl, scope );
                        }
                    }
                    else
                    {
                        List<String> scope = new ArrayList<>();
                        scope.add( exp.getName() );
                        result.add( createLocalParameter( prototype, scope ) );
                    }
                }
                for( String cl : cellLines.keySet() )
                    result.add( createLocalParameter( prototype, cellLines.get( cl ) ) );
            }
        }
        return result; 
    }
 
    private Parameter createLocalParameter(Parameter oldParam, List<String> scope)
    {
        Parameter newParam = new Parameter(oldParam.getName(), oldParam.getValue(), oldParam.getLowerBound(), oldParam.getUpperBound());
        newParam.setTitle(oldParam.getTitle());
        newParam.setComment(oldParam.getComment());
        newParam.setLocality(oldParam.getLocality());
        newParam.setScope(scope);
        return newParam;
    }

    @Override
    public double[][] testGoodnessOfFit(double[][] values, JobControl jobControl) throws Exception
    {
        double[][] result = null;
        for( int k = 0; k < singleEstimations.length; ++k )
        {
            double[][] singleEstimationValues = initSingleEstimationValues(values, singleEstimations[k].getExperiment());
            double[][] next = singleEstimations[k].testGoodnessOfFit(singleEstimationValues, jobControl);

            if (next == null)
                return result;
            
            if( k == 0 )
            {
                result = next;
            }
            else
            {
                for( int i = 0; i < result.length; ++i )
                {
                    for( int j = 0; j < result[0].length; ++j )
                    {
                        if( next[i][j] == Double.POSITIVE_INFINITY )
                            result[i][j] = Double.POSITIVE_INFINITY;
                        else
                            result[i][j] += next[i][j];
                    }
                }
            }

            evaluations += values.length;
        }
        return result;
    }

    private double[][] initSingleEstimationValues(double[][] values, OptimizationExperiment exp)
    {
        //All fitting parameters are global.
        if( params.getFittingParameters().size() == values[0].length )
            return values;

        return StreamEx.of(values).map( row -> getSingleEstimationValues(row, exp) ).toArray( double[][]::new );
    }

    private double[] getSingleEstimationValues(double[] values, OptimizationExperiment exp)
    {
        int length = params.getFittingParameters().size();
        double[] singleExperimentValues = new double[length];

        int index = 0;
        for( int j = 0; j < fParams.size(); ++j )
        {
            List<String> scope = fParams.get(j).getScope();
            if( scope == null || scope.contains(exp.getName()) )
            {
                singleExperimentValues[index] = values[j];
                index++;
            }
        }
        return singleExperimentValues;
    }

    @Override
    public double[] testGoodnessOfFit(double[] values, JobControl jobControl) throws Exception
    {
        double[] distances = new double[singleEstimations.length];
        double[] penalties = new double[singleEstimations.length];

        for( int k = 0; k < singleEstimations.length; ++k )
        {
            OptimizationExperiment exp = singleEstimations[k].getExperiment();
            double[] next = singleEstimations[k].testGoodnessOfFit(getSingleEstimationValues(values, exp), jobControl);

            distances[k] = next[0];
            penalties[k] = next[1];

            evaluations++;
        }
        return new double[] {sumArray(distances), sumArray(penalties)};
    }

    private double sumArray(double[] array)
    {
        double result = 0;
        for( double element : array )
        {
            if( element == Double.POSITIVE_INFINITY )
                return Double.POSITIVE_INFINITY;
            else
                result += element;
        }
        return result;
    }

    @Override
    public int getEvaluationsNumber()
    {
        return evaluations;
    }

    @Override
    public Object[] getResults(double[] values, DataCollection<?> origin) throws Exception
    {
        List<Object> results = new ArrayList<>();

        for( SingleExperimentParameterEstimation singleEstimation : singleEstimations )
        {
            OptimizationExperiment exp = singleEstimation.getExperiment();
            Object[] r = singleEstimation.getResults(getSingleEstimationValues(values, exp), origin);
            results.addAll( Arrays.asList(r) );
        }

        results.add(params);

        return results.toArray(new Object[results.size()]);
    }
}
