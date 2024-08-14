package ru.biosoft.analysis;

import org.apache.commons.lang.StringEscapeUtils;

import one.util.streamex.DoubleStreamEx;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementCreateException;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.graphics.access.ChartDataElement;
import ru.biosoft.graphics.chart.AxisOptions;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.graphics.chart.ChartOptions;
import ru.biosoft.graphics.chart.ChartSeries;
import ru.biosoft.graphics.chart.HistogramBuilder;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

@ClassIcon ( "resources/fold-change.gif" )
public class FoldChange extends UpDownIdentification
{
    private HistogramBuilder histogramBuilder;

    public FoldChange(DataCollection origin, String name) throws Exception
    {
        super(origin, name, new FoldChangeParameters());
    }

    @Override
    public void setParameters(AnalysisParameters params) throws IllegalArgumentException
    {
        if( ! ( params instanceof FoldChangeParameters ) )
            throw new IllegalArgumentException("Wrong parameters");

        parameters = (FoldChangeParameters)params;
    }

    @Override
    public FoldChangeParameters getParameters()
    {
        return (FoldChangeParameters)parameters;
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        super.validateParameters();
        FoldChangeParameters parameters = getParameters();
        if( parameters.getTypeCode() == FoldChangeParameters.ONE_TO_ONE
                && parameters.getExperimentData().getColumns().length != parameters.getControlData().getColumns().length )
            throw new IllegalArgumentException("For one-to-one analysis experiment and control columns lengths must agree");
    }

    @Override
    public String generateJavaScript(Object parametersObject)
    {
        try
        {
            FoldChangeParameters parameters = (FoldChangeParameters)parametersObject;

            StringBuffer getSourceScript = new StringBuffer();
            String[] params = {"null", "null", "", "", "Average all", "log2", "log2", "-Infinity", "Infinity", ""};

            if( parameters.getExperiment() != null )
            {
                getSourceScript.append("var experiment = data.get('"
                        + StringEscapeUtils.escapeJavaScript(parameters.getExperiment().getCompletePath().toString()) + "')\n");
                params[0] = "experiment";
            }
            if( parameters.getControl() != null )
            {
                if( parameters.getExperiment() != null && parameters.getExperiment().equals(parameters.getControl()) )
                    params[1] = "experiment";
                else
                {
                    getSourceScript.append("var control = data.get('"
                            + StringEscapeUtils.escapeJavaScript(parameters.getControl().getCompletePath().toString()) + "');\n");
                    params[1] = "control";
                }
            }

            if( parameters.getExperimentData().getColumns() != null )
                params[2] = "'" + StringEscapeUtils.escapeJavaScript(parameters.getExperimentData().getNamesDescription()) + "'";
            if( parameters.getControlData().getColumns() != null )
                params[3] = "'" + StringEscapeUtils.escapeJavaScript(parameters.getControlData().getNamesDescription()) + "'";
            if( parameters.getType() != null )
                params[4] = "'" + StringEscapeUtils.escapeJavaScript( parameters.getType() ) + "'";
            if( parameters.getInputLogarithmBase() != null )
                params[5] = "'" + StringEscapeUtils.escapeJavaScript(parameters.getInputLogarithmBase()) + "'";
            if( parameters.getOutputLogarithmBase() != null )
                params[6] = "'" + StringEscapeUtils.escapeJavaScript(parameters.getOutputLogarithmBase()) + "'";
            if( parameters.getThresholdDown() != null )
                params[7] = parameters.getThresholdDown().toString();
            if( parameters.getThresholdUp() != null )
                params[8] = parameters.getThresholdUp().toString();
            if( parameters.getOutputTablePath() != null )
                params[9] = "'" + StringEscapeUtils.escapeJavaScript(parameters.getOutputTablePath().toString()) + "'";

            String putTableScript = "data.save(result,'" + parameters.getOutputCollection().getCompletePath().toString() + "/');";

            return getSourceScript.append("var result = microarray.foldchange(" + String.join(", ", params) + ");\n").append(
                    putTableScript).toString();
        }
        catch( Exception ex )
        {
            return "No java script available";
        }
    }

    @Override
    protected TableDataCollection getAnalyzedData() throws Exception
    {
        initData(parameters.getExperimentData(), parameters.getControlData(), parameters.getThresholdDown(), parameters
                .getThresholdUp());
        if(getParameters().getHistogramOutput() != null && getParameters().getTypeCode() == FoldChangeParameters.AVERAGE_ALL)
            histogramBuilder = new HistogramBuilder();

        TableDataCollection calculate;
        try
        {
            calculate = calculate(data, control, false);
        }
        catch( Exception e1 )
        {
            throw new DataElementCreateException(e1, getParameters().getOutputTablePath(), TableDataCollection.class);
        }
        
        if(histogramBuilder != null)
        {
            try
            {
                ChartSeries series = histogramBuilder.createSeries();
                Chart chart = new Chart();
                ChartOptions options = new ChartOptions();
                AxisOptions xOptions = new AxisOptions();
                xOptions.setLabel(calculate.getColumnModel().getColumn(0).getName());
                options.setXAxis(xOptions);
                AxisOptions yOptions = new AxisOptions();
                yOptions.setLabel("%");
                options.setYAxis(yOptions);
                chart.setOptions(options);
                chart.addSeries(series);
                getParameters().getHistogramOutput().save(new ChartDataElement(getParameters().getHistogramOutput().getName(), getParameters().getHistogramOutput().optParentCollection(), chart));
            }
            catch( Exception e )
            {
                throw new DataElementCreateException(getParameters().getHistogramOutput(), ChartDataElement.class);
            }
        }
        
        return calculate;
    }

