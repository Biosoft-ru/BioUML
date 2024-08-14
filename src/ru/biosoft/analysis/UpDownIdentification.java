package ru.biosoft.analysis;

import org.apache.commons.lang.StringEscapeUtils;

import one.util.streamex.IntStreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementCreateException;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.plugins.javascript.JavaScriptHostObjectBase;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.columnbeans.ColumnGroup;

@ClassIcon ( "resources/up-and-down-identification.gif" )
public class UpDownIdentification extends MicroarrayAnalysis<UpDownIdentificationParameters>
{
    public UpDownIdentification(DataCollection<?> origin, String name) throws Exception
    {
        super(origin, name, new UpDownIdentificationParameters());
    }

    public UpDownIdentification(DataCollection<?> origin, String name, Class<? extends JavaScriptHostObjectBase> jsClass) throws Exception
    {
        super(origin, name, jsClass, new UpDownIdentificationParameters());
    }

    protected UpDownIdentification(DataCollection<?> origin, String name, UpDownIdentificationParameters parameters)
    {
        super(origin, name, parameters);
    }

    protected UpDownIdentification(DataCollection<?> origin, String name, Class<? extends JavaScriptHostObjectBase> jsClass, UpDownIdentificationParameters parameters) throws Exception
    {
        super(origin, name, jsClass, parameters);
    }

    protected double upRegulatedFound = 0;
    protected double downRegulatedFound = 0;
    protected double upRegulatedFoundByMistake = 0;
    protected double downRegulatedFoundByMistake = 0;
    protected double upFDR = 0;
    protected double downFDR = 0;

    protected String[] keys;
    protected double[][] data;
    protected double[][] control;

    @Override
    public void setParameters(AnalysisParameters params) throws IllegalArgumentException
    {
        if( ! ( params instanceof UpDownIdentificationParameters ) )
            throw new IllegalArgumentException("Wrong parameters");

        parameters = (UpDownIdentificationParameters)params;
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        checkPaths();
        UpDownIdentificationParameters parameters = getParameters();

        ColumnGroup experimentData = parameters.getExperimentData();
        if( experimentData.getTable() == null )
            throw new IllegalArgumentException("Please specify experiment table");

        boolean allExpColumns = false;
        if( experimentData.getColumns().length == 0 )
        {
            allExpColumns = true;
            experimentData.setAllColumnsFromTable();
        }

        ColumnGroup controlData = parameters.getControlData();
        if( controlData.getTable() == null )
            throw new IllegalArgumentException("Please specify control table");
        boolean allCtrlColumns = false;
        if( controlData.getColumns().length == 0 )
        {
            allCtrlColumns = true;
            controlData.setAllColumnsFromTable();
        }
        if( allExpColumns || allCtrlColumns )
        {
            String tableLine = "Control table";
            if( allExpColumns )
                if( allCtrlColumns )
                    tableLine = "Experiment and Control tables";
                else
                    tableLine = "Experiment table";

            log.info("All numeric columns from " + tableLine + " will be considered for " + getAnalysisGoalMessage() + ".");
        }

        if( ( parameters.getExperimentData().getColumns().length < 3 || controlData.getColumns().length < 3 )
                && ( parameters.getMethodCode() == UpDownIdentificationParameters.STUDENT )
                && ( this.getClass().equals(UpDownIdentification.class) ) )
            throw new IllegalArgumentException("The Up and Down Identification analysis (Student test)"
                    + " is designed to analyze datasets with more than two columns.");
    }

    @Override
    public String generateJavaScript(Object parametersObject)
    {
        try
        {
            UpDownIdentificationParameters parameters = (UpDownIdentificationParameters)parametersObject;

            StringBuffer getSourceScript = new StringBuffer();
            String[] params = {"null", "null", "", "", "Student", "None", "Up and down regulated", "0.01", "-Infinity", "Infinity",
                    "false", ""};

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
            if( parameters.getMethod() != null )
                params[4] = "'" + StringEscapeUtils.escapeJavaScript( parameters.getMethod() ) + "'";
            if( parameters.getInputLogarithmBase() != null )
                params[5] = "'" + StringEscapeUtils.escapeJavaScript( parameters.getInputLogarithmBase() ) + "'";
            if( parameters.getOutputType() != null )
                params[6] = "'" + StringEscapeUtils.escapeJavaScript(parameters.getOutputType()) + "'";
            if( parameters.getPvalue() != null )
                params[7] = parameters.getPvalue().toString();
            if( parameters.getThresholdDown() != null )
                params[8] = parameters.getThresholdDown().toString();
            if( parameters.getThresholdUp() != null )
                params[9] = parameters.getThresholdUp().toString();
            if( parameters.isFdr() != null )
                params[10] = parameters.isFdr().toString();
            if( parameters.getOutputTablePath() != null )
                params[11] = "'" + StringEscapeUtils.escapeJavaScript(parameters.getOutputTablePath().toString()) + "'";

            String putTableScript = "data.save(result,'" + parameters.getOutputCollection().getCompletePath().toString() + "/');";

            return getSourceScript.append("var result = microarray.updown(" + String.join(", ", params) + ");\n").append(
                    putTableScript).toString();
        }
        catch( Exception ex )
        {
            return "Error during java script generating: " + ex.getMessage();
        }
    }

