package ru.biosoft.analysis.optimization.methods;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.analysis.optimization.OptimizationMethod;
import ru.biosoft.analysis.optimization.OptimizationMethodParameters;
import ru.biosoft.analysis.optimization.OptimizationMethodParametersBeanInfo;
import ru.biosoft.analysis.optimization.OptimizationProblem;
import ru.biosoft.analysis.optimization.methods.solution.Solution;
import ru.biosoft.analysis.optimization.methods.solution.SolutionArchive;
import ru.biosoft.analysis.optimization.methods.solution.SolutionComparator;

/*
 * This algorithm was created based on the paper
 * AJ Nebro, JJ Durillo, F Luna, B Dorronsoro and E Alba
 * A Cellular Genetic Algorithm for Multiobjective Optimization.
 * (International Journal of Intelligent Systems,  24(7): 726-746, 2009)
 */
public class MOCellOptMethod extends OptimizationMethod<MOCellOptMethod.MOCellOptMethodParameters>
{
    private static final int FEEDBACK = 10;

    private static final double ACCURACY = 1.0e-14;

    private static final double P_c = 0.9; //crossover probability
    private double P_m; //mutation probability

    //distribution indexes
    private static final double ETA_c = 20;
    private static final double ETA_m = 20;

    public MOCellOptMethod(DataCollection<?> origin, String name)
    {
        super(origin, name, null);
        parameters = new MOCellOptMethodParameters();
    }

    @PropertyName("Method parameters")
    @PropertyDescription("Method parameters.")
    public class MOCellOptMethodParameters extends OptimizationMethodParameters
    {
        private int maxIterations;
        private int gridLength;
        private int gridWidth;

        public MOCellOptMethodParameters()
        {
            this.maxIterations = 500;
            this.gridLength = 5;
            this.gridWidth = 4;
            individualsNumber = gridLength * gridWidth;
            random = new Random();
        }

        @PropertyName ( "Iterations limit" )
        @PropertyDescription ( "The number of iterations after which the optimization process will be stopped." )
        public int getMaxIterations()
        {
            return this.maxIterations;
        }
        public void setMaxIterations(int maxIterations)
        {
            int oldValue = this.maxIterations;
            this.maxIterations = maxIterations;
            firePropertyChange("maxIterations", oldValue, maxIterations);
        }

        @PropertyName ( "Grid length" )
        @PropertyDescription ( "The number of solutions evaluated at each iteration of the optimization method is defined as the grid length multiplied by the grid width." )
        public int getGridLength()
        {
            return gridLength;
        }
        public void setGridLength(int gridLength)
        {
            int oldValue = this.gridLength;
            this.gridLength = gridLength;
            individualsNumber = gridLength * gridWidth;
            firePropertyChange("gridLength", oldValue, gridLength);
        }

        @PropertyName ( "Grid width" )
        @PropertyDescription ( "The number of solutions evaluated at each iteration of the optimization method is defined as the grid length multiplied by the grid width." )
        public int getGridWidth()
        {
            return gridWidth;
        }
        public void setGridWidth(int gridWidth)
        {
            int oldValue = this.gridWidth;
            this.gridWidth = gridWidth;
            individualsNumber = gridLength * gridWidth;
            firePropertyChange("gridWidth", oldValue, gridWidth);
        }

        @Override
        public void read(Properties properties, String prefix)
        {
            super.read(properties, prefix);
            try
            {
                maxIterations = Integer.parseInt(properties.getProperty(prefix + "maxIterations"));
            }
            catch( Exception e )
            {
            }
            try
            {
                gridLength = Integer.parseInt(properties.getProperty(prefix + "gridLength"));
            }
            catch( Exception e )
            {
            }
            try
            {
                gridWidth = Integer.parseInt(properties.getProperty(prefix + "gtidWidth"));
            }
            catch( Exception e )
            {
            }
        }

        @Override
        public void write(Properties properties, String prefix)
        {
            super.write(properties, prefix);
            properties.put(prefix + "maxIterations", Integer.toString(maxIterations));
            properties.put(prefix + "gridLength", Integer.toString(gridLength));
            properties.put(prefix + "gridWidth", Integer.toString(gridWidth));
        }
    }

