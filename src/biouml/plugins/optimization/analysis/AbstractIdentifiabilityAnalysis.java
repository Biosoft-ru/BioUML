package biouml.plugins.optimization.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import biouml.plugins.optimization.OptimizationParameters;
import biouml.plugins.optimization.ParameterEstimationProblem;
import biouml.plugins.optimization.analysis.IdentifiabilityHelper.Point;
import biouml.plugins.simulation.ode.jvode.Util;
import one.util.streamex.StreamEx;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.Stat;
import ru.biosoft.analysis.optimization.OptimizationMethod;
import ru.biosoft.analysis.optimization.OptimizationProblem;
import ru.biosoft.analysis.optimization.Parameter;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.analysiscore.AnalysisParametersFactory;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

public abstract class AbstractIdentifiabilityAnalysis<T extends AbstractIdentifiabilityAnalysisParameters> extends AnalysisMethodSupport<T>
{
    /**Parameters for which maximum steps number is achieved */
    private List<String> undefinedParams;
    /**Non identifiable parameters*/
    private List<String> nParams;
    /**Partially non identifiable parameters/*/
    private List<String> pnParams;

    public AbstractIdentifiabilityAnalysis(DataCollection<?> origin, String name, T parameters)
    {
        super( origin, name, parameters );
        nParams = new ArrayList<>();
        pnParams = new ArrayList<>();
        undefinedParams = new ArrayList<>();
    }

    public List<String> getNonidentifiableParams()
    {
        return this.nParams;
    }

    public List<String> getPartiallyNonidentifiableParams()
    {
        return this.pnParams;
    }

    public List<String> getUndefinedParams()
    {
        return this.undefinedParams;
    }

    @Override
    public void validateParameters()
    {
        super.validateParameters();
        checkRange( "maxStepSize", 0, 1 );
        checkRange( "confidenceLevel", 0, 1 );
        checkGreater( "maxStepsNumber", 1 );
    }

    @Override
    public TableDataCollection justAnalyzeAndPut() throws Exception
    {
        pnParams.clear();
        nParams.clear();
        undefinedParams.clear();

        OptimizationMethod<?> method = parameters.getOptimizationMethod();
        method.setLogger( log );

        DataCollection<DataElement> resultsFolder;
        if( parameters.getOutputPath().exists() )
            resultsFolder = parameters.getOutputPath().getDataCollection( ru.biosoft.access.core.DataElement.class );
        else
            resultsFolder = DataCollectionUtils.createSubCollection( parameters.getOutputPath() );

        if( parameters.isSaveSolutions() )
        {
            DataElementPath solutionsPath = DataElementPath.create( resultsFolder, "solutions" );
            DataCollectionUtils.createSubCollection( solutionsPath );
            method.getParameters().setResultPath( solutionsPath );
        }

        AnalysisParametersFactory.write( resultsFolder, this );
        AnalysisParametersFactory.writePersistent( resultsFolder, this );

        List<Parameter> allParams = getFittingParameters( );
        List<Parameter> selectedParams = selectParameters( allParams );

        jobControl.pushProgress( 0, 3 );
        jobControl.popProgress();

        OptimizationParameters optParams = prepareOptimizationParameters();

        OptimizationProblem problem = new ParameterEstimationProblem( optParams );
        method.setOptimizationProblem( problem );

        OptPointSeeker seeker = new OptPointSeeker( method );
        
        double[] estimatedSolution = method.getSolution();
        seeker.resetValues( estimatedSolution );

        if( parameters.isSaveSolutions() )
            putSolution( method, estimatedSolution, "start estimation", null );

        final double objFunctionEstimation = method.getDeviation();
        //        final double delta = parameters.getDelta();

        int parametersSize = selectedParams.size();
        double delta = parameters.isManualBound() ? parameters.getDelta()
                : Stat.quantileChiSquare( parametersSize, parameters.getConfidenceLevel() );

        log.info( "" );
        String estimated = StreamEx.of( allParams ).map( p -> p.getName() + " = '" + p.getValue() + "'" ).joining( ", " );
        log.info( "Estimated values: " + estimated + "." );
        log.info( "Objective function estimation = '" + objFunctionEstimation + "'; maximum deviation = '" + delta + "'." );
        jobControl.pushProgress( 3, 10 );
        jobControl.popProgress();
        if( jobControl.isStopped() )
            return null;

        jobControl.pushProgress( 10, 95 );
        TableDataCollection resultTDC = TableDataCollectionUtils.createTableDataCollection( resultsFolder, "Result table" );
        resultTDC.getColumnModel().addColumn( "Name", String.class );
        resultTDC.getColumnModel().addColumn( "Value", Double.class );
        resultTDC.getColumnModel().addColumn( "Estimated value", Double.class );
        resultTDC.getColumnModel().addColumn( "Objective function value", Double.class );
        resultTDC.getColumnModel().addColumn( "Plot path", DataElementPath.class );

        jobControl.forCollection( selectedParams, selectedParam -> {

            String selectedName = selectedParam.getName().replace("/", "$"); //replace is done for composite diagrams
            double paramValueEstimation = selectedParam.getValue();
            List<Point> points = getLikelihoodPoints( seeker, selectedParam, objFunctionEstimation, delta );

            Point estimatedPoint = new Point( paramValueEstimation, objFunctionEstimation );
            Point bestPoint = StreamEx.of( points ).minByDouble( Point::getY ).orElse( estimatedPoint );

            DataElementPath imagePath = savePlot( parameters.getPlotType(), resultsFolder, points, selectedName, estimatedPoint, delta );

            TableDataCollectionUtils.addRow( resultTDC, selectedName,
                    new Object[] {selectedName, bestPoint.getX(), paramValueEstimation, bestPoint.getY(), imagePath} );

            return true;
        } );

        resultsFolder.put( resultTDC );
        if( nParams.size() > 0 )
            log.info( "Non identifiable parameters: " + nParams.toString());
        if( pnParams.size() > 0 )
            log.info( "Partially non identifiable parameters: " + pnParams.toString());
        if( undefinedParams.size() > 0 )
            log.info( "Undefined parameters (for which maximum steps number was achieved): " + undefinedParams.toString());
        return resultTDC;
    }