    @Override
    protected TableDataCollection getAnalyzedData() throws Exception
    {
        validateParameters();
        upRegulatedFound = 0;
        downRegulatedFound = 0;

        int outputTypeCode = parameters.getOutputTypeCode();
        boolean upRegulated = ( outputTypeCode & UpDownIdentificationParameters.UP_REGULATED ) == UpDownIdentificationParameters.UP_REGULATED;
        boolean downRegulated = ( outputTypeCode & UpDownIdentificationParameters.DOWN_REGULATED ) == UpDownIdentificationParameters.DOWN_REGULATED;
        initData(parameters.getExperimentData(), parameters.getControlData(), parameters.getThresholdDown(), parameters
                .getThresholdUp());

        TableDataCollection result;
        try
        {
            result = calculate(data, control, false);
        }
        catch( Exception e )
        {
            throw new DataElementCreateException(e, getParameters().getOutputTablePath(), TableDataCollection.class);
        }

        if( upRegulated )
            log.info("Up regulated objects found: " + upRegulatedFound);
        if( downRegulated )
            log.info("Down regulated objects found: " + downRegulatedFound);

        if( parameters.isFdr() )
        {
            calculateFDR();
            result.getInfo().setDescription(" FDR Up = " + upFDR + " FDR Down = " + downFDR);
        }
        return result;
    }
    TableDataCollection calculate(double[][] matrix1, double[][] matrix2, boolean isPermutated) throws Exception
    {
        TableDataCollection result = null;
        int outputType = parameters.getOutputTypeCode();
        double pvalueCutoff = parameters.getPvalue();

        double inputLogarithmBase = Util.getLogarithmBase(parameters.getInputLogarithmBaseCode());

        if( !isPermutated )
        {
            result = parameters.getOutputTable();

            result.getColumnModel().addColumn("-log(P-value)", Float.class);
        }

        int method = parameters.getMethodCode();

        for( int i = 0; i < keys.length && go; i++ )
        {
            incPreparedness(step++);
            boolean sign = true;
            double pvalue = 1;
            double score;
            try
            {
                correctInput(matrix1[i], matrix2[i], inputLogarithmBase);

                double[] experiment = matrix1[i];
                double[] control = matrix2[i];

                int n = experiment.length;
                int m = control.length;

                switch( method )
                {
                    case UpDownIdentificationParameters.STUDENT:
                    {
                        double statistic = Stat.studentTest(experiment, control);
                        pvalue = Stat.studentDistribution(Math.abs(statistic), n + m - 2, 80)[1];
                        sign = statistic >= 0;
                        break;
                    }
                    case UpDownIdentificationParameters.WILCOXON:
                    {
                        double statistic = Stat.wilcoxonTest(experiment, control);
                        sign = 2 * statistic >= ( ( n + m ) * n + n + 1 );
                        pvalue = Stat.wilcoxonDistributionFast(n + m, n, statistic, sign);
                        break;
                    }
                    case UpDownIdentificationParameters.LEHMAN_ROSENBLATT:
                    {
                        double statistic = Stat.lehmannRosenblattTest(experiment, control);
                        pvalue = 1 - Stat.tabledMisesDistr(statistic);
                        sign = Stat.mean(experiment) >= Stat.mean(control);
                        break;
                    }
                    case UpDownIdentificationParameters.KOLMOGOROV_SMIRNOV:
                    {
                        double statistic = Stat.kolmogorovStatistic(experiment, control);
                        pvalue = 1 - Stat.kolmogorovDistr(statistic);
                        sign = Stat.mean(experiment) >= Stat.mean(control);
                        break;
                    }
                }

                score = -Math.log10(pvalue);

                if( (pvalue > pvalueCutoff) != (outputType == UpDownIdentificationParameters.NON_CHANGED) )
                    continue;
                else if( outputType == UpDownIdentificationParameters.UP_REGULATED && !sign )
                    continue;
                else if( outputType == UpDownIdentificationParameters.DOWN_REGULATED && sign )
                    continue;
            }
            catch( Exception ex )
            {
                score = Double.NaN;
            }

            if( sign )
            {
                if( isPermutated )
                    upRegulatedFoundByMistake++;
                else
                {
                    upRegulatedFound++;
                }
            }
            else
            {
                score *= -1;
                if( isPermutated )
                    downRegulatedFoundByMistake++;
                else
                    downRegulatedFound++;
            }

            if( result != null )
            {
                TableDataCollectionUtils.addRow(result, keys[i], new Object[] {score}, true);
            }
        }
        if( result != null )
        {
            result.finalizeAddition();
        }
        return result;
    }