    public static class MOCellOptMethodParametersBeanInfo extends OptimizationMethodParametersBeanInfo
    {
        public MOCellOptMethodParametersBeanInfo()
        {
            super(MOCellOptMethodParameters.class);
        }

        @Override
        public void initProperties() throws Exception
        {
            super.initProperties();
            add("maxIterations");
            add("gridLength");
            add("gridWidth");
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Optimization problem
    //
    private double[][] population;

    @Override
    public void setOptimizationProblem(OptimizationProblem problem)
    {
        super.setOptimizationProblem(problem);

        if( problem != null )
        {
            population = new double[individualsNumber][n];
            stepsNumber = parameters.getMaxIterations();
            P_m = 1.0 / n;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Realization of the method
    //

    @Override
    public double getDeviation()
    {
        return bestSolutions.get(0).getDistance();
    }

    @Override
    public double getPenalty()
    {
        return bestSolutions.get(0).getPenalty();
    }

    @Override
    public double[] getIntermediateSolution()
    {
        return bestSolutions.get(0).getValues().clone();
    }

    private SolutionArchive bestSolutions = new SolutionArchive();

    @Override
    public double[] getSolution() throws Exception
    {
        Random random = getParameters().initRandom();
        ArrayList<Solution> solutions = initData(random);
        calculateDistances(population);

        for( int i = 0; i < individualsNumber; i++ )
        {
            solutions.add(new Solution(population[i], distances[i], penalties[i]));
            bestSolutions.refresh(solutions.get(i));
        }

        int currIteration = 0;
        while( currIteration < parameters.getMaxIterations() && go )
        {
            for( int i = 0; i < individualsNumber && go; i++ )
            {
                SolutionArchive neighbors = getNeighbors(solutions, i);

                Solution neighbor1 = neighbors.getRandomSolution(random);
                Solution neighbor2 = neighbors.getRandomSolution(random);

                double[] solution = doCrossover(neighbor1, neighbor2, random);
                doMutation(solution, random);

                population[i] = solution;
            }

            calculateDistances(population);

            ArrayList<Solution> newSolutions = new ArrayList<>(individualsNumber);
            for( int i = 0; i < individualsNumber && go; i++ )
            {
                Solution oldSolution = solutions.get(i);
                Solution newSolution = new Solution(population[i], distances[i], penalties[i]);
                newSolutions.add(getDominatingSolution(oldSolution, newSolution, getNeighbors(solutions, i)));
            }

            bestSolutions.sort(SolutionComparator.Status.PENALTY);
            for( int j = 0; j < FEEDBACK && go; j++ )
            {
                if( bestSolutions.size() > j )
                {
                    int r = 0;
                    if( individualsNumber > 1 )
                        r = random.nextInt(individualsNumber - 1);

                    newSolutions.remove(r);
                    newSolutions.add(bestSolutions.get(j));
                }
            }

            solutions = newSolutions;
            displayInfo();
            incPreparedness(++currIteration);
        }

        return bestSolutions.get(0).getValues();
    }

    private Solution getDominatingSolution(Solution oldSolution, Solution newSolution, SolutionArchive neighbors)
    {
        if( Double.isNaN(newSolution.getDistance()) || Double.isNaN(newSolution.getPenalty()) || oldSolution.dominates(newSolution) )
            return oldSolution;

        bestSolutions.refresh(newSolution);
        if( newSolution.dominates(oldSolution) )
            return newSolution;

        neighbors.add(newSolution);
        SolutionArchive comparable = getComparableNeighbors(neighbors, neighbors.size() - 1);
        comparable.distanceDistributionCrowding();

        if( oldSolution.getCrowdingDistance() < newSolution.getCrowdingDistance() )
            return newSolution;

        return oldSolution;
    }

    private SolutionArchive getNeighbors(ArrayList<Solution> solutionArray, int index)
    {
        SolutionArchive neighbors = new SolutionArchive(10);
        for( int i = 0; i < 8; ++i )
            neighbors.add(solutionArray.get(neighborhood[index][i]));
        neighbors.add(solutionArray.get(index));
        return neighbors;
    }

    /**
     * The method of the Simulated Binary Crossover (SBX) was proposed by Deb et al. for the NSGA-II algorithm,
     * which implementation is available at http://www.iitk.ac.in/kangal/codes.shtml
     */
    private double[] doCrossover(Solution neighbor1, Solution neighbor2, Random random)
    {
        double[] solution = new double[n];

        if( random.nextDouble() <= P_c )
        {
            for( int i = 0; i < n; i++ )
            {
                double p1 = neighbor1.getValues()[i];
                double p2 = neighbor2.getValues()[i];

                if( Math.abs(p1 - p2) > ACCURACY )
                {
                    double p_min = Math.min(p1, p2);
                    double p_max = Math.max(p1, p2);

                    double lowerBound = params.get(i).getLowerBound();
                    double upperBound = params.get(i).getUpperBound();

                    double rand = random.nextDouble();

                    double beta = 1.0 + ( 2.0 * ( p_min - lowerBound ) / ( p_max - p_min ) );
                    double alpha = 2.0 - Math.pow(beta, - ( ETA_c + 1.0 ));

                    if( rand <= ( 1.0 / alpha ) )
                        beta = Math.pow( ( rand * alpha ), ( 1.0 / ( ETA_c + 1.0 ) ));
                    else
                        beta = Math.pow( ( 1.0 / ( 2.0 - rand * alpha ) ), ( 1.0 / ( ETA_c + 1.0 ) ));

                    double c1 = 0.5 * ( ( p_min + p_max ) - beta * ( p_max - p_min ) );
                    c1 = checkValue(c1, lowerBound, upperBound);

                    beta = 1.0 + ( 2.0 * ( upperBound - p_max ) / ( p_max - p_min ) );
                    alpha = 2.0 - Math.pow(beta, - ( ETA_c + 1.0 ));

                    if( rand <= ( 1.0 / alpha ) )
                        beta = Math.pow( ( rand * alpha ), ( 1.0 / ( ETA_c + 1.0 ) ));
                    else
                        beta = Math.pow( ( 1.0 / ( 2.0 - rand * alpha ) ), ( 1.0 / ( ETA_c + 1.0 ) ));

                    double c2 = 0.5 * ( ( p_min + p_max ) + beta * ( p_max - p_min ) );
                    c2 = checkValue(c2, lowerBound, upperBound);

                    if( random.nextDouble() <= 0.5 )
                    {
                        if( !Double.isNaN(c2) )
                            solution[i] = c2;
                    }
                    else
                    {
                        if( !Double.isNaN(c1) )
                            solution[i] = c1;
                    }
                }
                else
                {
                    solution[i] = p1;
                }
            }
        }
        else
        {
            if( random.nextDouble() <= 0.5 )
            {
                for( int i = 0; i < n; ++i )
                    solution[i] = neighbor1.getValues()[i];
            }
            else
            {
                for( int i = 0; i < n; ++i )
                    solution[i] = neighbor2.getValues()[i];
            }
        }
        return solution;
    }

    /**
     * The method was proposed by Deb et al. for the NSGA-II algorithm,
     * which implementation is available at http://www.iitk.ac.in/kangal/codes.shtml
     */
    private void doMutation(double[] solution, Random random)
    {
        for( int j = 0; j < n; j++ )
        {
            if( random.nextDouble() <= P_m )
            {
                double value = solution[j];

                double lowerBound = params.get(j).getLowerBound();
                double upperBound = params.get(j).getUpperBound();

                double delta1 = 1 - ( value - lowerBound ) / ( upperBound - lowerBound );
                double delta2 = 1 - ( upperBound - value ) / ( upperBound - lowerBound );

                double rnd = random.nextDouble();

                double delta;
                double val;
                if( rnd <= 0.5 )
                {
                    val = 2.0 * rnd + ( 1.0 - 2.0 * rnd ) * ( Math.pow(delta1, ( ETA_m + 1.0 )) );
                    delta = Math.pow(val, 1.0 / ( ETA_m + 1.0 )) - 1.0;
                }
                else
                {
                    val = 2.0 * ( 1.0 - rnd ) + 2.0 * ( rnd - 0.5 ) * ( Math.pow(delta2, ( ETA_m + 1.0 )) );
                    delta = 1.0 - ( Math.pow(val, 1.0 / ( ETA_m + 1.0 )) );
                }

                value = value + delta * ( upperBound - lowerBound );
                value = checkValue(value + delta * ( upperBound - lowerBound ), lowerBound, upperBound);

                if( !Double.isNaN(value) )
                    solution[j] = value;
            }
        }
        return;
    }

    private double checkValue(double value, double lowerBound, double upperBound)
    {
        if( value < lowerBound )
            return lowerBound;
        else if( value > upperBound )
            return upperBound;
        else
            return value;
    }

    private SolutionArchive getComparableNeighbors(SolutionArchive neighbors, int sourceIndex)
    {
        int size = neighbors.size();

        List<Integer> comparable = new ArrayList<>();
        List<Integer>[] better = new List[size];

        for( int i = 0; i < size; i++ )
        {
            better[i] = new ArrayList<>();

            for( int j = 0; j < size; j++ )
                if( neighbors.get(j).dominates(neighbors.get(i)) )
                    better[i].add(j);

            if( better[i].size() == 0 )
                comparable.add(i);
        }

        while( !comparable.contains(sourceIndex) )
        {
            List<Integer> nextComparable = new ArrayList<>();
            for( int i = 0; i < better.length; ++i )
            {
                if( better[i].size() != 0 )
                {
                    for( int j = better[i].size() - 1; j >= 0; --j )
                    {
                        Integer index = better[i].get(j);
                        if( comparable.contains(index) )
                            better[i].remove(j);
                    }

                    if( better[i].size() == 0 )
                        nextComparable.add(i);
                }
            }
            comparable = nextComparable;
        }
        SolutionArchive result = new SolutionArchive(comparable.size());
        for( int j : comparable )
            result.add(neighbors.get(j));
        return result;
    }

    private ArrayList<Solution> initData(Random random) throws Exception
    {
        bestSolutions = new SolutionArchive();
        ArrayList<Solution> solutions = new ArrayList<>(individualsNumber);

        for( int i = 0; i < individualsNumber; i++ )
        {
            double[] solution = new double[n];

            for( int j = 0; j < n; ++j )
            {
                double upperBound = params.get(j).getUpperBound();
                double lowerBound = params.get(j).getLowerBound();
                if( i == 0 )
                    solution[j] = params.get(j).getValue();
                else
                    solution[j] = random.nextDouble() * ( upperBound - lowerBound ) + lowerBound;
            }

            population[i] = solution;
        }

        neighborhood = initNeighborhood();
        return solutions;
    }

    private int[][] neighborhood;

    /**
     * For each individual I defines its neighbors 1-8:
     * 
     * o o o o o
     * o 1 2 3 o
     * o 8 I 4 o
     * o 7 6 5 o
     * o o o o o
     */
    private int[][] initNeighborhood()
    {
        int[][] neighborhood = new int[individualsNumber][8];

        int w = parameters.getGridWidth();

        int[] ind_i = new int[] { -1, -1, -1, 0, 1, 1, 1, 0};
        int[] ind_j = new int[] { -1, 0, 1, 1, 1, 0, -1, -1};

        for( int i = 0; i < parameters.getGridLength(); ++i )
        {
            for( int j = 0; j < w; ++j )
            {
                for( int k = 0; k < 8; ++k )
                    neighborhood[j + i * w][k] = trunc_j(j + ind_j[k]) + trunc_i(i + ind_i[k]) * w;
            }
        }
        return neighborhood;
    }

    private int trunc_j(int j)
    {
        if( j == parameters.getGridWidth() )
            return 0;
        if( j == -1 )
            return parameters.getGridWidth() - 1;
        return j;
    }

    private int trunc_i(int i)
    {
        if( i == parameters.getGridLength() )
            return 0;
        if( i == -1 )
            return parameters.getGridLength() - 1;
        return i;
    }
}