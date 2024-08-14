package biouml.plugins.modelreduction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import one.util.streamex.DoubleStreamEx;
import one.util.streamex.IntStreamEx;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.simulation.ModelRunner;
import biouml.plugins.simulation.ModelRunnerListener;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.ModelRunner.DataGenerator;
import biouml.standard.simulation.SimulationResult;
import biouml.standard.type.Compartment;
import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import Jama.SingularValueDecomposition;

/**
 * Functions based on the Principal Component Method.
 * Other method names: Proper Orthogonal Decomposition, Karhunen-Loeve expansion.
 */
public class PrincipalComponentsMethod implements ModelRunnerListener
{
    private static final int DENSITY = 1000;

    private int threadsNumber = 1;
    public void setThreadsNumber(int threadsNumber)
    {
        if( threadsNumber > 1 )
            this.threadsNumber = threadsNumber;
    }

    /**
     * Estimates the local dimensions of e-balls B(x) = {x<sub>i</sub> : ||x<sub>i</sub> - x|| < e} around the each point x of the trajectory
     * diagram species concentrations by the principal components method.
     * 
     * @param simulationEngine an engine for the trajectory calculation.
     * @param maxRadius the factor to calculate the maximum radius of vicinities for all spicies concentrations
     * by the formula maxRadius * (c<sub>max</sub> - c<sub>min</sub>),
     * where c<sub>max</sub> and c<sub>min</sub> are the maximum and minimum values of a species concentrations in the time series.
     * @param increment the increment of the factor for calculation vicinity radiuses of the species concentrations in every time point.
     * @return array of the local dimensions of the species concentrations space for all time points.
     * @throws Exception
     */
    public int[] estimateDynamicalLinearDimension(SimulationEngine simulationEngine, double maxRadius, double increment) throws Exception
    {
        SimulationResult simulationResult = simulate(simulationEngine);

        Diagram diagram = simulationEngine.getDiagram();
        String[] varNames = getVariableNames(diagram);

        double[] times = simulationResult.getTimes();
        double[][] values = simulationResult.getValues();
        Map<String, Integer> varMap = simulationResult.getVariableMap();

        Matrix trajectory = new Matrix(simulationResult.getValues(varNames));
        double[] vicinityRadiuses = generateVicinityRadiuses(trajectory, 1.0);

        int[] dimensions = new int[times.length];

        for( int i = 0; i < times.length; ++i )
        {
            double[] point = new double[varNames.length];
            for( int j = 0; j < point.length; ++j )
            {
                int ind = varMap.get(varNames[j]);
                point[j] = values[i][ind];
            }

            double[][] singularValueCurves = generateSingularValueCurves(vicinityRadiuses, point, maxRadius, increment);
        }

        return dimensions;
    }

    public double[][] generateSingularValueCurves(Diagram diagram, SimulationResult simulationResult, double timePoint, double maxRadius,
            double increment) throws Exception
    {
        String[] varNames = getVariableNames(diagram);

        Matrix trajectory = new Matrix(simulationResult.getValues(varNames));
        double[] vicinityRadiuses = generateVicinityRadiuses(trajectory, 1.0);

        double[] times = simulationResult.getTimes();
        double[][] values = simulationResult.getValues();
        Map<String, Integer> varMap = simulationResult.getVariableMap();

        int timeIndex = 0;
        if( timePoint >= times[times.length - 1] )
        {
            timeIndex = times.length - 1;
        }
        else if( timePoint > times[0] )
        {
            timeIndex = IntStreamEx.ofIndices( times, time -> time > timePoint ).findFirst().getAsInt() - 1;
        }

        double[] point = new double[varNames.length];
        for( int i = 0; i < point.length; ++i )
        {
            int ind = varMap.get(varNames[i]);
            point[i] = values[timeIndex][ind];
        }

        return this.generateSingularValueCurves(vicinityRadiuses, point, maxRadius, increment);
    }

