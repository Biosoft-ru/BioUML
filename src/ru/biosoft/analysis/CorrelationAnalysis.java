package ru.biosoft.analysis;

import java.util.Vector;
import java.util.logging.Level;

import one.util.streamex.StreamEx;

import org.apache.commons.lang.StringEscapeUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.columnbeans.ColumnGroup;

@ClassIcon ( "resources/correlation-analysis.gif" )
public class CorrelationAnalysis extends MicroarrayAnalysis<CorrelationAnalysisParameters>
{
    private int correlationsFoundByMistake = 0;
    private double correlationsFDR = 0;

    public CorrelationAnalysis(DataCollection origin, String name) throws Exception
    {
        super(origin, name, new CorrelationAnalysisParameters());
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        super.validateParameters();

        ColumnGroup controlData = parameters.getControlData();
        if( controlData == null || controlData.getColumns().length == 0 )
            throw new IllegalArgumentException("Please specify control columns");
        if( controlData.getTable() == null )
            throw new IllegalArgumentException("Please specify control table");
        if( controlData.getTablePath().equals(parameters.getOutputTablePath()) )
            throw new IllegalArgumentException("Output is the same as the input. Please specify different output name.");

        switch( parameters.getDataSourceCode() )
        {
            case CorrelationAnalysisParameters.COLUMNWISE:
            {
                if( parameters.getControlData().getTable().getSize() != parameters.getExperimentData().getTable().getSize() )
                    throw new IllegalArgumentException("In column-wise case row counts for experiment and control must be equal");
            }
            case CorrelationAnalysisParameters.ROWWISE:
            {
                if( parameters.getControlData().getNames().length != parameters.getExperimentData().getNames().length )
                    throw new IllegalArgumentException("In row-wise case column counts for experiment and control must be equal");
            }
        }
    }

