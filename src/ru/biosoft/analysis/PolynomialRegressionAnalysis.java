package ru.biosoft.analysis;

import one.util.streamex.DoubleStreamEx;

import org.apache.commons.text.StringEscapeUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.graphics.chart.ChartSeries;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.columnbeans.ColumnGroup;


@ClassIcon("resources/polynomial-regression-analysis.gif")
public class PolynomialRegressionAnalysis extends MicroarrayAnalysis<PolynomialRegressionAnalysisParameters>
{
    protected double upRegulatedFound = 0;
    protected double downRegulatedFound = 0;
    protected double upRegulatedFoundByMistake = 0;
    protected double downRegulatedFoundByMistake = 0;
    protected double upFDR = 0;
    protected double downFDR = 0;

    private double[][] data;

    public PolynomialRegressionAnalysis(DataCollection<?> origin, String name) throws Exception
    {
        super(origin, name, new PolynomialRegressionAnalysisParameters());
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        super.validateParameters();
        PolynomialRegressionAnalysisParameters parameters = getParameters();
        if( parameters.getExperimentData() == null || parameters.getExperimentData().getColumns() == null )
            throw new IllegalArgumentException("Please specify experiment data for analysis");
        if( parameters.getExperimentData().getColumns().length <= parameters.getRegressionPower() )
            throw new IllegalArgumentException("Number of columns must exceed regression power");
        if( parameters.getOutputTablePath() == null || parameters.getOutputTablePath().optParentCollection() == null
                || parameters.getOutputTablePath().getName().equals("") )
            throw new IllegalArgumentException("Please specify valid output table.");
        if( parameters.getExperimentData().getTablePath().equals(parameters.getOutputTablePath()))
            throw new IllegalArgumentException("Output is the same as the input. Please specify different output name.");
    }