    public static int identifier = 0;
    public void setIdentifier(int index)
    {
        identifier = index;
    }

    private void putSolution(OptimizationMethod<?> method, double[] solution, String folderName, String subfolderName) throws Exception
    {
        DataElementPath path = method.getParameters().getResultPath().getChildPath(folderName);
        if( subfolderName != null )
        {
            if( !path.exists() )
                DataCollectionUtils.createSubCollection( path );
            path = path.getChildPath(subfolderName);
        }
        method.saveResults(path, solution, method.getDeviation(), method.getPenalty(),
                method.getOptimizationProblem().getEvaluationsNumber(), false, true);
    }

    private DataElementPath savePlot(@Nonnull String plotType, DataCollection<DataElement> resultsFolder, List<Point> points,
            String selectedName, Point estimatedPoint, double delta)
    {
        boolean xLog = parameters.isLogX();
        boolean yLog = parameters.isLogY();
        switch( plotType )
        {
            case IdentifiabilityHelper.PLOT_TYPE_PNG:
                return IdentifiabilityHelper.savePlotAsPNG(resultsFolder, points, selectedName, estimatedPoint, delta, xLog, yLog);
            case IdentifiabilityHelper.PLOT_TYPE_CHART:
                return IdentifiabilityHelper.savePlotAsChart(resultsFolder, points, selectedName, estimatedPoint, delta, xLog, yLog);
            default:
                throw new IllegalArgumentException( "Incorrect result plot element type: '" + plotType + "'." );
        }
    }

