
package biouml.plugins.bindingregions.utils;

import java.util.Map;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.Stat;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.graphics.chart.Chart;

/**
 * @author yura
 *
 */
public class MultivariateSamples
{
    private String sampleName1;
    private String[] objectNames1;
    private double[][] sample1;
    private String sampleName2;
    private String[] objectNames2;
    private double[][] sample2;
    private String[] variableNames; // variableNames.length == sample1[0].length == sample2[0].length
    
    public MultivariateSamples(String sampleName1, String[] objectNames1, double[][] sample1, String sampleName2, String[] objectNames2, double[][] sample2, String[] variableNames)
    {
        this.sampleName1 = sampleName1;
        this.objectNames1 = objectNames1;
        this.sample1 = sample1;
        this.sampleName2 = sampleName2;
        this.objectNames2 = objectNames2;
        this.sample2 = sample2;
        this.variableNames = variableNames;
    }
    
    public void writeChartsWithVariablesDensities(DataElementPath pathToOutputs, Boolean doAddTwoZeroPoints, Map<String, Double> nameAndMultipliers, String windowSelector, Double givenWindow, AnalysisJobControl jobControl, int from, int to) throws Exception
    {
        int difference = to - from;
        for( int i = 0; i < variableNames.length; i++ )
        {
            double[] variableInSample1 = MatrixUtils.getColumn(sample1, i);
            double[] variableInSample2 = MatrixUtils.getColumn(sample2, i);
            SampleComparison sc = new SampleComparison(sampleName1, variableInSample1, sampleName2, variableInSample2, variableNames[i]);
            Chart chart = sc.chartWithSmoothedDensities(doAddTwoZeroPoints, nameAndMultipliers, windowSelector, givenWindow);
            TableUtils.addChartToTable(variableNames[i], chart, pathToOutputs.getChildPath("variables_densities_chart"));
            if( jobControl != null )
                jobControl.setPreparedness(from + (i + 1) * difference / variableNames.length);
        }
    }
    
    public void writeTableWithWilcoxonAndStudent(DataElementPath pathToOutputs, AnalysisJobControl jobControl, int from, int to)
    {
        int difference = to - from, m = variableNames.length;
        double[][] data = new double[m][];
        for( int i = 0; i < m; i++ )
        {
            double[] variableInSample1 = MatrixUtils.getColumn(sample1, i);
            double[] meanAndSigma1 = Stat.getMeanAndSigma(variableInSample1);
            double[] variableInSample2 = MatrixUtils.getColumn(sample2, i);
            double[] meanAndSigma2 = Stat.getMeanAndSigma(variableInSample2);
            double[] test = StatUtil.getWilcoxonTwoSampleRankTest(variableInSample1, variableInSample2);
            double statistic = Stat.studentTest(variableInSample1, variableInSample2);
            double pValue = 2.0 * Stat.studentDistribution(Math.abs(statistic), variableInSample1.length + variableInSample2.length - 2, 100)[1];
            data[i] = new double[]{meanAndSigma1[0], meanAndSigma1[1], meanAndSigma2[0], meanAndSigma2[1], test[0], test[1], statistic, pValue};
            if( jobControl != null )
                jobControl.setPreparedness(from + (i + 1) * difference / m);
        }
        TableUtils.writeDoubleTable(data, variableNames, new String[] {"Mean of (" + sampleName1 + ")", "Sigma of (" + sampleName1 + ")", "Mean of (" + sampleName2 + ")", "Sigma of (" + sampleName2 + ")", "Z-score (normal approximation of Wilcoxon statistic)", "p-value for Wilcoxon test", "Student statistic", "p-value for Student test"}, pathToOutputs, "variables_Wilcoxon_Student");
    }

    public void writeTableWithWilcoxon(DataElementPath pathToOutputs)
    {
        int m = variableNames.length;
        double[][] data = new double[m][];
        for( int i = 0; i < m; i++ )
        {
            double[] variableInSample1 = MatrixUtils.getColumn(sample1, i);
            double[] meanAndSigma1 = Stat.getMeanAndSigma(variableInSample1);
            double[] variableInSample2 = MatrixUtils.getColumn(sample2, i);
            double[] meanAndSigma2 = Stat.getMeanAndSigma(variableInSample2);
            double[] test = StatUtil.getWilcoxonTwoSampleRankTest(variableInSample1, variableInSample2);
            data[i] = new double[]{meanAndSigma1[0], meanAndSigma1[1], meanAndSigma2[0], meanAndSigma2[1], test[0], test[1]};
        }
        TableUtils.writeDoubleTable(data, variableNames, new String[] {"Mean of (" + sampleName1 + ")", "Sigma of (" + sampleName1 + ")", "Mean of (" + sampleName2 + ")", "Sigma of (" + sampleName2 + ")", SampleComparison.Z_SCORE, "p-value"}, pathToOutputs, "variables_WilcoxonPaired");
    }
    
    // it is copied
    public static Object[] getWithinAndBetweenAndTotalSSPmatrices(double[][] dataMatrix, int[] indicesOfSamples)
    {
        double[][] totalSSPmatrix = MultivariateSample.getSSPmatrix(dataMatrix);
        double[][] withinSSPmatrix = MatrixUtils.getLowerTriangularMatrix(dataMatrix[0].length);
        Map<Integer, double[][]> sampleIndexAndDataMatrix = Clusterization.createClusterIndexAndDataMatrix(dataMatrix, indicesOfSamples);
        for( double[][] matrix : sampleIndexAndDataMatrix.values() )
        {
            double[][] sspMatrix = MultivariateSample.getSSPmatrix(matrix);
            withinSSPmatrix = MatrixUtils.getSumOfMatrices(withinSSPmatrix, sspMatrix);
        }
        double[][] betweenSSPmatrix = MatrixUtils.getSubtractionOfMatrices(totalSSPmatrix, withinSSPmatrix);
        return new Object[]{withinSSPmatrix, betweenSSPmatrix, totalSSPmatrix};
    }
    
    /***
     * 
     * @param dataMatrix
     * @param indicesOfSamples
     * @return Object[] array : array[0] = double[][] meanVectors, array[1] = double[][] covarianceMatrix;
     */
    public static Object[] getMeanVectorsAndCovarianceMatrix(double[][] dataMatrix, int[] indicesOfSamples)
    {
        Map<Integer, double[][]> sampleIndexAndDataMatrix = Clusterization.createClusterIndexAndDataMatrix(dataMatrix, indicesOfSamples);
        double[][] meanVectors = new double[sampleIndexAndDataMatrix.size()][];
        double[][] covarianceMatrix = MatrixUtils.getLowerTriangularMatrix(dataMatrix[0].length);
        for( int i = 0; i < sampleIndexAndDataMatrix.size(); i++ )
        {
            double[][] dataSubmatrix = sampleIndexAndDataMatrix.get(i);
            meanVectors[i] = MultivariateSample.getMeanVector(dataSubmatrix);
            double[][] sspMatrix = MatrixUtils.getProductXtrHX(dataSubmatrix, meanVectors[i]);
            covarianceMatrix = MatrixUtils.getSumOfMatrices(covarianceMatrix, sspMatrix);
        }
        covarianceMatrix = MatrixUtils.getProductOfMatrixAndScalar(covarianceMatrix, 1.0 / (dataMatrix.length - sampleIndexAndDataMatrix.size()));
        return new Object[]{meanVectors, covarianceMatrix};
    }
}
