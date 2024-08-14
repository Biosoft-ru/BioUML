package ru.biosoft.analysis.optimization.methods;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.apache.commons.lang.ArrayUtils;

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
 * MR Sierra and CA Coello Coello
 * Improving PSO-Based Multi-objective Optimization Using Crowding, Mutation and e-Dominance
 * (Evolutionary Multi-Criterion Optimization, 3410/2005: 505-519, Springer Berlin/Heidelberg)
 */
public class MOPSOOptMethod extends OptimizationMethod<MOPSOOptMethod.MOPSOOptMethodParameters>
{
    private static final double PERTURBATION_PARAMETER = 0.5;

    public MOPSOOptMethod(DataCollection<?> origin, String name)
    {
        super(origin, name, null);
        parameters = new MOPSOOptMethodParameters();
        individualsNumber = parameters.getParticleNumber();
    }

    @PropertyName("Method parameters")
    @PropertyDescription("Method parameters.")
    public class MOPSOOptMethodParameters extends OptimizationMethodParameters
    {
        private int numberOfIterations;
        private int particleNumber;

        public MOPSOOptMethodParameters()
        {
            this.numberOfIterations = 1000;
            this.particleNumber = 50;
            random = new Random();
        }

        @PropertyName("Number of iterations")
        @PropertyDescription("The number of iterations after which the optimization process will be stopped.")
        public int getNumberOfIterations()
        {
            return this.numberOfIterations;
        }
        public void setNumberOfIterations(int numOfIterations)
        {
            int oldValue = this.numberOfIterations;
            this.numberOfIterations = numOfIterations;
            firePropertyChange("numberOfIterations", oldValue, numOfIterations);
        }

        @PropertyName("Number of partices")
        @PropertyDescription("The number of solutions evaluated at each iteration of the optimization method.")
        public int getParticleNumber()
        {
            return this.particleNumber;
        }
        public void setParticleNumber(int particleNumber)
        {
            int oldValue = this.particleNumber;
            individualsNumber = this.particleNumber = particleNumber;
            firePropertyChange("particleNumber", oldValue, particleNumber);
        }

       
        @Override
        public void read(Properties properties, String prefix)
        {
            super.read(properties, prefix);
            try
            {
                numberOfIterations = Integer.parseInt(properties.getProperty(prefix + "numberOfIterations"));
            }
            catch( Exception e )
            {
            }
            try
            {
                particleNumber = Integer.parseInt(properties.getProperty(prefix + "particleNumber"));
            }
            catch( Exception e )
            {
            }
        }

        @Override
        public void write(Properties properties, String prefix)
        {
            super.write(properties, prefix);
            properties.put(prefix + "numberOfIterations", Integer.toString(numberOfIterations));
            properties.put(prefix + "particleNumber", Integer.toString(particleNumber));
        }
    }

    public static class MOPSOOptMethodParametersBeanInfo extends OptimizationMethodParametersBeanInfo
    {
        public MOPSOOptMethodParametersBeanInfo()
        {
            super(MOPSOOptMethodParameters.class);
        }