    @Override
    public String generateJavaScript(Object parametersObject)
    {
        try
        {
            CorrelationAnalysisParameters parameters = (CorrelationAnalysisParameters)parametersObject;

            StringBuffer getSourceScript = new StringBuffer();
            String[] params = {"null", "null", "", "", "Columns", "Matrix", "Pearson correlation", "0.01", "-Infinity", "Infinity",
                    "false", ""};

            if( parameters.getExperiment() != null )
            {
                getSourceScript.append("var experiment = data.get('"
                        + StringEscapeUtils.escapeJavaScript(parameters.getExperiment().getCompletePath().toString()) + "')\n");
                params[0] = "experiment";
            }

            if( parameters.getControl() != null )
            {
                if( parameters.getExperiment() != null && parameters.getControl().equals(parameters.getExperiment()) )
                    params[1] = "experiment";
                else
                {
                    getSourceScript.append("var control = data.get('"
                            + StringEscapeUtils.escapeJavaScript(parameters.getControl().getCompletePath().toString()) + "')\n");
                    params[1] = "control";
                }
            }

            if( parameters.getExperimentData().getColumns() != null )
                params[2] = "'" + StringEscapeUtils.escapeJavaScript(parameters.getExperimentData().getNamesDescription()) + "'";
            if( parameters.getControlData().getColumns() != null )
                params[3] = "'" + StringEscapeUtils.escapeJavaScript(parameters.getControlData().getNamesDescription()) + "'";

            if( parameters.getDataSource() != null )
                params[4] = "'" + StringEscapeUtils.escapeJavaScript(parameters.getDataSource().getValue().toString()) + "'";
            if( parameters.getResultType() != null )
                params[5] = "'" + StringEscapeUtils.escapeJavaScript(parameters.getResultType().getValue().toString()) + "'";
            if( parameters.getCorrelationType() != null )
                params[6] = "'" + StringEscapeUtils.escapeJavaScript(parameters.getCorrelationType().getValue().toString()) + "'";
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

            return getSourceScript.append("var result = microarray.correlation(" + String.join(", ", params) + ");\n").append(
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

        TableDataCollection result = calculate();

        if( parameters.isFdr() )
        {
            calculateFDR();
            result.getInfo().setDescription(" FDR = " + correlationsFDR);
            log.info("FDR = " + correlationsFDR);
        }
        return result;
    }


    /**
     * Method to calculate correlations with less memory using (but can not be used to calculate FDR)
     * @return
     * @throws Exception
     */
    private TableDataCollection calculate() throws Exception
    {
        TableDataCollection output = createOutput();
        int correlationType = parameters.getCorrelationTypeCode();
        double cutoff = parameters.getPvalue();

        int id = 0;

        TableDataCollection experiment = parameters.getExperimentData().getTable();
        TableDataCollection control = parameters.getControlData().getTable();

        int[] columnIndices1 = TableDataCollectionUtils.getColumnIndexes(experiment, parameters.getExperimentData().getNames());
        int[] columnIndices2 = TableDataCollectionUtils.getColumnIndexes(control, parameters.getControlData().getNames());

        double lowerBoundary = parameters.getThresholdDown();
        double upperBoundary = parameters.getThresholdUp();

        boolean rowWise = ( parameters.getDataSourceCode() == CorrelationAnalysisParameters.ROWWISE );

        int experimentSize = rowWise ? experiment.getSize() : experiment.getColumnModel().getColumnCount();
        int controlSize = rowWise ? control.getSize() : control.getColumnModel().getColumnCount();

        boolean resultAsRows = ( parameters.getResultTypeCode() == CorrelationAnalysisParameters.COLUMNS );
        boolean resultAsPvalueMatrix = ( parameters.getResultTypeCode() == CorrelationAnalysisParameters.MATRIX_PVALUE );

        for( int i = 0; i < experimentSize; i++ )
        {
            incPreparedness(step++);
            String elementName1 = getElementName(experiment, i, rowWise);
            double[] element1 = this.getElement(experiment, elementName1, rowWise, columnIndices1, lowerBoundary, upperBoundary);
            //            System.out.printf("%d: %d/%d\n", step, Runtime.getRuntime().freeMemory(), Runtime.getRuntime().totalMemory());

            if( resultAsRows )
            {
                for( int j = 0; j < controlSize; j++ )
                {

                    if( !go )
                        return output;

                    String elementName2 = getElementName(control, j, rowWise);
                    double[] element2 = this.getElement(control, elementName2, rowWise, columnIndices2, lowerBoundary, upperBoundary);

                    double[][] samples = Util.avoidNaNs(element1, element2);

                    double[] result = calculateCorrelation(correlationType, samples[0], samples[1]);

                    if( Math.abs(result[1]) > cutoff )
                        continue;

                    StreamEx<Object> rowBuffer = StreamEx.of(elementName1, elementName2, result[0], result[1]);

                    TableDataCollectionUtils.addRow(output, String.valueOf(++id), rowBuffer.toArray(), true);
                }
            }
            else
            {
                Vector<Object> rowBuffer = new Vector<>();
                for( int j = 0; j < controlSize; j++ )
                {
                    incPreparedness(step++);

                    if( !go )
                        return output;
                    String elementName2 = getElementName(control, j, rowWise);
                    double[] element2 = this.getElement(control, elementName2, rowWise, columnIndices2, lowerBoundary, upperBoundary);

                    double[][] samples = Util.avoidNaNs(element1, element2);

                    double[] result = calculateCorrelation(correlationType, samples[0], samples[1]);

                    if( Math.abs(result[1]) > cutoff )
                        continue;

                    rowBuffer.add(resultAsPvalueMatrix ? result[1] : result[0]);
                }
                TableDataCollectionUtils.addRow(output, elementName1, rowBuffer.toArray(), true);
            }
        }

        output.finalizeAddition();
        return output;
    }
    /**
     * @param correlationType
     * @param sample1
     * @param sample2
     * @return array: result[0] - corelation, result[1] - pvalue
     */
    protected double[] calculateCorrelation(int correlationType, double[] sample1, double[] sample2)
    {
        double correlation = 0;
        double pvalue = 1;
        int n = sample1.length;
        try
        {
            switch( correlationType )
            {
                case CorrelationAnalysisParameters.PEARSON:
                {
                    correlation = Stat.pearsonCorrelation(sample1, sample2);
                    pvalue = Stat.pearsonSignificance(correlation, n);
                    break;
                }
                case CorrelationAnalysisParameters.SPEARMAN:
                {
                    correlation = Stat.spearmanCorrelationPearson(sample1, sample2);
                    pvalue = Stat.pearsonSignificance(correlation, n);
                    break;
                }
            }
        }
        catch( Exception ex )
        {
            correlation = Double.NaN;
        }
        return new double[] {correlation, pvalue};
    }


    private String getElementName(TableDataCollection table, int i, boolean rowWise)
    {
        return rowWise ? table.getName(i) : table.getColumnModel().getColumn(i).getName();
    }

    private double[] getElement(TableDataCollection table, String name, boolean rowWise, int[] indices, double lowerBoundary,
            double upperBoundary) throws Exception
    {
        return rowWise ? TableDataCollectionUtils.getDoubleRow(table, indices, name, lowerBoundary, upperBoundary)
                : TableDataCollectionUtils.getColumn(table, name);
    }

    private TableDataCollection createOutput()
    {
        TableDataCollection output = parameters.getOutputTable();

        TableDataCollection experiment = parameters.getExperiment();
        TableDataCollection control = parameters.getControlData().getTable();

        if( parameters.getResultTypeCode() == CorrelationAnalysisParameters.COLUMNS )
        {
            if( experiment.getName().equals(control.getName()) )
            {
                output.getColumnModel().addColumn(experiment.getName() + "_left", String.class);
                output.getColumnModel().addColumn(experiment.getName() + "_right", String.class);
            }
            else
            {
                output.getColumnModel().addColumn(experiment.getName(), String.class);
                output.getColumnModel().addColumn(control.getName(), String.class);
            }
            output.getColumnModel().addColumn("Correlation", Double.class);
            output.getColumnModel().addColumn("P-value", Double.class);

        }
        else
        {
            switch( parameters.getDataSourceCode() )
            {
                case CorrelationAnalysisParameters.COLUMNWISE:
                {
                    for( String name : parameters.getControlData().getNames() )
                        output.getColumnModel().addColumn(name, Double.class);
                    break;
                }

                case CorrelationAnalysisParameters.ROWWISE:
                {
                    for( String name : control.getNameList() )
                        output.getColumnModel().addColumn(name, Double.class);
                    break;
                }
            }
        }
        return output;
    }

    /**
     * Method iteratively permutates data and calculates number of found correlations
     * TODO: find a way to avoid large arrays storing in memory (data1, data2)
     * @throws Exception
     */
    private void calculateFDR() throws Exception
    {
        TableDataCollection experiment = parameters.getExperimentData().getTable();
        TableDataCollection control = parameters.getControlData().getTable();

        int[] columnIndices1 = TableDataCollectionUtils.getColumnIndexes(experiment, parameters.getExperimentData().getNames());
        int[] columnIndices2 = TableDataCollectionUtils.getColumnIndexes(control, parameters.getControlData().getNames());

        double[][] data1 = TableDataCollectionUtils.getMatrix( experiment, columnIndices1, parameters.getThresholdDown(),
                parameters.getThresholdUp() );
        double[][] data2 = TableDataCollectionUtils.getMatrix( control, columnIndices2, parameters.getThresholdDown(),
                parameters.getThresholdUp() );

        if( parameters.getDataSourceCode() == CorrelationAnalysisParameters.COLUMNWISE )
        {
            data1 = Util.matrixConjugate(data1);
            data2 = Util.matrixConjugate(data2);
        }

        correlationsFoundByMistake = 0;
        correlationsFDR = 0;
        int permutationNumber = MicroarrayAnalysisParameters.FDR_PERMUTATION_NUMBER;
        for( int niter = 0; niter < permutationNumber; niter++ )
        {
            data1 = Stat.permutationMatrix(data1);
            data2 = Stat.permutationMatrix(data2);
            calculatePermutated(data1, data2);
        }

        correlationsFDR = ( correlationsFoundByMistake ) / ( (double)permutationNumber * data1.length * data2.length );
    }

    private void calculatePermutated(double[][] data1, double[][] data2) throws Exception
    {
        int correlationType = parameters.getCorrelationTypeCode();
        double cutoff = parameters.getPvalue();

        for( double[] element : data1 )
        {
            incPreparedness(step++);
            for( double[] element2 : data2 )
            {
                if( !go )
                    return;
                try
                {
                    double[][] samples = Util.avoidNaNs(element, element2);


                    double[] result = calculateCorrelation(correlationType, samples[0], samples[1]);

                    if( Math.abs(result[1]) <= cutoff )
                        correlationsFoundByMistake++;
                }
                catch( Exception ex )
                {
                    log.log(Level.SEVERE, "Error during correlation analysis: " + ex.getMessage());
                }

            }
        }
    }

    @Override
    public int getStepCount()
    {
        int stepCount = 0;
        switch( parameters.getDataSourceCode() )
        {
            case CorrelationAnalysisParameters.ROWWISE:
            {
                stepCount = parameters.getExperiment().getSize();
                break;
            }

            case CorrelationAnalysisParameters.COLUMNWISE:
            {
                stepCount = parameters.getExperimentData().getNames().length;
                break;
            }
            default:
                return 0;
        }
        if( parameters.isFdr() )
            stepCount *= MicroarrayAnalysisParameters.FDR_PERMUTATION_NUMBER;
        return stepCount;
    }
}