    private boolean inputWarning = false;
    protected void correctInput(double[] vector1, double[] vector2, double inputLogarithmBase)
    {
        //Exponenting input data
        if( inputLogarithmBase != 1 )
        {
            boolean warn1 = Util.pow(inputLogarithmBase, vector1);
            boolean warn2 = Util.pow(inputLogarithmBase, vector2);
            if(!inputWarning && (warn1 || warn2))
            {
                log.warning("Numeric overflow occured for input data. Please check whether specified logarithm base is correct.");
                inputWarning = true;
            }
        }
    }
    protected void initData(ColumnGroup experimentData, ColumnGroup controlData, double thresholdDown, double thresholdUp) throws Exception
    {
        TableDataCollection experimentTable = experimentData.getTable();
        TableDataCollection controlTable = controlData.getTable();
        String[] experimentColumns = experimentData.getNames();
        String[] controlColumns = controlData.getNames();
        int[] experimentIndices = TableDataCollectionUtils.getColumnIndexes(experimentTable, experimentColumns);
        int[] controlIndices = TableDataCollectionUtils.getColumnIndexes(controlTable, controlColumns);

        if( !experimentTable.equals(controlTable) )
        {
            experimentTable = TableDataCollectionUtils.join(TableDataCollectionUtils.INNER_JOIN, experimentTable, controlTable, null,
                    experimentColumns, controlColumns);

            //we assume that after join all columns will be added consequently so new indices are just {1,2,...}
            experimentIndices = IntStreamEx.ofIndices( experimentIndices ).toArray();

            int offset = experimentIndices.length;//from this point experiment indices ends and starts control indices
            controlIndices = IntStreamEx.ofIndices( controlIndices ).map( i -> i + offset ).toArray();
        }

        data = TableDataCollectionUtils.getComplicatedMatrix(experimentTable, experimentIndices, thresholdDown, thresholdUp);
        control = TableDataCollectionUtils.getComplicatedMatrix(experimentTable, controlIndices, thresholdDown, thresholdUp);
        keys = TableDataCollectionUtils.getKeysUnsorted(experimentTable);
    }

    protected void calculateFDR() throws Exception
    {
        int outputType = parameters.getOutputTypeCode();
        boolean upRegulated = ( outputType & UpDownIdentificationParameters.UP_REGULATED ) == UpDownIdentificationParameters.UP_REGULATED;
        boolean downRegulated = ( outputType & UpDownIdentificationParameters.DOWN_REGULATED ) == UpDownIdentificationParameters.DOWN_REGULATED;

        upRegulatedFoundByMistake = 0;
        downRegulatedFoundByMistake = 0;

        upFDR = 0;
        downFDR = 0;

        for( int niter = 0; niter < 50; niter++ )
        {
            data = Stat.permutationComplicatedMatrix(data);
            control = Stat.permutationComplicatedMatrix(control);
            calculate(data, control, true);
        }

        upFDR = ( upRegulatedFoundByMistake ) / ( 50d * data.length );
        downFDR = ( downRegulatedFoundByMistake ) / ( 50d * data.length );

        if( upRegulated )
            log.info("FDR for up regulated: " + upFDR);
        if( downRegulated )
            log.info("FDR for down regulated:" + downFDR);
    }

    protected String getAnalysisGoalMessage()
    {
        return "p-value calculation and identification of up- and down-regulated probes/genes";
    }
}
