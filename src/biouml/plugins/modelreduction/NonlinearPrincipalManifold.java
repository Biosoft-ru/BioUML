package biouml.plugins.modelreduction;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import Jama.Matrix;

/**
 * The method was based on the article of
 * A Gorban and A Zinovyev (2005),
 * "Elastic Principal Graphs and Manifolds and their Practical Applications",
 * In Proceedings of Computing, 359-379.
 */
public class NonlinearPrincipalManifold
{
    private static final double ENERGY_TOLERANCE = 0.1;

    private static final double EDGES_INITIAL_ELASTICITY = 1; // A notation in the article is 'lambda_0'
    private static final double RIBS_INITIAL_ELASTICITY = 1; // A notation in the article is 'mu_0'

    /**
     * Dimension of the grid.
     */
    private double dim; // A notation in the article is 'd'

    /**
     * A list of the grid nodes.
     */
    private List<double[]> nodes;

    /**
     * <p>A list of the grid edges.
     * <p>Every edge E has a beginning node E[0] and an ending node E[1].
     * The definition edges = {{a,b}...} means that nodes.get(a)
     * and nodes.get(b) connected by the edge.
     */
    private List<int[]> edges;

    /**
     * <p>A list of the elementary ribs which are the pairs of incident edges.
     * <p>Every rib R has a beginning node R[1], an ending node R[2] and a central node R[0].
     * The definition ribs = {{b,a,c}...} means that nodes.get(a) and nodes.get(b) as well as
     * nodes.get(b) and nodes.get(c) connected by the edges.
     */
    private List<int[]> ribs;

    /**
     * Constructs the grid approximation of the principal manifold.
     * 
     * @param dimension a dimension of the grid.
     * @param dataPoints a list of data points (the principal manifold) which will be approximated by grid.
     * @param dataPointWeights an array of the data point weights.
     * @param strategy a strategy of the grid construction {@link Strategy}.
     */
    public void constructGridApproximation(int dimension, List<double[]> dataPoints, double[] dataPointWeights,
            StrategyParameters parameters)
    {
        this.dim = dimension;

        initGrid(dataPoints);
        processStrategy(dataPoints, dataPointWeights, parameters);
    }

    private void initGrid(List<double[]> dataPoints)
    {
        nodes = new ArrayList<>();

        int pointsLength = dataPoints.get(0).length;

        double[] minPoint = new double[pointsLength];
        double[] sizes = new double[pointsLength];
        initHypercube(dataPoints, minPoint, sizes);

        int[] numbers = generateNodeNumbers(sizes, dataPoints);

        GridNodesGenerator it = new GridNodesGenerator(minPoint, sizes, numbers);
        while( it.hasNext() )
        {
            nodes.add(it.next());
        }

        // Binary symmetric matrix containing information about nodes connection.
        // topology[i][j] = 1, if nodes.get(i) and nodes.get(j) connected by the edge
        // topology[i][j] = 0, otherwise.
        int[][] topology = getTopology(nodes, it.getIncrements());

        edges = initEdges(topology);
        ribs = initRibs(topology);
        initElasticities(edges, ribs);
    }

    /**
     * Inits a hypercube containing all data points.
     */
    private void initHypercube(List<double[]> dataPoints, double[] minPoint, double[] sizes)
    {
        for( int i = 0; i < dataPoints.get(0).length; i++ )
        {
            double max = Double.MIN_VALUE;
            double min = Double.MAX_VALUE;

            for( int j = 0; j < dataPoints.size(); j++ )
            {
                if( min > dataPoints.get(j)[i] )
                {
                    min = dataPoints.get(j)[i];
                }

                if( max < dataPoints.get(j)[i] )
                {
                    max = dataPoints.get(j)[i];
                }
            }

            minPoint[i] = min;
            sizes[i] = max - min;
        }
    }