    /**
     * Generates a sequence of growing e-balls around the center point and calculates singular value decomposition for every e-ball.
     */
    private double[][] generateSingularValueCurves(double[] vicinityRadiuses, double[] centerPoint, double maxRadius, double increment)
            throws Exception
    {
        double[] radiuses = new double[centerPoint.length];

        int stepsNumber = (int) ( maxRadius / increment );
        double[][] singularValueCurves = new double[centerPoint.length][stepsNumber + 1];

        for( int step = 0; step < stepsNumber + 1; ++step )
        {
            for( int i = 0; i < radiuses.length; ++i )
            {
                radiuses[i] = vicinityRadiuses[i] * step * increment;
            }
            Matrix eball = generateEBall(radiuses, centerPoint, DENSITY);

            if( eball.getRowDimension() < eball.getColumnDimension() )
                eball = eball.transpose();

            SingularValueDecomposition svd = eball.svd();
            double[] singularValues = svd.getSingularValues();
            for( int i = 0; i < singularValues.length; ++i )
            {
                singularValueCurves[i][step] = singularValues[i];
            }
        }
        return singularValueCurves;
    }

    /**
     * Estimates dimension of a linear manifold containing trajectory of the diagram species concentrations with a certain accuracy
     * by the principal components method.
     * 
     * @param simulationEngine an engine for the trajectory calculation.
     * @param numberOfSnapshots the number of additional snapshots in the vicinity of each trajectory point to take into account potential noise.
     * @param accuracy the real number in the interval [0...1] representing the percent of the trajectory points which must be located in the linear manifold.
     * @return calculated dimension of the linear manifold sufficiently closed to the species concentrations trajectory.
     * @throws Exception
     */
    public int estimateLinearDimension(SimulationEngine simulationEngine, int numberOfSnapshots, double accuracy) throws Exception
    {
        Diagram diagram = simulationEngine.getDiagram();
        SimulationResult simulationResult = simulate(simulationEngine);

        varNames = getVariableNames(diagram);
        Matrix trajectory = new Matrix(simulationResult.getValues(varNames));

        Matrix basis = getReducedBasis(trajectory, accuracy);
        int dim = basis.getColumnDimension();

        if( numberOfSnapshots > 0 )
        {
            processMatrix = trajectory.times(trajectory.transpose());

            DataGenerator generator = new SnapshotsDataGenerator(diagram, simulationResult, basis, numberOfSnapshots);
            ModelRunner modelRunner = new ModelRunner(simulationEngine, generator, threadsNumber);
            modelRunner.addModelRunnerListener(this);
            modelRunner.simulate(null);

            dim = getCovarianceMatrixReducedBasis(processMatrix, accuracy).getColumnDimension();
        }
        return dim;
    }

    private @Nonnull SimulationResult simulate(SimulationEngine simulationEngine) throws Exception
    {
        SimulationResult simulationResult = new SimulationResult(null, "");
        simulationEngine.simulate(simulationEngine.createModel(), simulationResult);
        return simulationResult;
    }

    /**
     * Estimates linear embedding dimension of a data matrix and gets corresponding subspase basis
     * using singular value decomposition of the matrix.
     * 
     * @param matrix data matrix to get reduced basis of.
     * @param accuracy the real number in the interval [0...1] to get reduced dimension of the data matrix
     * as the number of squared singular values which sum is greater than their total sum multiplied by the
     * accuracy value.
     * @return matrix of eigenvectors representing basis of the linear subspace in which the matrix is located.
     */
    private Matrix getReducedBasis(Matrix matrix, double accuracy)
    {
        int m = matrix.getRowDimension();
        int n = matrix.getColumnDimension();

        Matrix processMatrix = matrix;
        if( n > m )
        {
            processMatrix = processMatrix.transpose();
        }

        SingularValueDecomposition svd = processMatrix.svd();
        double[] eigenValues = svd.getSingularValues();
        for( int i = 0; i < eigenValues.length; ++i )
        {
            eigenValues[i] = Math.pow(eigenValues[i], 2);
        }

        Matrix basis;
        if( svd.getU().getRowDimension() == m )
        {
            basis = svd.getU();
        }
        else
        {
            basis = svd.getV();
        }

        int reducedDimension = getPrincipalComponentsNumber(eigenValues, accuracy);
        Matrix reducedBasis = new Matrix(m, reducedDimension);
        for( int i = 0; i < m; ++i )
        {
            for( int j = 0; j < reducedDimension; ++j )
            {
                reducedBasis.set(i, j, basis.get(i, j));
            }
        }
        return reducedBasis;
    }