    @Override
    TableDataCollection calculate(double[][] matrix1, double[][] matrix2, boolean isPermutated) throws Exception
    {
        TableDataCollection result = null;
        
        if( !isPermutated )
        {
            result = createOutputTable();
        }

        boolean averageExperiment = ( getParameters().getTypeCode() & FoldChangeParameters.AVERAGE_EXPERIMENT ) == FoldChangeParameters.AVERAGE_EXPERIMENT;
        boolean averageControl = ( getParameters().getTypeCode() & FoldChangeParameters.AVERAGE_CONTROL ) == FoldChangeParameters.AVERAGE_CONTROL;

        double inputLogarithmBase = Util.getLogarithmBase(getParameters().getInputLogarithmBaseCode());
        double outputLogarithmBase = Util.getLogarithmBase(getParameters().getOutputLogarithmBaseCode());

        boolean oneToOne = ( getParameters().getTypeCode() == FoldChangeParameters.ONE_TO_ONE );

        //for faster logarithm calculation
        double divider = ( outputLogarithmBase == 1 ) ? 0 : Math.log(outputLogarithmBase); //zero means that we should not take logarithm!

        for( int i = 0; i < keys.length && go; i++ )
        {
            incPreparedness(step++);

            correctInput(matrix1[i], matrix2[i], inputLogarithmBase);

            //Averaging input data
            if( averageExperiment )
                matrix1[i] = new double[] {Stat.mean(matrix1[i])};
            if( averageControl )
                matrix2[i] = new double[] {Stat.mean(matrix2[i])};

            Double[] foldChanges = ( oneToOne ) ? calculateOneToOne(matrix1[i], matrix2[i], divider) : calculateFoldChanges(matrix1[i],
                    matrix2[i], divider);
            
            if(histogramBuilder != null && foldChanges.length == 1) histogramBuilder.add(foldChanges[0]);

            if( result != null )
            {
                TableDataCollectionUtils.addRow(result, keys[i], foldChanges, true);
            }
        }
        if( result != null )
        {
            result.finalizeAddition();
        }
        return result;
    }

    /**
     * calculate fold change: each element of <b>experiment</b> array will be divided by each element of <b>control</b> array<br>
     * After that logarithm to the base outputLogarithmBase will be taken:<br> Math.log(result)/<b>divider</b>, here divider must be Math.log(outputLogarithmBase)
     * @param experiment
     * @param control
     * @param divider
     * @return
     */
    protected static Double[] calculateFoldChanges(double[] experiment, double[] control, double divider)
    {
        return DoubleStreamEx.of( control ).flatMap( ctl -> DoubleStreamEx.of( experiment ).map( exp -> fc( exp, ctl, divider ) ) ).boxed()
                .toArray( Double[]::new );
    }

    /**
     * calculate fold change: each element of <b>experiment</b> array will be divided by correspondent element of <b>control</b> array<br>
    * After that logarithm to the base outputLogarithmBase will be taken:<br> Math.log(result)/<b>divider</b>, here divider must be Math.log(outputLogarithmBase)
     * @param experiment
     * @param control
     * @param divider
     * @return
     */
    protected static Double[] calculateOneToOne(double[] experiment, double[] control, double divider)
    {
        return DoubleStreamEx.zip( experiment, control, (exp, ctl) -> fc(exp, ctl, divider) ).boxed().toArray( Double[]::new );
    }
    
    protected static double fc(double exp, double ctl, double divider)
    {
        double fc = exp / ctl;
        return divider == 0 ? fc : Math.log( fc ) / divider;
    }

    protected String[] createColumns(String[] experiment, String[] control)
    {
        switch( getParameters().getTypeCode() )
        {
            case FoldChangeParameters.AVERAGE_ALL:
                return new String[] {getParameters().getOutputLogarithmBaseCode() == Util.NONE?"FoldChange":"LogFoldChange"};
            case FoldChangeParameters.AVERAGE_EXPERIMENT:
                return control.clone();
            case FoldChangeParameters.AVERAGE_CONTROL:
            case FoldChangeParameters.ONE_TO_ONE:
                return experiment.clone();
            default:
                return StreamEx.of( control ).cross( experiment ).invert().join( "_" ).toArray( String[]::new );
        }
    }

    protected TableDataCollection createOutputTable()
    {
        TableDataCollection result = getParameters().getOutputTable();
        String[] columns = createColumns(getParameters().getExperimentData().getNames(), getParameters().getControlData().getNames());
        for( String column : columns )
        {
            result.getColumnModel().addColumn(column, Double.class);
        }
        return result;
    }
    
    @Override
    protected String getAnalysisGoalMessage()
    {
        return "fold change calculation";
    }
}