    /**
     * <p>Generates an array of the node numbers for all directions of the grid.
     * <p>Since the grid should have a specified dimension dim
     * it will be constructed for directions along which the hypercube
     * including all data points has the greatest sizes of the sides.
     */
    private int[] generateNodeNumbers(double[] sizes, List<double[]> dataPoints)
    {
        int[] numbers = new int[sizes.length];

        List<Double> maxSizes = getMaxSizes(sizes, dataPoints.get(0).length);

        for( int i = 0; i < sizes.length; ++i )
        {
            if( maxSizes.contains(sizes[i]) )
            {
                int value = (int)Math.pow(dataPoints.size(), ( 1 / dim ));
                if( value >= 2 )
                {
                    numbers[i] = value;
                }
                else
                {
                    numbers[i] = 2;
                }
            }
        }
        return numbers;
    }

    private List<Double> getMaxSizes(double[] sizes, int pointsLength)
    {
        double[] sizesClone = sizes.clone();

        List<Double> maxSizes = new ArrayList<>();
        for( int i = 0; i < dim; i++ )
        {
            double maxSize = 0.0;
            int index = 0;

            for( int j = 0; j < pointsLength; j++ )
            {
                if( maxSize < sizesClone[j] )
                {
                    maxSize = sizesClone[j];
                    index = j;
                }
            }

            maxSizes.add(sizesClone[index]);
            sizesClone[index] = 0.0;
        }
        return maxSizes;
    }

    private int[][] getTopology(List<double[]> nodes, double[] increments)
    {
        int[][] topology = new int[nodes.size()][nodes.size()];

        for( int i = 0; i < nodes.size(); ++i )
        {
            double[] node1 = nodes.get(i);

            for( int j = i; j < nodes.size(); ++j )
            {
                double[] node2 = nodes.get(j);

                int ind = 0;
                for( int k = 0; k < node1.length; ++k )
                {
                    if( Math.abs(node1[k] - node2[k]) == increments[k] )
                        ind++;
                    if( ind > 1 )
                        break;
                }

                if( ind == 1 )
                {
                    topology[i][j] = 1;
                    topology[j][i] = 1;
                }
            }
        }
        return topology;
    }

    private List<int[]> initEdges(int[][] topology)
    {
        List<int[]> edges = new ArrayList<>();
        for( int i = 0; i < topology.length; ++i )
        {
            for( int j = 0; j < topology.length; ++j )
            {
                if( topology[i][j] == 1 )
                {
                    edges.add(new int[] {i, j});
                }
            }
        }
        return edges;
    }

    private List<int[]> initRibs(int[][] topology)
    {
        List<int[]> ribs = new ArrayList<>();

        for( int i = 0; i < topology.length; ++i ) // middle node
        {
            List<Integer> connectedNodes = new ArrayList<>();
            for( int j = 0; j < topology.length; ++j )
            {
                if( topology[i][j] == 1 )
                {
                    connectedNodes.add(j);
                }
            }

            List<Integer> ribIndexes = new ArrayList<>();

            for( int j = 0; j < connectedNodes.size(); ++j ) // beginning node
            {
                for( int k = j; k < connectedNodes.size(); ++k ) // ending node
                {
                    ribIndexes.add(ribs.size());
                    ribs.add(new int[] {j, i, k});
                }
            }
        }

        return ribs;
    }

    private List<Double> edgesElasticities; // A notation in the article is 'lambda'
    private List<Double> ribsElasticities; // A notation in the article is 'mu'

    private void initElasticities(List<int[]> edges, List<int[]> ribs)
    {
        edgesElasticities = new ArrayList<>();
        for( int i = 0; i < edges.size(); ++i )
        {
            edgesElasticities.add(EDGES_INITIAL_ELASTICITY * Math.pow(edges.size(), ( 2 - dim ) / dim));
        }

        ribsElasticities = new ArrayList<>();
        for( int i = 0; i < ribs.size(); ++i )
        {
            ribsElasticities.add(RIBS_INITIAL_ELASTICITY * Math.pow(ribs.size(), ( 2 - dim ) / dim));
        }
    }