    /**
     * Estimates linear embedding dimension of a data matrix and gets corresponding subspase basis
     * using eigenvalue decomposition of the data covariance matrix.
     * 
     * @param covMatrix covariance matrix of the data to get reduced basis of.
     * @param accuracy the real number in the interval [0...1] to get reduced dimension of the data matrix
     * as the number of the covariance matrix eigenvalues which sum is greater than their total sum
     * multiplied by the accuracy value.
     * @return matrix of eigenvectors representing basis of the linear subspace in which the data matrix is located.
     */
    private Matrix getCovarianceMatrixReducedBasis(Matrix covMatrix, double accuracy)
    {
        if( covMatrix.getRowDimension() != covMatrix.getColumnDimension() )
            throw new IllegalArgumentException();

        int n = covMatrix.getRowDimension();

        EigenvalueDecomposition eig = covMatrix.eig();
        Matrix dec = eig.getD();

        double[] eigenValues = new double[n];
        for( int i = 0; i < eigenValues.length; ++i )
        {
            eigenValues[i] = dec.get(i, i);
        }

        Matrix basis = eig.getV();

        int reducedDimension = getPrincipalComponentsNumber(eigenValues, accuracy);
        Matrix reducedBasis = new Matrix(n, reducedDimension);
        for( int i = 0; i < n; ++i )
        {
            for( int j = 0; j < reducedDimension; ++j )
            {
                reducedBasis.set(i, j, basis.get(i, j));
            }
        }
        return reducedBasis;
    }