        @Override
        public void initProperties() throws Exception
        {
            super.initProperties();
            add( "numberOfIterations" );
            add( "particleNumber" );
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Optimization problem
    //
    private double[][] population;
    private double[][] velocities;
    private double probability;

    @Override
    public void setOptimizationProblem(OptimizationProblem problem)
    {
        super.setOptimizationProblem(problem);

        if( problem != null )
        {
            probability = (double)1 / n;
            population = new double[individualsNumber][n];
            velocities = new double[individualsNumber][n];
            stepsNumber = parameters.getNumberOfIterations();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Realization of the method
    //
    private SolutionArchive solutionArchive;

    @Override
    public double getDeviation()
    {
        return solutionArchive.get(0).getDistance();
    }

    @Override
    public double getPenalty()
    {
        return solutionArchive.get(0).getPenalty();
    }

    @Override
    public double[] getIntermediateSolution()
    {
        return solutionArchive.get(0).getValues().clone();
    }

    private List<Solution> bestParticles;

    @Override
    public double[] getSolution() throws IOException, Exception
    {
        Random random = getParameters().initRandom();
        initData(random);
        calculateDistances(population);
        refreshSolution();

        for( int t = 0; t < parameters.getNumberOfIterations() && go; ++t )
        {
            refreshBest();
            calculateNewVelocities(random);
            calculateNewPopulation();
            doMutations(t, random);
            calculateDistances(population);
            refreshSolution();
            displayInfo();
            incPreparedness(t + 1);
        }

        solutionArchive.sort(SolutionComparator.Status.PENALTY);
        return solutionArchive.get(0).getValues();
    }

    private void initData(Random random)
    {
        solutionArchive = new SolutionArchive();
        bestParticles = new ArrayList<>();

        for( int i = 0; i < individualsNumber; ++i )
        {
            for( int j = 0; j < n; ++j )
            {
                double lowerBound = params.get(j).getLowerBound();
                double upperBound = params.get(j).getUpperBound();

                if( i == 0 )
                    population[i][j] = params.get(j).getValue();
                else
                    population[i][j] = random.nextDouble() * ( upperBound - lowerBound ) + lowerBound;
                velocities[i][j] = 0.0;
            }
            bestParticles.add(new Solution(ArrayUtils.clone(population[i]), Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        }
    }

    private void calculateNewVelocities(Random random)
    {
        double r1, r2, w, c1, c2;
        double[] bestGlobal;

        for( int i = 0; i < individualsNumber && go; i++ )
        {
            bestGlobal = solutionArchive.getRandomSolution(random).getValues();

            //Parameters for velocities calculation
            r1 = random.nextDouble();
            r2 = random.nextDouble();

            c1 = 1 + 0.5 * random.nextDouble();
            c2 = 1 + 0.5 * random.nextDouble();

            w = 0.5 * ( 1 + random.nextDouble() );

            double[] best = bestParticles.get(i).getValues();
            for( int j = 0; j < n; j++ )
            {
                velocities[i][j] = w * velocities[i][j] + c1 * r1 * ( best[j] - population[i][j] ) + c2 * r2
                        * ( bestGlobal[j] - population[i][j] );
            }
        }
    }

    private void calculateNewPopulation()
    {
        for( int i = 0; i < individualsNumber && go; i++ )
            for( int j = 0; j < n; j++ )
            {
                double lowerBound = params.get(j).getLowerBound();
                double upperBound = params.get(j).getUpperBound();

                population[i][j] = population[i][j] + velocities[i][j];
                if( population[i][j] < lowerBound )
                {
                    population[i][j] = lowerBound;
                    velocities[i][j] = velocities[i][j] * -1.0;
                }
                if( population[i][j] > upperBound )
                {
                    population[i][j] = upperBound;
                    velocities[i][j] = velocities[i][j] * -1.0;
                }
            }
    }

    private void refreshBest()
    {
        for( int i = 0; i < individualsNumber && go; i++ )
        {
            Solution solution = new Solution(ArrayUtils.clone(population[i]), distances[i], penalties[i]);
            if( solution.dominates(bestParticles.get(i)) )
            {
                bestParticles.remove(i);
                bestParticles.add(i, solution);
            }
        }
    }

    private void refreshSolution()
    {
        for( int i = 0; i < individualsNumber && go; i++ )
        {
            Solution solution = new Solution(ArrayUtils.clone(population[i]), distances[i], penalties[i]);
            solutionArchive.refresh(solution);
        }
    }

    private void doMutations(int currentIteration, Random random)
    {
        for( int i = 0; i < individualsNumber && go; i++ )
        {
            if( i % 3 == 0 )
                doMutation(currentIteration, i, Mutation.UNIFORM, random);
            else if( i % 3 == 1 )
                doMutation(currentIteration, i, Mutation.NONUNIFORM, random);
        }
    }

    public enum Mutation
    {
        UNIFORM, NONUNIFORM;
    }

    private void doMutation(int currentIteration, int particleNumber, Mutation mutation, Random random)
    {
        int i = particleNumber;

        for( int j = 0; j < n && random.nextDouble() < probability; j++ )
        {
            double lowerBound = params.get(j).getLowerBound();
            double upperBound = params.get(j).getUpperBound();
            double alpha = random.nextDouble();
            double newValue = 0;

            switch( mutation )
            {
                case UNIFORM:
                {
                    newValue = ( alpha - 0.5 ) * PERTURBATION_PARAMETER + population[i][j];
                    break;
                }
                case NONUNIFORM:
                {
                    if( alpha <= 0.5 )
                    {
                        newValue = delta(upperBound - population[i][j], currentIteration, random);
                        newValue += population[i][j];
                    }
                    else
                    {
                        newValue = delta(lowerBound - population[i][j], currentIteration, random);
                        newValue += population[i][j];
                    }
                    break;
                }
            }

            if( newValue < lowerBound )
                newValue = lowerBound;
            else if( newValue > upperBound )
                newValue = upperBound;

            population[i][j] = newValue;
        }
    }

    private double delta(double bound, int currentIteration, Random random)
    {
        double rand = random.nextDouble();
        return ( bound * ( 1.0 - Math.pow(rand,
                Math.pow( ( 1.0 - (double) currentIteration / parameters.getNumberOfIterations() ),
                        PERTURBATION_PARAMETER)) ) );
    }
}