    private List<Integer>[] optimizeGrigEnergy(List<double[]> dataPoints, double[] dataPointWeights)
    {
        List<Integer>[] pointSeparation;

        double energy = 0;
        double prevEnergy = 0;

        do
        {
            prevEnergy = energy;

            // Given the nodes placement, separate the collection of data points into subcollections.
            pointSeparation = getPointSeparation(dataPoints);

            // Given the separation, find the grid energy.
            energy = getEnergy(dataPoints, dataPointWeights, pointSeparation);

            // Calculate new position of the nodes
            calculateNewPosition(dataPoints, dataPointWeights, pointSeparation);
        }
        while( Math.abs(energy - prevEnergy) > ENERGY_TOLERANCE );

        return pointSeparation;
    }

    /**
     * For each grid point y_i finds the list of data points K_i according to the rule:
     * K_i = {x_j : ||x_j - y_i|| <= ||x_j - y_m||}, for all m = 1,...,p},
     * where p = is the size of the grid points list.
     */
    private List<Integer>[] getPointSeparation(List<double[]> dataPoints)
    {
        List<Integer>[] pointSeparation = new List[nodes.size()];
        int nodesLength = nodes.get(0).length;

        for( int j = 0; j < dataPoints.size(); j++ )
        {
            int closestGridNodeIndex = 0;

            double minDistance = Double.MAX_VALUE;
            
            for( int i = 0; i < nodes.size(); i++ )
            {
                double sum = 0;
                for( int k = 0; k < nodesLength; k++ )
                {
                    double dist = dataPoints.get(j)[k] - nodes.get(i)[k];
                    sum += Math.pow(dist, 2);
                }

                if( minDistance > Math.sqrt(sum) )
                {
                    minDistance = Math.sqrt(sum);
                    closestGridNodeIndex = i;
                }
            }

            pointSeparation[closestGridNodeIndex].add(j);
        }

        return pointSeparation;
    }

    private double getEnergy(List<double[]> dataPoints, double[] weights, List<Integer>[] pointSeparation)
    {
        double energyY = calculateNodeEnergy(nodes, dataPoints, weights, pointSeparation);
        double energyE = calculateEdgeEnergy(nodes, edges);
        double energyR = calculateRibEnergy(nodes, ribs);

        return energyY + energyE + energyR;
    }

    private double calculateNodeEnergy(List<double[]> nodes, List<double[]> dataPoints, double[] weights, List<Integer>[] pointSeparation)
    {
        double energy = 0;
        for( int i = 0; i < nodes.size(); i++ )
        {
            for( int j : pointSeparation[i] )
            {
                double sum = 0;
                for( int k = 0; k < dataPoints.get(0).length; k++ )
                {
                    double dist = dataPoints.get(j)[k] - nodes.get(i)[k];
                    sum += Math.pow(dist, 2);
                }
                sum *= weights[j];
                energy += sum;
            }
        }

        double sumWeight = 0;
        for( int i = 0; i < dataPoints.size(); i++ )
        {
            sumWeight += weights[i];
        }

        return energy / sumWeight;
    }

    private double calculateEdgeEnergy(List<double[]> nodes, List<int[]> edges)
    {
        double energy = 0;
        for( int i = 0; i < edges.size(); i++ )
        {
            double sum = 0;
            for( int j = 0; j < nodes.get(0).length; j++ )
            {
                int[] edge = edges.get(i);
                double dist = nodes.get(edge[1])[j] - nodes.get(edge[0])[j];
                sum += edgesElasticities.get(i) * Math.pow(dist, 2);
            }
            energy += sum;
        }
        return energy;
    }