    private int getPrincipalComponentsNumber(double[] values, double accuracy)
    {
        int pcNumber = 0;
        if( values.length != 0 )
        {
            double totalSum = DoubleStreamEx.of( values ).greater( 0 ).sum();

            double sum = 0;
            while( sum < accuracy * totalSum )
            {
                sum += values[pcNumber++];
            }
        }
        return pcNumber;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private String[] getVariableNames(Diagram diagram)
    {
        List<String> varNames = new ArrayList<>();

        EModel emodel = (EModel)diagram.getRole();
        if( emodel != null )
        {
            for(String name : emodel.getVariableRoles().getNameList())
            {
                DiagramElement node = ( (VariableRole)emodel.getVariable(name) ).getDiagramElement();
                if( node != null && ! ( node.getKernel() instanceof Compartment ) )
                {
                    varNames.add(name);
                }
            }
        }
        return varNames.toArray(new String[varNames.size()]);
    }

    private String[] getParameterNames(Diagram diagram)
    {
        EModel emodel = (EModel)diagram.getRole();
        if( emodel != null )
        {
            return emodel.getVariableRoles().names().filter( e -> !e.equals( "time" ) ).toArray( String[]::new );
        }
        return new String[0];
    }

    private Matrix processMatrix;
    private String[] varNames;

    @Override
    public void resultReady(SimulationResult simulationResult, double[] variableValues, String[] variableNames)
    {
        Matrix trajectory = new Matrix(simulationResult.getValues(varNames));
        processMatrix.plusEquals(trajectory.times(trajectory.transpose()));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // Methods for generation of new points in the vicinity of species concentrations trajectory
    //

    public class SnapshotsDataGenerator extends DataGenerator
    {
        private static final int EPSILON = 1000;

        private final Matrix basis;
        private final int numberOfSnaphots; //The number of snapshots in each time point.

        private final double[] vicinityRadiuses;

        private final Matrix varValues;
        private final Matrix paramValues;

        private final String[] names;

        double[] times;

        public SnapshotsDataGenerator(Diagram diagram, SimulationResult simulationResult, Matrix basis, int numberOfSnapshots)
        {
            this.basis = basis;
            this.numberOfSnaphots = numberOfSnapshots;

            String[] paramNames = getParameterNames(diagram);

            varValues = new Matrix(simulationResult.getValues(varNames));
            paramValues = new Matrix(simulationResult.getValues(paramNames));

            names = new String[varNames.length + paramNames.length];
            for( int i = 0; i < varNames.length; ++i )
            {
                names[i] = varNames[i];
            }
            for( int i = 0; i < paramNames.length; ++i )
            {
                names[varNames.length + i] = paramNames[i];
            }

            vicinityRadiuses = generateVicinityRadiuses(varValues, EPSILON);
            times = simulationResult.getTimes();
        }

        private double[] varPoint;
        private double[] paramPoint;

        private int nextIndivid = 0;

        @Override
        public synchronized double[] getValues()
        {
            if( nextIndivid >= ( times.length - 2 ) * numberOfSnaphots )
                return null;

            if( nextIndivid % numberOfSnaphots == 0 )
            {
                int timePoint = nextIndivid / numberOfSnaphots;

                varPoint = new double[varValues.getRowDimension()];
                for( int i = 0; i < varValues.getRowDimension(); ++i )
                {
                    varPoint[i] = varValues.get(i, timePoint);
                }

                paramPoint = new double[paramValues.getRowDimension()];
                for( int i = 0; i < paramValues.getRowDimension(); ++i )
                {
                    paramPoint[i] = paramValues.get(i, timePoint);
                }

                initialTime = times[timePoint];
            }

            double[] gPoint = generatePoint(varPoint, vicinityRadiuses, basis);

            double[] values = new double[varValues.getRowDimension() + paramValues.getRowDimension()];
            for( int i = 0; i < varValues.getRowDimension(); ++i )
            {
                values[i] = gPoint[i];
            }
            for( int i = 0; i < paramValues.getRowDimension(); ++i )
            {
                values[varValues.getRowDimension() + i] = paramPoint[i];
            }

            nextIndivid++;
            return values;
        }

        private double initialTime;

        @Override
        public double getInitialTime()
        {
            return initialTime;
        }

        @Override
        public double getCompletionTime()
        {
            return times[times.length - 1];
        }

        @Override
        public String[] getNames()
        {
            return names;
        }
    }

    private double[] generateVicinityRadiuses(Matrix matrix, double epsilon)
    {
        double[] radiuses = new double[matrix.getRowDimension()];
        for( int i = 0; i < matrix.getRowDimension(); ++i )
        {
            double minValue = Double.MAX_VALUE;
            double maxValue = Double.MIN_VALUE;
            for( int j = 0; j < matrix.getColumnDimension(); ++j )
            {
                if( matrix.get(i, j) < minValue )
                    minValue = matrix.get(i, j);

                if( matrix.get(i, j) > maxValue )
                    maxValue = matrix.get(i, j);
            }
            radiuses[i] = epsilon * ( maxValue - minValue );
        }
        return radiuses;
    }

    /**
     * Generates a point in the vicinity of the certain point and projecting it onto the linear subspace defined by the specified basis.
     * The projection is calculated as the sum of the projections for each of the basis vectors.
     * 
     */
    private double[] generatePoint(double[] point, double[] vicinityRadiuses, Matrix basis)
    {
        double[] newPoint = DoubleStreamEx.zip( point, vicinityRadiuses, (p, v) -> ( p + v * Math.random() ) ).toArray();

        double[] projection = new double[point.length];
        for( int j = 0; j < basis.getColumnDimension(); ++j )
        {
            double norm = 0;
            double scalarProduct = 0;

            double[] vector = new double[point.length];
            for( int i = 0; i < point.length; ++i )
            {
                vector[i] = basis.get(i, j);
                norm += vector[i] * vector[i];
                scalarProduct += newPoint[i] * vector[i];
            }

            norm = Math.sqrt(norm);
            double factor = scalarProduct / norm;

            for( int i = 0; i < point.length; ++i )
            {
                projection[i] += factor * vector[i];
            }
        }
        return projection;
    }

    /**
     * @param vicinityRadiuses array of vicinity radiuses for all coordinates of the center point.
     * @param point the center point of e-ball
     * @param density the number of points which will be sampled in the vicinity of the center point.
     * @return the set of points randomly sampled in the vicinity of the center point and represented as columns of a matrix.
     */
    private Matrix generateEBall(double[] vicinityRadiuses, double[] point, int density)
    {
        Matrix eball = new Matrix(vicinityRadiuses.length, density);
        for( int i = 0; i < vicinityRadiuses.length; ++i )
        {
            for( int j = 0; j < density; ++j )
            {
                //generate random value from the interval [-1..1] and multiply it by vicinityRadiuses[i]
                double value = point[i] + ( 2 * Math.random() - 1 ) * vicinityRadiuses[i];
                eball.set(i, j, value);
            }
        }
        return eball;
    }
}