    @Override
    public String generateJavaScript(Object parametersObject)
    {
        try
        {
            PolynomialRegressionAnalysisParameters parameters = (PolynomialRegressionAnalysisParameters)parametersObject;

            StringBuffer getSourceScript = new StringBuffer();
            String[] params = {"null", "", "", "2", "0.01", "-Infinity", "Infinity", "false", ""};

            if( parameters.getExperiment() != null )
            {
                getSourceScript.append("var experiment = data.get('"
                        + StringEscapeUtils.escapeEcmaScript(parameters.getExperiment().getCompletePath().toString()) + "');\n");
                params[0] = "experiment";
            }

            if( parameters.getExperimentData().getColumns() != null )
                params[1] = "'" + StringEscapeUtils.escapeEcmaScript(parameters.getExperimentData().getNamesDescription()) + "'";
            if( parameters.getExperimentData().getTimePoints() != null )
                params[2] = "'" + StringEscapeUtils.escapeEcmaScript(parameters.getExperimentData().getTimePointsDescription()) + "'";
            if( parameters.getRegressionPower() != null )
                params[3] = parameters.getRegressionPower().toString();
            if( parameters.getPvalue() != null )
                params[4] = parameters.getPvalue().toString();
            if( parameters.getThresholdDown() != null )
                params[5] = parameters.getThresholdDown().toString();
            if( parameters.getThresholdUp() != null )
                params[6] = parameters.getThresholdUp().toString();
            if( parameters.isFdr() != null )
                params[7] = parameters.isFdr().toString();
            if( parameters.getOutputTablePath() != null )
                params[8] = "'" + StringEscapeUtils.escapeEcmaScript(parameters.getOutputTablePath().toString()) + "'";
            String putTableScript = "data.save(result,'" + parameters.getOutputCollection().getCompletePath().toString() + "/');";
            return getSourceScript.append("var result = microarray.regression(" + String.join(", ", params) + ");\n").append(
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

        TableDataCollection output = parameters.getOutputTable();

        output.getColumnModel().addColumn("SSE", Double.class);
        for( int i = 0; i <= parameters.getRegressionPower(); i++ )
        {
            output.getColumnModel().addColumn("Coefficient_" + i, String.class);
            output.getColumnModel().addColumn("Score_" + i, Double.class);
        }
        output.getColumnModel().addColumn("Profile", Chart.class);

        String[] keys = TableDataCollectionUtils.getKeysUnsorted(parameters.getExperiment());

        ColumnGroup columns = parameters.getExperimentData();
        data = TableDataCollectionUtils.getMatrix(columns.getTable(), TableDataCollectionUtils.getColumnIndexes(columns.getTable(), columns
                .getNames()), parameters.getThresholdDown(), parameters.getThresholdUp());

        double[] timePoints = parameters.getExperimentData().getTimePoints();

        int power = parameters.getRegressionPower();

        for( int i = 0; i < data.length; i++ )
        {
            incPreparedness(step++);

            double[][] safeData = Util.avoidNaNs(data[i], timePoints);
            double[] y = safeData[0];
            double[] x = safeData[1];
            Regression regression;
            try
            {
                regression = new Regression(y, x, power);
            }
            catch( Exception ex )
            {
                log.info("Regression for " + keys[i] + " can not be calculated because of." + ex.getMessage());
                continue;
            }
            double[] coefficients = regression.getCoefficients();
            double[] pvalues = regression.getPvalue();
            int n = pvalues.length;

            //Checking form of regression line (must be parabolic)
            if( power == 2 )
            {
                double[] newCoeff = new double[3];
                newCoeff[0] = ( coefficients[0] - ( coefficients[2] * coefficients[2] ) ) / ( 4 * coefficients[2] );
                newCoeff[1] = coefficients[2];
                newCoeff[2] = coefficients[1] / ( 2 * coefficients[2] );
                if( -newCoeff[2] > x[x.length - 1] || -newCoeff[2] < x[0] )
                    continue;
                coefficients = newCoeff;
            }

            if( pvalues[n - 1] >= parameters.getPvalue() )
                continue;
            else if( coefficients[n - 1] > 0 )
                upRegulatedFound++;
            else
                downRegulatedFound++;

            double[] score = DoubleStreamEx.zip( pvalues, coefficients, (pval, coef) -> -Math.log10( pval ) * Math.signum( coef ) )
                    .toArray();

            Chart profileChart = new Chart();

            ChartSeries values = new ChartSeries(x, regression.getY());
            values.setLabel("Profile");
            profileChart.addSeries(values);

            ChartSeries modeledValues = new ChartSeries(x, regression.getModelValues());
            modeledValues.setLabel("Restored profile");
            profileChart.addSeries(modeledValues);

            Object[] rowData = new Object[2 * n + 2];
            rowData[0] = regression.getSSE();
            for( int j = 0; j < n; j++ )
            {
                rowData[2 * j + 1] = coefficients[j];
                rowData[2 * j + 2] = score[j];
            }
            rowData[2 * n + 1] = profileChart;
            TableDataCollectionUtils.addRow(output, keys[i], rowData);
        }
        log.info("Up regulated found " + upRegulatedFound);
        log.info("Down regulated found " + downRegulatedFound);
        if( parameters.isFdr() )
        {
            getFDR();
            output.getInfo().setDescription(" FDR Up = " + upFDR + " FDR Down = " + downFDR);
            log.info("Up regulated FDR " + upFDR);
            log.info("Down regulated FDR " + downFDR);
        }
        return output;
    }

    void getFDR() throws Exception
    {
        upRegulatedFoundByMistake = 0;
        downRegulatedFoundByMistake = 0;
        upFDR = 0;
        downFDR = 0;

        for( int j = 0; j < 50; j++ )
        {
            data = Stat.permutationMatrix(data);
            double[] timePoints = parameters.getExperimentData().getTimePoints();
            int power = parameters.getRegressionPower();
            for( double[] element : data )
            {
                incPreparedness(step++);
                double[][] safeData = Util.avoidNaNs(element, timePoints);
                double[] y = safeData[0];
                double[] x = safeData[1];
                Regression regression = new Regression(y, x, power);
                double[] pvalues = regression.getPvalue();
                double[] coefficients = regression.getCoefficients();

                if( ( pvalues[pvalues.length - 1] ) >= parameters.getPvalue() )
                    continue;
                else if( coefficients[pvalues.length - 1] > 0 )
                    upRegulatedFoundByMistake++;
                else
                    downRegulatedFoundByMistake++;

            }
        }

        upFDR = ( upRegulatedFoundByMistake ) / ( 50 * data.length );
        downFDR = ( downRegulatedFoundByMistake ) / ( 50 * data.length );

    }
}