    private double calculateRibEnergy(List<double[]> nodes, List<int[]> ribs)
    {
        double energy = 0;
        for( int i = 0; i < ribs.size(); i++ )
        {
            double sum = 0;
            for( int j = 0; j < nodes.get(0).length; j++ )
            {
                int[] rib = ribs.get(i);
                double value = nodes.get(rib[1])[j] + nodes.get(rib[2])[j] - 2 * nodes.get(rib[0])[j];
                sum += ribsElasticities.get(i) * Math.pow(value, 2);
            }
            energy += sum;
        }
        return energy;
    }

    /**
     * Calculates new position of the grid nodes.
     */
    private void calculateNewPosition(List<double[]> dataPoints, double[] weights, List<Integer>[] pointSeparation)
    {
        int gridSize = nodes.size();

        double[][] e = calculateEdgesMatrix(gridSize, edges);
        double[][] r = calculateRibsMatrix(gridSize, ribs);

        double sumWeight = 0;
        for( int i = 0; i < dataPoints.size(); i++ )
        {
            sumWeight += weights[i];
        }

        int nodesLength = nodes.get(0).length;

        Matrix a = new Matrix(gridSize, gridSize);
        Matrix f = new Matrix(gridSize, nodesLength);

        for( int j = 0; j < gridSize; j++ )
        {
            double n_j = 0;
            for( int i : pointSeparation[j] )
            {
                n_j += weights[i];
            }

            for( int k = 0; k < gridSize; k++ )
            {
                double value = e[j][k] + r[j][k];
                if( j == k )
                {
                    value += n_j / sumWeight;
                }
                a.set(j, k, value);
            }

            for( int k = 0; k < nodesLength; ++k )
            {
                double sum = 0;
                for( int i : pointSeparation[j] )
                {
                    sum += weights[i] * dataPoints.get(i)[k];
                }

                f.set(j, k, sum / sumWeight);
            }
        }

        Matrix solution = a.solve(f);

        for( int j = 0; j < gridSize; j++ )
        {
            for( int k = 0; k < nodesLength; ++k )
            {
                nodes.get(j)[k] = solution.get(j, k);
            }
        }
    }

    private double[][] calculateEdgesMatrix(int gridSize, List<int[]> edges)
    {
        double[][] e = new double[gridSize][gridSize];
        for( int i = 0; i < edges.size(); i++ )
        {
            int in = edges.get(i)[0];
            int out = edges.get(i)[1];

            double lambda = edgesElasticities.get(i);

            e[in][in] += lambda;
            e[in][out] -= lambda;
            e[out][in] -= lambda;
            e[out][out] += lambda;
        }
        return e;
    }

