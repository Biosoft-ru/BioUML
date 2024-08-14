/* $Id$ */
// 05.03.22
package biouml.plugins.machinelearning.distribution_mixture;

import java.util.logging.Logger;

import org.apache.commons.lang.ArrayUtils;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.graphics.chart.Chart;
import biouml.plugins.machinelearning.utils.DataMatrix;
import biouml.plugins.machinelearning.utils.DataMatrixInteger;
import biouml.plugins.machinelearning.utils.MatrixUtils;
import biouml.plugins.machinelearning.utils.PrimitiveOperations;
import biouml.plugins.machinelearning.utils.StatUtils.UnivariateSample.DensityEstimation;
import biouml.plugins.machinelearning.utils.TableAndFileUtils;
import biouml.plugins.machinelearning.utils.UtilsGeneral.UtilsForArray;
import biouml.plugins.machinelearning.utils.VectorUtils.VectorOperations;

/**
 * @author yura
 *
 */
public abstract class DistributionMixture
{
    public static final String MIXTURE_1_NORMAL = "Normal mixture";

    protected String mixtureType;
    protected double[] sample;
    protected int numberOfComponents, maximalNumberOfIterations, numberOfIterations = 1;
    protected Object[] specificParametersOfComponents; // Initially dim(specificParametersOfComponents) = numberOfComponents;
    protected double[] probabilitiesOfComponents; // Initially dim(probabilitiesOfComponents) = numberOfComponents;
    protected double[] oldProbabilitiesOfComponents;
    protected int[] membershipIndices;	// membershipIndices[i] = index of maximum{probabilitiesPij[i][0],...,probabilitiesPij[i][numberOfComponents - 1]
    protected double[][] probabilitiesPij; // probabilitiesPij[i][j] = P(sample[i] (- j-th mixture component);
    
    public DistributionMixture(String mixtureType, double[] sample, int numberOfComponents, double[] initialProbabilitiesOfComponents, Object[] initialApproximationOfParameters, int maximalNumberOfIterations)
    {
        this.mixtureType = mixtureType;
        this.sample = sample;
        this.numberOfComponents = numberOfComponents;
        this.specificParametersOfComponents = initialApproximationOfParameters != null ? initialApproximationOfParameters : calculateInitialParameters();
        this.probabilitiesOfComponents = initialProbabilitiesOfComponents != null ? initialProbabilitiesOfComponents : calculateInitialProbabilitiesOfComponents();
        this.maximalNumberOfIterations = maximalNumberOfIterations;
        estimateMixtureByEmAlgorithm();
    }
    
    protected double[] calculateInitialProbabilitiesOfComponents()
    {
        return UtilsForArray.getConstantArray(numberOfComponents, 1.0 / (double)numberOfComponents);
    }
    
    protected Object[] calculateInitialParameters()
    {
        return null;
    }
    
    private void estimateMixtureByEmAlgorithm()
    {
    	calculateProbabilitiesPij();
    	oldProbabilitiesOfComponents = probabilitiesOfComponents.clone();
    	for( ; numberOfIterations <= maximalNumberOfIterations; numberOfIterations++ )
    	{

// TODO: temp
//log.info("\n ***********************************************************");
//log.info("****** NormalMixture: Iteration number = " + numberOfIterations);
           	

           	if( numberOfIterations > 1 )
                   calculateProbabilitiesPij(); // Expectation Step in EM-algorithm.
            
// TODO: temp
//log.info("****** NormalMixture: probabilitiesPij : dim = " + probabilitiesPij.length + " x " + probabilitiesPij[0].length);
//MatrixUtils.printMatrix(probabilitiesPij);

               
               calculateParametersOfMixture();  // Maximization Step in EM-algorithm.
               

//log.info("****** NormalMixture: probabilitiesOfComponents");
//for( int j = 0; j < probabilitiesOfComponents.length; j++ )
//log.info("****** NormalMixture: probabilitiesOfComponents[" + j + "] = " + probabilitiesOfComponents[j]);

            	   

               
               
               
               if( doStopIterations() ) break;
               int[] degenerateComponentIndices = determineDegenerateComponents();
               

               
//log.info("****** NormalMixture: degenerateComponentIndices");
//if( degenerateComponentIndices == null ) log.info("****** NormalMixture: degenerateComponentIndices == null");
//else
//	for( int j = 0; j < degenerateComponentIndices.length; j++ )
//		log.info("****** NormalMixture: degenerateComponentIndices[" + j + "] = " + degenerateComponentIndices[j]);


               
               if( degenerateComponentIndices != null )
                   changeArrays(degenerateComponentIndices);
               oldProbabilitiesOfComponents = probabilitiesOfComponents.clone();
               if( probabilitiesOfComponents.length < 2 ) break;
    	}
    	numberOfIterations = Math.min(numberOfIterations, maximalNumberOfIterations);
    }
    