    private List<Point> getLikelihoodPoints(OptPointSeeker seeker, Parameter selectedParam, double objFunction, double delta)
    {
        double lowerBound = selectedParam.getLowerBound();
        double upperBound = selectedParam.getUpperBound();
        double startParamValue = Util.restrict( lowerBound, upperBound, selectedParam.getValue() );
        selectedParam.setValue( startParamValue );
        List<Point> points = new ArrayList<>();
        String selectedName = selectedParam.getName();
        double[] initialSolution = seeker.getValues();
        log.info( "" );
        log.info( "Analysing '" + selectedName + "'." );
        Point point = seeker.findOptimalPoint( selectedName, startParamValue, 0 ); //process initial point separately
        double startObjFunction = point == null ? objFunction : point.getY();
        if( point != null )
            points.add( point );
        log.info( "(Step 0) " + selectedName + " = " + startParamValue + " (initial value), objective function = " + startObjFunction );
        log.info( "Executing steps to the left." );
        points.addAll( findPointsInDirection( seeker, selectedParam, startObjFunction, delta, false ) );
        seeker.resetValues( initialSolution );
        log.info( "Executing steps to the right." );
        points.addAll( findPointsInDirection( seeker, selectedParam, startObjFunction, delta, true ) );
        seeker.resetValues( initialSolution );
        seeker.resetBounds( selectedName, lowerBound, upperBound );
        return StreamEx.of( points ).sortedByDouble( Point::getX ).toList();
    }

    private List<Point> findPointsInDirection(OptPointSeeker seeker, Parameter fixed, double objVal, double objBound, boolean stepRight)
    {
        List<Point> points = new ArrayList<>();
        String paramName = fixed.getName();
        double startParamVal = fixed.getValue();
        double bound = stepRight ? fixed.getUpperBound() : fixed.getLowerBound();
        double objFunctionBound = objVal + objBound;
        StepCalculator calculator = createStepCalculator( fixed, objBound, stepRight );
        double stepSize = calculator.getNextStep( objVal );
        int stepsNumber = calculator.getMaxSteps();
        double paramValue = startParamVal + stepSize;
        for( int i = 1; i <= stepsNumber && !jobControl.isStopped(); i++ )
        {
            boolean exceedsBound = exceedsBound( paramValue, bound, stepRight );
            if( exceedsBound )
                paramValue = bound;
            Point optPoint = seeker.findOptimalPoint( paramName, paramValue, i );
            if( optPoint == null )
                continue;
            points.add( optPoint );
            double curObjValue = optPoint.getY();
            logStep( i, stepsNumber, paramName, paramValue, stepSize, curObjValue );

            if( i < stepsNumber )
            {
                stepSize = calculator.getNextStep( curObjValue );
                paramValue += stepSize;
            }
            if( curObjValue > objFunctionBound )
            {
                log.info( "Maximum deviance level is exceeded." );
                break;
            }
            if( exceedsBound || !checkBound( paramValue, bound, stepSize, stepRight ) )
            {
                log.info( "Parameter bound exceeded." );
                if( pnParams.contains( paramName ) )
                {
                    pnParams.remove( paramName );
                    nParams.add( paramName );
                }
                else
                {
                    pnParams.add( paramName );
                }
                break;
            }
            if(i == stepsNumber)
            {
                log.info( "Maximum steps number is achieved." );
                undefinedParams.add(paramName);
            }
        }
        return points;
    }

    private StepCalculator createStepCalculator(Parameter p, double objBound, boolean right)
    {
        if( parameters.isManualSteps() )
        {
            return new ArrayStepCalculator( right ? parameters.getStepsRight() : parameters.getStepsLeft(), right );
        }
        else
        {
            double bound = right ? p.getUpperBound() : p.getLowerBound();
            return new AdaptiveStepCalculator( parameters.maxSteps, parameters.maxStepSize, bound, p.getValue(), objBound );
        }
    }

    private void logStep(int i, int stepsNumber, String paramName, double paramValue, double stepSize, double objVal)
    {
        log.info( "(Step " + i + " of " + stepsNumber + ") " + paramName + " = " + paramValue + " (step size = " + Math.abs( stepSize )
                + "), objective function = " + objVal );
    }

    private static abstract class StepCalculator
    {
        public abstract double getNextStep(double curObjValue);
        public abstract int getMaxSteps();
    }

    private static class ArrayStepCalculator extends StepCalculator
    {
        private double[] steps;
        private int index = 0;
        
        public ArrayStepCalculator(String steps, boolean right)
        {
            this( StreamEx.of( steps.split( "," ) ).mapToDouble( s -> right ? Double.parseDouble( s ) : -Double.parseDouble( s ) )
                    .toArray() );
        }

        public ArrayStepCalculator(double[] steps)
        {
            this.steps = steps;
        }

        @Override
        public int getMaxSteps()
        {
            return steps.length;
        }