    private double[][] calculateRibsMatrix(int gridSize, List<int[]> ribs)
    {
        double[][] r = new double[gridSize][gridSize];
        for( int i = 0; i < ribs.size(); i++ )
        {
            int in = ribs.get(i)[1];
            int out = ribs.get(i)[2];
            int middle = ribs.get(i)[0];

            double mu = ribsElasticities.get(i);

            r[in][in] += mu;
            r[in][out] += mu;
            r[in][middle] -= 2 * mu;

            r[middle][in] -= 2 * mu;
            r[middle][out] -= 2 * mu;
            r[middle][middle] += 4 * mu;

            r[out][in] += mu;
            r[out][out] += mu;
            r[out][middle] -= 2 * mu;
        }
        return r;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Grid nodes generator
    //

    private static class GridNodesGenerator implements Iterator<double[]>
    {
        private final int[] currNumbers;

        private final double[] initialNode;
        private final double[] increments;
        private final int[] numbers;

        /**
         * @param initialNode the node relative to which the grid will be constructed.
         * @param sizes an array of sizes of the grid sides.
         * @param numbers an array of node numbers for all directions of the grid.
         */
        public GridNodesGenerator(double[] initialNode, double[] sizes, int[] numbers)
        {
            this.initialNode = initialNode;
            this.numbers = numbers;

            currNumbers = new int[initialNode.length];
            increments = new double[initialNode.length];

            for( int i = 0; i < increments.length; ++i )
            {
                if( numbers[i] != 0 )
                    increments[i] = sizes[i] / numbers[i];
            }
        }

        public double[] getIncrements()
        {
            return this.increments;
        }

        @Override
        public boolean hasNext()
        {
            return currNumbers[0] < numbers[0];
        }

        @Override
        public double[] next()
        {
            if(!hasNext())
                throw new NoSuchElementException();
            double[] next = new double[initialNode.length];

            for( int i = 0; i < initialNode.length; i++ )
            {
                next[i] = initialNode[i] + currNumbers[i] * increments[i];
            }
            inc(currNumbers);
            return next;
        }

        private void inc(int[] numbers)
        {
            for( int i = 0; i < numbers.length; ++i )
            {
                if( numbers[i] < numbers[i] - 1 )
                {
                    numbers[i]++;
                    return;
                }
                else
                {
                    if( i == numbers.length - 1 )
                    {
                        numbers[0] = numbers[0];
                        return;
                    }
                    numbers[i] = 0;
                }
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Strategy processing
    //

    /**
     * <h3>Adaptive strategies.</h3>
     * 
     * <p>'GROW_TYPE' strategy is applicable mainly to grids with planar topology (linear, rectangular, cubicgrids).
     * It consists of an iterative determining of those grid parts, which have the largest "load"
     * and doubling the number of nodes in this part of the grid.
     * 
     * <p>'BREAK_TYPE' strategy changes individual rib weights in order to adapt the grid to those regions
     * of data space where the "curvature" of data distribution has a break or is very different from the average.
     * 
     * <p>'PRINCIPAL_GRAPH' strategy allows performing clustering of curvilinear data features along principal curves.
     */
    public static enum Strategy
    {
        NONE, GROW_TYPE, BREAK_TYPE, PRINCIPAL_GRAPH;
    }

    private void processStrategy(List<double[]> dataPoints, double[] dataPointWeights, StrategyParameters parameters)
    {
        switch( parameters.getStrategy() )
        {
            case NONE:
            {
                break;
            }

            case GROW_TYPE:
            {
                processGrowTypeStrategy(dataPoints, dataPointWeights, parameters);
                break;
            }

            case BREAK_TYPE:
            {
                break;
            }

            case PRINCIPAL_GRAPH:
            {
                break;
            }
        }
    }

    private void processGrowTypeStrategy(List<double[]> dataPoints, double[] dataPointWeights, StrategyParameters parameters)
    {
        int maxNodesNumber = ( (GrowTypeStrategyParameters)parameters ).getMaximumNodesNumber();
        for( int i = 0; i < maxNodesNumber; i++ )
        {
            List<Integer>[] pointSeparation = optimizeGrigEnergy(dataPoints, dataPointWeights);
            int maxLoadedEdgeIndex = getMaxLoadedEdge(pointSeparation);
            addNewNode(dataPoints, maxLoadedEdgeIndex, parameters);
        }
    }

    /**
     * Insert a new node in the middle of the specified edge.
     */
    private void addNewNode(List<double[]> dataPoints, int edgeIndex, StrategyParameters parameters)
    {
        int pointsLength = dataPoints.get(0).length;

        int[] edge = edges.get(edgeIndex);

        int in = edge[0];
        int out = edge[1];

        double[] newNode = new double[pointsLength];
        for( int i = 0; i < pointsLength; i++ )
        {
            newNode[i] = ( nodes.get(in)[i] + nodes.get(out)[i] ) / 2;
        }

        int newNodeIndex = nodes.size();

        refreshRibs(edge, newNodeIndex, parameters);
        refreshEdges(edge, newNodeIndex, parameters);

        nodes.add(newNodeIndex, newNode);
    }

    /**
     * For the specified edge = [A, B] with the new center node M removes all ribs
     * [A, B, C], [A, C, B], [B, A, C] and [B, C, A] for any node C
     * connected with A or B. Instead puts ribs [A, M, C], [A, C, M], [B, M, C] and [B, C, M] respectively.
     */
    private void refreshRibs(int[] edge, int newNodeIndex, StrategyParameters parameters)
    {
        List<Integer> toRemove = new ArrayList<>();
        for( int i = 0; i < ribs.size(); ++i )
        {
            int[] rib = ribs.get(i);
            if( ( ( rib[0] == edge[0] ) && rib[1] == edge[1] || rib[2] == edge[1] )
                    || ( ( rib[0] == edge[1] ) && rib[1] == edge[0] || rib[2] == edge[0] ) )
            {
                toRemove.add(i);
            }
        }

        for( int i = toRemove.size() - 1; 0 <= i; --i )
        {
            int ind = toRemove.get(i);

            int[] rib = ribs.get(ind);
            double oldElasticity = ribsElasticities.get(ind);

            ribs.remove(ind);
            ribsElasticities.remove(ind);

            if( rib[1] == edge[1] || rib[1] == edge[0] )
            {
                ribs.add(new int[] {rib[0], newNodeIndex, rib[2]});
            }
            else if( rib[2] == edge[1] || rib[2] == edge[0] )
            {
                ribs.add(new int[] {rib[0], rib[1], newNodeIndex});
            }

            addElasticity(ribsElasticities, oldElasticity, parameters);
        }

        ribs.add(new int[] {edge[0], newNodeIndex, edge[1]});
    }

    /**
     * Deletes the specified edge and puts new edges connecting new node with
     * the beginning and the ending nodes of the deleted edge.
     */
    private void refreshEdges(int[] edge, int newNodeIndex, StrategyParameters parameters)
    {
        int ind = edges.indexOf(edge);
        if( ind > -1 )
        {
            double oldElasticity = edgesElasticities.get(ind);

            edges.remove(ind);
            edgesElasticities.remove(ind);

            edges.add(new int[] {edge[0], newNodeIndex});
            edges.add(new int[] {newNodeIndex, edge[1]});

            addElasticity(edgesElasticities, oldElasticity, parameters);
            addElasticity(edgesElasticities, oldElasticity, parameters);
        }
    }

    private void addElasticity(List<Double> elasticities, double oldElasticity, StrategyParameters parameters)
    {
        switch( parameters.getStrategy() )
        {
            case GROW_TYPE:
            {
                elasticities.add(oldElasticity);
                break;
            }

            case PRINCIPAL_GRAPH:
            {
                elasticities.add(2 * oldElasticity);
                break;
            }

            case BREAK_TYPE:
            {
                //to do: programm formulas
                break;
            }
        }
    }

    /**
     * Determines the edge which has the largest load by summing the number of data points
     * (or the sum of their weights) projected to both ends of every edge.
     */
    private int getMaxLoadedEdge(List<Integer>[] pointSeparation)
    {
        double largestLoad = 0;
        int edgeIndex = 0;

        for( int j = 0; j < edges.size(); j++ )
        {
            int in = edges.get(j)[0];
            int out = edges.get(j)[1];

            double load = pointSeparation[in].size() + pointSeparation[out].size();

            if( largestLoad < load )
            {
                largestLoad = load;
                edgeIndex = j;
            }
        }

        return edgeIndex;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Strategy parameters
    //

    public static class StrategyParameters
    {
        public Strategy getStrategy()
        {
            return Strategy.NONE;
        }
    }

    public static class GrowTypeStrategyParameters extends StrategyParameters
    {
        @Override
        public Strategy getStrategy()
        {
            return Strategy.GROW_TYPE;
        }

        public GrowTypeStrategyParameters(int initialNodesNumber)
        {
            maximumNodesNumber = 2 * initialNodesNumber;
        }

        private int maximumNodesNumber;
        public int getMaximumNodesNumber()
        {
            return maximumNodesNumber;
        }

        public void setMaximumNodesNumber(int maximumNodesNumber)
        {
            this.maximumNodesNumber = maximumNodesNumber;
        }
    }
}