    // The input array degenerateComponentIndices must be sorted!!!
    private void changeArrays(int[] degenerateComponentIndices)
    {
        for( int i = degenerateComponentIndices.length - 1; i >= 0; i-- )
        {
            probabilitiesOfComponents = ArrayUtils.remove(probabilitiesOfComponents, degenerateComponentIndices[i]);
            specificParametersOfComponents = ArrayUtils.remove(specificParametersOfComponents, degenerateComponentIndices[i]);
            MatrixUtils.removeColumn(probabilitiesPij, degenerateComponentIndices[i]);
        }
    }
    
    // It must return null or sorted indices of degenerate components.
    protected int[] determineDegenerateComponents()
    {
        return null;
    }
    
    private boolean doStopIterations()
    {
        return UtilsForArray.equal(probabilitiesOfComponents, oldProbabilitiesOfComponents);
    }
    
    /*** Maximization Step in EM-algorithm. ***/
    private void calculateParametersOfMixture()
    {
        oldProbabilitiesOfComponents = probabilitiesOfComponents.clone();
        for( int j = 0; j < probabilitiesPij[0].length; j++ )
        {
            double x = 0.0;
            for( int i = 0; i < probabilitiesPij.length; i++ )
                x += probabilitiesPij[i][j];
            probabilitiesOfComponents[j] = x / (double)probabilitiesPij.length;
        }
        calculateSpecificParametersOfComponents();
    }
    
    protected void calculateSpecificParametersOfComponents()
    {}
    
    /*** Expectation Step in EM-algorithm. ***/
    private void calculateProbabilitiesPij()
    {
        int n = sample.length, k = probabilitiesOfComponents.length;
        probabilitiesPij = new double[n][k];
        for( int i = 0; i < n; i++ )
            for( int j = 0; j < k; j++ )
                probabilitiesPij[i][j] = probabilitiesOfComponents[j] * getDensity(sample[i], specificParametersOfComponents[j]);
        for( int i = 0; i < n; i++ )
            probabilitiesPij[i] = VectorOperations.getProductOfVectorAndScalar(probabilitiesPij[i], 1.0 / PrimitiveOperations.getSum(probabilitiesPij[i]));
    }
    
    protected double getDensity(double x, Object specificParametersOfComponent)
    {
        return Double.NaN;
    }
    
    protected String[] getNamesOfSpecificParameters()
    {
        return null;
    }

    // TODO: To generalize: now specificParametersOfComponents[i] = double[]; in general it was not the case/
    public DataMatrix getParametersOfComponents()
    {
        String[] columnNames = getNamesOfSpecificParameters(), rowNames = new String[probabilitiesOfComponents.length];
        columnNames = (String[])ArrayUtils.addAll(new String[]{"Probability of component"}, columnNames);
        double[][] matrix = new double[probabilitiesOfComponents.length][];
        for( int i = 0; i < probabilitiesOfComponents.length; i++ )
        {
            rowNames[i] = "Mixture component " + Integer.toString(i + 1);
            matrix[i] = ArrayUtils.addAll(new double[]{probabilitiesOfComponents[i]}, (double[])specificParametersOfComponents[i]);
        }
        return new DataMatrix(rowNames, columnNames, matrix);
    }
    