        @Override
        public double getNextStep(double curObjValue)
        {
            return steps[index++];
        }
    }

    private static class AdaptiveStepCalculator extends StepCalculator
    {
        private int maxSteps;
        private double maxStepSize;
        //        private double multiplier;
        private double sign = 1;
        private double range;
        private double objBound;
        private double prevObjValue;
        private boolean firstStep = true;
        private double prevStep;
        public AdaptiveStepCalculator(int maxSteps, double maxStepFraction, double bound, double startVal, double objBound)
        {
            this.maxSteps = maxSteps;
            this.range = Math.abs( bound - startVal );
            this.sign = Math.signum( bound - startVal );
            this.maxStepSize = maxStepFraction * range;
            //            this.multiplier = 10 * objBound * range / maxSteps;
            this.objBound = objBound;
        }

        @Override
        public int getMaxSteps()
        {
            return maxSteps;
        }

        @Override
        public double getNextStep(double curObjValue)
        {
            if( firstStep )
            {
                prevObjValue = curObjValue;
                firstStep = false;
                prevStep = Math.min( range / maxSteps, maxStepSize );
                return sign * prevStep;
            }
            double stepSize = prevStep * objBound / ( maxSteps * Math.abs( curObjValue - prevObjValue ) );
            prevStep = stepSize;
            //            double stepSize = multiplier / ( objBound + 9 * Math.abs( curObjValue - prevObjValue ) * maxSteps );
            prevObjValue = curObjValue;
            return sign * Math.min( stepSize, maxStepSize );
        }
    }

    private boolean exceedsBound(double parValue, double boundValue, boolean isUpperBound)
    {
        return isUpperBound ? ( parValue >= boundValue ) : ( parValue <= boundValue );
    }
    private boolean checkBound(double parValue, double boundValue, double accuracy, boolean isUpperBound)
    {
        return ( isUpperBound ? parValue <= boundValue : parValue >= boundValue )
                || Math.abs( boundValue - parValue ) < Math.abs( accuracy );
    }

    private class OptPointSeeker
    {
        final OptimizationMethod<?> method;
        public OptPointSeeker(OptimizationMethod<?> method)
        {
            this.method = method;
        }

        public @CheckForNull Point findOptimalPoint(String paramName, double paramValue, int step)
        {
            List<Parameter> params = method.getOptimizationProblem().getParameters();
            for( int i = 0; i < params.size(); ++i )
            {
                Parameter param = params.get( i );
                if( param.getName().equals( paramName ) )
                {
                    param.setLowerBound( paramValue );
                    param.setValue( paramValue );
                    param.setUpperBound( paramValue );
                }
            }

            try
            {
                //try to use result from current optimization on the next step
                double[] solution = method.getSolution();
                if( parameters.isSaveSolutions() )
                    putSolution( method, solution, "fixed " + paramName, "(Step " + step + ") " + "fixed value = " + paramValue );
                resetValues( solution );
            }
            catch( Exception e )
            {
                log.log( Level.WARNING, "Cannot solve problem for '" + paramName + "'=" + paramValue, e );
                return null;
            }

            return new Point( paramValue, method.getDeviation() );
        }

        private void resetValues(double[] newValues)
        {
            List<Parameter> params = method.getOptimizationProblem().getParameters();
            for( int i = 0; i < params.size(); i++ )
                params.get( i ).setValue( newValues[i] );
        }

        private void resetBounds(String paramName, double lowerBound, double upperBound)
        {
            List<Parameter> params = method.getOptimizationProblem().getParameters();
            for( int i = 0; i < params.size(); ++i )
            {
                Parameter param = params.get( i );
                if( param.getName().equals( paramName ) )
                {
                    param.setLowerBound( lowerBound );
                    param.setUpperBound( upperBound );
                }
            }
        }

        private double[] getValues()
        {
            List<Parameter> params = method.getOptimizationProblem().getParameters();
            return StreamEx.of( params ).mapToDouble( Parameter::getValue ).toArray();
        }
    }

    protected abstract OptimizationParameters prepareOptimizationParameters();

    protected abstract List<Parameter> selectParameters(List<Parameter> allParameters);

    protected abstract List<Parameter> getFittingParameters();
}