    public Chart createChartWithDensities(String nameOfInitialSample)
    {
        int n = probabilitiesOfComponents.length == 1 ? 1 : 1 + probabilitiesOfComponents.length;
        String[] sampleNames = new String[n];
        sampleNames[0] = "Whole sample";
        double[] multipliers = probabilitiesOfComponents.length == 1 ? new double[]{1.0} : ArrayUtils.addAll(new double[]{1.0}, probabilitiesOfComponents); 
        double[][] xValuesForCurves = new double[n][], yValuesForCurves = new double[n][];
        double window = DensityEstimation.getWindow(sample, DensityEstimation.WINDOW_WIDTH_01, null);
        DensityEstimation de = new DensityEstimation(sample, window, true);
        double[][] curve = de.getCurve();
        xValuesForCurves[0] = curve[0];
        yValuesForCurves[0] = curve[1];
        double[] minAndMax = PrimitiveOperations.getMinAndMax(sample);
        double[] xValues = getXvalues(minAndMax[0], minAndMax[1], 1000);
        if( probabilitiesOfComponents.length > 1 )
            for( int i = 1; i <= probabilitiesOfComponents.length; i++ )
            {
                sampleNames[i] = "Mixture component " + Integer.toString(i);
                xValuesForCurves[i] = xValues.clone();
                yValuesForCurves[i] = getDensityValues(xValuesForCurves[i], specificParametersOfComponents[i - 1]);
            }
        return DensityEstimation.createChartWithSmoothedDensities(xValuesForCurves, yValuesForCurves, sampleNames, nameOfInitialSample,  multipliers);
    }
    
	// membershipIndices[i] = index of maximum{probabilitiesPij[i][0],...,probabilitiesPij[i][numberOfComponents - 1]
    public DataMatrixInteger getMembershipIndices()
    {
    	String[] rowNames = new String[sample.length];
    	membershipIndices = new int[sample.length];
        for( int i = 0; i < sample.length; i++ )
        {
        	Object[] objects = PrimitiveOperations.getMax(probabilitiesPij[i]);
        	membershipIndices[i] = (int) objects[0];
        	rowNames[i] = Integer.toString(i);
        }
    	return new DataMatrixInteger(rowNames, "Membership_indices", membershipIndices);
    }
    
    public void getResults(String nameOfInitialSample, DataElementPath pathToOutputFolder)
    {
    	DataMatrix dm = getParametersOfComponents();
    	dm.writeDataMatrix(false, pathToOutputFolder, "mixture_parameters", log);
    	Chart chart = createChartWithDensities(nameOfInitialSample);
    	TableAndFileUtils.addChartToTable("chart with " + nameOfInitialSample, chart, pathToOutputFolder.getChildPath("_chart_mixture"));
    	DataMatrixInteger dmi = getMembershipIndices();
    	dmi.writeDataMatrix(pathToOutputFolder, "Membership_indices");
    }
    
    private double[] getDensityValues(double[] xValues, Object specificParametersOfComponent)
    {
        double[] result = new double[xValues.length];
        for( int i = 0; i < xValues.length; i++ )
            result[i] = getDensity(xValues[i], specificParametersOfComponent);
        return result;
    }
    
    // TODO: To move to appropriate Class
    private static double[] getXvalues(double min, double max, int pointsNumber)
    {
        double[] result = new double[pointsNumber];
        double h = (max - min) / (double)(pointsNumber - 1);
        for( int i = 0; i < pointsNumber; i++ )
            result[i] = min + h * (double)i;
        return result;
    }
    
    protected static Logger log = Logger.getLogger(DistributionMixture.class.getName());

}